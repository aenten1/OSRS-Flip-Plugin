/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 */
package net.runelite.client.plugins.osrsflip;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
public class WikiPriceManager
{
	private static final String BASE_URL = "https://prices.runescape.wiki/api/v1/osrs";
	private static final String USER_AGENT = "osrs-flip-plugin";

	private final OkHttpClient okHttpClient;
	private final Gson gson;

	private final Map<Integer, LatestPrice> latestCache = new ConcurrentHashMap<>();
	private final Map<Integer, HourlyPrice> hourlyCache = new ConcurrentHashMap<>();

	public WikiPriceManager(OkHttpClient okHttpClient, Gson gson)
	{
		this.okHttpClient = okHttpClient;
		this.gson = gson;
	}

	/**
	 * Fetches latest + hourly prices for the given item IDs, then runs onComplete.
	 */
	public void fetchPrices(List<Integer> itemIds, Runnable onComplete)
	{
		if (itemIds.isEmpty())
		{
			onComplete.run();
			return;
		}

		String idParam = itemIds.stream()
			.map(String::valueOf)
			.collect(Collectors.joining("|"));

		AtomicInteger pending = new AtomicInteger(2);
		Runnable checkDone = () ->
		{
			if (pending.decrementAndGet() == 0)
			{
				onComplete.run();
			}
		};

		fetchLatest(idParam, checkDone);
		fetchHourly(idParam, checkDone);
	}

	private void fetchLatest(String idParam, Runnable onDone)
	{
		HttpUrl url = HttpUrl.parse(BASE_URL + "/latest").newBuilder()
			.addQueryParameter("id", idParam)
			.build();

		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();

		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Failed to fetch latest prices: {}", e.getMessage());
				onDone.run();
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response r = response)
				{
					if (!r.isSuccessful() || r.body() == null)
					{
						log.debug("Wiki latest API returned {}", r.code());
						return;
					}

					String body = r.body().string();
					JsonObject root = gson.fromJson(body, JsonObject.class);
					if (root == null)
					{
						return;
					}
					JsonObject data = root.getAsJsonObject("data");
					if (data == null)
					{
						return;
					}

					for (Map.Entry<String, JsonElement> entry : data.entrySet())
					{
						try
						{
							int itemId = Integer.parseInt(entry.getKey());
							JsonObject item = entry.getValue().getAsJsonObject();

							LatestPrice price = new LatestPrice();
							price.high = getIntOrNull(item, "high");
							price.low = getIntOrNull(item, "low");
							price.highTime = getLongOrZero(item, "highTime");
							price.lowTime = getLongOrZero(item, "lowTime");

							latestCache.put(itemId, price);
						}
						catch (Exception e)
						{
							log.debug("Failed to parse latest price for {}", entry.getKey());
						}
					}
				}
				catch (Exception e)
				{
					log.debug("Failed to parse latest response: {}", e.getMessage());
				}
				finally
				{
					onDone.run();
				}
			}
		});
	}

	private void fetchHourly(String idParam, Runnable onDone)
	{
		HttpUrl url = HttpUrl.parse(BASE_URL + "/1h").newBuilder()
			.addQueryParameter("id", idParam)
			.build();

		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();

		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Failed to fetch hourly prices: {}", e.getMessage());
				onDone.run();
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response r = response)
				{
					if (!r.isSuccessful() || r.body() == null)
					{
						log.debug("Wiki 1h API returned {}", r.code());
						return;
					}

					String body = r.body().string();
					JsonObject root = gson.fromJson(body, JsonObject.class);
					if (root == null)
					{
						return;
					}
					JsonObject data = root.getAsJsonObject("data");
					if (data == null)
					{
						return;
					}

					for (Map.Entry<String, JsonElement> entry : data.entrySet())
					{
						try
						{
							int itemId = Integer.parseInt(entry.getKey());
							JsonObject item = entry.getValue().getAsJsonObject();

							HourlyPrice price = new HourlyPrice();
							price.avgHighPrice = getIntOrNull(item, "avgHighPrice");
							price.avgLowPrice = getIntOrNull(item, "avgLowPrice");
							price.highPriceVolume = getIntOrZero(item, "highPriceVolume");
							price.lowPriceVolume = getIntOrZero(item, "lowPriceVolume");

							hourlyCache.put(itemId, price);
						}
						catch (Exception e)
						{
							log.debug("Failed to parse hourly price for {}", entry.getKey());
						}
					}
				}
				catch (Exception e)
				{
					log.debug("Failed to parse hourly response: {}", e.getMessage());
				}
				finally
				{
					onDone.run();
				}
			}
		});
	}

	/**
	 * Gets the effective buy price for an item, applying volatility check and offsets.
	 */
	public int getBuyPrice(int itemId, OsrsFlipConfig config)
	{
		Integer price = getRawPrice(itemId, true, config);
		if (price == null)
		{
			return -1;
		}
		return applyOffset(price, config.buyOffsetGp(), config.buyOffsetPercent());
	}

	/**
	 * Gets the effective sell price for an item, applying volatility check and offsets.
	 */
	public int getSellPrice(int itemId, OsrsFlipConfig config)
	{
		Integer price = getRawPrice(itemId, false, config);
		if (price == null)
		{
			return -1;
		}
		return applyOffset(price, config.sellOffsetGp(), config.sellOffsetPercent());
	}

	/**
	 * Gets the raw price before offsets, applying volatility logic.
	 */
	private Integer getRawPrice(int itemId, boolean isBuy, OsrsFlipConfig config)
	{
		LatestPrice latest = latestCache.get(itemId);
		HourlyPrice hourly = hourlyCache.get(itemId);

		// Determine which field to use based on price mode
		Integer latestValue = getLatestValue(latest, isBuy, config.priceMode());
		Integer hourlyValue = getHourlyValue(hourly, isBuy, config.priceMode());

		if (latestValue == null && hourlyValue == null)
		{
			return null;
		}

		if (latestValue == null)
		{
			return hourlyValue;
		}

		if (hourlyValue == null || hourlyValue == 0)
		{
			return latestValue;
		}

		// Volatility check: if latest deviates from hourly by more than threshold, use hourly
		double deviation = Math.abs((double) (latestValue - hourlyValue) / hourlyValue) * 100;
		if (deviation > config.volatilityThreshold())
		{
			log.debug("Item {} volatile ({}% > {}%), using hourly avg", itemId, (int) deviation, config.volatilityThreshold());
			return hourlyValue;
		}

		return latestValue;
	}

	private Integer getLatestValue(LatestPrice price, boolean isBuy, PriceMode mode)
	{
		if (price == null)
		{
			return null;
		}

		switch (mode)
		{
			case BUY_LOW_SELL_HIGH:
				return isBuy ? price.low : price.high;
			case BUY_HIGH_SELL_LOW:
				return isBuy ? price.high : price.low;
			case BUY_HIGH_SELL_HIGH:
				return price.high;
			default:
				return isBuy ? price.low : price.high;
		}
	}

	private Integer getHourlyValue(HourlyPrice price, boolean isBuy, PriceMode mode)
	{
		if (price == null)
		{
			return null;
		}

		switch (mode)
		{
			case BUY_LOW_SELL_HIGH:
				return isBuy ? price.avgLowPrice : price.avgHighPrice;
			case BUY_HIGH_SELL_LOW:
				return isBuy ? price.avgHighPrice : price.avgLowPrice;
			case BUY_HIGH_SELL_HIGH:
				return price.avgHighPrice;
			default:
				return isBuy ? price.avgLowPrice : price.avgHighPrice;
		}
	}

	private int applyOffset(int price, int offsetGp, double offsetPercent)
	{
		double result = price + offsetGp;
		if (offsetPercent != 0.0)
		{
			result += price * (offsetPercent / 100.0);
		}
		return Math.max(0, (int) Math.round(result));
	}

	public boolean hasPrice(int itemId)
	{
		return latestCache.containsKey(itemId) || hourlyCache.containsKey(itemId);
	}

	private Integer getIntOrNull(JsonObject obj, String key)
	{
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull())
		{
			return null;
		}
		return el.getAsInt();
	}

	private int getIntOrZero(JsonObject obj, String key)
	{
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull())
		{
			return 0;
		}
		return el.getAsInt();
	}

	private long getLongOrZero(JsonObject obj, String key)
	{
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull())
		{
			return 0;
		}
		return el.getAsLong();
	}

	static class LatestPrice
	{
		Integer high;
		Integer low;
		long highTime;
		long lowTime;
	}

	static class HourlyPrice
	{
		Integer avgHighPrice;
		Integer avgLowPrice;
		int highPriceVolume;
		int lowPriceVolume;
	}
}
