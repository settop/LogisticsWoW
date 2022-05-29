package com.settop.LogisticsWoW.GUI;

import com.settop.LogisticsWoW.Utils.FakeInventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FakeSlot extends Slot implements IActivatableSlot
{
    private boolean isActive = true;
    public final boolean includeCounts;

    public FakeSlot(FakeInventory inventory, int index, int xPosition, int yPosition)
    {
        super(inventory, index, xPosition, yPosition);
        this.includeCounts = inventory.includeCounts;
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

    @Override
    public int getMaxStackSize()
    {
        return includeCounts ? FakeInventory.MAX_STACK : 1;
    }

    @Override
    public boolean mayPickup(Player playerIn)
    {
        return false;
    }

    @Override
    public boolean mayPlace(ItemStack item) { return false; }
}
