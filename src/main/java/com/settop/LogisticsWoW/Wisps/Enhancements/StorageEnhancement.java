package com.settop.LogisticsWoW.Wisps.Enhancements;

import com.settop.LogisticsWoW.GUI.SubMenus.StorageEnhancementSubMenu;
import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import com.settop.LogisticsWoW.Utils.FakeInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StorageEnhancement implements IEnhancement
{
    public static class Factory implements IEnhancement.IFactory
    {
        @Override
        public IEnhancement Create()
        {
            return new StorageEnhancement();
        }

        @Override
        public SubMenu CreateSubMenu(int xPos, int yPos, BlockState blockState, BlockEntity blockEntity) { return new StorageEnhancementSubMenu(xPos, yPos, blockState, blockEntity); }
    }

    public enum eFilterType
    {
        Item,
        Tag
    }

    public static final Factory FACTORY = new Factory();
    public static final int FILTER_NUM_COLUMNS = 8;
    public static final int FILTER_NUM_ROWS = 4;
    public static final int FILTER_SIZE = FILTER_NUM_COLUMNS * FILTER_NUM_ROWS;

    private boolean isDefaultStore = false;
    private int priority = 0;
    private final FakeInventory filter = new FakeInventory(FILTER_SIZE, false);
    private ArrayList<ResourceLocation> tagFilter;
    private eFilterType filterType = eFilterType.Item;

    @Override
    public CompoundTag SerializeNBT()
    {
        CompoundTag nbt = new CompoundTag();

        nbt.putBoolean("isDefaultStore", isDefaultStore);
        nbt.putInt("priority", priority);

        if(!filter.isEmpty())
        {
            CompoundTag filterNBT = new CompoundTag();
            filter.Save(filterNBT);
            nbt.put("filter", filterNBT);
        }

        if(tagFilter != null)
        {
            ListTag tagFilterListNBT = new ListTag();

            for(ResourceLocation tag : tagFilter)
            {
                tagFilterListNBT.add(StringTag.valueOf(tag.toString()));
            }

            nbt.put("tagFilter", tagFilterListNBT);
        }

        if(filterType != eFilterType.Item)
        {
            nbt.putInt("filterType", filterType.ordinal());
        }

        return nbt;
    }

    @Override
    public void DeserializeNBT(CompoundTag nbt)
    {
        if (nbt == null)
        {
            return;
        }
        if (nbt.contains("isDefaultStore"))
        {
            isDefaultStore = nbt.getBoolean("isDefaultStore");
        }
        if (nbt.contains("priority"))
        {
            priority = nbt.getInt("priority");
        }

        if (nbt.contains("filter"))
        {
            CompoundTag filterNBT = nbt.getCompound("filter");
            filter.Load(filterNBT);
        }

        if (nbt.contains("tagFilter"))
        {
            tagFilter = new ArrayList<>();
            ListTag tagFilterListNBT = nbt.getList("tagFilter", 8);
            for(Tag tag : tagFilterListNBT)
            {
                StringTag stringNBT = (StringTag)tag;
                tagFilter.add(ResourceLocation.of(stringNBT.getAsString(), ':'));
            }
        }

        if(nbt.contains("filterType"))
        {
            int type = nbt.getInt("filterType");
            if(type >= 0 && type < eFilterType.values().length)
            {
                filterType = eFilterType.values()[type];
            }
        }
    }

    @Override
    public EnhancementTypes GetType()
    {
        return EnhancementTypes.STORAGE;
    }

    @Override
    public void AddTooltip(@NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn)
    {
        tooltip.add(new TranslatableComponent("logwow.priority").append(String.format(": %d", priority)));
        if(isDefaultStore)
        {
            tooltip.add(new TranslatableComponent("logwow.default_store"));
        }
        else
        {
            switch (filterType)
            {
                case Item:
                {
                    tooltip.add(new TranslatableComponent("logwow.item_filter").append(":"));
                    for(int i = 0; i < filter.getContainerSize(); ++i)
                    {
                        ItemStack item = filter.getItem(i);
                        if(!item.isEmpty())
                        {
                            tooltip.add(new TextComponent(" - ").append(item.getDisplayName()));
                        }
                    }
                }
                break;
                case Tag:
                {
                    tooltip.add(new TranslatableComponent("logwow.tag_filter"));
                    for(ResourceLocation tag : tagFilter)
                    {
                        tooltip.add(new TextComponent(" - ").append(new TextComponent(tag.toString())));
                    }
                }
                break;
            }
        }

    }

    public boolean IsDefaultStore() { return isDefaultStore; }
    public void SetIsDefaultStore( boolean enabled ) { isDefaultStore = enabled; }

    public int GetPriority() { return priority; }
    public void SetPriority(int prio) { priority = prio; }

    public FakeInventory GetFilter() { return filter; }

    public ArrayList<ResourceLocation> GetTagFilters() { return tagFilter; }
    public void SetTagFilters(ArrayList<String> tags)
    {
        tagFilter = null;
        if(tags != null && !tags.isEmpty())
        {
            tagFilter = new ArrayList<>();
            for (String tag : tags)
            {
                if(ResourceLocation.isValidResourceLocation(tag))
                {
                    tagFilter.add(ResourceLocation.of(tag, ':'));
                }
            }
        }
    }

    public eFilterType GetFilterType() { return filterType; }
    public void SetFilterType(eFilterType filterType) { this.filterType = filterType; }
}
