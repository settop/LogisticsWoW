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
import java.lang.ref.WeakReference;
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
                for (WispBase existingWisp : chunkWisps.unregisteredWispsInChunk)
                {
                    if (existingWisp.GetPos().equals(inPos))
                    {
                        if(existingWisp.claimed)
                        {
                            throw new RuntimeException("Trying to claim an already claimed wisp");
                        }
                        existingWisp.claimed = true;
                        return existingWisp;
                    }
                }

            }
        }

        for(LevelWispData levelData : worldData.values())
        {
            for(WispNetwork network : levelData.wispNetworks)
            {
                //network.TryClaimExistingWisp(inWorld, inPos);
                throw new RuntimeException("NYI");
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
                for (WispNode existingNode : chunkWisps.unregisteredWispConnectionNodes)
                {
                    if (existingNode.GetPos().equals(pos))
                    {
                        if(existingNode.claimed)
                        {
                            throw new RuntimeException("Trying to claim an already claimed node");
                        }
                        existingNode.claimed = true;
                        return existingNode;
                    }
                }

            }
        }

        for(LevelWispData levelData : worldData.values())
        {
            for(WispNetwork network : levelData.wispNetworks)
            {
                WispNode existingNode = network.TryClaimExistingNode(inWorld, pos);
                if(existingNode != null)
                {
                    return existingNode;
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
        newNode.claimed = true;

        LevelWispData dimData = EnsureWorldData(inWorld);
        ChunkWisps chunkData = dimData.EnsureChunkWisps(pos);
        chunkData.unregisteredWispConnectionNodes.add(newNode);

        return newNode;
    }

    public static synchronized WispNode GetNode(Level level, BlockPos pos)
    {
        if(level.isClientSide())
        {
            throw new RuntimeException("Trying to get chunk data on client");
        }

        LevelWispData dimData = GetWorldData(level);
        if(dimData != null)
        {
            ChunkWisps chunkWisps = dimData.GetChunkWisps(pos);
            if(chunkWisps != null)
            {
                for(WispNode unregisteredNode : chunkWisps.unregisteredWispConnectionNodes)
                {
                    if(unregisteredNode.GetPos().equals(pos))
                    {
                        return unregisteredNode;
                    }
                }
            }
        }

        for(LevelWispData data : worldData.values())
        {
            for(WispNetwork wispNetwork : data.wispNetworks)
            {
                WispNode node = wispNetwork.GetNode(level, pos);
                if(node != null)
                {
                    return node;
                }
            }
        }

        return null;
    }

    public static synchronized void RemoveNode(Level inWorld, WispNode node)
    {
        LevelWispData dimData = EnsureWorldData(inWorld);
        ChunkWisps chunkData = dimData.EnsureChunkWisps(node.GetPos());

        node.RemoveFromWorld(inWorld);
        if(chunkData.unregisteredWispConnectionNodes.remove(node))
        {
            return;
        }

        WispNetwork connectedNetwork = node.GetConnectedNetwork();
        if(connectedNetwork == null)
        {
            //done
            return;
        }
        ArrayList<WispNode> orphanedNodes = connectedNetwork.RemoveNode(inWorld, node);

        //any remaining orphaned nodes couldn't be re-connected to the network
        for(WispNode orphanedNode : orphanedNodes)
        {
            ChunkWisps otherChunkData = dimData.EnsureChunkWisps(orphanedNode.GetPos());
            otherChunkData.unregisteredWispConnectionNodes.add(orphanedNode);
        }
    }

    private static synchronized void TryResolveExistingConnections(Level level, WispNode node)
    {
        LevelWispData dimData = GetWorldData(level);
        if(dimData == null)
        {
            return;
        }
        for(Iterator<WispNode.Connection> it = node.connectedNodes.iterator(); it.hasNext();)
        {
            WispNode.Connection connection = it.next();
            if(connection.node.get() == null)
            {
                ChunkWisps chunkWisps = dimData.GetChunkWisps(connection.nodePos);
                if(chunkWisps != null)
                {
                    boolean foundNode = false;
                    for(WispNode unregisteredNode : chunkWisps.unregisteredWispConnectionNodes)
                    {
                        if(unregisteredNode.GetPos().equals(connection.nodePos))
                        {
                            //now check that this node has a connection to us still
                            Optional<WispNode.Connection> backConnection = unregisteredNode.connectedNodes.stream().filter(otherConnection->otherConnection.nodePos.equals(node.GetPos())).findFirst();

                            if(backConnection.isPresent())
                            {
                                connection.node = new WeakReference<>(unregisteredNode);
                                backConnection.get().node = new WeakReference<>(node);
                                foundNode = true;
                            }
                            break;
                        }
                    }
                    if(!foundNode)
                    {
                        //the node should be loaded since it's chunk is loaded
                        //don't need to check if the node is in a network, since if it was, then this node should be as well
                        it.remove();
                    }
                }
            }
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

        WispNetwork connectedNetwork = null;

        node.SetAutoConnectRange(autoConnectRange);
        for(LevelWispData levelData : worldData.values())
        {
            for(WispNetwork network : levelData.wispNetworks)
            {
                if(network.TryAndConnectNodeToNetwork(level, node))
                {
                    if(!chunkWisps.unregisteredWispConnectionNodes.remove(node))
                    {
                        LogisticsWoW.LOGGER.warn("Connecting a node to a network, but it is not unregistered");
                    }
                    connectedNetwork = network;
                    break;
                }
            }
            if(connectedNetwork != null)
            {
                break;
            }
        }

        if(connectedNetwork == null)
        {
            return;
        }

        final Vec3i maxAutoConnectRangeVec = new Vec3i(WispNode.MaxAutoConnectRange, 0, WispNode.MaxAutoConnectRange);

        ArrayDeque<WispNode> queuedTestConnection = new ArrayDeque<>();
        queuedTestConnection.add(node);

        while(!queuedTestConnection.isEmpty())
        {
            WispNode recentConnectionNode = queuedTestConnection.pop();
            ChunkPos chunkMinPos = Utils.GetChunkPos(recentConnectionNode.GetPos().subtract( maxAutoConnectRangeVec));
            ChunkPos chunkMaxPos = Utils.GetChunkPos(recentConnectionNode.GetPos().offset( maxAutoConnectRangeVec));
            for(int x = chunkMinPos.x; x <= chunkMaxPos.x; ++x)
            for(int z = chunkMinPos.z; z <= chunkMaxPos.z; ++z)
            {
                ChunkPos chunkToCheck = new ChunkPos(x, z);
                ChunkWisps nearbyChunk = dimData.GetChunkWisps(chunkToCheck);
                if(nearbyChunk == null) continue;
                for (Iterator<WispNode> it = nearbyChunk.unregisteredWispConnectionNodes.iterator(); it.hasNext();)
                {
                    WispNode unregisteredNode = it.next();
                    TryResolveExistingConnections(level, unregisteredNode);

                    if(connectedNetwork.TryAndConnectNodeToNetworkViaNode(level, unregisteredNode, recentConnectionNode))
                    {
                        it.remove();
                        queuedTestConnection.add(unregisteredNode);
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
        }

        poseStack.popPose();
        buffer.endBatch(RenderType.LINES);
    }
}
