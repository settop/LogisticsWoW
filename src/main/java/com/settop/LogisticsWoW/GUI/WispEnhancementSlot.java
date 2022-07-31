package com.settop.LogisticsWoW.GUI;

import com.settop.LogisticsWoW.Items.WispEnhancementItem;
import com.settop.LogisticsWoW.Wisps.WispInteractionContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WispEnhancementSlot extends Slot implements IActivatableSlot
{
    private boolean isActive = true;

    public WispEnhancementSlot(WispInteractionContents contentsIn, int index, int xPosition, int yPosition)
    {
        super(contentsIn, index, xPosition, yPosition);
     }

    @Override
    public boolean mayPlace(ItemStack stack)
    {
        if(!(stack.getItem() instanceof WispEnhancementItem))
        {
            return false;
        }
        WispEnhancementItem enhancementItem = (WispEnhancementItem)stack.getItem();
        if(enhancementItem.AllowMultiplePerNode())
        {
            return true;
        }
        for(int i = 0; i < container.getContainerSize(); ++i)
        {
            if(container.getItem(i).getItem() == stack.getItem())
            {
                //only one of each enhancement per wisp contents
                return false;
            }
        }
        return true;
    }

    @Override
    public int getMaxStackSize()
    {
        return 1;
    }

    @Override
    public boolean isActive()
    {
        return isActive;
    }

    @Override
    public void SetActive(boolean active)
    {
        isActive = active;
    }
}
