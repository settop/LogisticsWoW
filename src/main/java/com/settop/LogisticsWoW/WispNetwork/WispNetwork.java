package com.settop.LogisticsWoW.WispNetwork;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Wisps.ChunkWisps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.settop.LogisticsWoW.Utils.Utils;
import com.settop.LogisticsWoW.Wisps.WispBase;
import com.settop.LogisticsWoW.Wisps.WispFactory;
import com.settop.LogisticsWoW.Wisps.WispNode;
import net.minecraftforge.client.event.RenderLevelLastEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.*;

public class WispNetwork
{
    private static class ItemSources
    {
        private int countCache = 0;
        private boolean craftable = false;
        private boolean isDirty = false;

        private class WispItemSource
        {
            public WeakReference<WispBase> sourceWisp;
            public int wispCountCache = 0;
            public boolean wispCraftCache = false;
        }

        private ArrayList<WispItemSource> itemSources;
    }

    public static class ChunkData
    {
        public final ArrayList<WispBase> wisps = new ArrayList<>();
        private final ArrayList<WispNode> nodes = new ArrayList<>();
    }

    public static class DimensionData
    {
        public HashMap<ChunkPos, ChunkData> chunkData = new HashMap<>();

        @Nullable
        public ChunkData GetChunkData(BlockPos blockPos)
        {
            ChunkPos chunkPos = Utils.GetChunkPos(blockPos);
            return chunkData.get(chunkPos);
        }
        @Nullable
        public ChunkData GetChunkData(int chunkX, int chunkZ)
        {
            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
            return chunkData.get(chunkPos);
        }
        @Nonnull
        public ChunkData EnsureChunkData(BlockPos blockPos)
        {
            ChunkPos chunkPos = Utils.GetChunkPos(blockPos);
            return chunkData.computeIfAbsent(chunkPos, (key)->new ChunkData());
        }
    }

    public final ResourceLocation dim;
    public final BlockPos pos;

    private final HashMap<ResourceLocation, DimensionData> dimensionData = new HashMap<>();


    private static void SetupNodeConnection(WispNode nodeA, WispNode nodeB, WispNode.eConnectionType type)
    {
        nodeA.connectedNodes.add(new WispNode.Connection(nodeB, type));
        nodeB.connectedNodes.add(new WispNode.Connection(nodeA, type));
    }

    public WispNetwork(ResourceLocation dim, BlockPos pos)
    {
        this.dim = dim;
        this.pos = pos;
    }

    public ResourceLocation GetDim() { return dim; }
    public BlockPos GetPos() { return pos; }

    @Nullable
    private DimensionData GetDimensionData(Level world)
    {
        return dimensionData.get(world.dimension().location());
    }

    @Nullable
    private ChunkData GetChunkData(ResourceLocation dimKey, BlockPos pos)
    {
        DimensionData dimData = dimensionData.get(dimKey);
        if(dimData != null)
        {
            return dimData.GetChunkData(pos);
        }
        return null;
    }
    @Nullable
    private ChunkData GetChunkData(Level world, BlockPos pos)
    {
        return GetChunkData(world.dimension().location(), pos);
    }
    @Nullable
    private ChunkData GetChunkData(Level world, int chunkX, int chunkZ)
    {
        DimensionData dimData = dimensionData.get(world.dimension().location());
        if(dimData != null)
        {
            return dimData.GetChunkData(chunkX, chunkZ);
        }
        return null;
    }

    @Nonnull
    private ChunkData EnsureChunkData(Level world, BlockPos pos)
    {
        DimensionData dimData = dimensionData.computeIfAbsent(world.dimension().location(), (key)->new DimensionData());
        return dimData.EnsureChunkData(pos);
    }

    public WispBase GetWisp(Level world, BlockPos inPos)
    {
        ChunkData chunkData = GetChunkData(world, inPos);
        if(chunkData != null)
        {
            for (WispBase wisp : chunkData.wisps)
            {
                if (wisp.GetPos().equals(inPos))
                {
                    return wisp;
                }
            }
        }
        return null;
    }

    public WispNode TryClaimExistingNode(Level world, BlockPos inPos)
    {
        ChunkData chunkData = GetChunkData(world, inPos);
        if(chunkData == null)
        {
            return null;
        }

        for (WispNode existingNode : chunkData.nodes)
        {
            if (existingNode.GetPos().equals(inPos))
            {
                if(existingNode.claimed)
                {
                    throw new RuntimeException("Trying to claim an already claimed node");
                }
                existingNode.claimed = true;
                return existingNode;
            }
        }

        return null;
    }

    public WispNode GetNode(ResourceLocation dimKey, BlockPos inPos)
    {
        ChunkData chunkData = GetChunkData(dimKey, inPos);
        if(chunkData != null)
        {
            for (WispNode node : chunkData.nodes)
            {
                if (node.GetPos().equals(inPos))
                {
                    return node;
                }
            }
        }
        return null;
    }

    public WispNode GetNode(Level world, BlockPos inPos)
    {
        return GetNode(world.dimension().location(), inPos);
    }

    public boolean TryConnectWisp(Level world, WispBase wisp)
    {
        if(!wisp.connectedNodes.isEmpty())
        {
            throw new RuntimeException("Trying to connect wisp to wisp network, but it already has connections");
        }

        int minChunkX = (wisp.GetPos().getX() - WispNode.MaxAutoConnectRange) >> 4;
        int maxChunkX = (wisp.GetPos().getX() + WispNode.MaxAutoConnectRange) >> 4;
        int minChunkZ = (wisp.GetPos().getZ() - WispNode.MaxAutoConnectRange) >> 4;
        int maxChunkZ = (wisp.GetPos().getZ() + WispNode.MaxAutoConnectRange) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX)
        {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ)
            {
                ChunkData chunkData = GetChunkData(world, chunkX, chunkZ);
                if(chunkData == null)
                {
                    continue;
                }
                for(WispNode node : chunkData.nodes)
                {
                    if(node.CanConnectToPos(world, Vec3.atCenterOf(wisp.GetPos()), node.autoConnectRange))
                    {
                        SetupNodeConnection(node, wisp, WispNode.eConnectionType.AutoConnect);
                    }
                }
            }
        }

        if(!wisp.connectedNodes.isEmpty())
        {
            EnsureChunkData(world, wisp.GetPos()).wisps.add(wisp);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean TryAndConnectNodeToNetwork(Level level, WispNode node)
    {
        boolean hasConnected = false;
        if(level.dimension().location() == dim)
        {
            //same dimension, try and connect directly
            Vec3 testPos = GetClosestPos(node.GetPos());
            if (node.CanConnectToPos(level, testPos, node.autoConnectRange))
            {
                //node is connected directly to the network
                AddNode(level, node);
                hasConnected = true;
            }
        }

        DimensionData dimData = GetDimensionData(level);
        if(dimData == null)
        {
            return hasConnected;
        }

        final Vec3i maxAutoConnectRangeVec = new Vec3i(WispNode.MaxAutoConnectRange, 0, WispNode.MaxAutoConnectRange);
        //see if we can get a connection via any nearby nodes
        ChunkPos chunkMinPos = Utils.GetChunkPos(node.GetPos().subtract( maxAutoConnectRangeVec));
        ChunkPos chunkMaxPos = Utils.GetChunkPos(node.GetPos().offset( maxAutoConnectRangeVec));

        boolean needsAdding = false;

        for(int x = chunkMinPos.x; x <= chunkMaxPos.x; ++x)
        for(int z = chunkMinPos.z; z <= chunkMaxPos.z; ++z)
        {
            ChunkData nearbyChunk = dimData.GetChunkData(x, z);
            if(nearbyChunk != null)
            {
                for(WispNode registeredNode : nearbyChunk.nodes)
                {
                    if(registeredNode == node)
                    {
                        continue;
                    }
                    int autoConnectRangeToCheck = Math.max(registeredNode.GetAutoConnectRange(), node.GetAutoConnectRange());
                    if(node.CanConnectToPos(level, Vec3.atCenterOf(registeredNode.GetPos()), autoConnectRangeToCheck))
                    {
                        if(!hasConnected)
                        {
                            node.AddConnectionAndNetworkConnection(registeredNode, WispNode.eConnectionType.AutoConnect);
                            needsAdding = true;
                            hasConnected = true;
                        }
                        else
                        {
                            node.EnsureConnection(registeredNode, WispNode.eConnectionType.AutoConnect);
                        }
                    }
                }
            }
        }

        if(needsAdding)
        {
            AddNode(level, node);
        }

        return hasConnected;
    }

    private void EnsureConnectionToAllNodesInRange(Level level, WispNode nodeToConnect)
    {
        final Vec3i maxAutoConnectRangeVec = new Vec3i(WispNode.MaxAutoConnectRange, 0, WispNode.MaxAutoConnectRange);
        //see if we can get a connection via any nearby nodes
        ChunkPos chunkMinPos = Utils.GetChunkPos(nodeToConnect.GetPos().subtract( maxAutoConnectRangeVec));
        ChunkPos chunkMaxPos = Utils.GetChunkPos(nodeToConnect.GetPos().offset( maxAutoConnectRangeVec));

        DimensionData dimData = GetDimensionData(level);
        if(dimData == null)
        {
            throw new RuntimeException("EnsureConnectionToAllNodesInRange dimData is null unexpectedly");
        }

        for(int x = chunkMinPos.x; x <= chunkMaxPos.x; ++x)
        for(int z = chunkMinPos.z; z <= chunkMaxPos.z; ++z)
        {
            ChunkData nearbyChunk = dimData.GetChunkData(x, z);
            if(nearbyChunk != null)
            {
                for(WispNode registeredNode : nearbyChunk.nodes)
                {
                    if(registeredNode == nodeToConnect)
                    {
                        continue;
                    }
                    int autoConnectRangeToCheck = Math.max(registeredNode.GetAutoConnectRange(), nodeToConnect.GetAutoConnectRange());
                    if(nodeToConnect.CanConnectToPos(level, Vec3.atCenterOf(registeredNode.GetPos()), autoConnectRangeToCheck))
                    {
                        nodeToConnect.EnsureConnection(registeredNode, WispNode.eConnectionType.AutoConnect);
                    }
                }
            }
        }
    }

    public boolean TryAndConnectNodeToNetworkViaNode(Level level, WispNode nodeToConnect, WispNode connectedNode)
    {
        if(nodeToConnect.connectedNetwork != null)
        {
            throw new RuntimeException("Trying to connect a node that is already connected");
        }
        if(connectedNode.connectedNetwork != this)
        {
            throw new RuntimeException("Trying to connect via a node that is not connected to this network");
        }
        int autoConnectRangeToCheck = Math.max(nodeToConnect.GetAutoConnectRange(), connectedNode.GetAutoConnectRange());

        if(connectedNode.CanConnectToPos(level, Vec3.atCenterOf(nodeToConnect.GetPos()), autoConnectRangeToCheck))
        {
            nodeToConnect.AddConnectionAndNetworkConnection(connectedNode, WispNode.eConnectionType.AutoConnect);
            EnsureConnectionToAllNodesInRange(level, nodeToConnect);
            AddNode(level, nodeToConnect);
            return true;
        }
        else
        {
            return false;
        }
    }

    public void RemoveWisp(Level world, WispBase wisp)
    {
        EnsureChunkData(world, wisp.GetPos()).wisps.remove(wisp);
        //ToDo
    }

    public void AddNode(Level world, WispNode node)
    {
        node.ConnectToWispNetwork(this);
        EnsureChunkData(world, node.GetPos()).nodes.add(node);
    }


    private void ConnectNewNodeToWisps(Level world, WispNode node)
    {
        int minChunkX = (node.GetPos().getX() - node.autoConnectRange) >> 4;
        int maxChunkX = (node.GetPos().getX() + node.autoConnectRange) >> 4;
        int minChunkZ = (node.GetPos().getZ() - node.autoConnectRange) >> 4;
        int maxChunkZ = (node.GetPos().getZ() + node.autoConnectRange) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX)
        {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ)
            {
                ChunkData chunkData = GetChunkData(world, chunkX, chunkZ);
                if(chunkData == null)
                {
                    continue;
                }
                for(WispBase wisp : chunkData.wisps)
                {
                    if(node.CanConnectToPos(world, Vec3.atCenterOf(wisp.GetPos()), node.autoConnectRange))
                    {
                        SetupNodeConnection(node, wisp, WispNode.eConnectionType.AutoConnect);
                    }
                }
            }
        }
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
                //it's another orphaned node
                return false;
            }
            else if(n.networkConnectionNode.node.get() == sourceNode)
            {
                //the network loops back to this source node
                return false;
            }
        }
    }

    private void RemoveNodeInternal(Level world, WispNode node)
    {
        EnsureChunkData(world, node.GetPos()).nodes.remove(node);
        node.DisconnectFromWispNetwork(this);

        //now ensure that all the auto connections are removed
        for(Iterator<WispNode.Connection> connectionIt = node.connectedNodes.iterator(); connectionIt.hasNext();)
        {
            WispNode.Connection connection = connectionIt.next();
            if(connection.connectionType != WispNode.eConnectionType.AutoConnect)
            {
                continue;
            }

            WispNode connectedNode = connection.node.get();
            connectedNode.connectedNodes.removeIf(otherConnection->otherConnection.nodePos.equals(node.GetPos()));
            connectionIt.remove();
        }
    }

    //Returns any nodes that are no longer connected to the network
    public ArrayList<WispNode> RemoveNode(Level world, WispNode node)
    {
        //now need to remove it's connections
        ArrayList<WispNode> orphanedNodes = new ArrayList<>();
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
        node.connectedNodes.clear();
        RemoveNodeInternal(world, node);

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
                        orphanedNode.connectedNetwork = this;
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
                        WispNode connectedNode = connection.node.get();
                        if (connectedNode.networkConnectionNode != null && connectedNode.networkConnectionNode.nodePos.equals(orphanedNode.GetPos()))
                        {
                            connectedNode.networkConnectionNode = null;
                            connectedNode.connectedNetwork = null;
                            orphanedNodes.add(connection.node.get());
                        }
                    }
                }
            }
        }
        for(WispNode orphanedNode : orphanedNodes)
        {
            RemoveNodeInternal(world, orphanedNode);
        }

        return orphanedNodes;
    }

    public Vec3 GetClosestPos(Vec3 inPos)
    {
        //the network is a 3x3x3 multiblock
        //so want to test to the closest block of the multiblock
        Vec3 networkPos = Vec3.atCenterOf(pos);
        Vec3 offset = inPos.subtract(networkPos);
        //clamp the offset to within the bounds of the network
        offset = new Vec3(
                Math.min(Math.max(offset.x(), -1.5f), 1.5f),
                Math.min(Math.max(offset.y(), -1.5f), 1.5f),
                Math.min(Math.max(offset.z(), -1.5f), 1.5f)
        );
        return networkPos.add(offset);
    }

    public Vec3 GetClosestPos(BlockPos inPos)
    {
        return GetClosestPos(Vec3.atCenterOf(inPos));
    }

    public static WispNetwork CreateAndRead(ResourceLocation dim, BlockPos pos, CompoundTag nbt)
    {
        WispNetwork network = new WispNetwork(dim, pos);
        network.read(nbt);
        return network;
    }

    public void read(CompoundTag nbt)
    {
        CompoundTag dimensionDataNBT = nbt.getCompound("dimensionData");

        for(String dimName : dimensionDataNBT.getAllKeys())
        {
            ResourceLocation dimensionName = ResourceLocation.tryParse(dimName);
            if(dimensionName == null)
            {
                continue;
            }

            CompoundTag dimDataNBT = dimensionDataNBT.getCompound(dimName);
            DimensionData dimData = new DimensionData();

            ListTag networkWisps = dimDataNBT.getList("wisps", nbt.getId());
            for(int i = 0; i < networkWisps.size(); ++i)
            {
                CompoundTag wispNBT = networkWisps.getCompound(i);
                WispBase loadedWisp = WispFactory.LoadWisp(dimensionName, wispNBT);

                BlockPos wispPos = loadedWisp.GetPos();
                dimData.EnsureChunkData(wispPos).wisps.add(loadedWisp);
            }
            ListTag networkNodes = dimDataNBT.getList("nodes", nbt.getId());
            for(int i = 0; i < networkNodes.size(); ++i)
            {
                WispNode loadedNode = WispNode.ReadNode(networkNodes.getCompound(i));
                dimData.EnsureChunkData(loadedNode.GetPos()).nodes.add(loadedNode);
            }

            dimensionData.put(dimensionName, dimData);
        }

        //now connect all the node references
        for(Map.Entry<ResourceLocation, DimensionData> dimData : dimensionData.entrySet())
        {
             for(ChunkData chunkData : dimData.getValue().chunkData.values())
             {
                 for(WispNode node : chunkData.nodes)
                 {
                     WeakReference<WispNode> nodeWeak = new WeakReference<>(node);
                     node.ConnectToWispNetwork(this);
                     for(WispNode.Connection connection : node.connectedNodes)
                     {
                         WispNode otherNode = GetNode(dimData.getKey(), connection.nodePos);
                         for(WispNode.Connection otherConnection : otherNode.connectedNodes)
                         {
                             if(otherConnection.nodePos.equals(node.GetPos()))
                             {
                                 otherConnection.node = nodeWeak;
                             }
                         }
                     }
                 }
             }
        }

    }

    public CompoundTag write(CompoundTag compound)
    {
        CompoundTag dimensionDataNBT = new CompoundTag();
        for(Map.Entry<ResourceLocation, DimensionData> dimensionEntry : dimensionData.entrySet())
        {
            DimensionData dimData = dimensionEntry.getValue();

            ListTag networkWisps = new ListTag();
            ListTag networkNodes = new ListTag();
            for(ChunkData chunkData : dimData.chunkData.values())
            {
                for(WispBase wisp : chunkData.wisps)
                {
                    networkWisps.add(wisp.Save());
                }
                for(WispNode node : chunkData.nodes)
                {
                    networkNodes.add(WispNode.Write(node, new CompoundTag()));
                }
            }

            CompoundTag dimDataNBT = new CompoundTag();
            dimDataNBT.put("wisps", networkWisps);
            dimDataNBT.put("nodes", networkNodes);

            dimensionDataNBT.put(dimensionEntry.getKey().toString(), dimDataNBT);
        }
        compound.put("dimensionData", dimensionDataNBT);

        return compound;
    }

    @OnlyIn(Dist.CLIENT)
    public void Render(RenderLevelLastEvent evt, PoseStack poseStack, VertexConsumer builder)
    {
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        Matrix4f matrix = poseStack.last().pose();


        for (DimensionData dimData : dimensionData.values())
        {
            for (ChunkData chunkData : dimData.chunkData.values())
            {
                chunkData.nodes.forEach((node) ->
                {
                    for (WispNode.Connection connection : node.connectedNodes)
                    {
                        float rgb[] = {0.f, 1.f, 0.f};
                        if (connection.connectionType == WispNode.eConnectionType.Link)
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
                            .color(1.f, 1.f, 1.f, 0.8f)
                            //.overlayCoords(OverlayTexture.NO_OVERLAY)
                            //.lightmap(15728880)
                            .normal(0.f, 1.f, 0.f)
                            .endVertex();

                    builder.vertex(matrix, pos.getX(), pos.getY(), pos.getZ())
                            .color(0.4f, 0.4f, 0.4f, 0.8f)
                            //.overlayCoords(OverlayTexture.NO_OVERLAY)
                            //.lightmap(15728880)
                            .normal(0.f, 1.f, 0.f)
                            .endVertex();
                });
            }
            poseStack.popPose();
        }
    }
}
