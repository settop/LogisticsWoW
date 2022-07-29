package com.settop.LogisticsWoW.Utils;

import java.util.Random;

//ToDo Setup config for these variables
public class Constants
{
    private static final Random rng = new Random();

    //ToDo: Add mod filter and a filter that matches the connected inventory
    public enum eFilterType
    {
        Type,
        Tag,
        Default
    }

    //5 seconds
    public static final int SLEEP_TICK_TIMER = 100;
    public static final int BASE_EXTRACTION_TICK_DELAY = 32;
    public static final int BASE_CARRY_AMOUNT = 16;
    public static final float BASE_CARRY_SPEED = 0.8f;

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

    public static int GetCarryCount(int carryAmountRank)
    {
        if(carryAmountRank <= 0)
        {
            return BASE_CARRY_AMOUNT;
        }
        return BASE_CARRY_AMOUNT << carryAmountRank;
    }

    public static float GetCarrySpeed(int carrySpeedRank)
    {
        if(carrySpeedRank <= 0)
        {
            return BASE_CARRY_SPEED;
        }
        return BASE_CARRY_SPEED * (1 << carrySpeedRank);
    }
}
