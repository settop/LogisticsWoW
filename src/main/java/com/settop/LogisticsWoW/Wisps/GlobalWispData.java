package com.settop.LogisticsWoW.Wisps;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
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
    private static synchronized LevelWispData EnsureWorldData(ResourceLocation dimension)
    {
        return worldData.computeIfAbsent(dimension, (key)->new LevelWispData(dimension));
    }

    @Nullable
    private static synchronized LevelWispData GetWorldData(ResourceLocation dimension)
    {
        return worldData.get(dimension);
    }

    //Returns null if wisp does not exist at this position
    public static synchronized WispInteractionNodeBase GetWisp(Level inWorld, BlockPos inPos)
    {
        WispNode node = GetNode(inWorld, inPos);
        if(node instanceof WispInteractionNodeBase)
        {
            return (WispInteractionNodeBase)node;
        }

        return null;
    }

    private static synchronized WispInteractionNodeBase TryClaimWisp(Level inWorld, BlockPos inPos)
    {
        WispNode node = TryClaimExistingNode(inWorld, inPos);
        if(node == null)
        {
            return null;
        }
        if(!(node instanceof WispInteractionNodeBase))
        {
            throw new RuntimeException("Claiming a wisp claimed a node instead");
        }

        return (WispInteractionNodeBase)node;
    }

    /**
     * Get's the wisp at the given position in the world if there is one
     * Else creates a new wisp of the supplied type
     *
     * Return's the wisp and a boolean indicating if the wisp is a newly created one
     **/
    public static synchronized Tuple<WispInteractionNodeBase, Boolean> CreateOrGetWisp(String type, Level inWorld, BlockPos inPos, CompoundTag tagData)
    {
        if(inWorld.isClientSide())
        {
            throw new RuntimeException("Trying to get chunk data on client");
        }

        WispInteractionNodeBase existingWisp = GetWisp(inWorld, inPos);
        if(existingWisp != null)
        {
            if(!existingWisp.GetType().equals(type))
            {
                LogisticsWoW.LOGGER.error("Getting a wisp, but it is of the wrong type. Existing type: {} Expected type: {}", existingWisp.GetType(), type);
            }
            return new Tuple<> (existingWisp, false);
        }

        WispInteractionNodeBase newWisp = WispFactory.CreateNewWisp(type, inWorld.dimension().location(), inPos);
        newWisp.InitFromTagData(tagData);
        newWisp.claimed = true;

        LevelWispData dimData = EnsureWorldData(inWorld.dimension().location());
        ChunkWisps chunkWisps = dimData.EnsureChunkWisps(inPos);
        chunkWisps.unregisteredNodes.add(newWisp);
        TryAndConnectNodeToANetwork(inWorld, newWisp);
        newWisp.SetConnectedBlockEntity(inWorld.getBlockEntity(inPos));

        inWorld.getChunk(inPos).setUnsaved(true);

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
            if(existingNetwork.GetPos().equals(pos))
            {
                return existingNetwork;
            }
        }
        return null;
    }

    public static synchronized WispNetwork CreateOrClaimWispNetwork(Level world, BlockPos pos)
    {
        LevelWispData worldData = EnsureWorldData(world.dimension().location());
        for(WispNetwork existingNetwork : worldData.wispNetworks)
        {
            if(existingNetwork.GetPos().equals(pos))
            {
                if(existingNetwork.claimed)
                {
                    throw new RuntimeException("Claiming an already claimed wisp network");
                }
                existingNetwork.claimed = true;
                return existingNetwork;
            }
        }

        WispNetwork newNetwork = new WispNetwork(worldData.dim, pos);
        newNetwork.claimed = true;
        worldData.wispNetworks.add(newNetwork);
        world.getChunk(pos).setUnsaved(true);
        return newNetwork;
    }

    public static synchronized void RemoveWispNetwork(Level world, WispNetwork network)
    {
        LevelWispData worldData = EnsureWorldData(world.dimension().location());
        worldData.wispNetworks.remove(network);
        HandleOrphanedNodes(network.RemoveFromWorld());
    }

    public static synchronized void TryAndConnectNetworkToNodes(Level level, WispNetwork network)
    {
        LevelWispData dimData = GetWorldData(level.dimension().location());
        if(dimData == null)
        {
            return;
        }
        if(LogisticsWoW.DEBUG)
        {
            if (!dimData.wispNetworks.contains(network))
            {
                throw new RuntimeException("Trying to connect a network we don't know about");
            }
        }

        ArrayDeque<WispNode> queuedTestConnection = new ArrayDeque<>();

        {
            final Vec3i maxNetworkAutoConnectRangeVec = new Vec3i(WispNode.MaxAutoConnectRange + 1, 0, WispNode.MaxAutoConnectRange + 1);
            ChunkPos chunkMinPos = Utils.GetChunkPos(network.GetPos().subtract( maxNetworkAutoConnectRangeVec));
            ChunkPos chunkMaxPos = Utils.GetChunkPos(network.GetPos().offset( maxNetworkAutoConnectRangeVec));

            for(int x = chunkMinPos.x; x <= chunkMaxPos.x; ++x)
            for(int z = chunkMinPos.z; z <= chunkMaxPos.z; ++z)
            {
                ChunkPos chunkToCheck = new ChunkPos(x, z);
                ChunkWisps nearbyChunk = dimData.GetChunkWisps(chunkToCheck);
                if(nearbyChunk == null) continue;
                for (Iterator<WispNode> it = nearbyChunk.unregisteredNodes.iterator(); it.hasNext();)
                {
                    WispNode unregisteredNode = it.next();
                    TryResolveExistingConnections(level, unregisteredNode);

                    if(network.TryAndConnectNodeToNetwork(level, unregisteredNode))
                    {
                        it.remove();
                        queuedTestConnection.add(unregisteredNode);
                    }
                }
            }
        }


        final Vec3i maxAutoConnectRangeVec = new Vec3i(WispNode.MaxAutoConnectRange, 0, WispNode.MaxAutoConnectRange);

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
                for (Iterator<WispNode> it = nearbyChunk.unregisteredNodes.iterator(); it.hasNext();)
                {
                    WispNode unregisteredNode = it.next();
                    TryResolveExistingConnections(level, unregisteredNode);

                    if(network.TryAndConnectNodeToNetworkViaNode(level, unregisteredNode, recentConnectionNode))
                    {
                        it.remove();
                        queuedTestConnection.add(unregisteredNode);
                    }
                }
            }
        }
    }

    private static synchronized WispNode TryClaimExistingNode(Level inWorld, BlockPos pos)
    {
        if(inWorld.isClientSide())
        {
            throw new RuntimeException("Trying to get chunk data on client");
        }
        LevelWispData dimData = GetWorldData(inWorld.dimension().location());
        if(dimData != null)
        {
            ChunkWisps chunkWisps = dimData.GetChunkWisps(pos);
            if(chunkWisps != null)
            {
                for (WispNode existingNode : chunkWisps.unregisteredNodes)
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
            if(existingNode instanceof WispInteractionNodeBase)
            {
                throw new RuntimeException("Claiming a wisp node claimed a wisp instead");
            }
            return existingNode;
        }


        WispNode newNode = new WispNode(inWorld.dimension().location(), pos);
        newNode.claimed = true;

        LevelWispData dimData = EnsureWorldData(inWorld.dimension().location());
        ChunkWisps chunkData = dimData.EnsureChunkWisps(pos);
        chunkData.unregisteredNodes.add(newNode);
        inWorld.getChunk(pos).setUnsaved(true);

        return newNode;
    }

    public static synchronized WispNode GetNode(Level level, BlockPos pos)
    {
        if(level.isClientSide())
        {
            throw new RuntimeException("Trying to get chunk data on client");
        }

        LevelWispData dimData = GetWorldData(level.dimension().location());
        if(dimData != null)
        {
            ChunkWisps chunkWisps = dimData.GetChunkWisps(pos);
            if(chunkWisps != null)
            {
                for(WispNode unregisteredNode : chunkWisps.unregisteredNodes)
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

    private static synchronized void HandleOrphanedNodes(HashMap<ResourceLocation,ArrayList<WispNode>> orphanedNodes)
    {
        if(orphanedNodes == null)
            return;

        for(Map.Entry<ResourceLocation, ArrayList<WispNode>> dimOrphanedNodes : orphanedNodes.entrySet())
        {
            LevelWispData dimData =  EnsureWorldData(dimOrphanedNodes.getKey());
            for(WispNode orphanedNode : dimOrphanedNodes.getValue())
            {
                ChunkPos chunkPos = Utils.GetChunkPos(orphanedNode.GetPos());
                ChunkWisps otherChunkData = dimData.EnsureChunkWisps(chunkPos);
                otherChunkData.unregisteredNodes.add(orphanedNode);

                dimData.MarkChunkDirty(chunkPos);
            }
        }
    }

    public static synchronized void RemoveNode(Level inWorld, WispNode node)
    {
        LevelWispData dimData = EnsureWorldData(inWorld.dimension().location());
        ChunkWisps chunkData = dimData.EnsureChunkWisps(node.GetPos());

        node.RemoveFromWorld(inWorld);
        if(chunkData.unregisteredNodes.remove(node))
        {
            return;
        }

        WispNetwork connectedNetwork = node.GetConnectedNetwork();
        if(connectedNetwork == null)
        {
            //done
            return;
        }
        HandleOrphanedNodes(connectedNetwork.RemoveNode(inWorld.dimension().location(), node));
    }

    private static synchronized void TryResolveExistingConnections(Level level, WispNode node)
    {
        LevelWispData dimData = GetWorldData(level.dimension().location());
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
                    for(WispNode unregisteredNode : chunkWisps.unregisteredNodes)
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

    public static synchronized void TryAndConnectNodeToANetwork(Level level, WispNode node)
    {
        LevelWispData dimData = GetWorldData(level.dimension().location());
        if(dimData == null)
        {
            return;
        }
        ChunkWisps chunkWisps = dimData.GetChunkWisps(node.GetPos());
        if(chunkWisps == null)
        {
            return;
        }

        if(node.connectedNetwork != null)
        {
            LogisticsWoW.LOGGER.error("Trying to connect a node to a network when it is already connected");
            chunkWisps.unregisteredNodes.remove(node);
            return;
        }

        WispNetwork connectedNetwork = null;

        for(LevelWispData levelData : worldData.values())
        {
            for(WispNetwork network : levelData.wispNetworks)
            {
                if(network.TryAndConnectNodeToNetwork(level, node))
                {
                    if(!chunkWisps.unregisteredNodes.remove(node))
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
                for (Iterator<WispNode> it = nearbyChunk.unregisteredNodes.iterator(); it.hasNext();)
                {
                    WispNode unregisteredNode = it.next();
                    TryResolveExistingConnections(level, unregisteredNode);

                    if(connectedNetwork.TryAndConnectNodeToNetworkViaNode(level, unregisteredNode, recentConnectionNode))
                    {
                        it.remove();
                        queuedTestConnection.add(unregisteredNode);
                        dimData.MarkChunkDirty(chunkToCheck);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static synchronized void OnTick(TickEvent.WorldTickEvent tickEvent)
    {
        if(tickEvent.side != LogicalSide.SERVER)
        {
            return;
        }

        if(tickEvent.phase != TickEvent.Phase.END)
        {
            return;
        }

        ResourceLocation tickDim = tickEvent.world.dimension().location();
        LevelWispData dimData = worldData.get(tickDim);
        if(dimData == null)
        {
            return;
        }

        for(Iterator<LevelWispData.LoadingChunkData> it = dimData.loadingChunks.iterator(); it.hasNext();)
        {
            LevelWispData.LoadingChunkData loadingData = it.next();
            --loadingData.tickCounter;
            if(loadingData.tickCounter <= 0)
            {
                if(!tickEvent.world.hasChunk(loadingData.chunkPos.x, loadingData.chunkPos.z))
                {
                    //chunk has vanished in the few ticks since it loaded
                    it.remove();
                    continue;
                }
                LevelChunk chunk = tickEvent.world.getChunk(loadingData.chunkPos.x, loadingData.chunkPos.z);

                ChunkWisps chunkWisps = dimData.chunkData.get(loadingData.chunkPos);
                if(chunkWisps != null)
                {
                    chunkWisps.OnChunkFinishLoad(chunk);
                    if(chunkWisps.ClearUnclaimed())
                    {
                        chunk.setUnsaved(true);
                    }
                }

                for(Iterator<WispNetwork> networkIt = dimData.wispNetworks.iterator(); networkIt.hasNext();)
                {
                    WispNetwork network = networkIt.next();
                    if(!network.claimed && Utils.GetChunkPos(network.GetPos()).equals(loadingData.chunkPos))
                    {
                        //unclaimed network in the chunk that the network is present in
                        HandleOrphanedNodes(network.RemoveFromWorld());
                        networkIt.remove();
                    }
                }

                for(LevelWispData otherDimData : worldData.values())
                {
                    for(WispNetwork wispNetwork : otherDimData.wispNetworks)
                    {
                        wispNetwork.OnChunkFinishLoad(chunk);
                        Tuple<Boolean, HashMap<ResourceLocation,ArrayList<WispNode>>> unclaimedAndOrphans = wispNetwork.ClearUnclaimed(tickDim, loadingData.chunkPos);
                        if(unclaimedAndOrphans.getA())
                        {
                            HandleOrphanedNodes(unclaimedAndOrphans.getB());
                            chunk.setUnsaved(true);
                        }
                    }
                }

                it.remove();
            }
        }

        for(ChunkPos dirtyChunk : dimData.dirtyChunks)
        {
            ChunkAccess chunk = tickEvent.world.getChunk(dirtyChunk.x, dirtyChunk.z, ChunkStatus.FULL, false);
            if(chunk != null)
            {
                chunk.setUnsaved(true);
            }
        }
        dimData.dirtyChunks.clear();

        for(Iterator<Map.Entry<ChunkPos, LevelWispData.UpdatedChunk>> it = dimData.updatedChunkTimers.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry<ChunkPos, LevelWispData.UpdatedChunk> updatedChunkEntry = it.next();
            updatedChunkEntry.getValue().tickCounter -= 1;
            if(updatedChunkEntry.getValue().tickCounter > 0)
            {
                continue;
            }

            if(!tickEvent.haveTime())
            {
                //wait until the server has some time to do the checks for connection forming/breaking
                continue;
            }

            ChunkWisps chunkWisps = dimData.chunkData.get(updatedChunkEntry.getKey());
            if(chunkWisps != null)
            {
                for(int i = 0; i < chunkWisps.unregisteredNodes.size();)
                {
                    WispNode node = chunkWisps.unregisteredNodes.get(i);
                    TryAndConnectNodeToANetwork(tickEvent.world, node);
                    if(node.connectedNetwork == null)
                    {
                        ++i;
                    }
                    //else it was connected, so it has been removed from unregisteredNodes
                    //keep i the same, since this node was removed from position i
                }
            }

            for(LevelWispData otherDimData : worldData.values())
            {
                for(WispNetwork wispNetwork : otherDimData.wispNetworks)
                {
                    HandleOrphanedNodes(wispNetwork.CheckConnections(tickEvent.world, updatedChunkEntry.getKey()));
                }
            }

            it.remove();
        }
    }
    @SubscribeEvent
    public static synchronized void OnServerTick(TickEvent.ServerTickEvent tickEvent)
    {
        for(LevelWispData dimData : worldData.values())
        {
            for(WispNetwork wispNetwork : dimData.wispNetworks)
            {
                wispNetwork.Tick(tickEvent);
            }
        }
    }

    @SubscribeEvent
    public static synchronized void OnChunkSave(ChunkDataEvent.Save saveEvent)
    {
        if(saveEvent.getWorld().isClientSide()) return;

        LevelWispData dimData = GetWorldData(((Level) saveEvent.getWorld()).dimension().location());

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

        Level level = (Level)loadEvent.getWorld();
        LevelWispData dimData = EnsureWorldData(level.dimension().location());
        dimData.OnChunkLoad(loadEvent);

        boolean caresAboutChunk = dimData.HasChunkData(loadEvent.getChunk().getPos());
        if(!caresAboutChunk)
        {
            for(LevelWispData otherDimData : worldData.values())
            {
                for(WispNetwork wispNetwork : otherDimData.wispNetworks)
                {
                    if(wispNetwork.HasChunkData(level.dimension().location(), loadEvent.getChunk().getPos()))
                    {
                        caresAboutChunk = true;
                        break;
                    }
                }
                if(caresAboutChunk) break;
            }
        }

        if(caresAboutChunk)
        {
            LevelWispData.LoadingChunkData loadingChunk = new LevelWispData.LoadingChunkData();
            loadingChunk.chunkPos = loadEvent.getChunk().getPos();
            //the block entities are loading on the chunks first tick
            //so wait 2 ticks to ensure that the chunk has a change to do it's first tick since I don't know the tick ordering
            loadingChunk.tickCounter = 2;

            dimData.loadingChunks.add(loadingChunk);
        }
    }

    @SubscribeEvent
    public static synchronized void OnChunkUnload(ChunkEvent.Unload unloadEvent)
    {
        if(unloadEvent.getWorld().isClientSide()) return;

        LevelWispData dimData = GetWorldData(((Level)unloadEvent.getWorld()).dimension().location());
        if(dimData != null)
        {
            dimData.OnChunkUnload(unloadEvent);
        }

        ResourceLocation dim = ((Level) unloadEvent.getWorld()).dimension().location();

        for(LevelWispData otherDimData : worldData.values())
        {
            for (WispNetwork wispNetwork : otherDimData.wispNetworks)
            {
                wispNetwork.ClearClaims(dim, unloadEvent.getChunk().getPos());
            }
        }
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
                            LevelWispData wispData = EnsureWorldData(serverWorld.dimension().location());
                            wispData.load(tag);
                            return wispData;
                        },
                        ()->EnsureWorldData(serverWorld.dimension().location()),
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

        if(updateEvent.getState().getBlock() == Blocks.AIR)
        {
            //block was removed
            WispInteractionNodeBase wisp = GetWisp((Level)updateEvent.getWorld(), updateEvent.getPos());

            if(wisp != null)
            {
                RemoveNode((Level) updateEvent.getWorld(), wisp);
                wisp.DropItemStackIntoWorld(updateEvent.getWorld());
            }
        }
        LevelWispData dimData = GetWorldData(((Level) updateEvent.getWorld()).dimension().location());
        if(dimData == null)
        {
            return;
        }

        ChunkPos updatedChunkPos = Utils.GetChunkPos(updateEvent.getPos());
        for(int x = -1; x <= 1; ++x)
        for(int z = -1; z <= 1; ++z)
        {
            ChunkPos chunkToNotify = new ChunkPos(updatedChunkPos.x + x, updatedChunkPos.z + z);
            LevelWispData.UpdatedChunk updateData = dimData.updatedChunkTimers.get(chunkToNotify);
            if(updateData == null)
            {
                LevelWispData.UpdatedChunk newUpdateData = new LevelWispData.UpdatedChunk();
                newUpdateData.tickCounter = LevelWispData.InitialChunkTickUpdateWait;
                newUpdateData.totalTicksToWait = LevelWispData.InitialChunkTickUpdateWait;
                dimData.updatedChunkTimers.put(chunkToNotify, newUpdateData);
            }
            else
            {
                int initialTotalWait = updateData.totalTicksToWait;
                updateData.totalTicksToWait = Math.min(initialTotalWait + LevelWispData.AdditionalChunkTickUpdateWait, LevelWispData.MaxChunkTickUpdateWait);

                int addedTicks = updateData.totalTicksToWait - initialTotalWait;
                updateData.tickCounter += addedTicks;
            }
        }

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
        poseStack.translate(0.5f, 0.5f, 0.5f);
        Matrix4f matrix = poseStack.last().pose();

        for(LevelWispData worldData : worldData.values())
        {
            for (WispNetwork wispNetwork : worldData.wispNetworks)
            {
                wispNetwork.Render(evt, poseStack, builder);
            }

            for(ChunkWisps chunkWisps : worldData.chunkData.values())
            {
                for(WispNode node : chunkWisps.unregisteredNodes)
                {
                    for (Direction.Axis axis : Direction.Axis.values())
                    {
                        Direction direction = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
                        Vector3f offset = direction.step();
                        offset.mul(0.6f);

                        builder.vertex(matrix, node.GetPos().getX() + offset.x(), node.GetPos().getY() + offset.y(), node.GetPos().getZ() + offset.z())
                                .color(0.f, 0.f, 1.f, 0.8f)
                                //.overlayCoords(OverlayTexture.NO_OVERLAY)
                                //.lightmap(15728880)
                                .normal(0.f, 1.f, 0.f)
                                .endVertex();

                        builder.vertex(matrix, node.GetPos().getX() - offset.x(), node.GetPos().getY() - offset.y(), node.GetPos().getZ() - offset.z())
                                .color(0.f, 0.f, 1.f, 0.8f)
                                //.overlayCoords(OverlayTexture.NO_OVERLAY)
                                //.lightmap(15728880)
                                .normal(0.f, 1.f, 0.f)
                                .endVertex();
                    }
                }
            }
        }


        poseStack.popPose();
        buffer.endBatch(RenderType.LINES);
    }
}
