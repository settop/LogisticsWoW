package com.settop.LogisticsWoW.WispNetwork.Tasks;

import com.settop.LogisticsWoW.WispNetwork.CarryWisp;
import com.settop.LogisticsWoW.WispNetwork.ItemSink;
import com.settop.LogisticsWoW.WispNetwork.ItemSource;
import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import com.settop.LogisticsWoW.Wisps.WispNode;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.OptionalInt;

public class TransferTask implements WispTask
{
    enum eState
    {
        GoingToPickup,
        GoingToDropoff,
        GoingHome
    }
    private CarryWisp carryWisp;
    private ItemSource.Reservation pickupReservation;
    private ItemSink.Reservation dropoffReservation;

    private eState currentState = eState.GoingToPickup;
    private WispNode.NextPathStep nextPathStep = null;
    private float leftoverFractionalTick = 0.f;
    private ItemStack carriedItems;

    public TransferTask(@Nonnull CarryWisp carryWisp, @Nonnull ItemSource.Reservation pickupReservation, @Nonnull ItemSink.Reservation dropoffReservation)
    {
        this.carryWisp = carryWisp;
        this.pickupReservation = pickupReservation;
        this.dropoffReservation = dropoffReservation;
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
        assert pickupReservation.sourceSource.GetAttachedInteractionNode().connectedNetwork == network;
        assert dropoffReservation.sourceSink.GetAttachedInteractionNode().connectedNetwork == network;

        nextPathStep = network.GetNextPathStep(carryWisp.sourceNode, pickupReservation.sourceSource.GetAttachedInteractionNode());
        return startTickTime + GetNextTickOffset();
    }

    @Override
    public OptionalInt Tick(@NotNull TickEvent.ServerTickEvent tickEvent, @NotNull WispNetwork network, int currentTickTime, int tickOffset)
    {
        //reached the next node
        WispNode targetNode = null;
        switch (currentState)
        {
            case GoingToPickup ->
            {
                if(!pickupReservation.IsValid() || pickupReservation.sourceSource.GetAttachedInteractionNode().connectedNetwork != network)
                {
                    //go home
                    currentState = eState.GoingHome;
                    return OptionalInt.of(currentTickTime + tickOffset);
                }
                else if(nextPathStep.node == pickupReservation.sourceSource.GetAttachedInteractionNode())
                {
                    //extract and proceed
                    carriedItems = pickupReservation.sourceSource.ReservedExtract(pickupReservation);
                    if(carriedItems.isEmpty())
                    {
                        //nothing to carry
                        //cancel this task and send the wisp back home
                        currentState = eState.GoingHome;
                        return OptionalInt.of(currentTickTime + tickOffset);
                    }
                    currentState = eState.GoingToDropoff;

                    return OptionalInt.of(currentTickTime + tickOffset);
                }
                targetNode = pickupReservation.sourceSource.GetAttachedInteractionNode();
            }
            case GoingToDropoff ->
            {
                if(!dropoffReservation.IsValid() || dropoffReservation.sourceSink.GetAttachedInteractionNode().connectedNetwork != network)
                {
                    //oh no
                    //find somewhere else to place the item
                    dropoffReservation = network.GetItemManagement().ReserveSpaceInBestSink(carriedItems);
                    if(dropoffReservation == null)
                    {
                        //oh no
                        //nowhere to put this item
                        DropItems();
                        currentState = eState.GoingHome;
                        return OptionalInt.of(currentTickTime + tickOffset);
                    }
                }
                else if(nextPathStep.node == dropoffReservation.sourceSink.GetAttachedInteractionNode())
                {
                    //drop off
                    ItemStack leftover = dropoffReservation.sourceSink.ReservedInsert(dropoffReservation, carriedItems);
                    if(leftover.isEmpty())
                    {
                        //go home
                        currentState = eState.GoingHome;
                        return OptionalInt.of(currentTickTime + tickOffset);
                    }
                    carriedItems = leftover;
                    //else handle it not fitting in
                    dropoffReservation = network.GetItemManagement().ReserveSpaceInBestSink(carriedItems);
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
                targetNode = dropoffReservation.sourceSink.GetAttachedInteractionNode();
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
}
