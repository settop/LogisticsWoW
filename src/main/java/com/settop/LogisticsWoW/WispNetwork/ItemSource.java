package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Utils.Invalidable;

//ToDo: Handle NBT
public class ItemSource extends Invalidable
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
}
