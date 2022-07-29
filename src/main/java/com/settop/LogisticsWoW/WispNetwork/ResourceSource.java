package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Utils.Invalidable;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public abstract class ResourceSource<T> extends Invalidable
{
    public final StoreableResourceMatcher<T> matcher;
    private int previousNumAvailable = 0;
    private int numAvailable;

    public ResourceSource(StoreableResourceMatcher<T> matcher, int numAvailable)
    {
        this.matcher = matcher;
        assert numAvailable >= 0;
        this.numAvailable = numAvailable;
    }

    public int GetNumAvailable()
    {
        return numAvailable;
    }

    public void UpdateNumAvailable(int newCount)
    {
        assert newCount >= 0;
        this.numAvailable = newCount;
    }

    public void Reset()
    {
        previousNumAvailable = numAvailable;
        numAvailable = 0;
    }

    public boolean HasChanged()
    {
        return previousNumAvailable != numAvailable;
    }

    abstract public Reservation ReserveExtract(StoreableResourceMatcher<T> extractionMatcher, int count);
    abstract public T Extract(Reservation reservation, StoreableResourceMatcher<T> extractionMatcher, int count);
    abstract public WispInteractionNodeBase GetAttachedInteractionNode();
    abstract public Direction GetExtractionDirection();
}
