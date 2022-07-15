package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Utils.Invalidable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ReservableInventory extends Invalidable
{
    public static class Reservation extends Invalidable
    {
        public final int reservedCount;

        public Reservation(int reservedCount)
        {
            assert reservedCount != 0;
            this.reservedCount = reservedCount;
        }

        public boolean IsInsertReservation() { return reservedCount > 0; }
        public boolean IsExtractReservation() { return reservedCount < 0; }

        public int GetInsertCount()
        {
            assert IsInsertReservation();
            return reservedCount;
        }
        public int GetExtractCount()
        {
            assert IsExtractReservation();
            return -reservedCount;
        }
    }

    private class ReservationCollection
    {
        final ArrayList<Reservation> reservations = new ArrayList<>();

        int GetReservationTotalCount()
        {
            int count = 0;
            for(Iterator<Reservation> it = reservations.iterator(); it.hasNext();)
            {
                Reservation reservation = it.next();
                if(reservation.IsValid())
                {
                    count += reservation.reservedCount;
                }
                else
                {
                    it.remove();
                }
            }
            return count;
        }
    }

    private final HashMap<Item, ReservationCollection> reservedItems = new HashMap<>();
    private final IItemHandler baseInv;
    private final SimulatedInventory simulatedInventory;

    public ReservableInventory(@Nonnull IItemHandler baseInv)
    {
        this.baseInv = baseInv;
        simulatedInventory = new SimulatedInventory(baseInv);
    }

    @Override
    public void SetInvalid()
    {
        super.SetInvalid();
        for(ReservationCollection reservationCollection : reservedItems.values())
        {
            for(Reservation reservation : reservationCollection.reservations)
            {
                reservation.SetInvalid();
            }
        }
    }

    public void RefreshCache()
    {
        simulatedInventory.Reset();
        for(Map.Entry<Item, ReservationCollection> reservation : reservedItems.entrySet())
        {
            Item item = reservation.getKey();
            int totalItemReservations = reservation.getValue().GetReservationTotalCount();
            if(totalItemReservations > 0)
            {
                ItemStack insertItem = new ItemStack(item, totalItemReservations);
                //simulate inserting
                for(int slot = 0; slot < simulatedInventory.getSlots(); ++slot)
                {
                    insertItem = simulatedInventory.insertItem(slot, insertItem, false);
                    if(insertItem.isEmpty())
                    {
                        break;
                    }
                }
            }
            else if(totalItemReservations < 0)
            {
                //simulate extraction
                totalItemReservations = -totalItemReservations;
                for(int slot = 0; slot < simulatedInventory.getSlots(); ++slot)
                {
                    ItemStack slotStack = simulatedInventory.getStackInSlot(slot);
                    if(slotStack.getItem().equals(item))
                    {
                        ItemStack simulatedExtractedItem = simulatedInventory.extractItem(slot, totalItemReservations, false);
                        totalItemReservations -= simulatedExtractedItem.getCount();
                    }
                    if(totalItemReservations <= 0)
                    {
                        break;
                    }
                }
            }
        }
    }

    public IItemHandler GetSimulatedInv()
    {
        return simulatedInventory;
    }

    private ItemStack SimulateInsertion(ItemStack insertItem)
    {
        for(int slot = 0; slot < simulatedInventory.getSlots(); ++slot)
        {
            insertItem = simulatedInventory.insertItem(slot, insertItem, false);
            if(insertItem.isEmpty())
            {
                break;
            }
        }
        return insertItem;
    }

    private ItemStack SimulateExtraction(Item item, int count)
    {
        ItemStack extractedStack = ItemStack.EMPTY;
        for(int slot = 0; slot < simulatedInventory.getSlots(); ++slot)
        {
            ItemStack slotStack = simulatedInventory.getStackInSlot(slot);
            if((extractedStack.isEmpty() && slotStack.is(item)) ||
                    (!extractedStack.isEmpty() && ItemHandlerHelper.canItemStacksStack(extractedStack, slotStack)))
            {
                ItemStack slotExtractedStack = simulatedInventory.extractItem(slot, count - extractedStack.getCount(), false);
                if(extractedStack.isEmpty())
                {
                    extractedStack = slotExtractedStack;
                }
                else
                {
                    extractedStack.grow(slotExtractedStack.getCount());
                }
            }
            if(extractedStack.getCount() >= count)
            {
                break;
            }
        }
        return extractedStack;
    }

    public Reservation ReserveInsertion(ItemStack insertItem)
    {
        Item item = insertItem.getItem();
        int initialCount = insertItem.getCount();

        insertItem = SimulateInsertion(insertItem);

        int insertedCount = initialCount - insertItem.getCount();

        if(insertedCount == 0)
        {
            return null;
        }

        ReservationCollection reservationCollection = reservedItems.computeIfAbsent(item, (key)->new ReservationCollection());
        Reservation reservation = new Reservation(insertedCount);
        reservationCollection.reservations.add(reservation);
        return reservation;
    }

    public Reservation ReserveExtraction(Item item, int count)
    {
        ItemStack extractedStack = SimulateExtraction(item, count);

        if(extractedStack.isEmpty())
        {
            return null;
        }

        ReservationCollection reservationCollection = reservedItems.computeIfAbsent(item, (key)->new ReservationCollection());
        Reservation reservation = new Reservation(-extractedStack.getCount());
        reservationCollection.reservations.add(reservation);
        return reservation;
    }

    public ItemStack InsertItems(Reservation reservation, ItemStack stack)
    {
        assert reservation == null || reservation.IsInsertReservation();
        assert reservation == null || stack.getCount() <= reservation.reservedCount;

        ItemStack insertStack = stack;
        for(int slot = 0; slot < baseInv.getSlots(); ++slot)
        {
            insertStack = baseInv.insertItem(slot, insertStack, false);
            if(insertStack.isEmpty())
            {
                break;
            }
        }

        if(reservation != null)
        {
            reservation.SetInvalid();
        }

        return insertStack;
    }

    public ItemStack ExtractItems(Reservation reservation, Item item, int count)
    {
        assert reservation == null || reservation.IsExtractReservation();
        assert reservation == null || count <= -reservation.reservedCount;

        ItemStack extractedStack = ItemStack.EMPTY;
        for(int slot = 0; slot < baseInv.getSlots(); ++slot)
        {
            ItemStack slotStack = baseInv.getStackInSlot(slot);
            if((extractedStack.isEmpty() && slotStack.is(item)) ||
                    (!extractedStack.isEmpty() && ItemHandlerHelper.canItemStacksStack(extractedStack, slotStack)))
            {
                ItemStack slotExtractedStack = baseInv.extractItem(slot, count - extractedStack.getCount(), false);
                if(extractedStack.isEmpty())
                {
                    extractedStack = slotExtractedStack;
                }
                else
                {
                    extractedStack.grow(slotExtractedStack.getCount());
                }
            }
            if(extractedStack.getCount() >= count)
            {
                break;
            }
        }

        if(reservation != null)
        {
            reservation.SetInvalid();
        }

        return extractedStack;
    }
}
