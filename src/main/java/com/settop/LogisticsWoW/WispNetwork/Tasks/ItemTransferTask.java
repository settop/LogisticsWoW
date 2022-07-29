package com.settop.LogisticsWoW.WispNetwork.Tasks;

import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.WispNetwork.*;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class ItemTransferTask extends TransferTask
{
    public static final String SERIALISABLE_NAME = "ItemTransferTask";
    public static class Factory extends WispTaskFactory
    {
        @Override
        public SerialisableWispTask CreateAndRead(WispNetwork network, CompoundTag nbt)
        {
            ItemTransferTask task = new ItemTransferTask();
            task.DeserialiseNBT(network, nbt);
            return task;
        }
    }

    private ItemStack heldItemStack = ItemStack.EMPTY;
    private ItemResource itemMatcher;


    public ItemTransferTask(@Nonnull ItemResource itemMatcher, @Nonnull CarryWisp carryWisp, @Nonnull SourcedReservation pickupReservation, @Nonnull SourcedReservation dropoffReservation)
    {
        super(carryWisp, pickupReservation, dropoffReservation);
        this.itemMatcher = itemMatcher;
    }

    protected ItemTransferTask()
    {
    }


    @Override
    protected boolean Pickup(SourcedReservation pickupReservation)
    {
        ReservableInventory inv = pickupReservation.inventoryNode.GetReservableInventory(pickupReservation.inventoryDirection);
        heldItemStack = inv.ExtractItems(pickupReservation.reservation, itemMatcher, pickupReservation.reservation.GetExtractCount());
        return !heldItemStack.isEmpty();
    }

    @Override
    protected boolean Dropoff(SourcedReservation dropoffReservation)
    {
        ReservableInventory inv = dropoffReservation.inventoryNode.GetReservableInventory(dropoffReservation.inventoryDirection);
        ItemStack leftover = inv.InsertItems(dropoffReservation.reservation, heldItemStack);
        if(leftover.isEmpty())
        {
            heldItemStack = ItemStack.EMPTY;
            return true;
        }
        heldItemStack = leftover;
        return false;
    }

    @Override
    protected SourcedReservation GetNewReservationForHeldResource(WispNetwork network)
    {
        return network.GetItemManagement().ReserveSpaceInBestSink(new ItemResource(heldItemStack));
    }

    @Override
    protected Reservation ReserveExtractionFromNode(WispInteractionNodeBase node, Direction direction, int count)
    {
        ReservableInventory inv = node.GetReservableInventory(direction);
        if(inv == null)
        {
            LogisticsWoW.LOGGER.error("Transfer task load failed to get inventory from pickup node");
            return null;
        }
        else
        {
            return inv.ReserveExtraction(itemMatcher, count);
        }
    }

    @Override
    protected Reservation ReserveInsertionIntoNode(WispInteractionNodeBase node, Direction direction)
    {
        ReservableInventory inv = node.GetReservableInventory(direction);
        if(inv == null)
        {
            LogisticsWoW.LOGGER.error("Transfer task load failed to get inventory from dropoff node");
            return null;
        }
        else if(!heldItemStack.isEmpty())
        {
            return inv.ReserveInsertion(heldItemStack);
        }
        else if(itemMatcher != null)
        {
            return inv.ReserveInsertion(itemMatcher.GetStack());
        }
        else
        {
            return null;
        }
    }

    @Override
    public ItemStack GetHeldResourceAsItemStack()
    {
        return heldItemStack;
    }

    @Override
    public String GetSerialisableName()
    {
        return SERIALISABLE_NAME;
    }

    @Override
    public CompoundTag SerialiseNBT(WispNetwork network)
    {
        CompoundTag nbt = super.SerialiseNBT(network);

        if(itemMatcher != null)
        {
            nbt.put("itemMatch", itemMatcher.Serialize());
        }
        if(!heldItemStack.isEmpty())
        {
            nbt.put("heldItem", heldItemStack.serializeNBT());
        }
        return nbt;
    }


    @Override
    public void DeserialiseNBT(WispNetwork network, CompoundTag nbt)
    {
        if(nbt.contains("itemMatch"))
        {
            itemMatcher = new ItemResource(nbt.getCompound("itemMatch"));
        }
        if(nbt.contains("heldItem"))
        {
            heldItemStack = ItemStack.of(nbt.getCompound("heldItem"));
        }
        super.DeserialiseNBT(network, nbt);
    }
}
