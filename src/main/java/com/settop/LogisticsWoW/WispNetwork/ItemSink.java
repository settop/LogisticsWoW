package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Utils.Invalidable;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public abstract class ItemSink extends Invalidable
{
    public final int priority;

    public ItemSink(int priority)
    {
        this.priority = priority;
    }

    //returns the reservation or null is no reservation was made
    abstract public ReservableInventory.Reservation ReserveInsert(ItemStack stack);
    abstract public ItemStack Insert(ReservableInventory.Reservation reservation, ItemStack stack);
    abstract public WispInteractionNodeBase GetAttachedInteractionNode();
    abstract public Direction GetInsertDirection();
}
