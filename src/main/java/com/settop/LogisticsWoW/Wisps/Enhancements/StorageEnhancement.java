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

public abstract class StorageEnhancement implements IEnhancement
{
    private class PeriodicTask implements WispTask
    {
        private boolean active = true;
        private boolean doneFirstTick = false;
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
            if(RefreshResources() || extractionState != ItemStorageEnhancement.eExtractionState.ASLEEP || !doneFirstTick)
            {
                doneFirstTick = true;
                //check to see if we have anything not in our filter
                extractionState = ItemStorageEnhancement.eExtractionState.ASLEEP;
                for(int i = 0; i < Constants.GetExtractionOperationsPerTick(extractionSpeedRank); ++i)
                {
                    ItemStorageEnhancement.eExtractionState nextState = TickExtraction();
                    if(nextState == ItemStorageEnhancement.eExtractionState.ASLEEP)
                    {
                        break;
                    }
                    else
                    {
                        extractionState = ItemStorageEnhancement.eExtractionState.values()[Math.max(extractionState.ordinal(), nextState.ordinal())];
                    }
                }
                if(extractionState == ItemStorageEnhancement.eExtractionState.ACTIVE)
                {
                    return OptionalInt.of(currentTickTime + tickOffset + Constants.GetExtractionTickDelay(extractionSpeedRank));
                }
                else
                {
                    //Ignore the tick offset whilst sleeping, don't care about missing some ticks whilst asleep
                    return OptionalInt.of(currentTickTime + Constants.SLEEP_TICK_TIMER);
                }
            }
            else
            {
                //Ignore the tick offset whilst sleeping, don't care about missing some ticks whilst asleep
                return OptionalInt.of(currentTickTime + Constants.SLEEP_TICK_TIMER);
            }
        }
    }

    //data player can tweak
    private int priority = 0;
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

    public abstract boolean RefreshResources();
    protected abstract eExtractionState TickExtraction();

    @Override
    public CompoundTag SerializeNBT()
    {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt("priority", priority);
        nbt.putInt("extractionSpeedRank", extractionSpeedRank);

        return nbt;
    }

    @Override
    public void DeserializeNBT(CompoundTag nbt)
    {
        if (nbt == null)
        {
            return;
        }
        if (nbt.contains("priority"))
        {
            priority = nbt.getInt("priority");
        }
        if (nbt.contains("extractionSpeedRank"))
        {
            extractionSpeedRank = nbt.getInt("extractionSpeedRank");
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

    public int GetPriority() { return priority; }
    public void SetPriority(int prio) { priority = prio; }

    public Direction GetInvAccessDirection() { return invAccessDirection; }
    public void SetInvAccessDirection(Direction invAccessDirection) { this.invAccessDirection = invAccessDirection; }
}
