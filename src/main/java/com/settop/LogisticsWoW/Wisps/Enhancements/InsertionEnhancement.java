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

public abstract class InsertionEnhancement implements IEnhancement
{
    //ToDO: Add crafting support when crafting is added

    private class PeriodicTask implements WispTask
    {
        boolean active = true;

        @Override
        public int Start(@NotNull WispNetwork network, int startTickTime)
        {
            return startTickTime;
        }

        @Override
        public OptionalInt Tick(@NotNull TickEvent.ServerTickEvent tickEvent, @NotNull WispNetwork network, int currentTickTime, int tickOffset)
        {
            if(!active)
            {
                return OptionalInt.empty();
            }
            RefreshCache(currentTickTime);
            return OptionalInt.of(currentTickTime + Constants.SLEEP_TICK_TIMER);
        }
    }

    //data player can tweak
    private Direction invAccessDirection = null;

    //operation data
    private int insertionSpeedRank = 0;
    private WispInteractionNodeBase parentWisp;
    private PeriodicTask currentTask;

    protected abstract void RefreshCache(int currentTick);

    @Override
    public CompoundTag SerializeNBT()
    {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("insertionSpeedRank", insertionSpeedRank);
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
        if (nbt.contains("insertionSpeedRank"))
        {
            insertionSpeedRank = nbt.getInt("insertionSpeedRank");
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
