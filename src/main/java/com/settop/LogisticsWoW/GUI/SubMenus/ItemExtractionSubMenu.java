package com.settop.LogisticsWoW.GUI.SubMenus;

import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.ItemExtractionSubScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.StorageSubScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.SubScreen;
import com.settop.LogisticsWoW.Utils.Utils;
import com.settop.LogisticsWoW.WispNetwork.ReservableInventory;
import com.settop.LogisticsWoW.Wisps.Enhancements.ItemExtractionEnhancement;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ItemExtractionSubMenu extends ItemFilterSubMenu
{
    private final ItemExtractionEnhancement enhancement;
    private final DataSlot direction = DataSlot.standalone();
    private final BlockState blockState;
    private final WispInteractionNodeBase parentWisp;

    public static final int DIRECTION_PROPERTY_ID = NEXT_PROPERTY_ID;

    public ItemExtractionSubMenu(ItemExtractionEnhancement enhancement, int xPos, int yPos, BlockState blockState, BlockEntity blockEntity, WispInteractionNodeBase parentWisp)
    {
        super(enhancement.GetFilter(), xPos, yPos, false, false, true);
        addDataSlot(direction);
        this.enhancement = enhancement;
        this.blockState = blockState;
        this.parentWisp = parentWisp;

        direction.set(Utils.DirectionToInt(enhancement.GetInvAccessDirection()));
    }

    @Override
    public void HandlePropertyUpdate(int propertyId, int value)
    {
        switch (propertyId)
        {
            case DIRECTION_PROPERTY_ID -> direction.set(value);
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
            enhancement.SetInvAccessDirection(Utils.IntToDirection(direction.get()));
        }
    }

    @Override
    public SubScreen CreateScreen(MultiScreen<?> parentScreen)
    {
        return new ItemExtractionSubScreen(this, parentScreen);
    }

    private void DoPolymorphicFilterSet()
    {
        WispInteractionNodeBase node = GetConnectedNode();
        if(node == null)
        {
            return;
        }
        ReservableInventory inv = node.GetReservableInventory(Utils.IntToDirection(direction.get()));
        if(inv == null)
        {
            return;
        }

        super.DoPolymorphicFilterSet(inv.GetSimulatedInv());
    }

    public BlockState GetBlockState() { return blockState; }

    public Direction GetAccessDirection() { return Utils.IntToDirection(direction.get()); }
    public void SetAccessDirection(Direction dir) { direction.set(Utils.DirectionToInt(dir)); }

    public WispInteractionNodeBase GetConnectedNode()
    {
        return parentWisp;
    }
}
