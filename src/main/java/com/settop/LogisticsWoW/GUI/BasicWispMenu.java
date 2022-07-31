package com.settop.LogisticsWoW.GUI;

import com.settop.LogisticsWoW.GUI.SubMenus.*;
import com.settop.LogisticsWoW.Items.WispCommandStaff;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Wisps.WispInteractionContents;
import com.settop.LogisticsWoW.Wisps.Enhancements.IEnhancement;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BasicWispMenu extends MultiScreenMenu implements WispInteractionContents.OnEnhancementChanged
{
    private final WispInteractionContents wispContents;
    private final WispInteractionNodeBase parentWisp;
    private final BlockState blockState;
    private final BlockEntity blockEntity;
    private final DataSlot openSubContainerIndex;
    private final CommandStaffInvSubMenu commandStaffInvSubMenu;
    private final PlayerInventorySubMenu playerInvSubMenu;
    private final WispContentsMenu wispContentsSubMenu;

    private final ArrayList<IEnhancement> currentEnhancements;

    private SubMenu activeEnhancementSubMenu = null;

    public static BasicWispMenu CreateMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData)
    {
        int contentsSize = extraData.readInt();
        BlockPos pos = extraData.readBlockPos();
        int staffSlot = extraData.readInt();
        int staffInvSize = extraData.readInt();

        BlockState blockState = playerInventory.player.level.getBlockState(pos);
        BlockEntity blockEntity = playerInventory.player.level.getBlockEntity(pos);
        Container staffPlaceholderContainer = new WispCommandStaff.StaffContainer(staffInvSize);

        return new BasicWispMenu(id, playerInventory, null, new WispInteractionContents( contentsSize ), null, blockState, blockEntity, new InvWrapper(staffPlaceholderContainer), staffSlot);
    }

    public static BasicWispMenu CreateMenu(int id, Inventory playerInventory, Player player, WispInteractionContents inWispContents, WispInteractionNodeBase inParentWisp, IItemHandler staffInv, int staffSlot)
    {
        BlockPos pos = inParentWisp.GetPos();
        BlockState blockState = playerInventory.player.level.getBlockState(pos);
        BlockEntity blockEntity = playerInventory.player.level.getBlockEntity(pos);
        return new BasicWispMenu(id, playerInventory, player, inWispContents, inParentWisp, blockState, blockEntity, staffInv, staffSlot);
    }


    public static final int WISP_SLOT_XPOS = 3;
    public static final int WISP_SLOT_YPOS = 16;
    public static final int PLAYER_INVENTORY_XPOS = 3;
    public static final int STAFF_INVENTORY_YPOS = 95;
    public static final int PLAYER_INVENTORY_YPOS = 125;

    private BasicWispMenu(int id, Inventory playerInventory, Player player, WispInteractionContents inWispContents, WispInteractionNodeBase inParentWisp, BlockState inBlockState, BlockEntity inBlockEntity, IItemHandler staffInv, int staffSlot)
    {
        super(LogisticsWoW.Menus.BASIC_WISP_MENU, id, player);

        commandStaffInvSubMenu = new CommandStaffInvSubMenu(staffInv, PLAYER_INVENTORY_XPOS, STAFF_INVENTORY_YPOS);
        playerInvSubMenu = new PlayerInventorySubMenu(playerInventory, PLAYER_INVENTORY_XPOS, PLAYER_INVENTORY_YPOS, staffSlot);
        wispContentsSubMenu = new WispContentsMenu(inWispContents, WISP_SLOT_XPOS, WISP_SLOT_YPOS);

        wispContents = inWispContents;
        parentWisp = inParentWisp;
        blockState = inBlockState;
        blockEntity = inBlockEntity;
        openSubContainerIndex = DataSlot.standalone();
        openSubContainerIndex.set(0);

        addDataSlot(openSubContainerIndex);

        List<SubMenu> subContainers = new ArrayList<>();
        subContainers.add(commandStaffInvSubMenu);
        subContainers.add(playerInvSubMenu);
        subContainers.add(wispContentsSubMenu);
        SetSubMenus(subContainers);
        ReserveTempSubMenuSlots(IEnhancement.MAX_NUM_GUI_SLOTS, IEnhancement.MAX_NUM_GUI_DATA, IEnhancement.MAX_NUM_GUI_STRINGS);

        currentEnhancements = new ArrayList<>(inWispContents.getContainerSize());
        for(int i = 0; i < inWispContents.getContainerSize(); ++i)
        {
            currentEnhancements.add(null);
        }
        wispContents.SetListener(this);
    }

    @Override
    public boolean stillValid(@NotNull Player playerIn)
    {
        return wispContents.stillValid(playerIn);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerEntity, int sourceSlotIndex)
    {
        Slot sourceSlot = slots.get(sourceSlotIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        //first slots are player inventory
        int playerInvSlotCount = playerInvSubMenu.inventorySlots.size() + commandStaffInvSubMenu.inventorySlots.size();

        if(sourceSlotIndex < playerInvSlotCount)
        {
            boolean onBaseScreen = openSubContainerIndex.get() == 0;
            int slotStart = playerInvSlotCount;
            int slotEnd;
            if(onBaseScreen)
            {
                slotEnd = slotStart + wispContentsSubMenu.inventorySlots.size();
            }
            else
            {
                slotStart += wispContentsSubMenu.inventorySlots.size();
                slotEnd = slotStart + GetTempSubMenu().inventorySlots.size();
            }
            //from the player inventory
            if (!moveItemStackTo(sourceStack, slotStart, slotEnd, false))
            {
                moveItemStackToFakeSlots(sourceStack, slotStart, slotEnd);
                return ItemStack.EMPTY;
            }
        }
        else if(sourceSlotIndex < slots.size())
        {
            //from the sub container
            if (!moveItemStackTo(sourceStack, 0, playerInvSlotCount, false))
            {
                return ItemStack.EMPTY;
            }
        }
        else
        {
            LogisticsWoW.LOGGER.warn("Invalid slotIndex:" + sourceSlotIndex);
            return ItemStack.EMPTY;
        }

        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0)
        {
            sourceSlot.set(ItemStack.EMPTY);
        }
        else
        {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(playerEntity, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public void removed(@NotNull Player playerIn)
    {
        wispContents.SetListener(null);
        super.removed(playerIn);
        if(parentWisp != null)
        {
            parentWisp.UpdateFromContents();
        }
    }

    @Override
    public void setData(int id, int data)
    {
        super.setData(id, data);
        if(id == 0)
        {
            UpdateActiveSubContainers();
        }
    }

    private void UpdateActiveSubContainers()
    {
        if(activeEnhancementSubMenu != null)
        {
            RemoveTempSubMenu(activeEnhancementSubMenu);
            activeEnhancementSubMenu = null;
        }
        wispContentsSubMenu.SetActive(openSubContainerIndex.get() == 0);
        if(openSubContainerIndex.get() != 0)
        {
            IEnhancement enhancement = currentEnhancements.get(openSubContainerIndex.get() - 1);
            if(enhancement != null)
            {
                activeEnhancementSubMenu = enhancement.CreateSubMenu(0, 0, blockState, blockEntity, parentWisp);
                AddTempSubMenu(activeEnhancementSubMenu);
            }
        }
    }

    @Override
    public void OnEnhancementChange(int index, IEnhancement previousEnhancement, IEnhancement nextEnhancement)
    {
        currentEnhancements.set(index, nextEnhancement);
    }

    public WispInteractionContents GetWispContents() { return wispContents; }

    public CommandStaffInvSubMenu GetCommandStaffInvSubMenu()
    {
        return commandStaffInvSubMenu;
    }

    public PlayerInventorySubMenu GetPlayerInventorySubMenu()
    {
        return playerInvSubMenu;
    }

    public ArrayList<IEnhancement> GetEnhancements() { return currentEnhancements; }

    public boolean IsTabActive(int index)
    {
        if(index == 0)
        {
            return true;
        }
        else if(index - 1 < currentEnhancements.size())
        {
            return currentEnhancements.get(index - 1) != null;
        }
        else
        {
            return false;
        }
    }

    public boolean IsTabSelected(int index)
    {
        return index == openSubContainerIndex.get();
    }

    public void SelectTab(int index)
    {
        if(!IsTabActive(index))
        {
            LogisticsWoW.LOGGER.warn("Setting selected tab to an inactive tab");
            return;
        }
        if(index >= currentEnhancements.size() + 1)
        {
            LogisticsWoW.LOGGER.error("Setting selected tab to an invalid tab");
            return;
        }
        if(openSubContainerIndex.get() == index)
        {
            return;
        }

        openSubContainerIndex.set(index);
        UpdateActiveSubContainers();
    }
}
