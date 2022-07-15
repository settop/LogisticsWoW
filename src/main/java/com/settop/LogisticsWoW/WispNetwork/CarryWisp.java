package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Utils.Constants;
import com.settop.LogisticsWoW.Wisps.WispNode;
import net.minecraft.world.item.ItemStack;

public class CarryWisp
{
    private boolean isClaimed = false;
    public final WispNode sourceNode;
    private final float speed;
    private final int carryCapacity;

    public CarryWisp(WispNode sourceNode, int speedRank, int carryRank)
    {
        this.sourceNode = sourceNode;
        this.speed = Constants.GetCarrySpeed(speedRank);
        this.carryCapacity = Constants.GetCarryCount(carryRank);
    }

    public void Claim() { isClaimed = true; }
    public void ReleaseReservation() {}

    public boolean IsClaimed() { return isClaimed; }

    public boolean IsInstantTransport()
    {
        return speed <= 0.f;
    }
    //blocks per tick
    public float GetSpeed()
    {
        return speed;
    }

    public int GetCarryCapacity()
    {
        return carryCapacity;
    }

    public int CalculateCarryStackSize(ItemStack stack)
    {
        int targetStackSize = (stack.getMaxStackSize() * GetCarryCapacity()) / 64;
        return Math.max(targetStackSize, 1);
    }
}
