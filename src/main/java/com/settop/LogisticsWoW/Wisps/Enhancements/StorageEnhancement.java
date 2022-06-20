package com.settop.LogisticsWoW.Wisps.Enhancements;

import com.settop.LogisticsWoW.GUI.SubMenus.StorageEnhancementSubMenu;
import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import com.settop.LogisticsWoW.Utils.Constants;
import com.settop.LogisticsWoW.Utils.FakeInventory;
import com.settop.LogisticsWoW.WispNetwork.*;
import com.settop.LogisticsWoW.WispNetwork.Tasks.TransferTask;
import com.settop.LogisticsWoW.WispNetwork.Tasks.WispTask;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;
import net.minecraftforge.registries.tags.ITagManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;

import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

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


    private class PeriodicTask implements WispTask
    {
        private boolean active = true;
        private boolean doneFirstTick = false;
        @Override
        public int Start(@NotNull WispNetwork network, int startTickTime)
        {
           //randomise a bit to prevent them all from ticking at the same time after a world load
            return startTickTime + Constants.GetInitialSleepTimer();
        }

        @Override
        public OptionalInt Tick(@NotNull TickEvent.ServerTickEvent tickEvent, @NotNull WispNetwork network, int currentTickTime, int tickOffset)
        {
            if(!active)
            {
                return OptionalInt.empty();
            }
            itemSink.CleanupInvalidReservations();
            if(RefreshItems() || extractionState != eExtractionState.ASLEEP || !doneFirstTick)
            {
                doneFirstTick = true;
                //check to see if we have anything not in our filter
                extractionState = eExtractionState.ASLEEP;
                for(int i = 0; i < Constants.GetExtractionOperationsPerTick(extractionSpeedRank); ++i)
                {
                    eExtractionState nextState = TickExtraction();
                    if(nextState == eExtractionState.ASLEEP)
                    {
                        break;
                    }
                    else
                    {
                        extractionState = eExtractionState.values()[Math.max(extractionState.ordinal(), nextState.ordinal())];
                    }
                }
                if(extractionState == eExtractionState.ACTIVE)
                {
                    return OptionalInt.of(currentTickTime + tickOffset + Constants.GetExtractionTickDelay(extractionSpeedRank));
                }
                else
                {
                    //Ignore the tick offset whilst sleeping, don't care about missing some ticks whilst asleep
                    return OptionalInt.of(currentTickTime + Constants.SLEEP_TICK_TIMER);
                }
            }
            else
            {
                //Ignore the tick offset whilst sleeping, don't care about missing some ticks whilst asleep
                return OptionalInt.of(currentTickTime + Constants.SLEEP_TICK_TIMER);
            }
        }
    }

    private static class ReservationPair
    {
        public final ItemSink.Reservation targetReservation;
        public final ItemSource.Reservation selfReservation;

        private ReservationPair(@Nonnull ItemSink.Reservation targetReservation, @Nonnull ItemSource.Reservation selfReservation)
        {
            this.targetReservation = targetReservation;
            this.selfReservation = selfReservation;
        }

        public void SetInvalid()
        {
            targetReservation.SetInvalid();
            selfReservation.SetInvalid();
        }
    }
    private class ItemTracker extends ItemSource
    {
        ArrayList<ReservationPair> extractionReservations;
        final Item trackedItem;

        public ItemTracker(Item trackedItem)
        {
            super(0);
            this.trackedItem = trackedItem;
        }

        @Override
        public void SetInvalid()
        {
            super.SetInvalid();
            if(extractionReservations != null)
            {
                extractionReservations.forEach(ReservationPair::SetInvalid);
            }
        }

        @Override
        public ItemStack Extract(int count)
        {
            return connectedItemHandler.map(itemHandler->
            {
                int localCount = count;
                ItemStack retItem = ItemStack.EMPTY;
                for(int slot = 0; slot < itemHandler.getSlots() && localCount > 0; ++slot)
                {
                    ItemStack slotStack = itemHandler.getStackInSlot(slot);
                    if(slotStack.getItem() != trackedItem ||
                            (!retItem.isEmpty() && !ItemHandlerHelper.canItemStacksStack(retItem, slotStack)))
                    {
                        continue;
                    }
                    ItemStack extractedStack = itemHandler.extractItem(slot, localCount, false);
                    localCount -= extractedStack.getCount();
                    if(retItem.isEmpty())
                    {
                        retItem = extractedStack;
                    }
                    else
                    {
                        retItem.grow(extractedStack.getCount());
                    }
                }
                return retItem;
            }).orElse(ItemStack.EMPTY);
        }

        @Override
        public ItemStack ReservedExtract(Reservation reservation)
        {
            assert reservation.IsValid();
            extractionReservations.removeIf(pair->pair.selfReservation == reservation);

            return Extract(reservation.reservationSize);
        }

        @Override
        public WispInteractionNodeBase GetAttachedInteractionNode()
        {
            return parentWisp;
        }
    }

    private class StorageItemSink extends ItemSink
    {
        private class ItemReservation
        {
            private final ArrayList<Reservation> reservations = new ArrayList<>();
            private int totalReservations = 0;
        }
        private final HashMap<Item, ItemReservation> itemReservations = new HashMap<>();
        private IItemHandler reservationInventory;

        public StorageItemSink(int priority)
        {
            super(priority);
        }

        private ItemStack AddItemStackToInv(IItemHandler inv, ItemStack stack)
        {
            stack = stack.copy();
            for(int i = 0; i < inv.getSlots(); ++i)
            {
                stack = inv.insertItem(i, stack, true);
                if(stack.isEmpty())
                {
                    break;
                }
            }
            return stack;
        }

        @Override
        public Reservation ReserveInsert(ItemStack stack)
        {
            assert IsValid();
            if(!connectedItemHandler.isPresent())
            {
                return null;
            }
            Optional<IItemHandler> itemHandlerOptional = connectedItemHandler.resolve();
            if(itemHandlerOptional.isEmpty())
            {
                return null;
            }

            IItemHandler iItemHandler;
            ItemStack originalStack = stack;

            ItemReservation itemReservation = itemReservations.get(stack.getItem());
            if(itemReservations.isEmpty() || (itemReservations.size() == 1 && itemReservation != null))
            {
                //either first reservation, or a reservation of the same items as before
                iItemHandler = itemHandlerOptional.get();

                stack = stack.copy();
                //need to do all the reserved insertions as well to check there is additional room
                int currentReserveCount = itemReservation == null ? 0 : itemReservation.totalReservations;
                stack.setCount(originalStack.getCount() + currentReserveCount);
            }
            else
            {
                //need to use a placeholder fake inventory to do the insertion into
                if(reservationInventory == null)
                {
                    reservationInventory = new SimulatedInventory(itemHandlerOptional.get());
                    itemReservations.forEach((item, reservation)->AddItemStackToInv(reservationInventory, new ItemStack(item, reservation.totalReservations)));
                }
                iItemHandler = reservationInventory;
            }

            stack = AddItemStackToInv(iItemHandler, stack);
            if(stack.getCount() < originalStack.getCount())
            {
                if(itemReservation == null)
                {
                    itemReservation = new ItemReservation();
                    itemReservations.put(originalStack.getItem(), itemReservation);
                }
                //there is space for this
                Reservation reservation = new Reservation(this, originalStack.getCount() - stack.getCount());
                itemReservation.reservations.add(reservation);
                itemReservation.totalReservations += reservation.reservationSize;
                return reservation;
            }
            else
            {
                return null;
            }
        }

        @Override
        public ItemStack Insert(ItemStack stack)
        {
            assert IsValid();
            if(!connectedItemHandler.isPresent())
            {
                return null;
            }
            Optional<IItemHandler> itemHandlerOptional = connectedItemHandler.resolve();
            if(itemHandlerOptional.isEmpty())
            {
                return null;
            }

            IItemHandler iItemHandler = itemHandlerOptional.get();
            for(int i = 0; i < iItemHandler.getSlots(); ++i)
            {
                stack = iItemHandler.insertItem(i, stack, false);
                if(stack.isEmpty())
                {
                    break;
                }
            }
            return stack;
        }

        @Override
        public ItemStack ReservedInsert(Reservation reservation, ItemStack stack)
        {
            ItemReservation itemReservation = itemReservations.get(stack.getItem());
            if(itemReservation != null)
            {
                if(itemReservation.reservations.remove(reservation))
                {
                    itemReservation.totalReservations -= reservation.reservationSize;
                }
            }
            reservation.SetInvalid();
            return Insert(stack);
        }

        @Override
        public WispInteractionNodeBase GetAttachedInteractionNode()
        {
            assert IsValid();
            return parentWisp;
        }

        @Override
        public void SetInvalid()
        {
            super.SetInvalid();
            itemReservations.values().forEach(reservations->reservations.reservations.forEach(Reservation::SetInvalid));
        }

        public void CleanupInvalidReservations()
        {
            for(Iterator<ItemReservation> itemIt = itemReservations.values().iterator(); itemIt.hasNext();)
            {
                ItemReservation itemReservation = itemIt.next();
                for(Iterator<Reservation> it = itemReservation.reservations.iterator(); it.hasNext();)
                {
                    Reservation reservation = it.next();
                    if(!reservation.IsValid())
                    {
                        itemReservation.totalReservations -= reservation.reservationSize;
                        it.remove();
                        reservationInventory = null;
                    }
                }
                if(itemReservation.reservations.isEmpty())
                {
                    itemIt.remove();
                }
            }
        }

        public void InventoryChanged()
        {
            //the connected inventory has changed, will need to recalculate the cached reservations
            reservationInventory = null;
        }
    }

    public static final Factory FACTORY = new Factory();
    public static final int FILTER_NUM_COLUMNS = 8;
    public static final int FILTER_NUM_ROWS = 4;
    public static final int FILTER_SIZE = FILTER_NUM_COLUMNS * FILTER_NUM_ROWS;

    //data player can tweak
    private int priority = 0;
    private final FakeInventory filter = new FakeInventory(FILTER_SIZE, false);
    private final ArrayList<ResourceLocation> tagFilter = new ArrayList<>();
    private Constants.eFilterType filterType = Constants.eFilterType.Item;
    private final int extractionSpeedRank = 0;

    //cached versions of the player data
    private Constants.eFilterType effectiveFilterType = Constants.eFilterType.Item;
    private final HashSet<Item> filteredItems = new HashSet<>();
    private final ArrayList<ITag<Item>> filteredTags = new ArrayList<>();

    //operation data
    private WispInteractionNodeBase parentWisp;
    private LazyOptional<IItemHandler> connectedItemHandler;
    private final HashMap<Item, ItemTracker> itemSources = new HashMap<>();
    private StorageItemSink itemSink;

    //running data
    private enum eExtractionState
    {
        ASLEEP,
        NO_DESTINATION,
        ACTIVE
    }
    private PeriodicTask currentTask;
    private eExtractionState extractionState = eExtractionState.ASLEEP;

    @Override
    public CompoundTag SerializeNBT()
    {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt("priority", priority);

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

        if(filterType != Constants.eFilterType.Item)
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
            if(type >= 0 && type < Constants.eFilterType.values().length)
            {
                filterType = Constants.eFilterType.values()[type];
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
        switch (CalculateEffectiveFilterType())
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
            case Default:
            {
                tooltip.add(new TranslatableComponent("logwow.default_store"));
            }
        }
    }

    @Override
    public void Setup(WispInteractionNodeBase parentWisp, BlockEntity blockEntity)
    {
        if(blockEntity == null)
        {
            connectedItemHandler = null;
            ClearNetworkData();
            return;
        }

        this.parentWisp = parentWisp;
        LazyOptional<IItemHandler> nextItemHandler = blockEntity.getCapability(ITEM_HANDLER_CAPABILITY);
        if(nextItemHandler != connectedItemHandler)
        {
            connectedItemHandler = nextItemHandler;
            connectedItemHandler.addListener(invalidOptional ->
            {
                if (connectedItemHandler == invalidOptional)
                {
                    connectedItemHandler = null;
                    ClearNetworkData();
                }
            });
        }
        if(currentTask == null)
        {
            currentTask = new PeriodicTask();
            parentWisp.connectedNetwork.StartTask(currentTask);
        }
        if(itemSink != null)
        {
            itemSink.SetInvalid();
        }
        itemSink = new StorageItemSink(priority);
        effectiveFilterType = CalculateEffectiveFilterType();
        UpdateFilterCache();
        parentWisp.connectedNetwork.GetItemManagement().AddItemSink(itemSink, effectiveFilterType, filteredItems, filteredTags);
        RefreshItems();
    }

    private Constants.eFilterType CalculateEffectiveFilterType()
    {
        switch (filterType)
        {
            case Item ->
                    {
                        for(int i = 0; i < filter.getContainerSize(); ++i)
                        {
                            if(!filter.getItem(i).isEmpty())
                            {
                                return filterType;
                            }
                        }
                        return Constants.eFilterType.Default;
                    }
            case Tag ->
                    {
                        if(tagFilter.isEmpty())
                        {
                            return Constants.eFilterType.Default;
                        }
                        else
                        {
                            return filterType;
                        }
                    }
            case Default ->
                    {
                        return Constants.eFilterType.Default;
                    }
            default ->
                    {
                        assert false;
                        return Constants.eFilterType.Default;
                    }
        }
    }

    private void UpdateFilterCache()
    {
        filteredItems.clear();
        for(int i = 0 ; i < filter.getContainerSize(); ++i)
        {
            ItemStack itemStack = filter.getItem(i);
            if(itemStack.isEmpty())
            {
                continue;
            }
            filteredItems.add(itemStack.getItem());
        }

        filteredTags.clear();
        ITagManager<Item> itemTagManager = ForgeRegistries.ITEMS.tags();
        assert itemTagManager != null;
        for(ResourceLocation tagName : tagFilter)
        {
            filteredTags.add(itemTagManager.getTag(ItemTags.create(tagName)));
        }
    }

    private boolean RefreshItems()
    {
        itemSources.values().forEach(ItemSource::Reset);
        connectedItemHandler.ifPresent(iItemHandler ->
        {
            for(int slot = 0; slot < iItemHandler.getSlots(); ++slot)
            {
                ItemStack slotStack = iItemHandler.getStackInSlot(slot);
                if(slotStack.isEmpty())
                {
                    continue;
                }

                ItemSource itemSource = itemSources.computeIfAbsent(slotStack.getItem(), ItemTracker::new);
                itemSource.UpdateNumAvailable(itemSource.GetNumAvailable() + slotStack.getCount());
            }
        });
        boolean anyChanges = false;
        for(Iterator<ItemTracker> it = itemSources.values().iterator(); it.hasNext();)
        {
            ItemSource source = it.next();
            if(source.HasChanged())
            {
                anyChanges = true;
            }
            if(source.GetNumAvailable() == 0)
            {
                anyChanges = true;
                source.SetInvalid();
                it.remove();
            }
        }

        if(anyChanges)
        {
            itemSink.InventoryChanged();
            parentWisp.connectedNetwork.GetItemManagement().AddItemSources(itemSources);
        }
        return anyChanges;
    }

    private boolean MatchesFilter(Item item)
    {
        return switch (effectiveFilterType)
        {
            case Item -> filteredItems.contains(item);
            case Tag -> filteredTags.stream().anyMatch(tag->tag.contains(item));
            case Default -> false;
        };
    }

    //Returns true if doing any extraction
    private eExtractionState TickExtraction()
    {
        //first find an item we are not extracting that we want to extract
        eExtractionState fallbackState = eExtractionState.ASLEEP;
        for(Map.Entry<Item, ItemTracker> entry : itemSources.entrySet())
        {
            Item item = entry.getKey();
            ItemTracker tracker = entry.getValue();
            if(MatchesFilter(item))
            {
                continue;
            }

            int queuedExtractionAmount = 0;
            if(tracker.extractionReservations != null)
            {
                for(Iterator<ReservationPair> it = tracker.extractionReservations.iterator(); it.hasNext();)
                {
                    ReservationPair reservation = it.next();
                    if(reservation.targetReservation.IsValid() && reservation.selfReservation.IsValid())
                    {
                        queuedExtractionAmount += reservation.targetReservation.reservationSize;
                    }
                    else
                    {
                        reservation.SetInvalid();
                        it.remove();
                    }
                }
            }
            if(queuedExtractionAmount >= tracker.GetNumAvailable())
            {
                continue;
            }
            fallbackState = eExtractionState.NO_DESTINATION;

            ItemSink.Reservation reservation;
            int idealExtractionCount = tracker.GetNumAvailable() - queuedExtractionAmount;
            CarryWisp reservedCarryWisp = parentWisp.connectedNetwork.TryReserveCarryWisp(parentWisp, idealExtractionCount);
            if(reservedCarryWisp == null)
            {
                continue;
            }
            ItemStack testStack = new ItemStack(item, Math.min(idealExtractionCount, reservedCarryWisp.GetCarryCapacity()));
            if(effectiveFilterType == Constants.eFilterType.Default)
            {
                reservation = parentWisp.connectedNetwork.GetItemManagement().ReserveSpaceInBestSink(testStack, Integer.MIN_VALUE, priority, true );
            }
            else
            {
                reservation = parentWisp.connectedNetwork.GetItemManagement().ReserveSpaceInBestSink(testStack, priority, priority, false );
            }
            if(reservation == null)
            {
                reservedCarryWisp.ReleaseReservation();
                continue;
            }
            reservedCarryWisp.Claim();
            if(tracker.extractionReservations == null)
            {
                tracker.extractionReservations = new ArrayList<>();
            }
            ItemSource.Reservation selfReservation = new ItemSource.Reservation(tracker,  reservation.reservationSize);
            tracker.extractionReservations.add(new ReservationPair(reservation, selfReservation));
            parentWisp.connectedNetwork.StartTask(new TransferTask(reservedCarryWisp, selfReservation, reservation));
            return eExtractionState.ACTIVE;
        }
        return fallbackState;
    }

    private void ClearNetworkData()
    {
        itemSources.values().forEach(ItemSource::SetInvalid);
        itemSources.clear();
        if(currentTask != null)
        {
            currentTask.active = false;
            currentTask = null;
        }
        if(itemSink != null)
        {
            itemSink.SetInvalid();
        }
        itemSink = null;
    }

    public int GetPriority() { return priority; }
    public void SetPriority(int prio) { priority = prio; }

    public FakeInventory GetFilter() { return filter; }

    public ArrayList<ResourceLocation> GetTagFilters() { return tagFilter; }
    public void SetTagFilters(ArrayList<String> tags)
    {
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
    public Constants.eFilterType GetEffectiveFilterType()
    {
        return effectiveFilterType;
    }
    public Constants.eFilterType GetFilterType()
    {
        return filterType;
    }
    public void SetFilterType(Constants.eFilterType filterType) { this.filterType = filterType; }
}
