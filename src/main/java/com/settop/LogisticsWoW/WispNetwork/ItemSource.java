package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Utils.Invalidable;
import com.settop.LogisticsWoW.Wisps.WispInteractionNode;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.world.item.ItemStack;

//ToDo: Handle NBT
public abstract class ItemSource extends Invalidable
{
    public static class Reservation extends Invalidable
    {
        public final ItemSource sourceSource;
        public final int reservationSize;
        public Reservation(ItemSource sourceSource, int reservationSize)
        {
            assert reservationSize > 0;
            this.sourceSource = sourceSource;
            this.reservationSize = reservationSize;
        }
    }

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

    abstract public ItemStack Extract(int count);
    abstract public ItemStack ReservedExtract(Reservation reservation);
    abstract public WispInteractionNodeBase GetAttachedInteractionNode();
}
