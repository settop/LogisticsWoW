package com.settop.LogisticsWoW.Wisps.Enhancements;

import com.settop.LogisticsWoW.Utils.Constants;
import com.settop.LogisticsWoW.WispNetwork.Tasks.WispTask;
import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;

public abstract class ExtractionEnhancement implements IEnhancement
{
    private class PeriodicTask implements WispTask
    {
        private static final int SLEEP_TICK_MOD_AMOUNT = 5;

        private boolean active = true;
        private int sequentialSleeps = 0;
        private int sequentialShortSleeps = 0;
        private int sequentialLongSleeps = 0;
        private int sleepTime = Constants.SLEEP_TICK_TIMER;
        @Override
        public int Start(@NotNull WispNetwork network, int startTickTime)
        {
            //randomise a bit to prevent them all from ticking at the same time after a world load
            return startTickTime + Constants.GetInitialSleepTimer();
        }

        @Override
        public OptionalInt Tick(@NotNull TickEvent.ServerTickEvent tickEvent, @NotNull WispNetwork network, int currentTickTime, int tickOffset)
        {
            if(!active)
            {
                return OptionalInt.empty();
            }
            //check to see if we have anything not in our filter
            extractionState = eExtractionState.ASLEEP;
            for(int i = 0; i < Constants.GetExtractionOperationsPerTick(extractionSpeedRank); ++i)
            {
                eExtractionState nextState = TickExtraction(currentTickTime);
                if(nextState == eExtractionState.ASLEEP)
                {
                    break;
                }
                else
                {
                    extractionState = eExtractionState.values()[Math.max(extractionState.ordinal(), nextState.ordinal())];
                }
            }
            if(extractionState == eExtractionState.ACTIVE)
            {
                if(sequentialSleeps == 1)
                {
                    ++sequentialShortSleeps;
                    sequentialLongSleeps = 0;
                    if(sequentialShortSleeps >= 4)
                    {
                        //4 times in a row we have done a sleep, then extraction, then sleep
                        //then shorten the sleep timer
                        sequentialShortSleeps = 0;
                        int tickDelay = Constants.GetExtractionTickDelay(extractionSpeedRank);
                        if(sleepTime > tickDelay)
                        {
                            sleepTime = Math.max(tickDelay, sleepTime - SLEEP_TICK_MOD_AMOUNT);
                        }
                    }
                }
                sequentialSleeps = 0;
                return OptionalInt.of(currentTickTime + tickOffset + Constants.GetExtractionTickDelay(extractionSpeedRank));
            }
            //Ignore the tick offset whilst sleeping, don't care about missing some ticks whilst asleep
            if(sequentialSleeps >= 4)
            {
                ++sequentialLongSleeps;
                sequentialShortSleeps = 0;
                if(sequentialLongSleeps >= 2)
                {
                    if(sleepTime < Constants.SLEEP_TICK_TIMER)
                    {
                        sleepTime = Math.min(Constants.SLEEP_TICK_TIMER, sleepTime + SLEEP_TICK_MOD_AMOUNT);
                    }
                }
            }
            ++sequentialSleeps;
            return OptionalInt.of(currentTickTime + sleepTime);
        }
    }

    //data player can tweak
    private Direction invAccessDirection = null;

    //operation data
    private int extractionSpeedRank = 0;
    private WispInteractionNodeBase parentWisp;

    //running data
    protected enum eExtractionState
    {
        ASLEEP,
        NO_DESTINATION,
        ACTIVE
    }
    private PeriodicTask currentTask;
    private eExtractionState extractionState = eExtractionState.ASLEEP;

    protected abstract eExtractionState TickExtraction(int currentTick);

    @Override
    public CompoundTag SerializeNBT()
    {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("extractionSpeedRank", extractionSpeedRank);
        if(invAccessDirection != null)
        {
            nbt.putInt("invAccessDirection", invAccessDirection.get3DDataValue());
        }

        return nbt;
    }

    @Override
    public void DeserializeNBT(CompoundTag nbt)
    {
        if (nbt == null)
        {
            return;
        }
        if (nbt.contains("extractionSpeedRank"))
        {
            extractionSpeedRank = nbt.getInt("extractionSpeedRank");
        }
        if (nbt.contains("invAccessDirection"))
        {
            invAccessDirection = Direction.from3DDataValue(nbt.getInt("invAccessDirection"));
        }
    }

    @Override
    public void Setup(WispInteractionNodeBase parentWisp)
    {
        this.parentWisp = parentWisp;
    }

    @Override
    public void OnConnectToNetwork()
    {
        assert parentWisp != null;
        if(currentTask == null)
        {
            currentTask = new PeriodicTask();
            parentWisp.connectedNetwork.StartTask(currentTask);
        }
    }

    @Override
    public void OnDisconnectFromNetwork()
    {
        if(currentTask != null)
        {
            currentTask.active = false;
            currentTask = null;
        }
    }

    @Override
    public WispInteractionNodeBase GetAttachedInteractionNode()
    {
        return parentWisp;
    }

    public Direction GetInvAccessDirection() { return invAccessDirection; }
    public void SetInvAccessDirection(Direction invAccessDirection) { this.invAccessDirection = invAccessDirection; }
}
