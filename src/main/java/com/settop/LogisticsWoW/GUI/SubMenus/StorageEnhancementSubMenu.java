package com.settop.LogisticsWoW.GUI.SubMenus;

import com.settop.LogisticsWoW.Client.Client;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.StorageSubScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.SubScreen;
import com.settop.LogisticsWoW.GUI.FakeSlot;
import com.settop.LogisticsWoW.GUI.IActivatableSlot;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.Constants;
import com.settop.LogisticsWoW.Utils.FakeInventory;
import com.settop.LogisticsWoW.Utils.StringVariableArrayReferenceHolder;
import com.settop.LogisticsWoW.WispNetwork.ReservableInventory;
import com.settop.LogisticsWoW.Wisps.Enhancements.IEnhancement;
import com.settop.LogisticsWoW.Wisps.Enhancements.ItemStorageEnhancement;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;

public class StorageEnhancementSubMenu extends ItemFilterSubMenu
{
    private final ItemStorageEnhancement enhancement;
    private final DataSlot priority = DataSlot.standalone();
    private final BlockState blockState;
    private final WispInteractionNodeBase parentWisp;

    public static final int PRIORITY_PROPERTY_ID = NEXT_PROPERTY_ID;


    public StorageEnhancementSubMenu(ItemStorageEnhancement enhancement, int xPos, int yPos, BlockState blockState, BlockEntity blockEntity, WispInteractionNodeBase parentWisp)
    {
        super(enhancement.GetFilter(), xPos, yPos, true, true, false);
        addDataSlot(priority);
        this.enhancement = enhancement;
        this.blockState = blockState;
        this.parentWisp = parentWisp;

        priority.set(enhancement.GetPriority());
    }

    @Override
    public void HandlePropertyUpdate(int propertyId, int value)
    {
        switch (propertyId)
        {
            case PRIORITY_PROPERTY_ID -> priority.set(value);
            case POLYMORPHIC_PROPERTY_ID -> DoPolymorphicFilterSet();
        }
        super.HandlePropertyUpdate(propertyId, value);
    }

    @Override
    protected void WriteDataToSource()
    {
        super.WriteDataToSource();
        if(enhancement != null)
        {
            enhancement.SetPriority(priority.get());
        }
    }

    @Override
    public SubScreen CreateScreen(MultiScreen<?> parentScreen)
    {
        return new StorageSubScreen(this, parentScreen);
    }

    private void DoPolymorphicFilterSet()
    {
        WispInteractionNodeBase node = GetConnectedNode();
        if(node == null)
        {
            return;
        }
        ReservableInventory inv = node.GetReservableInventory();
        if(inv == null)
        {
            return;
        }

        super.DoPolymorphicFilterSet(inv.GetSimulatedInv());
    }

    public BlockState GetBlockState() { return blockState; }

    public int GetPriority() { return priority.get(); }
    public void SetPriority(int prio) { priority.set(prio); }

    public WispInteractionNodeBase GetConnectedNode()
    {
        return parentWisp;
    }
}
