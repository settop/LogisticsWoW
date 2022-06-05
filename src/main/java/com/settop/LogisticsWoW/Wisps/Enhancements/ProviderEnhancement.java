package com.settop.LogisticsWoW.Wisps.Enhancements;

import com.settop.LogisticsWoW.GUI.SubMenus.ProviderEnhancementSubMenu;
import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import com.settop.LogisticsWoW.Utils.FakeInventory;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

public class ProviderEnhancement implements IEnhancement
{
    public static class Factory implements IEnhancement.IFactory
    {
        @Override
        public IEnhancement Create()
        {
            return new ProviderEnhancement();
        }

        @Override
        public SubMenu CreateSubMenu(int xPos, int yPos, BlockState blockState, BlockEntity blockEntity) { return new ProviderEnhancementSubMenu(xPos, yPos, blockState, blockEntity); }
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

    private boolean providedDirections[];
    private boolean whitelistEnabled = false;
    private FakeInventory filter = new FakeInventory(FILTER_SIZE, false);
    private ArrayList<ResourceLocation> tagFilter;
    private eFilterType filterType = eFilterType.Item;

    @Override
    public CompoundTag SerializeNBT()
    {
        CompoundTag nbt = new CompoundTag();

        if(providedDirections != null)
        {
            boolean anyFalse = false;

            for (boolean b : providedDirections)
            {
                if (!b)
                {
                    anyFalse = true;
                    break;
                }
            }

            if (anyFalse)
            {
                CompoundTag directionsNBT = new CompoundTag();
                for (int i = 0; i < 6; ++i)
                {
                    directionsNBT.putBoolean(Direction.from3DDataValue(i).getSerializedName(), providedDirections[i]);
                }

                nbt.put("providerDirections", directionsNBT);
            }
        }

        if(whitelistEnabled != false)
        {
            nbt.putBoolean("whitelistEnabled", whitelistEnabled);
        }

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
        if (nbt.contains("providerDirections"))
        {
            providedDirections = new boolean[6];
            CompoundTag directionsNBT = nbt.getCompound("providerDirections");

            for(int i = 0; i < 6; ++i)
            {
                providedDirections[i] = directionsNBT.getBoolean(Direction.from3DDataValue(i).getSerializedName());
            }
        }
        if (nbt.contains("whitelistEnabled"))
        {
            whitelistEnabled = nbt.getBoolean("whitelistEnabled");
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
        return EnhancementTypes.PROVIDER;
    }

    @Override
    public void AddTooltip(@NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn)
    {
        tooltip.add(new TranslatableComponent(whitelistEnabled ? "logwow.whitelist" : "logwow.blacklist"));
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

    public boolean IsDirectionSet(Direction dir)
    {
        return providedDirections == null || providedDirections[dir.get3DDataValue()];
    }

    public void SetDirectionProvided(Direction dir, boolean isProvided)
    {
        if(providedDirections == null)
        {
            providedDirections = new boolean[6];
            for(int i = 0; i < 6; ++i)
            {
                providedDirections[i] = true;
            }
        }
        providedDirections[dir.get3DDataValue()] = isProvided;
    }

    public boolean IsWhitelistEnabled() { return whitelistEnabled; }
    public void SetWhitelistEnabled( boolean enabled ) { whitelistEnabled = enabled; }

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
