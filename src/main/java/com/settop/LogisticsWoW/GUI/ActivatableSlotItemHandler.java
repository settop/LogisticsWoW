package com.settop.LogisticsWoW.GUI;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ActivatableSlotItemHandler extends SlotItemHandler implements IActivatableSlot
{
    private boolean isActive = true;

    public ActivatableSlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition)
    {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public void SetActive(boolean active)
    {
        isActive = active;
    }

    @Override
    public boolean isActive()
    {
        return isActive;
    }
}
