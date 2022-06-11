package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Utils.Invalidable;
import com.settop.LogisticsWoW.Wisps.WispBase;
import net.minecraft.world.item.ItemStack;

public abstract class ItemSink extends Invalidable
{
    public static class Reservation extends Invalidable
    {
        public final ItemSink sourceSink;
        public final int reservationSize;
        public Reservation(ItemSink sourceSink, int reservationSize)
        {
            assert reservationSize > 0;
            this.sourceSink = sourceSink;
            this.reservationSize = reservationSize;
        }
    }
    public final int priority;

    public ItemSink(int priority)
    {
        this.priority = priority;
    }

    //returns the reservation or null is no reservation was made
    abstract public Reservation ReserveInsert(ItemStack stack);
    abstract public WispBase GetAttachedWisp();
}
