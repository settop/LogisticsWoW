package com.settop.LogisticsWoW.GUI;

import com.settop.LogisticsWoW.GUI.SubMenus.*;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.BoolArray;
import com.settop.LogisticsWoW.Wisps.BasicWispContents;
import com.settop.LogisticsWoW.Wisps.Enhancements.EnhancementTypes;
import com.settop.LogisticsWoW.Wisps.Enhancements.IEnhancement;
import com.settop.LogisticsWoW.Wisps.WispBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BasicWispMenu extends MultiScreenMenu implements BasicWispContents.OnEnhancementChanged
{
    private BasicWispContents wispContents;
    private WispBase parentWisp;
    private BlockState blockState;
    private BlockEntity blockEntity;
    private DataSlot openSubContainerIndex;
    private List<SubMenu> tabbedContainers;
    private PlayerInventorySubMenu playerInvSubMenu;
    private BoolArray enhancementPresent;

    public static BasicWispMenu CreateMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData)
    {
        int contentsSize = extraData.readInt();
        BlockPos pos = extraData.readBlockPos();
        BlockState blockState = playerInventory.player.level.getBlockState(pos);
        BlockEntity blockEntity = playerInventory.player.level.getBlockEntity(pos);
        return new BasicWispMenu(id, playerInventory, null, new BasicWispContents( contentsSize ), null, blockState, blockEntity);
    }

    public static BasicWispMenu CreateMenu(int id, Inventory playerInventory, Player player, BasicWispContents inWispContents, WispBase inParentWisp)
    {
        BlockPos pos = inParentWisp.GetPos();
        BlockState blockState = playerInventory.player.level.getBlockState(pos);
        BlockEntity blockEntity = playerInventory.player.level.getBlockEntity(pos);
        return new BasicWispMenu(id, playerInventory, player, inWispContents, inParentWisp, blockState, blockEntity);
    }


    public static final int WISP_SLOT_XPOS = 3;
    public static final int WISP_SLOT_YPOS = 16;
    public static final int PLAYER_INVENTORY_XPOS = 3;
    public static final int PLAYER_INVENTORY_YPOS = 101;

    private BasicWispMenu(int id, Inventory playerInventory, Player player, BasicWispContents inWispContents, WispBase inParentWisp, BlockState inBlockState, BlockEntity inBlockEntity)
    {
        super(LogisticsWoW.Menus.BASIC_WISP_MENU, id, player);

        playerInvSubMenu = new PlayerInventorySubMenu(playerInventory, PLAYER_INVENTORY_XPOS, PLAYER_INVENTORY_YPOS);
        WispContentsMenu wispContentsContainer = new WispContentsMenu(inWispContents, WISP_SLOT_XPOS, WISP_SLOT_YPOS);

        wispContents = inWispContents;
        parentWisp = inParentWisp;
        blockState = inBlockState;
        blockEntity = inBlockEntity;
        openSubContainerIndex = DataSlot.standalone();
        openSubContainerIndex.set(0);
        enhancementPresent = new BoolArray(EnhancementTypes.NUM);

        addDataSlot(openSubContainerIndex);
        addDataSlots(enhancementPresent);

        tabbedContainers = new ArrayList<>();
        tabbedContainers.add(wispContentsContainer);
        for(int i = 0; i < EnhancementTypes.NUM; ++i)
        {
            SubMenu enhancementSubContainer = EnhancementTypes.values()[i].GetFactory().CreateSubMenu(0, 0, blockState, blockEntity);
            enhancementSubContainer.SetActive(false);
            tabbedContainers.add(enhancementSubContainer);
        }

        List<SubMenu> subContainers = new ArrayList<>();
        subContainers.add(playerInvSubMenu);
        subContainers.addAll(tabbedContainers);
        SetSubContainers(subContainers);

        if(!playerInventory.player.level.isClientSide())
        {
            //only care about this on the server
            wispContents.SetListener(this);
        }
    }

    @Override
    public boolean stillValid(@NotNull Player playerIn)
    {
        return wispContents.stillValid(playerIn);
    }

    @Override
    public ItemStack quickMoveStack(Player playerEntity, int sourceSlotIndex)
    {
        Slot sourceSlot = slots.get(sourceSlotIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        //first slots are player inventory
        int playerInvSlotCount = playerInvSubMenu.inventorySlots.size();

        if(sourceSlotIndex < playerInvSlotCount)
        {
            int openIndex = openSubContainerIndex.get();
            int slotStart = playerInvSlotCount;
            for(int i = 0; i < openIndex; ++i)
            {
                slotStart += tabbedContainers.get(i).inventorySlots.size();
            }
            int slotEnd = slotStart + tabbedContainers.get(openIndex).inventorySlots.size();
            //from the player inventory
            if (!moveItemStackTo(sourceStack, slotStart, slotEnd, false))
            {
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
        if(parentWisp != null)
        {
            parentWisp.UpdateFromContents();
        }
        super.removed(playerIn);
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
        for(int i = 0; i < tabbedContainers.size(); ++i)
        {
            tabbedContainers.get(i).SetActive(i == openSubContainerIndex.get());
        }
    }

    @Override
    public void OnEnhancementChange(int index, IEnhancement previousEnhancement, IEnhancement nextEnhancement)
    {
        if(previousEnhancement != null)
        {
            enhancementPresent.SetBool(previousEnhancement.GetType().ordinal(), false);
            SubMenu enhancementContainer = tabbedContainers.get(previousEnhancement.GetType().ordinal() + 1);
            IEnhancementSubMenu enhanceCont = (IEnhancementSubMenu)enhancementContainer;
            enhanceCont.SetEnhancement(null);
        }

        if(nextEnhancement != null)
        {
            enhancementPresent.SetBool(nextEnhancement.GetType().ordinal(), true);
            SubMenu enhancementContainer = tabbedContainers.get(nextEnhancement.GetType().ordinal() + 1);
            IEnhancementSubMenu enhanceCont = (IEnhancementSubMenu)enhancementContainer;
            enhanceCont.SetEnhancement(nextEnhancement);
        }
    }

    public BasicWispContents GetWispContents() { return wispContents; }

    public SubMenu GetEnhancementSubContainer(EnhancementTypes enhancementType)
    {
        return tabbedContainers.get(enhancementType.ordinal() + 1);
    }

    public PlayerInventorySubMenu GetPlayerInventorySubMenu()
    {
        return playerInvSubMenu;
    }

    public boolean IsTabActive(int index)
    {
        if(index == 0)
        {
            return true;
        }
        else if(index - 1 < EnhancementTypes.NUM)
        {
            return enhancementPresent.GetBool(index - 1);
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
        if(index >= tabbedContainers.size())
        {
            LogisticsWoW.LOGGER.error("Setting selected tab to an invalid tab");
            return;
        }

        openSubContainerIndex.set(index);
        UpdateActiveSubContainers();
    }
}
