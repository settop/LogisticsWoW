package com.settop.LogisticsWoW.GUI.SubMenus;

import com.settop.LogisticsWoW.Client.Client;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.StorageSubScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.SubScreen;
import com.settop.LogisticsWoW.GUI.FakeSlot;
import com.settop.LogisticsWoW.GUI.IActivatableSlot;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.FakeInventory;
import com.settop.LogisticsWoW.Utils.StringVariableArrayReferenceHolder;
import com.settop.LogisticsWoW.Wisps.Enhancements.IEnhancement;
import com.settop.LogisticsWoW.Wisps.Enhancements.StorageEnhancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class StorageEnhancementSubMenu extends SubMenu implements IEnhancementSubMenu
{
    private StorageEnhancement currentEnhancement;
    private final DataSlot isDefaultStore = DataSlot.standalone();
    private final DataSlot priority = DataSlot.standalone();
    private final BlockState blockState;
    private final FakeInventory filter = new FakeInventory( StorageEnhancement.FILTER_SIZE, false );
    private final StringVariableArrayReferenceHolder tagFilters = new StringVariableArrayReferenceHolder(';');
    private final DataSlot filterType = DataSlot.standalone();

    private FakeInventory tagGetHelper = new FakeInventory( 1, false );

    public static final int IS_DEFAULT_STORE_PROPERTY_ID = 0;
    public static final int PRIORITY_PROPERTY_ID = 1;
    public static final int FILTER_TYPE_PROPERTY_ID = 2;

    public static final int FILTER_TAGS_STRING_PROPERTY_ID = 0;

    public static final int FILTER_SLOT_X = 21;
    public static final int FILTER_SLOT_Y = 1;

    public static final int TAG_FETCH_HELPER_SLOT_X = 1;
    public static final int TAG_FETCH_HELPER_SLOT_Y = 64;

    public StorageEnhancementSubMenu(int xPos, int yPos, BlockState blockState, BlockEntity blockEntity)
    {
        super(xPos, yPos);
        addDataSlot(isDefaultStore);
        addDataSlot(priority);
        addDataSlot(filterType);
        trackStr(tagFilters);
        this.blockState = blockState;

        for(int i = 0; i < filter.getContainerSize(); ++i)
        {
            int column = i % StorageEnhancement.FILTER_NUM_COLUMNS;
            int row = i / StorageEnhancement.FILTER_NUM_COLUMNS;
            inventorySlots.add( new FakeSlot(filter, i, xPos + FILTER_SLOT_X + column * Client.SLOT_X_SPACING, yPos + FILTER_SLOT_Y + row * Client.SLOT_Y_SPACING));
        }
        inventorySlots.add( new FakeSlot(tagGetHelper, 0, xPos + TAG_FETCH_HELPER_SLOT_X, yPos + TAG_FETCH_HELPER_SLOT_Y));
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

        boolean filterSlotsActive = filterType.get() == StorageEnhancement.eFilterType.Item.ordinal();
        for (int i = 0; i < filter.getContainerSize(); ++i)
        {
            Slot slot = inventorySlots.get(i);
            if (slot instanceof IActivatableSlot)
            {
                ((IActivatableSlot) slot).SetActive(active && filterSlotsActive && !GetIsDefaultStore());
            }
        }
        GetTagFetchHelperSlot().SetActive(active && !filterSlotsActive && !GetIsDefaultStore());
    }

    @Override
    public void HandlePropertyUpdate(int propertyId, int value)
    {
        switch (propertyId)
        {
            case IS_DEFAULT_STORE_PROPERTY_ID -> isDefaultStore.set(value);
            case PRIORITY_PROPERTY_ID -> priority.set(value);
            case FILTER_TYPE_PROPERTY_ID -> filterType.set(value);
        }
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
        currentEnhancement = null;
    }

    private void UpdateEnhancement()
    {
        if(currentEnhancement != null)
        {
            currentEnhancement.SetIsDefaultStore(isDefaultStore.get() != 0);
            currentEnhancement.SetPriority(priority.get());
            for(int i = 0; i < filter.getContainerSize(); ++i)
            {
                currentEnhancement.GetFilter().setItem(i, filter.getItem(i));
            }
            currentEnhancement.SetTagFilters( GetFilterTags() );
            currentEnhancement.SetFilterType(StorageEnhancement.eFilterType.values()[filterType.get()]);
        }
    }

    @Override
    public SubScreen CreateScreen(MultiScreen<?> parentScreen)
    {
        return new StorageSubScreen(this, parentScreen);
    }

    @Override
    public void SetEnhancement(IEnhancement enhancement)
    {
        if(enhancement != null)
        {
            if(enhancement instanceof StorageEnhancement)
            {
                currentEnhancement = (StorageEnhancement)enhancement;

                isDefaultStore.set(currentEnhancement.IsDefaultStore() ? 1 : 0);
                priority.set(currentEnhancement.GetPriority());
                for(int i = 0; i < filter.getContainerSize(); ++i)
                {
                    filter.setItem(i, currentEnhancement.GetFilter().getItem(i));
                }
                if(currentEnhancement.GetTagFilters() != null)
                {
                    ArrayList<String> tags = new ArrayList<>();
                    for (ResourceLocation tag : currentEnhancement.GetTagFilters())
                    {
                        tags.add(tag.toString());
                    }
                    tagFilters.setArray(tags);
                }
                filterType.set(currentEnhancement.GetFilterType().ordinal());
            }
            else
            {
                LogisticsWoW.LOGGER.warn("Setting a non-provider enhancement to provider enhancement sub container");
            }
        }
        else
        {
            currentEnhancement = null;
            filter.clearContent();
        }
    }


    public BlockState GetBlockState() { return blockState; }

    public boolean GetIsDefaultStore() { return isDefaultStore.get() != 0; }
    public void SetIsDefaultStore(boolean enabled)
    {
        isDefaultStore.set(enabled ? 1 : 0);
        //make sure to refresh this
        SetActive(isActive);
    }

    public int GetPriority() { return priority.get(); }
    public void SetPriority(int prio) { priority.set(prio); }

    public FakeSlot GetTagFetchHelperSlot()
    {
        return (FakeSlot)inventorySlots.get(filter.getContainerSize());
    }

    public ArrayList<String> GetFilterTags()
    {
        return tagFilters.getArray();
    }

    public String SetFilterTags(ArrayList<String> tags)
    {
        tagFilters.setArray(tags);
        return tagFilters.get();
    }

    public StorageEnhancement.eFilterType GetFilterType()
    {
        return StorageEnhancement.eFilterType.values()[filterType.get()];
    }

    public void SetFilterType(StorageEnhancement.eFilterType filterType)
    {
        this.filterType.set(filterType.ordinal());
        //make sure to refresh this
        SetActive(isActive);
    }
}
