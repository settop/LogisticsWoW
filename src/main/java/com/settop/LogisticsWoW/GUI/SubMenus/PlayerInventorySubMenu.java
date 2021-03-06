package com.settop.LogisticsWoW.GUI.SubMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;
import com.settop.LogisticsWoW.Client.Client;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.SubScreen;
import com.settop.LogisticsWoW.GUI.ActivatableSlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class PlayerInventorySubMenu extends SubMenu
{
    public static final int HOTBAR_YOFFSET = 4;

    private static class LockedSlot extends ActivatableSlotItemHandler
    {
        public LockedSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition)
        {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPickup(Player playerIn)
        {
            return false;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack)
        {
            return false;
        }
    }

    public PlayerInventorySubMenu(Inventory playerInventory, int xPos, int yPos, int lockedSlot)
    {
        super(xPos, yPos);
        PlayerInvWrapper playerInventoryForge = new PlayerInvWrapper(playerInventory);

        int numRows = (playerInventory.items.size() - Client.PLAYER_INVENTORY_COLUMN_COUNT) / Client.PLAYER_INVENTORY_COLUMN_COUNT;

        int slotIndex = 0;
        for (int i = 0; i < Client.PLAYER_INVENTORY_COLUMN_COUNT; ++i)
        {
            if(i == lockedSlot)
            {
                this.inventorySlots.add(new LockedSlot(playerInventoryForge, slotIndex++, xPos + Client.SLOT_X_SPACING * i, yPos + HOTBAR_YOFFSET + Client.SLOT_Y_SPACING * numRows));
            }
            else
            {
                this.inventorySlots.add(new ActivatableSlotItemHandler(playerInventoryForge, slotIndex++, xPos + Client.SLOT_X_SPACING * i, yPos + HOTBAR_YOFFSET + Client.SLOT_Y_SPACING * numRows));
            }
         }
        for(int i = 0; i < playerInventory.items.size() - Client.PLAYER_INVENTORY_COLUMN_COUNT; ++i)
        {
            int row = i / Client.PLAYER_INVENTORY_COLUMN_COUNT;
            int column = i % Client.PLAYER_INVENTORY_COLUMN_COUNT;
            this.inventorySlots.add(new ActivatableSlotItemHandler(playerInventoryForge, slotIndex++,  xPos + column * Client.SLOT_X_SPACING, yPos + row * Client.SLOT_Y_SPACING));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public SubScreen CreateScreen(MultiScreen<?> parentScreen)
    {
        return null;
    }
}
