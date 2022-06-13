package com.settop.LogisticsWoW.WispNetwork.Tasks;

import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import net.minecraftforge.event.TickEvent;

import javax.annotation.Nonnull;
import java.util.OptionalInt;

public interface WispTask
{
    //Start the task
    //currentTickTime - the current tick time of the network
    //Returns the tick time that the first tick should be executed on
    int Start(@Nonnull WispNetwork network, int startTickTime);
    //Tick the task
    //currentTickTime - the current tick time of the network
    //tickOffset - the amount the task tick has been delayed, will be <= 0
    //Returns the tick time for the next tick to be executed on, or empty if the task is done
    OptionalInt Tick(@Nonnull TickEvent.ServerTickEvent tickEvent, @Nonnull WispNetwork network, int currentTickTime, int tickOffset);

}
