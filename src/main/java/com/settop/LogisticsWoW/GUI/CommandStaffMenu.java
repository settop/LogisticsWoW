package com.settop.LogisticsWoW.GUI;

import com.settop.LogisticsWoW.Items.WispCommandStaff;
import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CommandStaffMenu extends ChestMenu
{
    private static final int SLOTS_PER_ROW = 9;

    private static class CommandStaffSlot extends Slot
    {
        public CommandStaffSlot(Container container, int slot, int x, int y)
        {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack)
        {
            return WispCommandStaff.CanPlaceInStaffInv(stack);
        }
    }

    public static CommandStaffMenu CreateMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData)
    {
        int rowCount = extraData.readInt();
        return new CommandStaffMenu(id, playerInventory, new SimpleContainer(rowCount * SLOTS_PER_ROW), rowCount);
    }

    public static CommandStaffMenu CreateMenu(int id, Inventory playerInventory, Container container, int containerRows)
    {
        return new CommandStaffMenu(id, playerInventory, container, containerRows);
    }

    public CommandStaffMenu(int windowID, Inventory playerInventory, Container container, int containerRows)
    {
        super(LogisticsWoW.Menus.COMMAND_STAFF_MENU, windowID, playerInventory, container, containerRows);

        for(int j = 0; j < containerRows; ++j)
        {
            for(int k = 0; k < 9; ++k)
            {
                //replace the slots for the command staff
                int slotIndex = k + j * 9;
                Slot currentSlot = slots.get(slotIndex);
                CommandStaffSlot newSlot = new CommandStaffSlot(container, slotIndex, currentSlot.x, currentSlot.y);
                newSlot.index = slotIndex;
                slots.set(slotIndex, newSlot);
            }
        }
        /*
        checkContainerSize(container, containerRows * 9);
        this.container = container;
        this.containerRows = containerRows;
        container.startOpen(playerInventory.player);
        int i = (this.containerRows - 4) * 18;

        for(int j = 0; j < this.containerRows; ++j)
        {
            for(int k = 0; k < 9; ++k)
            {
                this.addSlot(new CommandStaffSlot(container, k + j * 9, 8 + k * 18, 18 + j * 18));
            }
        }

        for(int l = 0; l < 3; ++l)
        {
            for(int j1 = 0; j1 < 9; ++j1)
            {
                this.addSlot(new Slot(playerInventory, j1 + l * 9 + 9, 8 + j1 * 18, 103 + l * 18 + i));
            }
        }

        for(int i1 = 0; i1 < 9; ++i1)
        {
            this.addSlot(new Slot(playerInventory, i1, 8 + i1 * 18, 161 + i));
        }


         */
    }
/*
    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem())
        {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (slotIndex < this.containerRows * 9)
            {
                if (!this.moveItemStackTo(itemstack1, this.containerRows * 9, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.moveItemStackTo(itemstack1, 0, this.containerRows * 9, false))
            {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void removed(@NotNull Player player)
    {
        super.removed(player);
        this.container.stopOpen(player);
    }

    public Container getContainer() {
        return this.container;
    }

    public int getRowCount() {
        return this.containerRows;
    }

 */
}
