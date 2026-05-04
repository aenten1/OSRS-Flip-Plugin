/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 */
package net.runelite.client.plugins.osrsflip;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Tracks historical price data for items (last 5 high/low prices).
 */
public class HistoricalPriceTracker
{
private static final int MAX_HISTORY_SIZE = 5;

private final Map<Integer, List<PricePoint>> history;

public HistoricalPriceTracker()
{
this.history = new ConcurrentHashMap<>();
}

public void addPricePoint(int itemId, int highPrice, int lowPrice)
{
history.computeIfAbsent(itemId, k -> new ArrayList<>());
List<PricePoint> points = history.get(itemId);

points.add(new PricePoint(highPrice, lowPrice));

if (points.size() > MAX_HISTORY_SIZE)
{
points.remove(0);
}
}

public List<PricePoint> getHistory(int itemId)
{
return new ArrayList<>(history.getOrDefault(itemId, new ArrayList<>()));
}

public double getAverageHigh(int itemId)
{
List<PricePoint> points = getHistory(itemId);
if (points.isEmpty())
{
return 0;
}

return points.stream().mapToInt(p -> p.highPrice).average().orElse(0);
}

public double getAverageLow(int itemId)
{
List<PricePoint> points = getHistory(itemId);
if (points.isEmpty())
{
return 0;
}

return points.stream().mapToInt(p -> p.lowPrice).average().orElse(0);
}

public void clearHistory(int itemId)
{
history.remove(itemId);
}

public int getHistorySize(int itemId)
{
return history.getOrDefault(itemId, new ArrayList<>()).size();
}

@Getter
public static class PricePoint
{
private final int highPrice;
private final int lowPrice;

public PricePoint(int highPrice, int lowPrice)
{
this.highPrice = highPrice;
this.lowPrice = lowPrice;
}
}
}
