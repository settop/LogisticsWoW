package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.Constants;
import net.minecraftforge.registries.tags.ITag;

import javax.annotation.Nonnull;
import java.util.*;

public class WispNetworkResourceManagement<T>
{
    private static class ResourceSourceCollection<T>
    {
        private int countCache = 0;
        private boolean craftable = false;

        private static class ResourceItemSource<T>
        {
            public ResourceSource<T> source;
            public int sourceCountCache = 0;
        }
        private final ArrayList<ResourceItemSource<T>> resourceSources = new ArrayList<>();
    }

    private static class ResourceSinkCollection<T>
    {
        private final ArrayList<ResourceSink<T>> resourceSinks = new ArrayList<>();

        void add(ResourceSink<T> sink)
        {
            resourceSinks.add(sink);
            resourceSinks.sort((l, r)->r.priority - l.priority);
        }

        public SourcedReservation ReserveSpaceInBestSink(StorableResource<T> stack, int minPriority)
        {
            for(Iterator<ResourceSink<T>> it = resourceSinks.iterator(); it.hasNext();)
            {
                ResourceSink<T> sink = it.next();
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
                Reservation reservation = sink.ReserveInsert(stack.GetStack());
                if(reservation != null)
                {
                    return new SourcedReservation(reservation, sink.GetAttachedInteractionNode(), sink.GetInsertDirection());
                }
            }
            return null;
        }
    }

    private boolean resourceSourceCollectionsDirty = false;
    private final HashMap<Object, ResourceSourceCollection<T>> resourceSourceCollections = new HashMap<>();

    private final HashMap<Object, ResourceSinkCollection<T>> resourceSinkCollectionItem = new HashMap<>();
    //private final HashMap<ResourceLocation, ItemSinkCollection> itemSinkCollectionTag = new HashMap<>();
    private final ResourceSinkCollection<T> resourceSinkCollectionDefault = new ResourceSinkCollection<>();

    public void Tick()
    {
        if(resourceSourceCollectionsDirty)
        {
            resourceSourceCollectionsDirty = false;
            resourceSourceCollections.forEach((type, sourceCollection)->
            {
                for(int i = 0; i < sourceCollection.resourceSources.size();)
                {
                    ResourceSourceCollection.ResourceItemSource<T> resourceItemSource = sourceCollection.resourceSources.get(i);
                    int newCount = 0;
                    if(resourceItemSource.source.IsValid())
                    {
                        newCount = resourceItemSource.source.GetNumAvailable();
                    }
                    int countChange = newCount - resourceItemSource.sourceCountCache;
                    resourceItemSource.sourceCountCache = newCount;
                    sourceCollection.countCache += countChange;
                    if(sourceCollection.countCache < 0)
                    {
                        sourceCollection.countCache = 0;
                        LogisticsWoW.LOGGER.error("Count cache for item went below 0");
                    }
                    if(!resourceItemSource.source.IsValid())
                    {
                        //remove it
                        //move the one at the back into this position and pop the back
                        sourceCollection.resourceSources.set(i, sourceCollection.resourceSources.get(sourceCollection.resourceSources.size() - 1));
                        sourceCollection.resourceSources.remove(sourceCollection.resourceSources.size() - 1);
                    }
                    else
                    {
                        ++i;
                    }
                }
            });
        }
    }

    public void AddResourceSources(ArrayList<? extends ResourceSource<T>> sources)
    {
        resourceSourceCollectionsDirty = true;
        sources.forEach(source->
        {
            ResourceSourceCollection<T> collection = resourceSourceCollections.computeIfAbsent(source.matcher.GetType(), (key)->new ResourceSourceCollection<>());
            for(ResourceSourceCollection.ResourceItemSource<T> resourceItemSource : collection.resourceSources)
            {
                if(resourceItemSource.source == source)
                {
                    //already registered
                    return;
                }
            }
            ResourceSourceCollection.ResourceItemSource<T> resourceItemSource = new ResourceSourceCollection.ResourceItemSource<>();
            resourceItemSource.source = source;

            collection.resourceSources.add(resourceItemSource);
        });
    }

    public <U> void AddResourceSink(@Nonnull ResourceSink<T> sink, Constants.eFilterType filterType, @Nonnull HashSet<U> typeFilter, @Nonnull ArrayList<ITag<U>> tagFilter)
    {
        switch (filterType)
        {
            case Type ->
            {
                if(typeFilter.isEmpty())
                {
                    AddResourceSink(sink, Constants.eFilterType.Default, typeFilter, tagFilter);
                    return;
                }
                for(Object filterItem : typeFilter)
                {
                    ResourceSinkCollection<T> sinkCollection = resourceSinkCollectionItem.computeIfAbsent(filterItem, key->new ResourceSinkCollection<>());
                    sinkCollection.add(sink);
                }
            }
            case Tag ->
            {
                if(tagFilter.isEmpty())
                {
                    AddResourceSink(sink, Constants.eFilterType.Default, typeFilter, tagFilter);
                    return;
                }
                //assume that the tags provided are valid
                for(ITag<?> tag : tagFilter)
                {
                    //ItemSinkCollection sinkCollection = itemSinkCollectionTag.computeIfAbsent(tag, key -> new ItemSinkCollection());
                    //sinkCollection.add(sink);

                    tag.stream().forEach(taggedResource->
                    {
                        ResourceSinkCollection<T> resourceSinkCollection = resourceSinkCollectionItem.computeIfAbsent(taggedResource, key->new ResourceSinkCollection<>());
                        resourceSinkCollection.add(sink);
                    });
                }
            }
            case Default -> resourceSinkCollectionDefault.add(sink);
        }
    }


    public SourcedReservation ReserveSpaceInBestSink(StorableResource<T> stack)
    {
        return ReserveSpaceInBestSink(stack, Integer.MIN_VALUE, Integer.MIN_VALUE, true);
    }
    public SourcedReservation ReserveSpaceInBestSink(StorableResource<T> stack, int minPriority, int minDefaultPriority, boolean allowDefault)
    {
        //Maybe this should return an array of all valid item sinks at the highest priority?
        ResourceSinkCollection<T> resourceCollection = resourceSinkCollectionItem.get(stack.GetType());
        if(resourceCollection != null)
        {
            SourcedReservation reservedSink = resourceCollection.ReserveSpaceInBestSink(stack, minPriority);
            if(reservedSink != null)
            {
                return reservedSink;
            }
        }
        if(allowDefault)
        {
            return resourceSinkCollectionDefault.ReserveSpaceInBestSink(stack, minDefaultPriority);
        }
        return null;
    }
}
