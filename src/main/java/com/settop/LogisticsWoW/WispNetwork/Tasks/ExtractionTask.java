package com.settop.LogisticsWoW.WispNetwork.Tasks;

import com.settop.LogisticsWoW.WispNetwork.ItemSink;
import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import com.settop.LogisticsWoW.Wisps.WispNode;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.OptionalInt;

public abstract class ExtractionTask implements WispTask
{
    enum eState
    {
        GoingToPickup,
        GoingToDropoff,
        GoingHome
    }

    public static class ExtractionData
    {
        public ItemSink.Reservation reservation;
        public ItemStack itemStack;

        public ExtractionData(@Nonnull ItemSink.Reservation reservation, @Nonnull ItemStack itemStack)
        {
            this.reservation = reservation;
            this.itemStack = itemStack;
        }
    }

    private WispNode wispSourceNode;
    private WispNode extractFromNode;
    private WispNode extractToNode;
    //blocks per tick
    private final float speed = 0.8f;

    private eState currentState = eState.GoingToPickup;
    private WispNode.NextPathStep nextPathStep = null;
    private float leftoverFractionalTick = 0.f;
    private ExtractionData extractedData = null;

    public ExtractionTask(@Nonnull WispNode wispSourceNode, @Nonnull WispNode extractFromNode, @Nonnull WispNode extractToNode)
    {
        this.wispSourceNode = wispSourceNode;
        this.extractFromNode = extractFromNode;
        this.extractToNode = extractToNode;
    }

    public int GetNextTickOffset()
    {
        float timeToNextNode = (nextPathStep.distToNode / speed) - leftoverFractionalTick;
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
        assert wispSourceNode.connectedNetwork == network;
        assert extractFromNode.connectedNetwork == network;
        assert extractToNode.connectedNetwork == network;

        nextPathStep = network.GetNextPathStep(wispSourceNode, extractFromNode);
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
                if(extractFromNode.connectedNetwork != network)
                {
                    //go home
                    currentState = eState.GoingHome;
                    return OptionalInt.of(currentTickTime + tickOffset);
                }
                else if(nextPathStep.node == extractFromNode)
                {
                    //extract and proceed
                    extractedData = DoExtraction();
                    if(extractedData == null)
                    {
                        //oh no
                        //cancel this task and send the wisp back home
                        currentState = eState.GoingHome;
                        return OptionalInt.of(currentTickTime + tickOffset);
                    }
                    currentState = eState.GoingToDropoff;

                    nextPathStep = network.GetNextPathStep(extractFromNode, extractToNode);
                    return OptionalInt.of(currentTickTime + tickOffset + GetNextTickOffset());
                }
                targetNode = extractFromNode;
            }
            case GoingToDropoff ->
            {
                if(!extractedData.reservation.IsValid() || extractToNode.connectedNetwork != network)
                {
                    //oh no
                    //find somewhere else to place the item
                    extractedData.reservation = network.GetItemManagement().ReserveSpaceInBestSink(extractedData.itemStack);
                    if(extractedData.reservation == null)
                    {
                        //oh no
                        //nowhere to put this item
                        DropItems();
                        return OptionalInt.empty();
                    }
                    extractToNode = extractedData.reservation.sourceSink.GetAttachedWisp();
                }
                else if(nextPathStep.node == extractToNode)
                {
                    //drop off
                    ItemStack leftover = extractedData.reservation.sourceSink.ReservedInsert(extractedData.reservation, extractedData.itemStack);
                    if(leftover.isEmpty())
                    {
                        //go home
                        currentState = eState.GoingHome;
                        return OptionalInt.of(currentTickTime + tickOffset);
                    }
                    extractedData.itemStack = leftover;
                    //else handle it not fitting in
                    extractedData.reservation = network.GetItemManagement().ReserveSpaceInBestSink(extractedData.itemStack);
                    if(extractedData.reservation == null)
                    {
                        //oh no
                        //nowhere to put this item
                        DropItems();
                        //go home
                        currentState = eState.GoingHome;
                        return OptionalInt.of(currentTickTime + tickOffset);
                    }
                    //take the leftover to the new destination
                    extractToNode = extractedData.reservation.sourceSink.GetAttachedWisp();
                }
                targetNode = extractToNode;
            }
            case GoingHome ->
            {
                //ToDo handle wisps properly
                if(wispSourceNode.connectedNetwork != network)
                {
                    return OptionalInt.empty();
                }
                else if(nextPathStep.node == wispSourceNode)
                {
                    //we are done
                    //insert the wisp back
                    return OptionalInt.empty();
                }
                targetNode = wispSourceNode;
            }
        }

        //continue to next node
        if(nextPathStep.node.connectedNetwork != network)
        {
            //the node we just arrived at is no longer part of the network
            //path to the nearest node, ignoring any visibility
            BlockPos currentNodePos = nextPathStep.node.GetPos();
            //ToDo get the dim correctly
            WispNode node = network.GetClosestNodeToPos(new ResourceLocation("minecraft:dimension/overworld"), currentNodePos, 16);
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

    //returns null if the extraction failed
    abstract public ExtractionData DoExtraction();
}
