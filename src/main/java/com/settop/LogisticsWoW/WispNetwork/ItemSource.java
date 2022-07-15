package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Utils.Invalidable;
import com.settop.LogisticsWoW.Wisps.WispInteractionNode;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

//ToDo: Handle NBT
public abstract class ItemSource extends Invalidable
{
    private int previousNumAvailable = 0;
    private int numAvailable;

    public ItemSource(int numAvailable)
    {
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

    abstract public ReservableInventory.Reservation ReserveExtract(int count);
    abstract public ItemStack Extract(ReservableInventory.Reservation reservation, int count);
    abstract public WispInteractionNodeBase GetAttachedInteractionNode();
    abstract public Direction GetExtractionDirection();
}
