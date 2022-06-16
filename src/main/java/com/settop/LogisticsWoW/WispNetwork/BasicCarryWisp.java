package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Utils.Constants;
import com.settop.LogisticsWoW.Wisps.WispNode;

public class BasicCarryWisp extends CarryWisp
{
    private final float speed;
    private final int carryCapacity;

    public BasicCarryWisp(WispNode sourceNode, int speedRank, int carryRank)
    {
        super(sourceNode);
        this.speed = Constants.GetCarrySpeed(speedRank);
        this.carryCapacity = Constants.GetCarryCount(carryRank);
    }

    @Override
    public boolean IsInstantTransport()
    {
        return false;
    }

    @Override
    public float GetSpeed()
    {
        return speed;
    }

    @Override
    public int GetCarryCapacity()
    {
        return carryCapacity;
    }
}
