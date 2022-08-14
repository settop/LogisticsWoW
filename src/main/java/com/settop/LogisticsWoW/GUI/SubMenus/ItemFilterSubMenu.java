package com.settop.LogisticsWoW.GUI.SubMenus;

import com.settop.LogisticsWoW.Client.Client;
import com.settop.LogisticsWoW.GUI.FakeSlot;
import com.settop.LogisticsWoW.GUI.IActivatableSlot;
import com.settop.LogisticsWoW.Utils.Constants;
import com.settop.LogisticsWoW.Utils.FakeInventory;
import com.settop.LogisticsWoW.Utils.ItemFilter;
import com.settop.LogisticsWoW.Utils.StringVariableArrayReferenceHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class ItemFilterSubMenu extends SubMenu
{
    private final ItemFilter sourceFilter;
    private final FakeInventory filterInv;
    private final StringVariableArrayReferenceHolder tagFilters = new StringVariableArrayReferenceHolder(';');
    private final DataSlot isWhitelist = DataSlot.standalone();
    private final DataSlot filterType = DataSlot.standalone();
    private final DataSlot matchNBT = DataSlot.standalone();

    private final FakeInventory tagGetHelper = new FakeInventory( 1, false );

    public final boolean hideWhitelist;
    public final boolean hideMatchNBT;
    public final boolean hideDefaultFilter;

    public static final int FILTER_TYPE_PROPERTY_ID = 0;
    public static final int WHITELIST_PROPERTY_ID = 1;
    public static final int MATCH_NBT_PROPERTY_ID = 2;
    public static final int NEXT_PROPERTY_ID = 3;
    //not real properties, just used to trigger things
    public static final int POLYMORPHIC_PROPERTY_ID = 100;

    public static final int FILTER_TAGS_STRING_PROPERTY_ID = 0;

    public static final int FILTER_SLOT_X = 21;
    public static final int FILTER_SLOT_Y = 1;

    public static final int TAG_FETCH_HELPER_SLOT_X = 1;
    public static final int TAG_FETCH_HELPER_SLOT_Y = 64;

    public ItemFilterSubMenu(ItemFilter filter, int xPos, int yPos, boolean hideWhitelist, boolean hideMatchNBT, boolean hideDefaultFilter)
    {
        super(xPos, yPos);
        sourceFilter = filter;
        addDataSlot(filterType);
        addDataSlot(isWhitelist);
        addDataSlot(matchNBT);
        trackStr(tagFilters);
        this.hideWhitelist = hideWhitelist;
        this.hideMatchNBT = hideMatchNBT;
        this.hideDefaultFilter = hideDefaultFilter;

        filterInv = new FakeInventory( ItemFilter.FILTER_SIZE, filter.GetFilter().includeCounts );

        for(int i = 0; i < filterInv.getContainerSize(); ++i)
        {
            int column = i % ItemFilter.FILTER_NUM_COLUMNS;
            int row = i / ItemFilter.FILTER_NUM_COLUMNS;
            inventorySlots.add( new FakeSlot(filterInv, i, xPos + FILTER_SLOT_X + column * Client.SLOT_X_SPACING, yPos + FILTER_SLOT_Y + row * Client.SLOT_Y_SPACING));
        }
        inventorySlots.add( new FakeSlot(tagGetHelper, 0, xPos + TAG_FETCH_HELPER_SLOT_X, yPos + TAG_FETCH_HELPER_SLOT_Y));

        for(int i = 0; i < filterInv.getContainerSize(); ++i)
        {
            filterInv.setItem(i, filter.GetFilter().getItem(i));
        }
        if(filter.GetTagFilters() != null)
        {
            ArrayList<String> tags = new ArrayList<>();
            for (ResourceLocation tag : filter.GetTagFilters())
            {
                tags.add(tag.toString());
            }
            tagFilters.setArray(tags);
        }
        filterType.set(filter.GetFilterType().ordinal());
        isWhitelist.set(filter.GetIsWhiteList() ? 1 : 0);
        matchNBT.set(filter.GetMatchNBT() ? 1 : 0);
    }

    public FakeSlot GetTagFetchHelperSlot()
    {
        return (FakeSlot)inventorySlots.get(filterInv.getContainerSize());
    }

    @Override
    public void OnDataRefresh()
    {
        boolean filterSlotsActive = filterType.get() == Constants.eFilterType.Type.ordinal();
        for (int i = 0; i < filterInv.getContainerSize(); ++i)
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
    public void SetActive(boolean active)
    {
        if(isActive != active)
        {
            if(!active)
            {
                WriteDataToSource();
            }
            isActive = active;
        }

        OnDataRefresh();
    }

    @Override
    public void HandlePropertyUpdate(int propertyId, int value)
    {
        switch (propertyId)
        {
            case FILTER_TYPE_PROPERTY_ID -> filterType.set(value);
            case WHITELIST_PROPERTY_ID -> isWhitelist.set(value);
            case MATCH_NBT_PROPERTY_ID -> matchNBT.set(value);
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
        WriteDataToSource();
    }

    protected void WriteDataToSource()
    {
        sourceFilter.SetFilterType(Constants.eFilterType.values()[filterType.get()]);
        sourceFilter.SetIsWhitelist(isWhitelist.get() == 1);
        sourceFilter.SetMatchNBT(matchNBT.get() == 1);
        for(int i = 0; i < filterInv.getContainerSize(); ++i)
        {
            sourceFilter.GetFilter().setItem(i, filterInv.getItem(i));
        }
        sourceFilter.SetTagFilters( GetFilterTags() );
    }

    protected void DoPolymorphicFilterSet(IItemHandler sourceInv)
    {
        HashSet<Item> currentFilteredItems = new HashSet<>();
        for(int i = 0; i < filterInv.getContainerSize(); ++i)
        {
            ItemStack filteredItem = filterInv.getItem(i);
            if(filteredItem.isEmpty())
            {
                continue;
            }
            currentFilteredItems.add(filteredItem.getItem());
        }

        for(int i = 0; i < sourceInv.getSlots(); ++i)
        {
            ItemStack stack = sourceInv.getStackInSlot(i);
            if(stack.isEmpty() || currentFilteredItems.contains(stack.getItem()))
            {
                continue;
            }
            //need to add it
            for(Slot slot : inventorySlots)
            {
                if(slot.container != filterInv || slot.hasItem())
                {
                    continue;
                }
                slot.set(stack);
                currentFilteredItems.add(stack.getItem());
                break;
            }
        }
    }

    public FakeInventory GetFilterInventory() { return filterInv; }
    public ArrayList<String> GetFilterTags()
    {
        return tagFilters.getArray();
    }

    public String SetFilterTags(ArrayList<String> tags)
    {
        tagFilters.setArray(tags);
        return tagFilters.get();
    }

    public boolean GetIsWhitelist() { return isWhitelist.get() == 1; }
    public void SetWhitelist(boolean whitelist)
    {
        isWhitelist.set(whitelist ? 1 : 0);
        OnDataRefresh();
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

    public boolean GetMatchNBT() { return matchNBT.get() == 1; }
    public void SetMatchNBT(boolean match)
    {
        matchNBT.set(match ? 1 : 0);
        OnDataRefresh();
    }
}
