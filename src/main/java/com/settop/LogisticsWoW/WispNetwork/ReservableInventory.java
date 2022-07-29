package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Utils.Invalidable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;

public class ReservableInventory extends Invalidable
{

    private static class ReservationCollection
    {
        final StoreableResourceMatcher<ItemStack> itemMatcher;
        final ArrayList<Reservation> reservations = new ArrayList<>();

        private ReservationCollection(StoreableResourceMatcher<ItemStack> itemMatcher)
        {
            this.itemMatcher = itemMatcher;
        }

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

    private final ArrayList<ReservationCollection> reservedItems = new ArrayList<>();
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
        for(ReservationCollection reservationCollection : reservedItems)
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
        for(ReservationCollection reservation : reservedItems)
        {
            int totalItemReservations = reservation.GetReservationTotalCount();
            if(totalItemReservations > 0)
            {
                ItemStack insertItem = null;
                //simulate inserting
                for(int slot = 0; slot < simulatedInventory.getSlots(); ++slot)
                {
                    ItemStack slotStack = simulatedInventory.getStackInSlot(slot);
                    if (insertItem == null)
                    {
                        if (slotStack.isEmpty())
                        {
                            //can't get the exact stack we need, so just add a stack of the correct item without any nbt
                            insertItem = new ItemStack((Item) reservation.itemMatcher.GetType(), totalItemReservations);
                            insertItem = simulatedInventory.insertItem(slot, insertItem, false);
                            totalItemReservations = insertItem.getCount();
                            insertItem = null;
                            if(totalItemReservations <= 0)
                            {
                                break;
                            }
                            else
                            {
                                continue;
                            }
                        }
                        else if (reservation.itemMatcher.Matches(slotStack))
                        {
                            //this is the exact type of item we want, so reuse it for the rest of the inserts
                            insertItem = slotStack.copy();
                            insertItem.setCount(totalItemReservations);
                        }
                        else
                        {
                            //can't insert onto this item
                            continue;
                        }
                    }
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
                    if(reservation.itemMatcher.Matches(slotStack))
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

    private ItemStack SimulateExtraction(StoreableResourceMatcher<ItemStack> extractionMatcher, int count)
    {
        ItemStack extractedStack = ItemStack.EMPTY;
        for(int slot = 0; slot < simulatedInventory.getSlots(); ++slot)
        {
            ItemStack slotStack = simulatedInventory.getStackInSlot(slot);
            if((extractedStack.isEmpty() && extractionMatcher.Matches(slotStack)) ||
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
        ItemStack remainderStack = SimulateInsertion(insertItem);

        int insertedCount = insertItem.getCount() - remainderStack.getCount();

        if(insertedCount == 0)
        {
            return null;
        }

        for(ReservationCollection reservationCollection : reservedItems)
        {
            if(reservationCollection.itemMatcher.Matches(insertItem))
            {
                Reservation reservation = new Reservation(insertedCount);
                reservationCollection.reservations.add(reservation);
                return reservation;
            }
        }

        ReservationCollection reservationCollection = new ReservationCollection(new ItemResource(insertItem));
        reservedItems.add(reservationCollection);

        Reservation reservation = new Reservation(insertedCount);
        reservationCollection.reservations.add(reservation);
        return reservation;
    }

    public Reservation ReserveExtraction(StoreableResourceMatcher<ItemStack> extractionMatcher, int count)
    {
        ItemStack extractedStack = SimulateExtraction(extractionMatcher, count);

        if(extractedStack.isEmpty())
        {
            return null;
        }

        for(ReservationCollection reservationCollection : reservedItems)
        {
            if(reservationCollection.itemMatcher.equals(extractionMatcher))
            {
                Reservation reservation = new Reservation(-extractedStack.getCount());
                reservationCollection.reservations.add(reservation);
                return reservation;
            }
        }

        ReservationCollection reservationCollection = new ReservationCollection(extractionMatcher);
        reservedItems.add(reservationCollection);

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

    public ItemStack ExtractItems(Reservation reservation, StoreableResourceMatcher<ItemStack> extractionMatcher, int count)
    {
        assert reservation == null || reservation.IsExtractReservation();
        assert reservation == null || count <= -reservation.reservedCount;

        ItemStack extractedStack = ItemStack.EMPTY;
        for(int slot = 0; slot < baseInv.getSlots(); ++slot)
        {
            ItemStack slotStack = baseInv.getStackInSlot(slot);
            if((extractedStack.isEmpty() && extractionMatcher.Matches(slotStack)) ||
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
