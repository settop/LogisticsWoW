package com.settop.LogisticsWoW.GUI.SubMenus;

import com.settop.LogisticsWoW.Client.Client;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.ProviderSubScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.SubScreen;
import com.settop.LogisticsWoW.GUI.FakeSlot;
import com.settop.LogisticsWoW.GUI.IActivatableSlot;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.BoolArray;
import com.settop.LogisticsWoW.Utils.FakeInventory;
import com.settop.LogisticsWoW.Utils.StringVariableArrayReferenceHolder;
import com.settop.LogisticsWoW.Wisps.Enhancements.IEnhancement;
import com.settop.LogisticsWoW.Wisps.Enhancements.ProviderEnhancement;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class ProviderEnhancementSubMenu extends SubMenu implements IEnhancementSubMenu
{
    private ProviderEnhancement currentEnhancement;
    private BoolArray directionsValue = new BoolArray(6);
    private DataSlot whiteListEnabled = DataSlot.standalone();
    private BlockState blockState;
    private FakeInventory filter = new FakeInventory( ProviderEnhancement.FILTER_SIZE, false );
    private StringVariableArrayReferenceHolder tagFilters = new StringVariableArrayReferenceHolder(';');
    private DataSlot filterType = DataSlot.standalone();

    private FakeInventory tagGetHelper = new FakeInventory( 1, false );

    public static final int WHITELIST_PROPERTY_ID = 0;
    public static final int FILTER_TYPE_PROPERTY_ID = 1;

    public static final int FILTER_TAGS_STRING_PROPERTY_ID = 0;

    public static final int FILTER_SLOT_X = 21;
    public static final int FILTER_SLOT_Y = 1;

    public static final int TAG_FETCH_HELPER_SLOT_X = 1;
    public static final int TAG_FETCH_HELPER_SLOT_Y = 64;

    public ProviderEnhancementSubMenu(int xPos, int yPos, BlockState blockState, BlockEntity blockEntity)
    {
        super(xPos, yPos);
        addDataSlots(directionsValue);
        addDataSlot(whiteListEnabled);
        addDataSlot(filterType);
        trackStr(tagFilters);
        this.blockState = blockState;

        for(int i = 0; i < filter.getContainerSize(); ++i)
        {
            int column = i % ProviderEnhancement.FILTER_NUM_COLUMNS;
            int row = i / ProviderEnhancement.FILTER_NUM_COLUMNS;
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

        boolean filterSlotsActive = filterType.get() == ProviderEnhancement.eFilterType.Item.ordinal();
        for (int i = 0; i < filter.getContainerSize(); ++i)
        {
            Slot slot = inventorySlots.get(i);
            if (slot instanceof IActivatableSlot)
            {
                ((IActivatableSlot) slot).SetActive(active && filterSlotsActive);
            }
        }
        GetTagFetchHelperSlot().SetActive(active && !filterSlotsActive);
    }

    @Override
    public void HandlePropertyUpdate(int propertyId, int value)
    {
        switch(propertyId)
        {
            case WHITELIST_PROPERTY_ID:
                whiteListEnabled.set(value);
                break;
            case FILTER_TYPE_PROPERTY_ID:
                filterType.set(value);
                break;
        }
    }

    @Override
    public void HandleStringPropertyUpdate(int propertyId, String value)
    {
        switch (propertyId)
        {
            case FILTER_TAGS_STRING_PROPERTY_ID:
                tagFilters.set(value);
                break;
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
            for(int i = 0; i < 6; ++i)
            {
                currentEnhancement.SetDirectionProvided(Direction.from3DDataValue(i), directionsValue.GetBool(i));
            }
            currentEnhancement.SetWhitelistEnabled(whiteListEnabled.get() != 0);
            for(int i = 0; i < filter.getContainerSize(); ++i)
            {
                currentEnhancement.GetFilter().setItem(i, filter.getItem(i));
            }
            currentEnhancement.SetTagFilters( GetFilterTags() );
            currentEnhancement.SetFilterType(ProviderEnhancement.eFilterType.values()[filterType.get()]);
        }
    }

    @Override
    public SubScreen CreateScreen(MultiScreen<?> parentScreen)
    {
        return new ProviderSubScreen(this, parentScreen);
    }

    @Override
    public void SetEnhancement(IEnhancement enhancement)
    {
        if(enhancement != null)
        {
            if(enhancement instanceof ProviderEnhancement)
            {
                currentEnhancement = (ProviderEnhancement)enhancement;
                for(int i = 0; i < 6; ++i)
                {
                    directionsValue.SetBool(i, currentEnhancement.IsDirectionSet(Direction.from3DDataValue(i)));
                }

                whiteListEnabled.set(currentEnhancement.IsWhitelistEnabled() ? 1 : 0);
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

    public void SetDirectionProvided(Direction direction, boolean isSet)
    {
        directionsValue.SetBool( direction.ordinal(), isSet );
    }

    public BoolArray GetDirectionsProvided()
    {
        return directionsValue;
    }
    public BlockState GetBlockState() { return blockState; }

    public boolean GetWhitelistEnabled()
    {
        return whiteListEnabled.get() != 0;
    }
    public void SetWhitelistEnabled(boolean enabled)
    {
        whiteListEnabled.set(enabled ? 1 : 0);
    }

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

    public ProviderEnhancement.eFilterType GetFilterType()
    {
        return ProviderEnhancement.eFilterType.values()[filterType.get()];
    }

    public void SetFilterType(ProviderEnhancement.eFilterType filterType)
    {
        this.filterType.set(filterType.ordinal());
        //make sure to refresh this
        SetActive(isActive);
    }
}
