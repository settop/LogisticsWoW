package com.settop.LogisticsWoW.Utils;

import com.settop.LogisticsWoW.WispNetwork.StoreableResourceMatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;
import net.minecraftforge.registries.tags.ITagManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ItemFilter
{
    public static final int FILTER_NUM_COLUMNS = 8;
    public static final int FILTER_NUM_ROWS = 4;
    public static final int FILTER_SIZE = FILTER_NUM_COLUMNS * FILTER_NUM_ROWS;

    //modifiable data
    private Constants.eFilterType filterType = Constants.eFilterType.Type;
    private boolean isWhiteList = true;
    private boolean matchNBT = false;
    private final FakeInventory filter;
    private final ArrayList<ResourceLocation> tagFilter = new ArrayList<>();

    public ItemFilter(boolean includeCounts)
    {
        filter = new FakeInventory(FILTER_SIZE, includeCounts)
        {
            @Override
            public void setChanged()
            {
                super.setChanged();
                cacheDirty = true;
            }
        };
    }

    public ItemFilter()
    {
        this(false);
    }

    //cached data
    private boolean cacheDirty = false;
    private Constants.eFilterType effectiveFilterType = Constants.eFilterType.Type;
    private final HashMap<Item, ArrayList<ItemStack>> filteredItems = new HashMap<>();
    private final ArrayList<ITag<Item>> filteredTags = new ArrayList<>();

    public void SetFilterType(Constants.eFilterType filterType)
    {
        cacheDirty = true;
        this.filterType = filterType;
    }
    public Constants.eFilterType GetFilterType() { return filterType; }
    public Constants.eFilterType GetEffectiveFilterType()
    {
        if(cacheDirty)
        {
            UpdateCache();
        }
        return effectiveFilterType;
    }

    public void SetIsWhitelist(boolean isWhiteList)
    {
        cacheDirty = true;
        this.isWhiteList = isWhiteList;
    }
    public boolean GetIsWhiteList() { return isWhiteList; }

    public void SetMatchNBT(boolean matchNBT)
    {
        cacheDirty = true;
        this.matchNBT = matchNBT;
    }
    public boolean GetMatchNBT() { return matchNBT; }

    public FakeInventory GetFilter() { return filter; }

    public void SetTagFilters(ArrayList<String> tags)
    {
        cacheDirty = true;
        tagFilter.clear();
        if(tags != null && !tags.isEmpty())
        {
            for (String tag : tags)
            {
                if(ResourceLocation.isValidResourceLocation(tag))
                {
                    tagFilter.add(ResourceLocation.of(tag, ':'));
                }
            }
        }
    }
    public ArrayList<ResourceLocation> GetTagFilters() { return tagFilter; }

    public Set<Item> GetFilteredItems()
    {
        if(cacheDirty)
        {
            UpdateCache();
        }
        return filteredItems.keySet();
    }

    public ArrayList<ITag<Item>> GetFilteredTags()
    {
        if(cacheDirty)
        {
            UpdateCache();
        }
        return filteredTags;
    }

    public boolean MatchesFilter(ItemStack item)
    {
        if(cacheDirty)
        {
            UpdateCache();
        }
        boolean matches = switch (effectiveFilterType)
                {
                    case Type ->
                            {
                                if(matchNBT)
                                {
                                    ArrayList<ItemStack> filterItems = filteredItems.get(item.getItem());
                                    if(filterItems == null)
                                    {
                                        yield false;
                                    }
                                    for(ItemStack filterItem : filterItems)
                                    {
                                        if(ItemStack.isSameItemSameTags(filterItem, item))
                                        {
                                            yield true;
                                        }
                                    }
                                    yield false;
                                }
                                else
                                {
                                    yield filteredItems.containsKey(item.getItem());
                                }
                            }
                    case Tag -> filteredTags.stream().anyMatch(tag->tag.contains(item.getItem()));
                    case Default -> false;
                };
        return isWhiteList == matches;
    }

    public boolean MatchesFilter(StoreableResourceMatcher<ItemStack> itemMatcher)
    {
        if(cacheDirty)
        {
            UpdateCache();
        }
        boolean matches = switch (effectiveFilterType)
                {
                    case Type ->
                            {
                                if(matchNBT)
                                {
                                    ArrayList<ItemStack> filterItems = filteredItems.get((Item)itemMatcher.GetType());
                                    for(ItemStack filterItem : filterItems)
                                    {
                                        if(itemMatcher.Matches(filterItem))
                                        {
                                            yield true;
                                        }
                                    }
                                    yield false;
                                }
                                else
                                {
                                    yield filteredItems.containsKey((Item)itemMatcher.GetType());
                                }
                            }
                    case Tag -> filteredTags.stream().anyMatch(tag->tag.contains((Item)itemMatcher.GetType()));
                    case Default -> false;
                };
        return isWhiteList == matches;
    }

    private void UpdateCache()
    {
        cacheDirty = false;
        effectiveFilterType = switch (filterType)
        {
            case Type ->
                    {
                        for(int i = 0; i < filter.getContainerSize(); ++i)
                        {
                            if(!filter.getItem(i).isEmpty())
                            {
                                yield Constants.eFilterType.Type;
                            }
                        }
                        yield Constants.eFilterType.Default;
                    }
            case Tag ->
                    {
                        if(tagFilter.isEmpty())
                        {
                            yield Constants.eFilterType.Default;
                        }
                        else
                        {
                            yield Constants.eFilterType.Tag;
                        }
                    }
            case Default -> Constants.eFilterType.Default;
        };

        filteredItems.clear();
        for(int i = 0 ; i < filter.getContainerSize(); ++i)
        {
            ItemStack itemStack = filter.getItem(i);
            if(itemStack.isEmpty())
            {
                continue;
            }
            if(matchNBT)
            {
                ArrayList<ItemStack> nbtItems = filteredItems.computeIfAbsent(itemStack.getItem(), (k)->new ArrayList<>());
                boolean alreadyPresent = false;
                for(ItemStack nbtTestItem : nbtItems)
                {
                    if(ItemStack.isSameItemSameTags(nbtTestItem, itemStack))
                    {
                        alreadyPresent = true;
                        break;
                    }
                }
                if(!alreadyPresent)
                {
                    nbtItems.add(itemStack);
                }
            }
            else
            {
                filteredItems.put(itemStack.getItem(), null);
            }
        }

        filteredTags.clear();
        ITagManager<Item> itemTagManager = ForgeRegistries.ITEMS.tags();
        assert itemTagManager != null;
        for(ResourceLocation tagName : tagFilter)
        {
            filteredTags.add(itemTagManager.getTag(ItemTags.create(tagName)));
        }
    }

    public CompoundTag SerializeNBT()
    {
        CompoundTag nbt = new CompoundTag();
        if(filterType != Constants.eFilterType.Type)
        {
            nbt.putString("filterType", filterType.toString());
        }

        if(!isWhiteList)
        {
            nbt.putBoolean("isWhiteList", isWhiteList);
        }

        if(matchNBT)
        {
            nbt.putBoolean("matchNBT", matchNBT);
        }

        if(!filter.isEmpty())
        {
            CompoundTag filterNBT = new CompoundTag();
            filter.Save(filterNBT);
            nbt.put("filter", filterNBT);
        }

        if(!tagFilter.isEmpty())
        {
            ListTag tagFilterListNBT = new ListTag();

            for(ResourceLocation tag : tagFilter)
            {
                tagFilterListNBT.add(StringTag.valueOf(tag.toString()));
            }

            nbt.put("tagFilter", tagFilterListNBT);
        }
        return nbt;
    }

    public void DeserializeNBT(CompoundTag nbt)
    {
        if(nbt.contains("filterType"))
        {
            filterType = Constants.eFilterType.valueOf(nbt.getString("filterType"));
        }

        if(nbt.contains("isWhiteList"))
        {
            isWhiteList = nbt.getBoolean("isWhiteList");
        }

        if(nbt.contains("matchNBT"))
        {
            matchNBT = nbt.getBoolean("matchNBT");
        }

        if (nbt.contains("filter"))
        {
            CompoundTag filterNBT = nbt.getCompound("filter");
            filter.Load(filterNBT);
        }

        if (nbt.contains("tagFilter"))
        {
            ListTag tagFilterListNBT = nbt.getList("tagFilter", 8);
            for(Tag tag : tagFilterListNBT)
            {
                StringTag stringNBT = (StringTag)tag;
                tagFilter.add(ResourceLocation.of(stringNBT.getAsString(), ':'));
            }
        }
        UpdateCache();
    }

    public void AddTooltip(@NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn)
    {
        tooltip.add(new TranslatableComponent(GetIsWhiteList() ? "logwow.whitelist" : "logwow.blacklist"));
        switch (GetEffectiveFilterType())
        {
            case Type:
            {
                tooltip.add(new TranslatableComponent("logwow.item_filter").append(":"));
                for(int i = 0; i < GetFilter().getContainerSize(); ++i)
                {
                    ItemStack item = GetFilter().getItem(i);
                    if(!item.isEmpty())
                    {
                        MutableComponent component = new TextComponent(" - ").append(item.getDisplayName());
                        if(filter.includeCounts)
                        {
                            component.append(new TextComponent("(%d)".formatted(item.getCount())));
                        }
                        tooltip.add(component);
                    }
                }
            }
            break;
            case Tag:
            {
                tooltip.add(new TranslatableComponent("logwow.tag_filter"));
                for(ResourceLocation tag : GetTagFilters())
                {
                    tooltip.add(new TextComponent(" - ").append(new TextComponent(tag.toString())));
                }
            }
            break;
            case Default:
            {
                tooltip.add(new TranslatableComponent("logwow.default_store"));
            }
        }
    }
}
