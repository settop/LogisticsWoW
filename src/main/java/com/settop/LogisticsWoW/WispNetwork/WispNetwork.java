package com.settop.LogisticsWoW.WispNetwork;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.WispNetwork.Tasks.WispTask;
import com.settop.LogisticsWoW.WispNetwork.Tasks.WispTaskManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.settop.LogisticsWoW.Utils.Utils;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import com.settop.LogisticsWoW.Wisps.WispFactory;
import com.settop.LogisticsWoW.Wisps.WispNode;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.event.TickEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Stream;

public class WispNetwork extends WispNode
{

    public static class ChunkData
    {
        public final ArrayList<WispNode> nodes = new ArrayList<>();//includes both nodes and wisps

        public Stream<WispInteractionNodeBase> GetWisps()
        {
            return nodes.stream().filter(node->node instanceof WispInteractionNodeBase).map(node->(WispInteractionNodeBase)node);
        }
        public Stream<WispNode> GetOnlyNodes()
        {
            return nodes.stream().filter(node->!(node instanceof WispInteractionNodeBase));
        }
    }

    public static class DimensionData
    {
        public HashMap<ChunkPos, ChunkData> chunkData = new HashMap<>();

        @Nullable
        public ChunkData GetChunkData(ChunkPos chunkPos)
        {
            return chunkData.get(chunkPos);
        }
        @Nullable
        public ChunkData GetChunkData(int chunkX, int chunkZ)
        {
            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
            return chunkData.get(chunkPos);
        }
        @Nonnull
        public ChunkData EnsureChunkData(ChunkPos chunkPos)
        {
            return chunkData.computeIfAbsent(chunkPos, (key)->new ChunkData());
        }
    }

    private final HashMap<ResourceLocation, DimensionData> dimensionData = new HashMap<>();

    private final WispTaskManager taskManager = new WispTaskManager();
    private int tickTime = 0;

    private final WispNetworkItemManagement itemMangement = new WispNetworkItemManagement();

    private static ResourceLocation GetDim(Level level) { return level.dimension().location(); }

    private static void SetupNodeConnection(WispNode nodeA, WispNode nodeB, WispNode.eConnectionType type)
    {
        nodeA.connectedNodes.add(new WispNode.Connection(nodeB, type));
        nodeB.connectedNodes.add(new WispNode.Connection(nodeA, type));
    }

    public WispNetwork(ResourceLocation dim, BlockPos pos)
    {
        super(dim, pos);
        super.connectedNetwork = this;
    }

    @Override
    public boolean CanConnectToPos(Level world, BlockPos target, int connectionRange)
    {
        throw new RuntimeException("Wisp network should not have CanConnectToPos called on it.");
    }
    @Override
    public void DisconnectFromWispNetwork(WispNetwork wispNetwork)
    {
        throw new RuntimeException("Wisp network should not have DisconnectFromWispNetwork called on it.");
    }

    @Override
    public void AddConnectionAndNetworkConnection(WispNode node, eConnectionType type)
    {
        throw new RuntimeException("Wisp network should not have AddConnectionAndNetworkConnection called on it.");
    }

    @Nullable
    private DimensionData GetDimensionData(ResourceLocation dim)
    {
        return dimensionData.get(dim);
    }

    @Nullable
    private ChunkData GetChunkData(ResourceLocation dim, ChunkPos pos)
    {
        DimensionData dimData = dimensionData.get(dim);
        if(dimData != null)
        {
            return dimData.GetChunkData(pos);
        }
        return null;
    }
    @Nullable
    private ChunkData GetChunkData(ResourceLocation dim, int chunkX, int chunkZ)
    {
        DimensionData dimData = dimensionData.get(dim);
        if(dimData != null)
        {
            return dimData.GetChunkData(chunkX, chunkZ);
        }
        return null;
    }

    @Nonnull
    private ChunkData EnsureChunkData(ResourceLocation dim, ChunkPos pos)
    {
        DimensionData dimData = dimensionData.computeIfAbsent(dim, (key)->new DimensionData());
        return dimData.EnsureChunkData(pos);
    }

    public boolean HasChunkData(ResourceLocation dim, ChunkPos chunkPos)
    {
        DimensionData dimData = dimensionData.get(dim);
        return dimData != null && dimData.chunkData.containsKey(chunkPos);
    }

    public HashMap<ResourceLocation,ArrayList<WispNode>> RemoveFromWorld()
    {
        HashMap<ResourceLocation,ArrayList<WispNode>> orphanedNodes = new HashMap<>();
        dimensionData.forEach((dim, dimData)->
        {
            ArrayList<WispNode> dimOrphans = new ArrayList<>();
            dimData.chunkData.forEach((chunkPos, chunkData)->
            {
                for(WispNode node : chunkData.nodes)
                {
                    dimOrphans.add(node);
                    CleanupNodeInternal(node);
                }
            });
            orphanedNodes.put(dim, dimOrphans);
        });
        return orphanedNodes;
    }

    public WispNode TryClaimExistingNode(Level world, BlockPos inPos)
    {
        ChunkData chunkData = GetChunkData(GetDim(world), Utils.GetChunkPos(inPos));
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
        if(inPos.equals(GetPos()) && dimKey.equals(GetDim()))
        {
            return this;
        }
        ChunkData chunkData = GetChunkData(dimKey, Utils.GetChunkPos(inPos));
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

    public WispNode GetNode(Level world, BlockPos inPos){ return GetNode(GetDim(world), inPos); }

    public void OnChunkFinishLoad(LevelChunk chunk)
    {
        ChunkData chunkData = GetChunkData(GetDim(Objects.requireNonNull(chunk.getWorldForge())), chunk.getPos());
        if(chunkData == null)
        {
            return;
        }
        chunkData.GetWisps().forEach(wisp->
        {
            BlockEntity blockEntity = chunk.getBlockEntity(wisp.GetPos());
            if( blockEntity!= null)
            {
                wisp.claimed = true;
                wisp.SetConnectedBlockEntity(blockEntity);
            }
        });
    }

    public Tuple<Boolean, HashMap<ResourceLocation,ArrayList<WispNode>>> ClearUnclaimed(ResourceLocation dim, ChunkPos chunk)
    {
        ChunkData chunkData = GetChunkData(dim, chunk);
        if(chunkData == null)
        {
            return new Tuple<>(false, null);
        }

        ArrayList<WispNode> unclaimedNodes = new ArrayList<>();
        for(WispNode node : chunkData.nodes)
        {
            if(!node.claimed)
            {
                unclaimedNodes.add(node);
            }
        }

        if(unclaimedNodes.isEmpty())
        {
            return new Tuple<>(false, null);
        }
        HashMap<ResourceLocation,ArrayList<WispNode>> orphanedNodes  = new HashMap<>();

        for(WispNode unclaimedNode : unclaimedNodes)
        {
            HashMap<ResourceLocation,ArrayList<WispNode>> newOrphanedNodes = RemoveNode(dim, unclaimedNode);
            newOrphanedNodes.forEach
                    (
                            (orphanedDim, dimOrphans)->orphanedNodes.computeIfAbsent(orphanedDim, key2->new ArrayList<>()).addAll(dimOrphans)
                    );
        }

        //make sure we aren't returning any of the unclaimed nodes we are trying to return
        for(WispNode unclaimedNode : unclaimedNodes)
        {
            orphanedNodes.forEach((key, dimOrphans)-> dimOrphans.remove(unclaimedNode));
        }

        return new Tuple<>(true, orphanedNodes);
    }

    public void ClearClaims(ResourceLocation dim, ChunkPos chunk)
    {
        ChunkData chunkData = GetChunkData(dim, chunk);
        if(chunkData == null)
        {
            return;
        }

        for(WispNode node : chunkData.nodes)
        {
            node.claimed = false;
        }
    }

    public boolean TryAndConnectNodeToNetwork(Level level, WispNode node)
    {
        boolean hasConnected = false;
        if(level.dimension().location() == GetDim())
        {
            //same dimension, try and connect directly
            if (node.CanConnectToPos(level, GetClosestPos(node.GetPos()), node.autoConnectRange))
            {
                //node is connected directly to the network
                AddNode(GetDim(level), node);
                hasConnected = true;
                node.AddConnectionAndNetworkConnection(this, eConnectionType.AutoConnect);
            }
        }

        DimensionData dimData = GetDimensionData(GetDim(level));
        if(dimData == null)
        {
            return hasConnected;
        }

        final Vec3i maxAutoConnectRangeVec = new Vec3i(WispNode.MaxAutoConnectRange, 0, WispNode.MaxAutoConnectRange);
        //see if we can get a connection via any nearby nodes
        ChunkPos chunkMinPos = Utils.GetChunkPos(node.GetPos().subtract( maxAutoConnectRangeVec));
        ChunkPos chunkMaxPos = Utils.GetChunkPos(node.GetPos().offset( maxAutoConnectRangeVec));

        boolean needsAdding = false;
        ArrayList<WispNode> nodesToRecheckConnection = new ArrayList<>();

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
                    if(!hasConnected && !registeredNode.CanBeUsedAsNetworkConnection())
                    {
                        nodesToRecheckConnection.add(registeredNode);
                        continue;
                    }
                    int autoConnectRangeToCheck = Math.max(registeredNode.GetAutoConnectRange(), node.GetAutoConnectRange());
                    if(node.CanConnectToPos(level, registeredNode.GetPos(), autoConnectRangeToCheck))
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
            AddNode(GetDim(level), node);
        }

        if(hasConnected)
        {
            for(WispNode registeredNode : nodesToRecheckConnection)
            {
                int autoConnectRangeToCheck = Math.max(registeredNode.GetAutoConnectRange(), node.GetAutoConnectRange());
                if(node.CanConnectToPos(level, registeredNode.GetPos(), autoConnectRangeToCheck))
                {
                    node.EnsureConnection(registeredNode, WispNode.eConnectionType.AutoConnect);
                }
            }
        }

        return hasConnected;
    }

    private void EnsureConnectionToAllNodesInRange(Level level, WispNode nodeToConnect)
    {
        ResourceLocation levelDim = GetDim(level);
        DimensionData dimData = GetDimensionData(levelDim);
        if(dimData == null)
        {
            throw new RuntimeException("EnsureConnectionToAllNodesInRange dimData is null unexpectedly");
        }

        final Vec3i maxAutoConnectRangeVec = new Vec3i(WispNode.MaxAutoConnectRange, 0, WispNode.MaxAutoConnectRange);
        //see if we can get a connection via any nearby nodes
        ChunkPos chunkMinPos = Utils.GetChunkPos(nodeToConnect.GetPos().subtract( maxAutoConnectRangeVec));
        ChunkPos chunkMaxPos = Utils.GetChunkPos(nodeToConnect.GetPos().offset( maxAutoConnectRangeVec));

        if(levelDim.equals(GetDim()))
        {
            if (nodeToConnect.CanConnectToPos(level, GetClosestPos(nodeToConnect.GetPos()), nodeToConnect.autoConnectRange))
            {
                nodeToConnect.EnsureConnection(this, WispNode.eConnectionType.AutoConnect);
            }
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
                    if(nodeToConnect.CanConnectToPos(level, registeredNode.GetPos(), autoConnectRangeToCheck))
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

        if(!connectedNode.CanBeUsedAsNetworkConnection())
        {
            return false;
        }

        int autoConnectRangeToCheck = Math.max(nodeToConnect.GetAutoConnectRange(), connectedNode.GetAutoConnectRange());

        BlockPos connectPos = connectedNode == this ? GetClosestPos(nodeToConnect.GetPos()) : connectedNode.GetPos();
        if(nodeToConnect.CanConnectToPos(level, connectPos, autoConnectRangeToCheck))
        {
            nodeToConnect.AddConnectionAndNetworkConnection(connectedNode, WispNode.eConnectionType.AutoConnect);
            EnsureConnectionToAllNodesInRange(level, nodeToConnect);
            AddNode(GetDim(level), nodeToConnect);
            return true;
        }
        else
        {
            return false;
        }
    }

    private void AddNode(ResourceLocation dim, WispNode node)
    {
        node.ConnectToWispNetwork(this);
        EnsureChunkData(dim, Utils.GetChunkPos(node.GetPos())).nodes.add(node);
    }

    private static boolean CheckIfValidNetworkConnection(WispNode sourceNode, WispNode testNode)
    {
        for(WispNode n = testNode; ; n = n.networkConnectionNode.node.get())
        {
            assert n != null;
            if(!n.CanBeUsedAsNetworkConnection())
            {
                return false;
            }
            else if(n.GetConnectedNetwork() != null && n.networkConnectionNode == null)
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

    private void CleanupNodeInternal(WispNode node)
    {
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
            assert connectedNode != null;
            connectedNode.connectedNodes.removeIf(otherConnection->otherConnection.nodePos.equals(node.GetPos()));
            connectionIt.remove();
        }
    }

    private void RemoveNodeInternal(ResourceLocation dim, WispNode node)
    {
        EnsureChunkData(dim, Utils.GetChunkPos(node.GetPos())).nodes.remove(node);
        CleanupNodeInternal(node);
    }

    private void ProcessOrphanedNodes(ResourceLocation dim, ArrayList<WispNode> orphanedNodes)
    {
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
                        assert connectedNode != null;
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
            RemoveNodeInternal(dim, orphanedNode);
        }
    }

    //Returns any nodes that are no longer connected to the network
    public HashMap<ResourceLocation,ArrayList<WispNode>> RemoveNode(ResourceLocation dim, WispNode node)
    {
        //now need to remove it's connections
        ArrayList<WispNode> orphanedNodes = new ArrayList<>();
        for(WispNode.Connection connection : node.connectedNodes)
        {
            WispNode otherNode = connection.node.get();
            assert otherNode != null;
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
        RemoveNodeInternal(dim, node);

        ProcessOrphanedNodes(dim, orphanedNodes);

        HashMap<ResourceLocation,ArrayList<WispNode>> orphanedNodesMap = new HashMap<>();
        orphanedNodesMap.put(dim, orphanedNodes);
        return orphanedNodesMap;
    }

    public HashMap<ResourceLocation,ArrayList<WispNode>> CheckConnections(Level level, ChunkPos chunkToCheck)
    {
        //first try and connect the nodes to any unconnected nodes
        ResourceLocation dim = GetDim(level);
        ChunkData chunkData = GetChunkData(dim, chunkToCheck);
        if(chunkData == null)
        {
            return null;
        }
        ArrayList<WispNode> orphanedNodes = new ArrayList<>();

        //use some large connection range, since we already know we are in range since the connection already exists
        final int TestRange = 10000;
        for(WispNode node : chunkData.nodes)
        {
            //first check to see if any existing connections are broken
            if(dim == GetDim())
            {
                BlockPos testPos = GetClosestPos(node.GetPos());
                if(node.networkConnectionNode == null || node.networkConnectionNode.node.get() == this)
                {
                    //check to see if the direct network connection is broken
                    if (!node.CanConnectToPos(level, testPos, TestRange))
                    {
                        node.networkConnectionNode = null;
                        node.connectedNetwork = null;
                        node.connectedNodes.removeIf(connection->connection.node.get() == this);
                        orphanedNodes.add(node);
                    }
                }
                else
                {
                    //check to see if we can form a direct connection
                    if (node.CanConnectToPos(level, testPos, node.autoConnectRange))
                    {
                        node.AddConnectionAndNetworkConnection(this, eConnectionType.AutoConnect);
                    }
                }
            }
            for(Iterator<WispNode.Connection> connIt = node.connectedNodes.iterator(); connIt.hasNext();)
            {
                WispNode.Connection connection = connIt.next();
                if(connection.node.get() == this)
                {
                    //already handled above
                    continue;
                }
                if(!node.CanConnectToPos(level, connection.nodePos, TestRange))
                {
                    //break the connection
                    if(node.networkConnectionNode == connection)
                    {
                        node.networkConnectionNode = null;
                        node.connectedNetwork = null;
                        orphanedNodes.add(node);
                    }

                    WispNode otherNode = connection.node.get();
                    assert otherNode != null;

                    for(Iterator<WispNode.Connection> otherConnIt = otherNode.connectedNodes.iterator(); otherConnIt.hasNext();)
                    {
                        WispNode.Connection otherConnection = otherConnIt.next();
                        if(otherConnection.nodePos.equals(node.GetPos()))
                        {
                            if(otherNode.networkConnectionNode == otherConnection)
                            {
                                otherNode.networkConnectionNode = null;
                                otherNode.connectedNetwork = null;
                                orphanedNodes.add(otherNode);
                            }
                            otherConnIt.remove();
                            break;
                        }
                    }

                    connIt.remove();
                }
            }

            //Second make sure any new connections are made
            EnsureConnectionToAllNodesInRange(level, node);
        }


        if(orphanedNodes.isEmpty())
        {
            return null;
        }
        else
        {
            ProcessOrphanedNodes(dim, orphanedNodes);

            HashMap<ResourceLocation,ArrayList<WispNode>> orphanedNodesMap = new HashMap<>();
            orphanedNodesMap.put(dim, orphanedNodes);
            return orphanedNodesMap;
        }
    }

    public WispNode GetClosestNodeToPos(ResourceLocation dim, BlockPos pos, int maxRange)
    {
        DimensionData dimData = GetDimensionData(dim);
        if(dim == GetDim())
        {
            BlockPos offset = GetPos().subtract(pos);
            int distance = Math.abs(offset.getX()) + Math.abs(offset.getY()) + Math.abs(offset.getZ());
            if(distance <= maxRange)
            {
                return this;
            }
        }
        if(dimData == null)
        {
            return null;
        }

        WispNode bestNode = null;
        int bestNodeRange = maxRange + 1;

        BlockPos maxRangeVec = new BlockPos(maxRange, 0, maxRange);
        ChunkPos chunkMinPos = Utils.GetChunkPos(pos.subtract(maxRangeVec));
        ChunkPos chunkMaxPos = Utils.GetChunkPos(pos.offset(maxRangeVec));
        for(int x = chunkMinPos.x; x <= chunkMaxPos.x; ++x)
        for(int z = chunkMinPos.z; z <= chunkMaxPos.z; ++z)
        {
            ChunkData chunkData = dimData.GetChunkData(x, z);
            if(chunkData == null)
            {
                continue;
            }
            for(WispNode node : chunkData.nodes)
            {
                if(!node.CanBeUsedAsNetworkConnection())
                {
                    continue;
                }
                BlockPos offset = node.GetPos().subtract(pos);
                int distance = Math.abs(offset.getX()) + Math.abs(offset.getY()) + Math.abs(offset.getZ());
                if(distance < bestNodeRange)
                {
                    bestNode = node;
                    bestNodeRange = distance;
                }
            }
        }
        return bestNode;
    }

    public BlockPos GetClosestPos(BlockPos inPos)
    {
        BlockPos offset = inPos.subtract(GetPos());
        offset = Utils.clamp(offset, -1, 1);
        return GetPos().offset(offset);
    }

    public void Tick(TickEvent.ServerTickEvent tickEvent)
    {
        ++tickTime;
        taskManager.Advance(tickEvent, this, tickTime);
        itemMangement.Tick();
    }

    public void StartTask(WispTask task)
    {
        taskManager.StartTask(task, this, tickTime);
    }

    public WispNetworkItemManagement GetItemManagement()
    {
        return itemMangement;
    }

    private @Nonnull WispNode.NextPathStep GeneratePath(WispNode start, WispNode end)
    {
        WispNode.NextPathStep cachedPath = start.cachedPaths.get(end);
        if(cachedPath != null)
        {
            assert cachedPath.node != null;
            return cachedPath;
        }

        class PathNode implements Comparable<PathNode>
        {
            float distanceTraveled = Float.MAX_VALUE;
            float estimatedScore = Float.MAX_VALUE;
            WispNode node;
            PathNode previousNode;

            @Override
            public int compareTo(PathNode other)
            {
                return Float.compare(this.estimatedScore, other.estimatedScore);
            }
        }

        final HashMap<WispNode, PathNode> visitedNodes = new HashMap<>();
        final PriorityQueue<PathNode> nodesToVisit = new PriorityQueue<>();

        PathNode startNode = new PathNode();
        startNode.distanceTraveled = 0.f;
        startNode.estimatedScore = (float)Math.sqrt(start.GetPos().distSqr(end.GetPos()));
        startNode.node = start;
        startNode.previousNode = null;
        visitedNodes.put(start, startNode);
        nodesToVisit.add(startNode);

        while(!nodesToVisit.isEmpty())
        {
            PathNode currentNode = nodesToVisit.poll();
            WispNode.NextPathStep currentNodeCachedPath = currentNode.node.cachedPaths.get(end);
            if(currentNode.node == end || currentNodeCachedPath != null)
            {
                //done
                //either reached out goal, or reached a node with a cached path to the goal
                //reconstruct the path by going backwards from this node until we reach the start

                //now go through and cache the path into the nodes
                float lengthToDestination = currentNode.node == end ? 0.f : currentNodeCachedPath.distToDestination;
                for(PathNode pathNode = currentNode; pathNode.previousNode != null; pathNode = pathNode.previousNode)
                {
                    float connectionLength = pathNode.distanceTraveled - pathNode.previousNode.distanceTraveled;
                    lengthToDestination += connectionLength;

                    WispNode.NextPathStep previousCachedPathStep = new WispNode.NextPathStep();
                    previousCachedPathStep.node = pathNode.node;
                    previousCachedPathStep.distToDestination = lengthToDestination;
                    previousCachedPathStep.distToNode = connectionLength;
                    pathNode.previousNode.node.cachedPaths.put(end, previousCachedPathStep);
                }

                WispNode.NextPathStep startNextPathStep = start.cachedPaths.get(end);
                assert startNextPathStep != null;
                assert startNextPathStep.node != null;
                return startNextPathStep;
            }
            for(WispNode.Connection connection : currentNode.node.connectedNodes)
            {
                WispNode otherNode = connection.node.get();
                assert otherNode != null;
                float connectionLength = (float)Math.sqrt(currentNode.node.GetPos().distSqr(otherNode.GetPos()));
                float totalDistanceTraveled = currentNode.distanceTraveled + connectionLength;
                PathNode visitedNodeInfo = visitedNodes.computeIfAbsent(otherNode,l-> new PathNode());
                boolean preExisting = visitedNodeInfo.node != null;
                visitedNodeInfo.node = otherNode;
                if(totalDistanceTraveled < visitedNodeInfo.distanceTraveled)
                {
                    if(preExisting)
                    {
                        nodesToVisit.remove(visitedNodeInfo);
                    }
                    visitedNodeInfo.previousNode = currentNode;
                    visitedNodeInfo.distanceTraveled = totalDistanceTraveled;

                    WispNode.NextPathStep otherNodeCachedPath = otherNode.cachedPaths.get(end);
                    if(otherNodeCachedPath != null)
                    {
                        visitedNodeInfo.estimatedScore = visitedNodeInfo.distanceTraveled + otherNodeCachedPath.distToDestination;
                    }
                    else
                    {
                        float estimatedDistanceToEnd = (float)Math.sqrt(otherNode.GetPos().distSqr(end.GetPos()));
                        visitedNodeInfo.estimatedScore = visitedNodeInfo.distanceTraveled + estimatedDistanceToEnd;
                    }

                    nodesToVisit.add(visitedNodeInfo);
                }
            }
        }
        //no path :(
        LogisticsWoW.LOGGER.error("Failed to find path from node {} to {}", start.GetPos().toString(), end.GetPos().toString());
        throw new RuntimeException("No possible path between nodes in network");
    }

    public @Nonnull WispNode.NextPathStep GetNextPathStep(WispNode start, WispNode end)
    {
        assert start.connectedNetwork == this;
        assert end.connectedNetwork == this;
        if(end.CanBeUsedAsNetworkConnection())
        {
            return GeneratePath(start, end);
        }
        else
        {
            WispNode.NextPathStep bestPathStep = null;
            //don't add the end directly as a path to help cut down the number of cached paths
            //most of the time a node of this type is only going to be connected to a single node
            //or nodes that are close to each other in the network anyway
            assert !end.connectedNodes.isEmpty();
            for(WispNode.Connection connection : end.connectedNodes)
            {
                WispNode otherNode = connection.node.get();
                assert otherNode != null;
                if(otherNode == start)
                {
                    //we are connected directly to the end
                    WispNode.NextPathStep tempNextStep = new WispNode.NextPathStep();
                    tempNextStep.distToDestination = (float)Math.sqrt(start.GetPos().distSqr(end.GetPos()));
                    tempNextStep.distToNode = tempNextStep.distToDestination;
                    tempNextStep.node = end;
                    return tempNextStep;
                }
                WispNode.NextPathStep path = GeneratePath(start, otherNode);
                if(bestPathStep == null || path.distToDestination < bestPathStep.distToDestination)
                {
                    bestPathStep = path;
                }
            }
            assert bestPathStep.node != null;
            return bestPathStep;
        }
    }

    public CarryWisp TryReserveCarryWisp(WispNode destination, int targetCapacity)
    {
        return new CarryWisp(this, 0, 0);
    }

    public WispNode GetBestCarryWispSink(WispNode fromNode)
    {
        return this;
    }

    public static WispNetwork CreateAndRead(ResourceLocation dim, BlockPos pos, CompoundTag nbt)
    {
        WispNetwork network = new WispNetwork(dim, pos);
        network.read(nbt);
        return network;
    }

    public void read(CompoundTag nbt)
    {
        super.Load(GetDim(), nbt);
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
                WispInteractionNodeBase loadedWisp = WispFactory.LoadWisp(dimensionName, wispNBT);

                BlockPos wispPos = loadedWisp.GetPos();
                dimData.EnsureChunkData(Utils.GetChunkPos(wispPos)).nodes.add(loadedWisp);
            }
            ListTag networkNodes = dimDataNBT.getList("nodes", nbt.getId());
            for(int i = 0; i < networkNodes.size(); ++i)
            {
                WispNode loadedNode = WispNode.ReadNode(dimensionName, networkNodes.getCompound(i));
                dimData.EnsureChunkData(Utils.GetChunkPos(loadedNode.GetPos())).nodes.add(loadedNode);
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
                         WispNode otherNode = GetPos().equals(connection.nodePos) && dimData.getKey().equals(GetDim()) ? this : GetNode(dimData.getKey(), connection.nodePos);
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

        {
            WeakReference<WispNode> nodeWeak = new WeakReference<>(this);
            for(WispNode.Connection connection : connectedNodes)
            {
                WispNode otherNode = GetNode(GetDim(), connection.nodePos);
                for(WispNode.Connection otherConnection : otherNode.connectedNodes)
                {
                    if(otherConnection.nodePos.equals(GetPos()))
                    {
                        otherConnection.node = nodeWeak;
                    }
                }
            }
        }

        taskManager.DeserialiseNBT(this, tickTime, nbt.getCompound("taskManager"));
    }

    public CompoundTag write(CompoundTag compound)
    {
        CompoundTag taskManagerNBT = taskManager.SerialiseNBT(this, tickTime);
        if(taskManagerNBT != null)
        {
            compound.put("taskManager", taskManagerNBT);
        }
        CompoundTag dimensionDataNBT = new CompoundTag();
        for(Map.Entry<ResourceLocation, DimensionData> dimensionEntry : dimensionData.entrySet())
        {
            DimensionData dimData = dimensionEntry.getValue();

            ListTag networkWisps = new ListTag();
            ListTag networkNodes = new ListTag();
            for(ChunkData chunkData : dimData.chunkData.values())
            {
                for(WispNode node : chunkData.nodes)
                {
                    if(node instanceof WispInteractionNodeBase)
                    {
                        networkWisps.add(node.Save());
                    }
                    else
                    {
                        networkNodes.add(node.Save());
                    }
                }
            }

            CompoundTag dimDataNBT = new CompoundTag();
            dimDataNBT.put("wisps", networkWisps);
            dimDataNBT.put("nodes", networkNodes);

            dimensionDataNBT.put(dimensionEntry.getKey().toString(), dimDataNBT);
        }
        compound.put("dimensionData", dimensionDataNBT);
        Write(this, compound);

        return compound;
    }

    @OnlyIn(Dist.CLIENT)
    public void Render(RenderLevelLastEvent evt, PoseStack poseStack, VertexConsumer builder)
    {
        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();

        for (DimensionData dimData : dimensionData.values())
        {
            for (ChunkData chunkData : dimData.chunkData.values())
            {
                chunkData.nodes.forEach((node) ->
                {
                    for (WispNode.Connection connection : node.connectedNodes)
                    {
                        float[] rgb = {0.f, 1.f, 0.f};
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

                        builder.vertex(matrix, connection.nodePos.getX(), connection.nodePos.getY(), connection.nodePos.getZ())
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

                    builder.vertex(matrix, GetPos().getX(), GetPos().getY(), GetPos().getZ())
                            .color(0.4f, 0.4f, 0.4f, 0.8f)
                            //.overlayCoords(OverlayTexture.NO_OVERLAY)
                            //.lightmap(15728880)
                            .normal(0.f, 1.f, 0.f)
                            .endVertex();

                    //show the node as a cross
                    for(Direction.Axis axis : Direction.Axis.values())
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
                });
            }
            poseStack.popPose();
        }
    }
}
