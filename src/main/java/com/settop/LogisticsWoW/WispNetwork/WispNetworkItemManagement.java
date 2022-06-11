package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.Constants;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.tags.ITag;

import javax.annotation.Nonnull;
import java.util.*;

public class WispNetworkItemManagement
{
    private static class ItemSourceCollection
    {
        private int countCache = 0;
        private boolean craftable = false;

        private static class CachedItemSource
        {
            public ItemSource source;
            public int sourceCountCache = 0;
        }
        private final ArrayList<CachedItemSource> itemSources = new ArrayList<>();
    }

    private static class ItemSinkCollection
    {
        private final ArrayList<ItemSink> itemSink = new ArrayList<>();

        void add(ItemSink sink)
        {
            itemSink.add(sink);
            itemSink.sort((l, r)->r.priority - l.priority);
        }

        public ItemSink.Reservation ReserveSpaceInBestSink(ItemStack stack, int minPriority)
        {
            for(Iterator<ItemSink> it = itemSink.iterator(); it.hasNext();)
            {
                ItemSink sink = it.next();
                if(!sink.IsValid())
                {
                    it.remove();
                    continue;
                }
                if(sink.priority <= minPriority)
                {
                    //since this is sorted all other sinks will be equal or lower priority than this one
                    break;
                }
                ItemSink.Reservation reservation = sink.ReserveInsert(stack);
                if(reservation != null)
                {
                    return reservation;
                }
            }
            return null;
        }
    }

    private boolean itemSourceCollectionsDirty = false;
    private final HashMap<Item, ItemSourceCollection> itemSourceCollections = new HashMap<>();

    private final HashMap<Item, ItemSinkCollection> itemSinkCollectionItem = new HashMap<>();
    //private final HashMap<ResourceLocation, ItemSinkCollection> itemSinkCollectionTag = new HashMap<>();
    private final ItemSinkCollection itemSinkCollectionDefault = new ItemSinkCollection();

    public void Tick()
    {
        if(itemSourceCollectionsDirty)
        {
            itemSourceCollectionsDirty = false;
            itemSourceCollections.forEach((item, sourceCollection)->
            {
                for(int i = 0; i < sourceCollection.itemSources.size();)
                {
                    ItemSourceCollection.CachedItemSource cachedItemSource = sourceCollection.itemSources.get(i);
                    int newCount = 0;
                    if(cachedItemSource.source.IsValid())
                    {
                        newCount = cachedItemSource.source.GetNumAvailable();
                    }
                    int countChange = newCount - cachedItemSource.sourceCountCache;
                    cachedItemSource.sourceCountCache = newCount;
                    sourceCollection.countCache += countChange;
                    if(sourceCollection.countCache < 0)
                    {
                        sourceCollection.countCache = 0;
                        LogisticsWoW.LOGGER.error("Count cache for item went below 0");
                    }
                    if(!cachedItemSource.source.IsValid())
                    {
                        //remove it
                        //move the one at the back into this position and pop the back
                        sourceCollection.itemSources.set(i, sourceCollection.itemSources.get(sourceCollection.itemSources.size() - 1));
                        sourceCollection.itemSources.remove(sourceCollection.itemSources.size() - 1);
                    }
                    else
                    {
                        ++i;
                    }
                }
            });
        }
    }

    public void AddItemSources(HashMap<Item, ? extends ItemSource> sources)
    {
        itemSourceCollectionsDirty = true;
        sources.forEach((item, source)->
        {
            ItemSourceCollection collection = itemSourceCollections.computeIfAbsent(item, (key)->new ItemSourceCollection());
            for(ItemSourceCollection.CachedItemSource cachedItemSource : collection.itemSources)
            {
                if(cachedItemSource.source == source)
                {
                    //already registered
                    return;
                }
            }
            ItemSourceCollection.CachedItemSource cachedItemSource = new ItemSourceCollection.CachedItemSource();
            cachedItemSource.source = source;

            collection.itemSources.add(cachedItemSource);
        });
    }

    public void AddItemSink(@Nonnull ItemSink sink, Constants.eFilterType filterType, @Nonnull HashSet<Item> itemFilter, @Nonnull ArrayList<ITag<Item>> tagFilter)
    {
        switch (filterType)
        {
            case Item ->
            {
                if(itemFilter.isEmpty())
                {
                    AddItemSink(sink, Constants.eFilterType.Default, itemFilter, tagFilter);
                    return;
                }
                for(Item filterItem : itemFilter)
                {
                    ItemSinkCollection sinkCollection = itemSinkCollectionItem.computeIfAbsent(filterItem, key->new ItemSinkCollection());
                    sinkCollection.add(sink);
                }
            }
            case Tag ->
            {
                if(tagFilter.isEmpty())
                {
                    AddItemSink(sink, Constants.eFilterType.Default, itemFilter, tagFilter);
                    return;
                }
                //assume that the tags provided are valid
                for(ITag<Item> tag : tagFilter)
                {
                    //ItemSinkCollection sinkCollection = itemSinkCollectionTag.computeIfAbsent(tag, key -> new ItemSinkCollection());
                    //sinkCollection.add(sink);

                    tag.stream().forEach(taggedItem->
                    {
                        ItemSinkCollection itemSinkCollection = itemSinkCollectionItem.computeIfAbsent(taggedItem, key->new ItemSinkCollection());
                        itemSinkCollection.add(sink);
                    });
                }
            }
            case Default ->
            {
                itemSinkCollectionDefault.add(sink);
            }
        }
    }


    public ItemSink.Reservation  ReserveSpaceInBestSink(ItemStack stack)
    {
        return ReserveSpaceInBestSink(stack, Integer.MIN_VALUE, Integer.MIN_VALUE, true);
    }
    public ItemSink.Reservation ReserveSpaceInBestSink(ItemStack stack, int minPriority, int minDefaultPriority, boolean allowDefault)
    {
        //Maybe this should return an array of all valid item sinks at the highest priority?
        ItemSinkCollection itemCollection = itemSinkCollectionItem.get(stack.getItem());
        if(itemCollection != null)
        {
            ItemSink.Reservation reservedSink = itemCollection.ReserveSpaceInBestSink(stack, minPriority);
            if(reservedSink != null)
            {
                return reservedSink;
            }
        }
        if(allowDefault)
        {
            return itemSinkCollectionDefault.ReserveSpaceInBestSink(stack, minDefaultPriority);
        }
        return null;
    }
}
