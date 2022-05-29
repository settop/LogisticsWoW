package com.settop.LogisticsWoW.WispNetwork;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.core.BlockPos;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    private static class ChunkData
    {
        public final HashMap<BlockPos, WispBase> wisps = new HashMap<>();
        private final HashMap<BlockPos, WispNode> nodes = new HashMap<>();
    }

    private static class DimensionData
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

    private static void SetupNodeConnection(WispNode node, WispBase wisp, WispNode.eConnectionType type)
    {
        node.connectedNodes.add(new WispNode.Connection(wisp, type));
        wisp.connectedNodes.add(new WispNode.Connection(node, type));
    }

    public WispNetwork(ResourceLocation dim, BlockPos pos)
    {
        this.dim = dim;
        this.pos = pos;
    }

    public ResourceLocation GetDim() { return dim; }
    public BlockPos GetPos() { return pos; }

    @Nullable
    private ChunkData GetChunkData(Level world, BlockPos pos)
    {
        DimensionData dimData = dimensionData.get(world.dimension().location());
        if(dimData != null)
        {
            return dimData.GetChunkData(pos);
        }
        return null;
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
            return chunkData.wisps.get(inPos);
        }
        return null;
    }

    public WispNode GetNode(Level world, BlockPos inPos)
    {
        ChunkData chunkData = GetChunkData(world, inPos);
        if(chunkData != null)
        {
            return chunkData.nodes.get(inPos);
        }
        return null;
    }

    public boolean TryConnectWisp(Level world, WispBase wisp)
    {
        if(!wisp.inactiveConnections.isEmpty() || !wisp.connectedNodes.isEmpty())
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
                for(WispNode node : chunkData.nodes.values())
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
            EnsureChunkData(world, pos).wisps.put(wisp.GetPos(), wisp);
            return true;
        }
        else
        {
            return false;
        }
    }

    public void RemoveWisp(Level world, WispBase wisp)
    {
        EnsureChunkData(world, wisp.GetPos()).wisps.remove(wisp.GetPos(), wisp);
        //ToDo
    }

    public void AddNode(WispNode node)
    {
        node.ConnectToWispNetwork(this);
    }

    public boolean TryDirectConnectNode(Level world, WispNode node)
    {
        if(node.IsConnectedToANetwork())
        {
            throw new RuntimeException("Trying to connect node to wisp network, but it already has connections");
        }

        if(!world.dimension().location().equals(dim))
        {
            return false;
        }
        Vec3 testPos = GetClosestPos(node.GetPos());
        if (node.CanConnectToPos(world, testPos, node.autoConnectRange))
        {
            //node is connected directly to the network
            node.ConnectToWispNetwork(this);
            AddNode(node);
            return true;
        }

        return false;
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
                for(WispBase wisp : chunkData.wisps.values())
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
                return false;
            }
            else if(n.networkConnectionNode.node.get() == sourceNode)
            {
                return false;
            }
        }
    }

    private void RemoveNodeInternal(Level world, WispNode node, ArrayList<WispBase> o_orphanedWisps)
    {
        EnsureChunkData(world, node.GetPos()).nodes.remove(node.GetPos(), node);
/*
        for(WispBase wisp : node.connectedWisps)
        {
            wisp.connections.remove(node);
            if(wisp.connections.isEmpty())
            {
                o_orphanedWisps.add(wisp);
                RemoveWisp(world, wisp);
            }
        }
        node.connectedWisps.clear();
        */
    }

    //Returns any nodes wisps that are no longer connected to the network
    public void RemoveNode(Level world, WispNode node)
    {
        node.DisconnectFromWispNetwork(this);
        /*
        RemoveNodeInternal(world, node, o_orphanedWisps);

        ArrayList<WispNode> orphanedNodes = new ArrayList<>();

        for(WispNode.Connection connection : node.connectedNodes)
        {
            connection.node.connectedNodes.removeIf((otherConnection)->otherConnection.node == node);
            if(connection.node.networkConnectionNode != null && connection.node.networkConnectionNode.node == node)
            {
                //the connected node was connected to the network via the removed node
                orphanedNodes.add(connection.node);
                connection.node.connectedNetwork = null;
                connection.node.networkConnectionNode = null;
            }
        }
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
                    if (CheckIfValidNetworkConnection(orphanedNode, connection.node))
                    {
                        orphanedNode.connectedNetwork = connection.node.connectedNetwork;
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
                    continue;
                }
                else
                {
                    //spread the orphanness
                    for (WispNode.Connection connection : orphanedNode.connectedNodes)
                    {
                        if (connection.node.networkConnectionNode != null && connection.node.networkConnectionNode.node == orphanedNode)
                        {
                            connection.node.networkConnectionNode = null;
                            connection.node.connectedNetwork = null;
                            orphanedNodes.add(connection.node);
                        }
                    }
                }
            }
        }

        //any remaining orphaned nodes couldn't be re-connected to the network
        for(WispNode orphanedNode : orphanedNodes)
        {
            for(Iterator<WispNode.Connection> it = orphanedNode.connectedNodes.iterator(); it.hasNext();)
            {
                WispNode.Connection connection = it.next();
                connection.node.connectedNodes.removeIf((otherConnection) -> otherConnection.node == orphanedNode);
                it.remove();
            }

            RemoveNodeInternal(world, orphanedNode, o_orphanedWisps);
            o_orphanedNodes.add(orphanedNode);
        }

         */
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
                dimData.EnsureChunkData(wispPos).wisps.put(wispPos, loadedWisp);
            }

            dimensionData.put(dimensionName, dimData);
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
                for(WispBase wisp : chunkData.wisps.values())
                {
                    networkWisps.add(wisp.Save());
                }
                for(WispNode node : chunkData.nodes.values())
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

    }
}
