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

public class StorageEnhancementSubMenu extends SubMenu
{
    private final ItemStorageEnhancement enhancement;
    private final DataSlot priority = DataSlot.standalone();
    private final BlockState blockState;
    private final WispInteractionNodeBase parentWisp;
    private final FakeInventory filter = new FakeInventory( ItemStorageEnhancement.FILTER_SIZE, false );
    private final StringVariableArrayReferenceHolder tagFilters = new StringVariableArrayReferenceHolder(';');
    private final DataSlot filterType = DataSlot.standalone();

    private FakeInventory tagGetHelper = new FakeInventory( 1, false );

    public static final int PRIORITY_PROPERTY_ID = 0;
    public static final int FILTER_TYPE_PROPERTY_ID = 1;

    //not real properties, just used to trigger things
    public static final int POLYMORPHIC_PROPERTY_ID = 100;

    public static final int FILTER_TAGS_STRING_PROPERTY_ID = 0;

    public static final int FILTER_SLOT_X = 21;
    public static final int FILTER_SLOT_Y = 1;

    public static final int TAG_FETCH_HELPER_SLOT_X = 1;
    public static final int TAG_FETCH_HELPER_SLOT_Y = 64;

    public StorageEnhancementSubMenu(ItemStorageEnhancement enhancement, int xPos, int yPos, BlockState blockState, BlockEntity blockEntity, WispInteractionNodeBase parentWisp)
    {
        super(xPos, yPos);
        addDataSlot(priority);
        addDataSlot(filterType);
        trackStr(tagFilters);
        this.enhancement = enhancement;
        this.blockState = blockState;
        this.parentWisp = parentWisp;

        for(int i = 0; i < filter.getContainerSize(); ++i)
        {
            int column = i % ItemStorageEnhancement.FILTER_NUM_COLUMNS;
            int row = i / ItemStorageEnhancement.FILTER_NUM_COLUMNS;
            inventorySlots.add( new FakeSlot(filter, i, xPos + FILTER_SLOT_X + column * Client.SLOT_X_SPACING, yPos + FILTER_SLOT_Y + row * Client.SLOT_Y_SPACING));
        }
        inventorySlots.add( new FakeSlot(tagGetHelper, 0, xPos + TAG_FETCH_HELPER_SLOT_X, yPos + TAG_FETCH_HELPER_SLOT_Y));

        priority.set(enhancement.GetPriority());
        for(int i = 0; i < filter.getContainerSize(); ++i)
        {
            filter.setItem(i, enhancement.GetFilter().getItem(i));
        }
        if(enhancement.GetTagFilters() != null)
        {
            ArrayList<String> tags = new ArrayList<>();
            for (ResourceLocation tag : enhancement.GetTagFilters())
            {
                tags.add(tag.toString());
            }
            tagFilters.setArray(tags);
        }
        filterType.set(enhancement.GetFilterType().ordinal());
    }

    @Override
    public void SetActive(boolean active)
    {
        if(isActive != active)
        {
            if(!active)
            {
                UpdateEnhancement();
            }
            isActive = active;
        }

        OnDataRefresh();
    }

    @Override
    public void OnDataRefresh()
    {
        boolean filterSlotsActive = filterType.get() == Constants.eFilterType.Type.ordinal();
        for (int i = 0; i < filter.getContainerSize(); ++i)
        {
            Slot slot = inventorySlots.get(i);
            if (slot instanceof IActivatableSlot)
            {
                ((IActivatableSlot) slot).SetActive(isActive && filterSlotsActive);
            }
        }

        boolean tagFilterActive = filterType.get() == Constants.eFilterType.Tag.ordinal();
        GetTagFetchHelperSlot().SetActive(isActive && tagFilterActive);
    }

    @Override
    public void HandlePropertyUpdate(int propertyId, int value)
    {
        switch (propertyId)
        {
            case PRIORITY_PROPERTY_ID -> priority.set(value);
            case FILTER_TYPE_PROPERTY_ID -> filterType.set(value);
            case POLYMORPHIC_PROPERTY_ID -> DoPolymorphicFilterSet();
        }
        OnDataRefresh();
    }

    @Override
    public void HandleStringPropertyUpdate(int propertyId, String value)
    {
        switch (propertyId)
        {
            case FILTER_TAGS_STRING_PROPERTY_ID -> tagFilters.set(value);
        }
    }

    @Override
    public void OnClose()
    {
        super.OnClose();
        UpdateEnhancement();
    }

    private void UpdateEnhancement()
    {
        if(enhancement != null)
        {
            enhancement.SetPriority(priority.get());
            for(int i = 0; i < filter.getContainerSize(); ++i)
            {
                enhancement.GetFilter().setItem(i, filter.getItem(i));
            }
            enhancement.SetTagFilters( GetFilterTags() );
            enhancement.SetFilterType(Constants.eFilterType.values()[filterType.get()]);
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

        HashSet<Item> currentFilteredItems = new HashSet<>();
        for(int i = 0; i < filter.getContainerSize(); ++i)
        {
            ItemStack filteredItem = filter.getItem(i);
            if(filteredItem.isEmpty())
            {
                continue;
            }
            currentFilteredItems.add(filteredItem.getItem());
        }

        for(int i = 0; i < inv.GetSimulatedInv().getSlots(); ++i)
        {
            ItemStack stack = inv.GetSimulatedInv().getStackInSlot(i);
            if(stack.isEmpty() || currentFilteredItems.contains(stack.getItem()))
            {
                continue;
            }
            //need to add it
            for(Slot slot : inventorySlots)
            {
                if(slot.container != filter || slot.hasItem())
                {
                    continue;
                }
                slot.set(stack);
                currentFilteredItems.add(stack.getItem());
                break;
            }
        }
    }

    public BlockState GetBlockState() { return blockState; }

    public int GetPriority() { return priority.get(); }
    public void SetPriority(int prio) { priority.set(prio); }

    public FakeSlot GetTagFetchHelperSlot()
    {
        return (FakeSlot)inventorySlots.get(filter.getContainerSize());
    }

    public FakeInventory GetFilterInventory() { return filter; }
    public ArrayList<String> GetFilterTags()
    {
        return tagFilters.getArray();
    }

    public String SetFilterTags(ArrayList<String> tags)
    {
        tagFilters.setArray(tags);
        return tagFilters.get();
    }

    public Constants.eFilterType GetFilterType()
    {
        return Constants.eFilterType.values()[filterType.get()];
    }

    public void SetFilterType(Constants.eFilterType filterType)
    {
        this.filterType.set(filterType.ordinal());
        //make sure to refresh this
        OnDataRefresh();
    }

    public WispInteractionNodeBase GetConnectedNode()
    {
        return parentWisp;
    }
}
