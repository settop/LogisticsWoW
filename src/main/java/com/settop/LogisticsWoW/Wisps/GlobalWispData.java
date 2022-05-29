package com.settop.LogisticsWoW.Wisps;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.settop.LogisticsWoW.WispNetwork.WispNetwork;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Mod.EventBusSubscriber( modid = LogisticsWoW.MOD_ID)
public class GlobalWispData
{
    private static final HashMap<ResourceLocation, LevelWispData> worldData = new HashMap<>();

    @Nonnull
    private static synchronized LevelWispData EnsureWorldData(Level inWorld)
    {
        if(inWorld.isClientSide())
        {
            throw new RuntimeException("Trying to get chunk data on client");
        }

        ResourceLocation dimension = inWorld.dimension().location();
        return worldData.computeIfAbsent(dimension, (key)->new LevelWispData(dimension));
    }

    @Nullable
    private static synchronized LevelWispData GetWorldData(Level inWorld)
    {
        if(inWorld.isClientSide())
        {
            throw new RuntimeException("Trying to get chunk data on client");
        }

        ResourceLocation dimension = inWorld.dimension().location();
        return worldData.get(dimension);
    }

    //Returns null if wisp does not exist at this position
    public static synchronized WispBase GetWisp(Level inWorld, BlockPos inPos)
    {
        if(inWorld.isClientSide())
        {
            throw new RuntimeException("Trying to get chunk data on client");
        }
        LevelWispData dimData = GetWorldData(inWorld);
        if(dimData != null)
        {
            ChunkWisps chunkWisps = dimData.GetChunkWisps(inPos);
            if(chunkWisps != null)
            {
                for(WispBase unregisteredWisp : chunkWisps.unregisteredWispsInChunk)
                {
                    if(unregisteredWisp.GetPos().equals(inPos))
                    {
                        return unregisteredWisp;
                    }
                }
                for(WispBase registeredWisp : chunkWisps.registeredWispsInChunk)
                {
                    if(registeredWisp.GetPos().equals(inPos))
                    {
                        return registeredWisp;
                    }
                }
            }
        }

        return null;
    }

    private static synchronized WispBase TryClaimWisp(Level inWorld, BlockPos inPos)
    {
        if(inWorld.isClientSide())
        {
            throw new RuntimeException("Trying to get chunk data on client");
        }
        LevelWispData dimData = GetWorldData(inWorld);
        if(dimData != null)
        {
            ChunkWisps chunkWisps = dimData.GetChunkWisps(inPos);
            if(chunkWisps != null)
            {
                for(Iterator<WispBase> it = chunkWisps.unclaimedWispsInChunk.iterator(); it.hasNext(); )
                {
                    WispBase existingWisp = it.next();
                    if(existingWisp.GetPos().equals(inPos))
                    {
                        it.remove();
                        chunkWisps.unregisteredWispsInChunk.add(existingWisp);
                        return existingWisp;
                    }
                }

                if(LogisticsWoW.DEBUG)
                {
                    for(WispBase wisp : chunkWisps.unregisteredWispsInChunk)
                    {
                        if(wisp.GetPos().equals(inPos))
                        {
                            throw new RuntimeException("Trying to claim an already claimed wisp");
                        }
                    }

                    for(WispBase wisp : chunkWisps.registeredWispsInChunk)
                    {
                        if(wisp.GetPos().equals(inPos))
                        {
                            throw new RuntimeException("Trying to claim an already claimed wisp");
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get's the wisp at the given position in the world if there is one
     * Else creates a new wisp of the supplied type
     *
     * Return's the wisp and a boolean indicating if the wisp is a newly created one
     **/
    public static synchronized Tuple<WispBase, Boolean> CreateOrClaimWisp(String type, Level inWorld, BlockPos inPos, CompoundTag tagData)
    {
        if(inWorld.isClientSide())
        {
            throw new RuntimeException("Trying to get chunk data on client");
        }

        WispBase existingWisp = TryClaimWisp(inWorld, inPos);
        if(existingWisp != null)
        {
            return new Tuple<> (existingWisp, false);
        }

        WispBase newWisp = WispFactory.CreateNewWisp(type, inWorld.dimension().location(), inPos);
        newWisp.InitFromTagData(tagData);

        LevelWispData dimData = EnsureWorldData(inWorld);
        ChunkWisps chunkWisps = dimData.EnsureChunkWisps(inPos);
        chunkWisps.unregisteredWispsInChunk.add(newWisp);

        return new Tuple<>(newWisp, true);
    }

    public static synchronized WispNetwork GetWispNetwork(ResourceLocation dim, BlockPos pos)
    {
        LevelWispData dimData = worldData.get(dim);
        if(dimData == null)
        {
            return null;
        }
        for(WispNetwork existingNetwork : dimData.wispNetworks)
        {
            if(existingNetwork.pos.equals(pos))
            {
                return existingNetwork;
            }
        }
        return null;
    }

    public static synchronized WispNetwork GetOrCreateWispNetwork(Level world, BlockPos pos)
    {
        LevelWispData worldData = EnsureWorldData(world);
        for(WispNetwork existingNetwork : worldData.wispNetworks)
        {
            if(existingNetwork.pos.equals(pos))
            {
                return existingNetwork;
            }
        }

        WispNetwork newNetwork = new WispNetwork(worldData.dim, pos);
        worldData.wispNetworks.add(newNetwork);
        return newNetwork;
    }

    public static synchronized void RemoveWispNetwork(Level world, WispNetwork network)
    {
        LevelWispData worldData = EnsureWorldData(world);
        worldData.wispNetworks.remove(network);
    }

    private static synchronized WispNode TryClaimExistingNode(Level inWorld, BlockPos pos)
    {
        if(inWorld.isClientSide())
        {
            throw new RuntimeException("Trying to get chunk data on client");
        }
        LevelWispData dimData = GetWorldData(inWorld);
        if(dimData != null)
        {
            ChunkWisps chunkWisps = dimData.GetChunkWisps(pos);
            if(chunkWisps != null)
            {
                for(Iterator<WispNode> it = chunkWisps.unclaimedWispConnectionNodes.iterator(); it.hasNext(); )
                {
                    WispNode existingNode = it.next();
                    if(existingNode.GetPos().equals(pos))
                    {
                        it.remove();
                        if(existingNode.GetConnectedNetwork() == null)
                        {
                            chunkWisps.unregisteredWispConnectionNodes.add(existingNode);
                        }
                        else
                        {
                            chunkWisps.registeredWispConnectionNodes.add(existingNode);
                        }
                        return existingNode;
                    }
                }

                for(WispNode node : chunkWisps.unregisteredWispConnectionNodes)
                {
                    if(node.GetPos().equals(pos))
                    {
                        throw new RuntimeException("Trying to claim an already claimed node");
                    }
                }

                for(WispNode node : chunkWisps.registeredWispConnectionNodes)
                {
                    if(node.GetPos().equals(pos))
                    {
                        throw new RuntimeException("Trying to claim an already claimed node");
                    }
                }
            }
        }

        return null;
    }

    public static synchronized WispNode CreateOrClaimNode(Level inWorld, BlockPos pos)
    {
        if(inWorld.isClientSide())
        {
            throw new RuntimeException("Trying to get chunk data on client");
        }

        WispNode existingNode = TryClaimExistingNode(inWorld, pos);
        if(existingNode != null)
        {
            return existingNode;
        }

        WispNode newNode = new WispNode(pos);

        LevelWispData dimData = EnsureWorldData(inWorld);
        ChunkWisps chunkData = dimData.EnsureChunkWisps(pos);
        chunkData.unregisteredWispConnectionNodes.add(newNode);

        return newNode;
    }

    private static boolean CheckIfValidNetworkConnection(WispNode sourceNode, WispNode testNode)
    {
        for(WispNode n = testNode; ; n = n.networkConnectionNode.node.get())
        {
            if(n.GetConnectedNetwork() != null && n.networkConnectionNode == null)
            {
                //n is connected directly to the network
                return true;
            }
            else if(n.GetConnectedNetwork() == null)
            {
                return false;
            }
            else if(n.networkConnectionNode.node.get() == sourceNode)
            {
                //it's a loop
                return false;
            }
        }
    }

    public static synchronized void RemoveNode(Level inWorld, WispNode node)
    {
        LevelWispData dimData = EnsureWorldData(inWorld);
        ChunkWisps chunkData = dimData.EnsureChunkWisps(node.GetPos());

        node.RemoveFromWorld(inWorld);
        chunkData.unregisteredWispConnectionNodes.remove(node);
        chunkData.registeredWispConnectionNodes.remove(node);

        WispNetwork connectedNetwork = node.GetConnectedNetwork();
        if(connectedNetwork == null)
        {
            //done
            return;
        }
        connectedNetwork.RemoveNode(inWorld, node);
        node.connectedNetwork = null;
        node.networkConnectionNode = null;

        //now need to remove it's connections
        ArrayList<WispNode> orphanedNodes = new ArrayList<WispNode>();
        for(WispNode.Connection connection : node.connectedNodes)
        {
            WispNode otherNode = connection.node.get();
            for(Iterator<WispNode.Connection> otherConnectionIt = otherNode.connectedNodes.iterator(); otherConnectionIt.hasNext();)
            {
                WispNode.Connection otherConnection = otherConnectionIt.next();
                if(otherConnection.nodePos.equals(node.GetPos()))
                {
                    if(otherNode.networkConnectionNode == otherConnection)
                    {
                        otherNode.networkConnectionNode = null;
                        otherNode.connectedNetwork = null;
                        orphanedNodes.add(otherNode);
                    }
                    otherConnectionIt.remove();
                    break;
                }
            }
        }
        node.inactiveConnections.clear();
        node.connectedNodes.clear();

        boolean updated = true;
        while(updated)
        {
            updated = false;
            for (int i = 0; i < orphanedNodes.size(); ++i)
            {
                WispNode orphanedNode = orphanedNodes.get(i);
                //first check if we can connect via another connected node
                for (WispNode.Connection connection : orphanedNode.connectedNodes)
                {
                    if (CheckIfValidNetworkConnection(orphanedNode, connection.node.get()))
                    {
                        orphanedNode.connectedNetwork = connectedNetwork;
                        orphanedNode.networkConnectionNode = connection;
                        break;
                    }
                }
                if (orphanedNode.connectedNetwork != null)
                {
                    //we managed to re-connect this node to the network
                    //remove it from the orphaned list
                    updated = true;
                    orphanedNodes.remove(i);
                    --i;
                }
                else
                {
                    //spread the orphanness
                    for (WispNode.Connection connection : orphanedNode.connectedNodes)
                    {
                        if (connection.node.get().networkConnectionNode != null && connection.node.get().networkConnectionNode.node.get() == orphanedNode)
                        {
                            connection.node.get().networkConnectionNode = null;
                            connection.node.get().connectedNetwork = null;
                            orphanedNodes.add(connection.node.get());
                        }
                    }
                }
            }
        }

        //any remaining orphaned nodes couldn't be re-connected to the network
        for(WispNode orphanedNode : orphanedNodes)
        {
            for(WispNode.Connection connection : orphanedNode.connectedNodes)
            {
                connection.node.get().connectedNodes.removeIf((otherConnection) -> otherConnection.nodePos.equals(orphanedNode.GetPos()));
            }
            orphanedNode.connectedNodes.clear();
            orphanedNode.inactiveConnections.clear();

            ChunkWisps otherChunkData = dimData.EnsureChunkWisps(orphanedNode.GetPos());

            connectedNetwork.RemoveNode(inWorld, orphanedNode);
            if(!otherChunkData.registeredWispConnectionNodes.remove(orphanedNode))
            {
                LogisticsWoW.LOGGER.error("Failed to remove orphaned node from registered connection nodes");
            }
            otherChunkData.unregisteredWispConnectionNodes.add(orphanedNode);
        }
    }

    public static synchronized void TryAndConnectNodeToANetwork(Level level, WispNode node, int autoConnectRange)
    {
        LevelWispData dimData = GetWorldData(level);
        if(dimData == null)
        {
            return;
        }
        ChunkWisps chunkWisps = dimData.GetChunkWisps(node.GetPos());
        if(chunkWisps == null)
        {
            return;
        }

        boolean connectedToANetwork = false;
        WispNetwork connectedNetwork = null;

        node.SetAutoConnectRange(autoConnectRange);
        for(WispNetwork network : dimData.wispNetworks)
        {
            if(network.TryDirectConnectNode(level, node))
            {
                if(!chunkWisps.unregisteredWispConnectionNodes.remove(node))
                {
                    LogisticsWoW.LOGGER.warn("Connecting a node to a network, but it is not unregistered");
                }
                chunkWisps.registeredWispConnectionNodes.add(node);
                connectedToANetwork = true;
                connectedNetwork = network;
                break;
            }
        }

        final Vec3i maxAutoConnectRangeVec = new Vec3i(WispNode.MaxAutoConnectRange, 0, WispNode.MaxAutoConnectRange);
        if(!connectedToANetwork)
        {
            //see if we can get a connection via any nearby nodes
            ChunkPos chunkMinPos = Utils.GetChunkPos(node.GetPos().subtract( maxAutoConnectRangeVec));
            ChunkPos chunkMaxPos = Utils.GetChunkPos(node.GetPos().offset( maxAutoConnectRangeVec));

            for(int x = chunkMinPos.x; x <= chunkMaxPos.x && !connectedToANetwork; ++x)
            for(int z = chunkMinPos.z; z <= chunkMaxPos.z && !connectedToANetwork; ++z)
            {
                ChunkPos chunkToCheck = new ChunkPos(x, z);
                ChunkWisps nearbyChunk = dimData.GetChunkWisps(chunkToCheck);
                if(nearbyChunk != null)
                {
                    for(WispNode registeredNode : nearbyChunk.registeredWispConnectionNodes)
                    {
                        if(node.CanConnectToPos(level, Vec3.atCenterOf(registeredNode.GetPos()), Math.max(registeredNode.GetAutoConnectRange(), autoConnectRange)))
                        {
                            node.AddConnectionAndNetworkConnection(registeredNode, WispNode.eConnectionType.AutoConnect);
                            connectedNetwork = registeredNode.GetConnectedNetwork();
                            connectedNetwork.AddNode(node);
                            //else connecting to an unloaded network
                            connectedToANetwork = true;
                            if(!chunkWisps.unregisteredWispConnectionNodes.remove(node))
                            {
                                LogisticsWoW.LOGGER.warn("Connecting a node to a network, but it is not unregistered");
                            }
                            chunkWisps.registeredWispConnectionNodes.add(node);
                            break;
                        }
                    }
                }
            }
        }

        if(!connectedToANetwork)
        {
            //didn't manage to connect
            return;
        }

        HashSet<WispNode> newlyRegisteredNodes = new HashSet<>();
        ArrayDeque<WispNode> queuedTestConnection = new ArrayDeque<>();
        newlyRegisteredNodes.add(node);
        queuedTestConnection.add(node);

        while(!queuedTestConnection.isEmpty())
        {
            WispNode nextNodeToConnect = queuedTestConnection.pop();
            ChunkPos chunkMinPos = Utils.GetChunkPos(nextNodeToConnect.GetPos().subtract( maxAutoConnectRangeVec));
            ChunkPos chunkMaxPos = Utils.GetChunkPos(nextNodeToConnect.GetPos().offset( maxAutoConnectRangeVec));
            for(int x = chunkMinPos.x; x <= chunkMaxPos.x; ++x)
            for(int z = chunkMinPos.z; z <= chunkMaxPos.z; ++z)
            {
                ChunkPos chunkToCheck = new ChunkPos(x, z);
                ChunkWisps nearbyChunk = dimData.GetChunkWisps(chunkToCheck);
                if(nearbyChunk == null) continue;
                for (Iterator<WispNode> it = nearbyChunk.unregisteredWispConnectionNodes.iterator(); it.hasNext();)
                {
                    WispNode unregisteredNode = it.next();
                    int maxAutoConnectRange = Math.max(unregisteredNode.GetAutoConnectRange(), nextNodeToConnect.GetAutoConnectRange());
                    if(nextNodeToConnect.CanConnectToPos(level, Vec3.atCenterOf(unregisteredNode.GetPos()), maxAutoConnectRange))
                    {
                        unregisteredNode.AddConnectionAndNetworkConnection(nextNodeToConnect, WispNode.eConnectionType.AutoConnect);
                        nearbyChunk.registeredWispConnectionNodes.add(unregisteredNode);
                        if(connectedNetwork != null)
                        {
                            //the network we are connecting to is loaded
                            connectedNetwork.AddNode(unregisteredNode);
                        }
                        it.remove();
                        if(newlyRegisteredNodes.add(unregisteredNode))
                        {
                            queuedTestConnection.add(unregisteredNode);
                        }
                    }
                }
                for(WispNode registeredNode : nearbyChunk.registeredWispConnectionNodes)
                {
                    if(registeredNode == nextNodeToConnect)
                        continue;

                    int maxAutoConnectRange = Math.max(registeredNode.GetAutoConnectRange(), nextNodeToConnect.GetAutoConnectRange());
                    if(nextNodeToConnect.CanConnectToPos(level, Vec3.atCenterOf(registeredNode.GetPos()), maxAutoConnectRange))
                    {
                        nextNodeToConnect.EnsureConnection(registeredNode, WispNode.eConnectionType.AutoConnect);
                    }
                }
            }
        }
    }
/*
    @SubscribeEvent
    public static synchronized void OnTick(TickEvent.WorldTickEvent tickEvent)
    {
        if(tickEvent.side != LogicalSide.SERVER)
        {
            return;
        }

        LevelWispData dimData = GetWorldData(tickEvent.world);
        if(dimData != null)
        {
            ArrayList<WispNode> orphanedNodes = new ArrayList<>();
            ArrayList<WispBase> orphanedWisps = new ArrayList<>();
            for (WispNode node : dimData.queuedRemovedNode)
            {
                WispNetwork network = node.GetConnectedNetwork();
                if(network == null)
                {
                    dimData.EnsureChunkWisps(node.pos).unregisteredWispConnectionNodes.remove(node.pos, node);
                }
                else
                {
                    network.RemoveNode(tickEvent.world, node, orphanedNodes, orphanedWisps);
                }
            }
            dimData.queuedRemovedNode.clear();
            dimData.queuedAutoConnectNode.addAll(orphanedNodes);

            for(LevelWispData worldData : worldData.values())
            {
                for (WispNetwork wispNetwork : worldData.wispNetworks)
                {
                    boolean anyAdded = false;
                    for (Iterator<WispNode> it = dimData.queuedAutoConnectNode.iterator(); it.hasNext(); )
                    {
                        WispNode node = it.next();
                        if (wispNetwork.TryConnectNode(tickEvent.world, node))
                        {
                            anyAdded = true;
                            it.remove();
                        }
                    }

                    if (anyAdded)
                    {
                        //the new node might allow some unregistered nodes and wisps to connected to the network now
                        TryAddUnregisteredToNetwork(tickEvent.world, wispNetwork);
                    }
                }
            }
            //add any remaining nodes to the unregistered nodes
            for(WispNode node : dimData.queuedAutoConnectNode)
            {
                dimData.EnsureChunkWisps(node.pos).unregisteredWispConnectionNodes.put(node.pos, node);
            }
            dimData.queuedAutoConnectNode.clear();

            for(WispBase wisp : orphanedWisps)
            {
                boolean addedToANetwork = false;
                for(LevelWispData worldData : worldData.values())
                {
                    for (WispNetwork wispNetwork : worldData.wispNetworks)
                    {
                        if (wispNetwork.TryConnectWisp(tickEvent.world, wisp))
                        {
                            addedToANetwork = true;
                            break;
                        }
                    }
                }

                if(!addedToANetwork)
                {
                    dimData.EnsureChunkWisps(wisp.GetPos()).unregisteredWispsInChunk.put(wisp.GetPos(), wisp);
                }
            }
        }

    }

     */

    @SubscribeEvent
    public static synchronized void OnChunkSave(ChunkDataEvent.Save saveEvent)
    {
        if(saveEvent.getWorld().isClientSide()) return;

        LevelWispData dimData = GetWorldData((Level) saveEvent.getWorld());

        if(dimData != null)
        {
            dimData.OnChunkSave(saveEvent);
        }
    }

    @SubscribeEvent
    public static synchronized void OnChunkLoad(ChunkDataEvent.Load loadEvent)
    {
        if(loadEvent.getStatus() != ChunkStatus.ChunkType.LEVELCHUNK) return;
        if(loadEvent.getWorld().isClientSide()) return;

        LevelWispData dimData = EnsureWorldData((Level)loadEvent.getWorld());
        dimData.OnChunkLoad(loadEvent);
    }

    @SubscribeEvent
    public static synchronized void OnChunkUnload(ChunkDataEvent.Unload unloadEvent)
    {
        if(unloadEvent.getWorld().isClientSide()) return;
    }

    @SubscribeEvent
    public static synchronized void OnWorldLoad(WorldEvent.Load loadEvent)
    {
        if(loadEvent.getWorld().isClientSide()) return;

        ServerLevel serverWorld = (ServerLevel)loadEvent.getWorld();
        if(serverWorld == null)
        {
            return;
        }

        serverWorld.getChunkSource().getDataStorage().computeIfAbsent
                (
                        (CompoundTag tag)->
                        {
                            LevelWispData wispData = EnsureWorldData(serverWorld);
                            wispData.load(tag);
                            return wispData;
                        },
                        ()->EnsureWorldData(serverWorld),
                        LevelWispData.NAME
                );
    }

    @SubscribeEvent
    public static synchronized void OnWorldUnload(WorldEvent.Unload unloadEvent)
    {
        if(unloadEvent.getWorld().isClientSide()) return;

        ResourceLocation dimension =  ((Level)unloadEvent.getWorld()).dimension().location();
        worldData.remove(dimension);
    }

    @SubscribeEvent
    public static synchronized void OnNeighborUpdate(BlockEvent.NeighborNotifyEvent updateEvent)
    {
        if(updateEvent.getWorld().isClientSide()) return;

        if(updateEvent.getState().getBlock() != Blocks.AIR)
        {
            //we only care about blocks being removed, i.e. set to air
            return;
        }

        WispBase wisp = GetWisp((Level)updateEvent.getWorld(), updateEvent.getPos());

        if(wisp == null) return;

        wisp.RemoveFromWorld((Level)updateEvent.getWorld());
        wisp.DropItemStackIntoWorld(updateEvent.getWorld());
    }

    @OnlyIn(Dist.CLIENT)
    public static synchronized void RenderConnections(RenderLevelLastEvent evt)
    {
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer builder = buffer.getBuffer(RenderType.LINES);

        Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        //Matrix4f viewProj = evt.getProjectionMatrix();
        PoseStack poseStack = evt.getPoseStack();
        poseStack.pushPose();
        //poseStack.mulPoseMatrix(viewProj);
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for(LevelWispData worldData : worldData.values())
        {
            for (WispNetwork wispNetwork : worldData.wispNetworks)
            {
                wispNetwork.Render(evt, poseStack, builder);
            }
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.5f, 0.5f);
            Matrix4f matrix = poseStack.last().pose();
            for(ChunkWisps chunkData : worldData.chunkData.values())
            {
                chunkData.registeredWispConnectionNodes.forEach((node)->
                {
                    for(WispNode.Connection connection : node.connectedNodes)
                    {
                        float rgb[] = { 0.f, 1.f, 0.f };
                        if(connection.connectionType == WispNode.eConnectionType.Link)
                        {
                            rgb[0] = 0.f;
                            rgb[1] = 0.f;
                            rgb[2] = 1.f;
                        }

                        builder.vertex(matrix, node.GetPos().getX(), node.GetPos().getY(), node.GetPos().getZ())
                                .color(rgb[0], rgb[1], rgb[2], 0.8f)
                                //.overlayCoords(OverlayTexture.NO_OVERLAY)
                                //.lightmap(15728880)
                                .normal(0.f, 1.f, 0.f)
                                .endVertex();

                        builder.vertex(matrix, connection.node.get().GetPos().getX(), connection.node.get().GetPos().getY(), connection.node.get().GetPos().getZ())
                                .color(rgb[0], rgb[1], rgb[2], 0.8f)
                                //.overlayCoords(OverlayTexture.NO_OVERLAY)
                                //.lightmap(15728880)
                                .normal(0.f, 1.f, 0.f)
                                .endVertex();

                    }

                    //show connection to the network
                    builder.vertex(matrix, node.GetPos().getX(), node.GetPos().getY(), node.GetPos().getZ())
                            .color(0.4f, 0.4f, 0.4f, 0.8f)
                            //.overlayCoords(OverlayTexture.NO_OVERLAY)
                            //.lightmap(15728880)
                            .normal(0.f, 1.f, 0.f)
                            .endVertex();

                    builder.vertex(matrix, node.GetConnectedNetwork().GetPos().getX(), node.GetConnectedNetwork().GetPos().getY(), node.GetConnectedNetwork().GetPos().getZ())
                            .color(0.4f, 0.4f, 0.4f, 0.8f)
                            //.overlayCoords(OverlayTexture.NO_OVERLAY)
                            //.lightmap(15728880)
                            .normal(0.f, 1.f, 0.f)
                            .endVertex();
                });
            }
            poseStack.popPose();
        }

        poseStack.popPose();
        buffer.endBatch(RenderType.LINES);
    }
}
