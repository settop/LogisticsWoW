package com.settop.LogisticsWoW.Wisps.Enhancements;

import com.settop.LogisticsWoW.GUI.SubMenus.StorageEnhancementSubMenu;
import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.Constants;
import com.settop.LogisticsWoW.Utils.ItemFilter;
import com.settop.LogisticsWoW.WispNetwork.*;
import com.settop.LogisticsWoW.WispNetwork.Tasks.ItemTransferTask;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ItemStorageEnhancement extends StorageEnhancement
{
    public static class Factory implements IEnhancement.IFactory
    {
        @Override
        public IEnhancement Create()
        {
            return new ItemStorageEnhancement();
        }
    }

    private class ItemTracker extends ResourceSource<ItemStack>
    {
        public ItemTracker(StoreableResourceMatcher<ItemStack> matcher)
        {
            super(matcher, 0);
        }

        @Override
        public Reservation ReserveExtract(StoreableResourceMatcher<ItemStack> extractionMatcher, int count)
        {
            assert IsValid();
            if(!matcher.Matches(extractionMatcher))
            {
                return null;
            }
            ReservableInventory inv = GetAttachedInteractionNode().GetReservableInventory(GetInvAccessDirection());
            if(inv == null)
            {
                LogisticsWoW.LOGGER.error("Trying to reserve an extract from a missing inventory");
                return null;
            }
            return inv.ReserveExtraction(extractionMatcher, count);
        }

        @Override
        public ItemStack Extract(Reservation reservation, StoreableResourceMatcher<ItemStack> extractionMatcher, int count)
        {
            assert IsValid();
            if(!matcher.Matches(extractionMatcher))
            {
                return ItemStack.EMPTY;
            }
            ReservableInventory inv = GetAttachedInteractionNode().GetReservableInventory(GetInvAccessDirection());
            if(inv == null)
            {
                LogisticsWoW.LOGGER.error("Trying to extract from a missing inventory");
                if(reservation != null)
                {
                    reservation.SetInvalid();
                }
                return null;
            }

            return inv.ExtractItems(reservation, extractionMatcher, count);
        }

        @Override
        public WispInteractionNodeBase GetAttachedInteractionNode()
        {
            return ItemStorageEnhancement.this.GetAttachedInteractionNode();
        }

        @Override
        public Direction GetExtractionDirection() { return GetInvAccessDirection(); }
    }

    private class StorageItemSink extends ResourceSink<ItemStack>
    {
        public StorageItemSink(int priority)
        {
            super(priority);
        }

        @Override
        public Reservation ReserveInsert(ItemStack stack)
        {
            assert IsValid();
            ReservableInventory inv = GetAttachedInteractionNode().GetReservableInventory(GetInvAccessDirection());
            if(inv == null)
            {
                LogisticsWoW.LOGGER.error("Trying to reserve an insert into a missing inventory");
                return null;
            }
            return inv.ReserveInsertion(stack);
        }

        @Override
        public ItemStack Insert(Reservation reservation, ItemStack stack)
        {
            assert IsValid();
            ReservableInventory inv = GetAttachedInteractionNode().GetReservableInventory(GetInvAccessDirection());
            if(inv == null)
            {
                LogisticsWoW.LOGGER.error("Trying to insert into a missing inventory");
                if(reservation != null)
                {
                    reservation.SetInvalid();
                }
                return null;
            }

            return inv.InsertItems(reservation, stack);
        }

        @Override
        public WispInteractionNodeBase GetAttachedInteractionNode()
        {
            assert IsValid();
            return ItemStorageEnhancement.this.GetAttachedInteractionNode();
        }

        @Override
        public Direction GetInsertDirection() { return GetInvAccessDirection(); }
    }

    public static final Factory FACTORY = new Factory();

    private final ItemFilter filter = new ItemFilter();

    //operation data
    private final ArrayList<ItemTracker> itemSources = new ArrayList<>();
    private StorageItemSink itemSink;

    @Override
    public CompoundTag SerializeNBT()
    {
        CompoundTag nbt = super.SerializeNBT();

        nbt.put("filter", filter.SerializeNBT());

        return nbt;
    }

    @Override
    public void DeserializeNBT(CompoundTag nbt)
    {
        if (nbt == null)
        {
            return;
        }

        super.DeserializeNBT(nbt);
        filter.DeserializeNBT(nbt.getCompound("filter"));
        filter.SetIsWhitelist(true);
        filter.SetMatchNBT(false);
    }

    @Override
    public SubMenu CreateSubMenu(int xPos, int yPos, BlockState blockState, BlockEntity blockEntity, WispInteractionNodeBase parentWisp)
    {
        return new StorageEnhancementSubMenu(this, xPos, yPos, blockState, blockEntity, parentWisp);
    }

    @Override
    public String GetName()
    {
        return "item.logwow.wisp_storage_enhancement";
    }

    @Override
    public void AddTooltip(@NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn)
    {
        tooltip.add(new TranslatableComponent("logwow.priority").append(String.format(": %d", GetPriority())));
        switch (filter.GetEffectiveFilterType())
        {
            case Type:
            {
                tooltip.add(new TranslatableComponent("logwow.item_filter").append(":"));
                for(int i = 0; i < filter.GetFilter().getContainerSize(); ++i)
                {
                    ItemStack item = filter.GetFilter().getItem(i);
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
                for(ResourceLocation tag : filter.GetTagFilters())
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
    public void OnConnectToNetwork()
    {
        ReservableInventory inv = GetAttachedInteractionNode().GetReservableInventory();
        if(inv == null)
        {
            return;
        }

        super.OnConnectToNetwork();

        if(itemSink != null)
        {
            itemSink.SetInvalid();
        }
        itemSink = new StorageItemSink(GetPriority());

        GetAttachedInteractionNode().connectedNetwork.GetItemManagement().AddResourceSink(itemSink, filter.GetEffectiveFilterType(), filter.GetFilteredItems(), filter.GetFilteredTags());
        RefreshResources();
    }

    @Override
    public void OnDisconnectFromNetwork()
    {
        super.OnDisconnectFromNetwork();
        itemSources.forEach(ItemTracker::SetInvalid);
        itemSources.clear();
        if(itemSink != null)
        {
            itemSink.SetInvalid();
        }
        itemSink = null;
    }


    private ItemTracker EnsureItemTracker(ItemStack stack)
    {
        for(ItemTracker tracker : itemSources)
        {
            if(tracker.matcher.Matches(stack))
            {
                return tracker;
            }
        }
        ItemTracker itemTracker = new ItemTracker(new ItemResource(stack));
        itemSources.add(itemTracker);
        return itemTracker;
    }

    @Override
    public boolean RefreshResources()
    {
        itemSources.forEach(ItemTracker::Reset);
        ReservableInventory inv = GetAttachedInteractionNode().GetReservableInventory(GetInvAccessDirection());
        if(inv != null)
        {
            inv.RefreshCache();
            IItemHandler iItemHandler = inv.GetSimulatedInv();
            for(int slot = 0; slot < iItemHandler.getSlots(); ++slot)
            {
                ItemStack slotStack = iItemHandler.getStackInSlot(slot);
                if(slotStack.isEmpty())
                {
                    continue;
                }

                ItemTracker itemSource = EnsureItemTracker(slotStack);
                itemSource.UpdateNumAvailable(itemSource.GetNumAvailable() + slotStack.getCount());
            }
        }
        boolean anyChanges = false;
        for(Iterator<ItemTracker> it = itemSources.iterator(); it.hasNext();)
        {
            ItemTracker source = it.next();
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
            GetAttachedInteractionNode().connectedNetwork.GetItemManagement().AddResourceSources(itemSources);
        }
        return anyChanges;
    }

    //Returns true if doing any extraction
    @Override
    protected StorageEnhancement.eExtractionState TickExtraction()
    {
        //first find an item we are not extracting that we want to extract
        eExtractionState fallbackState = eExtractionState.ASLEEP;
        ReservableInventory inv = GetAttachedInteractionNode().GetReservableInventory();
        for(ItemTracker tracker : itemSources)
        {
            boolean matchesFilter = filter.MatchesFilter(tracker.matcher);

            fallbackState = eExtractionState.NO_DESTINATION;

            int idealExtractionCount = tracker.GetNumAvailable();
            CarryWisp reservedCarryWisp = GetAttachedInteractionNode().connectedNetwork.TryReserveCarryWisp(GetAttachedInteractionNode(), idealExtractionCount);
            if(reservedCarryWisp == null)
            {
                continue;
            }
            ItemResource testStack = new ItemResource(tracker.matcher, Math.min(idealExtractionCount, reservedCarryWisp.GetCarryCapacity()));
            SourcedReservation insertReservation;
            if(filter.GetEffectiveFilterType() == Constants.eFilterType.Default)
            {
                insertReservation = GetAttachedInteractionNode().connectedNetwork.GetItemManagement().ReserveSpaceInBestSink(testStack, Integer.MIN_VALUE, GetPriority(), true );
            }
            else
            {
                insertReservation = GetAttachedInteractionNode().connectedNetwork.GetItemManagement().ReserveSpaceInBestSink(testStack, matchesFilter ? GetPriority() : Integer.MIN_VALUE, 0, false );
            }
            if(insertReservation == null)
            {
                reservedCarryWisp.ReleaseReservation();
                continue;
            }
            Reservation selfExtractReservation = inv.ReserveExtraction(tracker.matcher, insertReservation.reservation.reservedCount);
            if(selfExtractReservation == null)
            {
                insertReservation.reservation.SetInvalid();
                reservedCarryWisp.ReleaseReservation();
                continue;
            }
            reservedCarryWisp.Claim();

            GetAttachedInteractionNode().connectedNetwork.StartTask(new ItemTransferTask
                    (
                            (ItemResource)tracker.matcher,
                            reservedCarryWisp,
                            new SourcedReservation(selfExtractReservation, GetAttachedInteractionNode(), GetInvAccessDirection()),
                            insertReservation
                    ));
            return eExtractionState.ACTIVE;
        }
        return fallbackState;
    }

    public ItemFilter GetFilter()
    {
        return filter;
    }
}
