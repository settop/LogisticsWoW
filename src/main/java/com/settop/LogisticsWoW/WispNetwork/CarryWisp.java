package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Wisps.WispNode;
import net.minecraft.world.item.ItemStack;

public abstract class CarryWisp
{
    private boolean isClaimed = false;
    public final WispNode sourceNode;

    public CarryWisp(WispNode sourceNode)
    {
        this.sourceNode = sourceNode;
    }


    public void Claim() { isClaimed = true; }
    public void ReleaseReservation() {}

    public boolean IsClaimed() { return isClaimed; }

    public abstract boolean IsInstantTransport();
    //blocks per tick
    public abstract float GetSpeed();
    public abstract int GetCarryCapacity();


    public int CalculateCarryStackSize(ItemStack stack)
    {
        int targetStackSize = (stack.getMaxStackSize() * GetCarryCapacity()) / 64;
        return Math.max(targetStackSize, 1);
    }
}
