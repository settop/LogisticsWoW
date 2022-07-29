package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Utils.Invalidable;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.core.Direction;

public abstract class ResourceSink<T> extends Invalidable
{
    public final int priority;

    public ResourceSink(int priority)
    {
        this.priority = priority;
    }

    //returns the reservation or null is no reservation was made
    abstract public Reservation ReserveInsert(T stack);
    abstract public T Insert(Reservation reservation, T stack);
    abstract public WispInteractionNodeBase GetAttachedInteractionNode();
    abstract public Direction GetInsertDirection();
}
