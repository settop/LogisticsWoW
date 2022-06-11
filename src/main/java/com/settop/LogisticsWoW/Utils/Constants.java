package com.settop.LogisticsWoW.Utils;

import java.util.Random;

//ToDo Setup config for these variables
public class Constants
{
    private static final Random rng = new Random();

    //ToDo: Add mod filter and a filter that matches the connected inventory
    public enum eFilterType
    {
        Item,
        Tag,
        Default
    }

    //5 seconds
    public static final int SLEEP_TICK_TIMER = 100;
    public static final int BASE_EXTRACTION_TICK_DELAY = 32;
    public static final int BASE_EXTRACTION_AMOUNT = 16;

    public static int GetInitialSleepTimer()
    {
        return rng.nextInt(SLEEP_TICK_TIMER);
    }

    public static int GetExtractionTickDelay(int extractionSpeedRank)
    {
        if(extractionSpeedRank <= 0)
        {
            return BASE_EXTRACTION_TICK_DELAY;
        }
        return Math.max(BASE_EXTRACTION_TICK_DELAY >> extractionSpeedRank, 1);
    }

    public static int GetExtractionOperationsPerTick(int extractionSpeedRank)
    {
        if(extractionSpeedRank <= 0)
        {
            return 1;
        }
        return Math.max((1 << extractionSpeedRank) / BASE_EXTRACTION_TICK_DELAY, 1);
    }

    public static int GetExtractionCountPerTick(int extractionAmountRank)
    {
        if(extractionAmountRank <= 0)
        {
            return BASE_EXTRACTION_AMOUNT;
        }
        return BASE_EXTRACTION_AMOUNT << extractionAmountRank;
    }
}
