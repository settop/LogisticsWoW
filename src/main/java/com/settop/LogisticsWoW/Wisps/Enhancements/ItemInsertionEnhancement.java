package com.settop.LogisticsWoW.Wisps.Enhancements;

import com.settop.LogisticsWoW.GUI.SubMenus.ItemInsertionSubMenu;
import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.Constants;
import com.settop.LogisticsWoW.Utils.ItemFilter;
import com.settop.LogisticsWoW.WispNetwork.ReservableInventory;
import com.settop.LogisticsWoW.WispNetwork.Reservation;
import com.settop.LogisticsWoW.WispNetwork.ResourceSink;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ItemInsertionEnhancement extends InsertionEnhancement
{
    public static class Factory implements IEnhancement.IFactory
    {
        @Override
        public IEnhancement Create()
        {
            return new ItemInsertionEnhancement();
        }
    }

    private class InsertItemSink extends ResourceSink<ItemStack>
    {
        public InsertItemSink()
        {
            super(Integer.MAX_VALUE);
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

            if(filter.GetEffectiveFilterType() == Constants.eFilterType.Type)
            {
                ArrayList<ItemStack> filterItems = missingFilteredItems.get(stack.getItem());
                if(filterItems == null)
                {
                    return null;
                }

                int countToInsert = stack.getCount();
                for(ItemStack filterStack : filterItems)
                {
                    if(filter.GetMatchNBT() && !ItemStack.isSameItemSameTags(filterStack, stack))
                    {
                        continue;
                    }
                    if(countToInsert <= filterStack.getCount())
                    {
                        countToInsert = 0;
                        break;
                    }
                    else
                    {
                        countToInsert -= filterStack.getCount();
                    }
                }
                if(countToInsert == stack.getCount())
                {
                    return null;
                }
                else if(countToInsert != 0)
                {
                    stack = stack.copy();
                    stack.shrink(countToInsert);
                }

                Reservation reservation = inv.ReserveInsertion(stack);
                if(reservation == null)
                {
                    return null;
                }
                int amountReserved = reservation.GetInsertCount();
                for(ItemStack filterStack : filterItems)
                {
                    if(filter.GetMatchNBT() && !ItemStack.isSameItemSameTags(filterStack, stack))
                    {
                        continue;
                    }
                    if(amountReserved <= filterStack.getCount())
                    {
                        filterStack.shrink(amountReserved);
                        break;
                    }
                    else
                    {
                        amountReserved -= filterStack.getCount();
                        filterStack.shrink(filterStack.getCount());
                    }
                }
                return reservation;
            }
            else
            {
                if(!filter.MatchesFilter(stack))
                {
                    return null;
                }
                return inv.ReserveInsertion(stack);
            }
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
                return stack;
            }

            return inv.InsertItems(reservation, stack);
        }

        @Override
        public WispInteractionNodeBase GetAttachedInteractionNode()
        {
            assert IsValid();
            return ItemInsertionEnhancement.this.GetAttachedInteractionNode();
        }

        @Override
        public Direction GetInsertDirection() { return GetInvAccessDirection(); }
    }

    public static final Factory FACTORY = new Factory();

    //data player can tweak
    private final ItemFilter filter = new ItemFilter(true);

    //operation data
    private InsertItemSink itemSink;
    private final HashMap<Item, ArrayList<ItemStack>> missingFilteredItems = new HashMap<>();

    @Override
    public SubMenu CreateSubMenu(int xPos, int yPos, BlockState blockState, BlockEntity blockEntity, WispInteractionNodeBase parentWisp)
    {
        return new ItemInsertionSubMenu(this, xPos, yPos, blockState, blockEntity, parentWisp);
    }

    public String GetName()
    {
        return "item.logwow.wisp_item_insertion_enhancement";
    }

    @Override
    public void AddTooltip(@NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn)
    {
        filter.AddTooltip(tooltip, flagIn);
    }

    @Override
    protected void RefreshCache(int currentTick)
    {
        ReservableInventory inv = GetAttachedInteractionNode().GetReservableInventory(GetInvAccessDirection());
        if(inv == null)
        {
            return;
        }
        inv.RefreshCacheThisTick(currentTick);
        missingFilteredItems.clear();
        if(filter.GetEffectiveFilterType() == Constants.eFilterType.Type)
        {
            for(int slot = 0; slot < filter.GetFilter().getContainerSize(); ++slot)
            {
                ItemStack filterStack = filter.GetFilter().getItem(slot);
                if(filterStack.isEmpty())
                {
                    continue;
                }
                ArrayList<ItemStack> filterItems = missingFilteredItems.computeIfAbsent(filterStack.getItem(), (item)->new ArrayList<>());
                filterItems.add(filterStack.copy());
            }
            for(int slot = 0; slot < inv.GetSimulatedInv().getSlots(); ++slot)
            {
                ItemStack slotStack = inv.GetSimulatedInv().getStackInSlot(slot);
                if(slotStack.isEmpty())
                {
                    continue;
                }
                ArrayList<ItemStack> filterItems = missingFilteredItems.get(slotStack.getItem());
                if(filterItems == null)
                {
                    continue;
                }

                int existingCount = slotStack.getCount();
                for(Iterator<ItemStack> it = filterItems.iterator(); it.hasNext();)
                {
                    ItemStack filterStack = it.next();
                    if(filter.GetMatchNBT() && !ItemStack.isSameItemSameTags(slotStack, filterStack))
                    {
                        continue;
                    }
                    if(existingCount >= filterStack.getCount())
                    {
                        existingCount -= filterStack.getCount();
                        it.remove();
                    }
                    else
                    {
                        filterStack.shrink(existingCount);
                        break;
                    }
                }
                if(filterItems.isEmpty())
                {
                    missingFilteredItems.remove(slotStack.getItem());
                }
            }
        }
    }

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
        if(nbt == null)
        {
            return;
        }
        super.DeserializeNBT(nbt);
        filter.DeserializeNBT(nbt.getCompound("filter"));
        filter.SetIsWhitelist(true);
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
        itemSink = new InsertItemSink();
        if(filter.GetEffectiveFilterType() != Constants.eFilterType.Default)
        {
            GetAttachedInteractionNode().connectedNetwork.GetItemManagement().AddResourceSink(itemSink, filter.GetEffectiveFilterType(), filter.GetFilteredItems(), filter.GetFilteredTags());
        }
    }

    @Override
    public void OnDisconnectFromNetwork()
    {
        super.OnDisconnectFromNetwork();
        if(itemSink != null)
        {
            itemSink.SetInvalid();
        }
        itemSink = null;
    }

    public ItemFilter GetFilter() { return filter; }
}
