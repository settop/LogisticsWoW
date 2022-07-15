package com.settop.LogisticsWoW.WispNetwork.Tasks;

import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.WispNetwork.*;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import com.settop.LogisticsWoW.Wisps.WispNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.OptionalInt;

public class TransferTask extends ItemHoldingTask
{
    public static final String SERIALISABLE_NAME = "TransferTask";
    public static class Factory extends WispTaskFactory
    {
        @Override
        public SerialisableWispTask CreateAndRead(WispNetwork network, CompoundTag nbt)
        {
            TransferTask task = new TransferTask();
            task.DeserialiseNBT(network, nbt);
            return task;
        }
    }

    enum eState
    {
        GoingToPickup,
        GoingToDropoff,
        GoingHome
    }

    private CompoundTag loadData;

    private Item item;
    private CarryWisp carryWisp;
    private SourcedReservation pickupReservation;
    private SourcedReservation dropoffReservation;

    private eState currentState = eState.GoingToPickup;
    private WispNode.NextPathStep nextPathStep = null;
    private float leftoverFractionalTick = 0.f;

    public TransferTask(@Nonnull Item item, @Nonnull CarryWisp carryWisp, @Nonnull SourcedReservation pickupReservation, @Nonnull SourcedReservation dropoffReservation)
    {
        this.item = item;
        this.carryWisp = carryWisp;
        this.pickupReservation = pickupReservation;
        this.dropoffReservation = dropoffReservation;
    }

    private TransferTask()
    {
    }

    public int GetNextTickOffset()
    {
        float timeToNextNode = (nextPathStep.distToNode / carryWisp.GetSpeed()) - leftoverFractionalTick;
        float numTicks = (float)Math.ceil(timeToNextNode);
        leftoverFractionalTick = numTicks - timeToNextNode;
        return (int)numTicks;
    }

    public void DropItems()
    {
        //ToDo
    }

    public void DropWisp()
    {
        //ToDo
    }

    @Override
    public int Start(@NotNull WispNetwork network, int startTickTime)
    {
        assert carryWisp.sourceNode.connectedNetwork == network;
        assert pickupReservation.inventoryNode.connectedNetwork == network;
        assert dropoffReservation.inventoryNode.connectedNetwork == network;

        nextPathStep = network.GetNextPathStep(carryWisp.sourceNode, pickupReservation.inventoryNode);
        return startTickTime + GetNextTickOffset();
    }

    @Override
    public OptionalInt Tick(@NotNull TickEvent.ServerTickEvent tickEvent, @NotNull WispNetwork network, int currentTickTime, int tickOffset)
    {
        if(loadData != null)
        {
            if(TryLoad(network, loadData))
            {
                loadData = null;
            }
            else
            {
                //ToDo: should this sleep more? Instead of checking every tick
                return OptionalInt.of(currentTickTime + tickOffset);
            }
        }
        //reached the next node
        WispNode targetNode = null;
        switch (currentState)
        {
            case GoingToPickup ->
            {
                if(pickupReservation == null || !pickupReservation.IsValid() || pickupReservation.inventoryNode.connectedNetwork != network)
                {
                    //go home
                    currentState = eState.GoingHome;
                    return OptionalInt.of(currentTickTime + tickOffset);
                }
                else if(nextPathStep.node == pickupReservation.inventoryNode)
                {
                    //extract and proceed
                    ReservableInventory inv = pickupReservation.inventoryNode.GetReservableInventory(pickupReservation.inventoryDirection);
                    heldItemStack = inv.ExtractItems(pickupReservation.reservation, item, pickupReservation.reservation.GetExtractCount());
                    pickupReservation = null;
                    if(heldItemStack.isEmpty())
                    {
                        //nothing to carry
                        //cancel this task and send the wisp back home
                        currentState = eState.GoingHome;
                        return OptionalInt.of(currentTickTime + tickOffset);
                    }
                    currentState = eState.GoingToDropoff;

                    return OptionalInt.of(currentTickTime + tickOffset);
                }
                targetNode = pickupReservation.inventoryNode;
            }
            case GoingToDropoff ->
            {
                if(dropoffReservation == null || !dropoffReservation.IsValid() || dropoffReservation.inventoryNode.connectedNetwork != network)
                {
                    //oh no
                    //find somewhere else to place the item
                    dropoffReservation = network.GetItemManagement().ReserveSpaceInBestSink(heldItemStack);
                    if(dropoffReservation == null)
                    {
                        //oh no
                        //nowhere to put this item
                        DropItems();
                        currentState = eState.GoingHome;
                        return OptionalInt.of(currentTickTime + tickOffset);
                    }
                }
                else if(nextPathStep.node == dropoffReservation.inventoryNode)
                {
                    //drop off
                    ReservableInventory inv = dropoffReservation.inventoryNode.GetReservableInventory(dropoffReservation.inventoryDirection);
                    ItemStack leftover = inv.InsertItems(dropoffReservation.reservation, heldItemStack);
                    if(leftover.isEmpty())
                    {
                        //go home
                        currentState = eState.GoingHome;
                        return OptionalInt.of(currentTickTime + tickOffset);
                    }
                    heldItemStack = leftover;
                    //else handle it not fitting in
                    dropoffReservation = network.GetItemManagement().ReserveSpaceInBestSink(heldItemStack);
                    if(dropoffReservation == null)
                    {
                        //oh no
                        //nowhere to put this item
                        DropItems();
                        //go home
                        currentState = eState.GoingHome;
                        return OptionalInt.of(currentTickTime + tickOffset);
                    }
                    //take the leftover to the new destination
                }
                targetNode = dropoffReservation.inventoryNode;
            }
            case GoingHome ->
            {
                //ToDo handle wisps properly
                if(carryWisp.sourceNode.connectedNetwork != network)
                {
                    return OptionalInt.empty();
                }
                else if(nextPathStep.node == carryWisp.sourceNode)
                {
                    //we are done
                    //insert the wisp back
                    return OptionalInt.empty();
                }
                targetNode = carryWisp.sourceNode;
            }
        }

        //continue to next node
        if(nextPathStep.node.connectedNetwork != network)
        {
            //the node we just arrived at is no longer part of the network
            //path to the nearest node, ignoring any visibility
            BlockPos currentNodePos = nextPathStep.node.GetPos();
            //ToDo get the dim correctly
            WispNode node = network.GetClosestNodeToPos(new ResourceLocation("minecraft:overworld"), currentNodePos, 16);
            if(node == null)
            {
                DropItems();
                DropWisp();
                return OptionalInt.empty();
            }
            nextPathStep = new WispNode.NextPathStep();
            nextPathStep.node = node;
            nextPathStep.distToNode = (float)Math.sqrt(node.GetPos().distSqr(currentNodePos));
        }
        else
        {
            nextPathStep = network.GetNextPathStep(nextPathStep.node, targetNode);
        }
        return OptionalInt.of(currentTickTime + tickOffset + GetNextTickOffset());
    }

    @Override
    public String GetSerialisableName()
    {
        return SERIALISABLE_NAME;
    }

    @Override
    public CompoundTag SerialiseNBT(WispNetwork network)
    {
        if(loadData != null)
        {
            return loadData;
        }
        CompoundTag nbt = super.SerialiseNBT(network);

        nbt.putInt("currentState", currentState.ordinal());
        nbt.putFloat("leftoverFractionalTick", leftoverFractionalTick);
        ResourceLocation itemResourcelocation = Registry.ITEM.getKey(item);
        nbt.putString("itemId", itemResourcelocation.toString());

        if(currentState.ordinal() <= eState.GoingToPickup.ordinal())
        {
            if(pickupReservation == null)
            {
                LogisticsWoW.LOGGER.error("Transfer task save failed, no pickup reservation");
                return null;
            }
            CompoundTag pickupNBT = new CompoundTag();
            pickupNBT.putInt("Count", pickupReservation.reservation.GetExtractCount());
            pickupNBT.putString("NodeDim", pickupReservation.inventoryNode.GetDim().toString());
            pickupNBT.put("NodePos", NbtUtils.writeBlockPos(pickupReservation.inventoryNode.GetPos()));
            if(pickupReservation.inventoryDirection != null)
            {
                pickupNBT.putInt("InvDirection", pickupReservation.inventoryDirection.get3DDataValue());
            }
            nbt.put("Pickup", pickupNBT);
        }
        if(currentState.ordinal() <= eState.GoingToDropoff.ordinal())
        {
            if(dropoffReservation == null)
            {
                LogisticsWoW.LOGGER.error("Transfer task save failed, no dropoff reservation");
                return null;
            }
            CompoundTag dropoffNBT = new CompoundTag();
            dropoffNBT.putInt("Count", dropoffReservation.reservation.GetInsertCount());
            dropoffNBT.putString("NodeDim", dropoffReservation.inventoryNode.GetDim().toString());
            dropoffNBT.put("NodePos", NbtUtils.writeBlockPos(dropoffReservation.inventoryNode.GetPos()));
            if(dropoffReservation.inventoryDirection != null)
            {
                dropoffNBT.putInt("InvDirection", dropoffReservation.inventoryDirection.get3DDataValue());
            }
            nbt.put("Dropoff", dropoffNBT);
        }
        /*

    private CarryWisp carryWisp;
        */

        if(nextPathStep != null)
        {
            CompoundTag nextStepNBT = new CompoundTag();
            nextStepNBT.putString("NodeDim", nextPathStep.node.GetDim().toString());
            nextStepNBT.put("NodePos", NbtUtils.writeBlockPos(nextPathStep.node.GetPos()));
            nextStepNBT.putFloat("DistToNode", nextPathStep.distToNode);
            nextStepNBT.putFloat("DistToDest", nextPathStep.distToDestination);

            nbt.put("NextStep", nextStepNBT);
        }

        return nbt;
    }

    private boolean TryLoad(WispNetwork network, CompoundTag nbt)
    {
        currentState = eState.values()[nbt.getInt("currentState")];
        leftoverFractionalTick = nbt.getFloat("leftoverFractionalTick");
        item = Registry.ITEM.get(new ResourceLocation(nbt.getString("itemId")));

        if(currentState.ordinal() <= eState.GoingToPickup.ordinal() && nbt.contains("Pickup"))
        {
            CompoundTag pickupNBT = nbt.getCompound("Pickup");
            int count = pickupNBT.getInt("Count");
            ResourceLocation nodeDim = new ResourceLocation(pickupNBT.getString("NodeDim"));
            BlockPos nodePos = NbtUtils.readBlockPos(pickupNBT.getCompound("NodePos"));
            Direction invDirection = null;
            if(pickupNBT.contains("InvDirection"))
            {
                invDirection = Direction.from3DDataValue(pickupNBT.getInt("InvDirection"));
            }

            WispInteractionNodeBase interactionNode = (WispInteractionNodeBase)network.GetNode(nodeDim, nodePos);
            if(interactionNode != null)
            {
                if(!interactionNode.IsActive())
                {
                    return false;
                }
                ReservableInventory inv = interactionNode.GetReservableInventory(invDirection);
                if(inv == null)
                {
                    LogisticsWoW.LOGGER.error("Transfer task load failed to get inventory from pickup node");
                    return false;
                }
                else
                {
                    ReservableInventory.Reservation reservation = inv.ReserveExtraction(item, count);

                    if(reservation != null)
                    {
                        pickupReservation = new SourcedReservation(reservation, interactionNode, invDirection);
                    }
                    else
                    {
                        LogisticsWoW.LOGGER.error("Failed to reserve pickup for transfer task");
                    }
                }
            }
            else
            {
                LogisticsWoW.LOGGER.error("Failed to find pickup node for transfer task");
            }
        }

        if(currentState.ordinal() <= eState.GoingToDropoff.ordinal() && nbt.contains("Dropoff"))
        {
            CompoundTag pickupNBT = nbt.getCompound("Dropoff");
            int count = pickupNBT.getInt("Count");
            ResourceLocation nodeDim = new ResourceLocation(pickupNBT.getString("NodeDim"));
            BlockPos nodePos = NbtUtils.readBlockPos(pickupNBT.getCompound("NodePos"));
            Direction invDirection = null;
            if(pickupNBT.contains("InvDirection"))
            {
                invDirection = Direction.from3DDataValue(pickupNBT.getInt("InvDirection"));
            }

            WispInteractionNodeBase interactionNode = (WispInteractionNodeBase)network.GetNode(nodeDim, nodePos);
            if(interactionNode != null)
            {
                if(!interactionNode.IsActive())
                {
                    return false;
                }
                ReservableInventory inv = interactionNode.GetReservableInventory(invDirection);
                if(inv == null)
                {
                    LogisticsWoW.LOGGER.error("Transfer task load failed to get inventory from dropoff node");
                    return false;
                }
                else
                {
                    ReservableInventory.Reservation reservation = inv.ReserveInsertion(new ItemStack(item, count));

                    if(reservation != null)
                    {
                        dropoffReservation = new SourcedReservation(reservation, interactionNode, invDirection);
                    }
                    else
                    {
                        LogisticsWoW.LOGGER.error("Failed to reserve dropoff for transfer task");
                    }
                }
            }
            else
            {
                LogisticsWoW.LOGGER.error("Failed to find dropoff node for transfer task");
            }
        }

        //ToDo
        carryWisp = network.TryReserveCarryWisp(null, 0);

        if(nbt.contains("NextStep"))
        {
            CompoundTag nextStepNBT = nbt.getCompound("NextStep");

            ResourceLocation nodeDim = new ResourceLocation(nextStepNBT.getString("NodeDim"));
            BlockPos nodePos = NbtUtils.readBlockPos(nextStepNBT.getCompound("NodePos"));

            nextPathStep = new WispNode.NextPathStep();
            nextPathStep.node = network.GetNode(nodeDim, nodePos);
            nextPathStep.distToNode = nextStepNBT.getFloat("DistToNode");
            nextPathStep.distToDestination = nextStepNBT.getFloat("DistToDest");
            assert nextPathStep.node != null;
        }
        else
        {
            nextPathStep = new WispNode.NextPathStep();
            nextPathStep.node = network;
            nextPathStep.distToNode = 0;
            nextPathStep.distToDestination = 0;
        }
        return true;
    }

    @Override
    public void DeserialiseNBT(WispNetwork network, CompoundTag nbt)
    {
        super.DeserialiseNBT(network, nbt);
        if(!TryLoad(network, nbt))
        {
            //failed to load, try again later
            loadData = nbt;
        }
    }
}
