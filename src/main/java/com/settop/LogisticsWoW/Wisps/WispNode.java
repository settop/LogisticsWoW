package com.settop.LogisticsWoW.Wisps;

import com.settop.LogisticsWoW.Utils.Utils;
import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

public class WispNode
{
    public static final int MaxAutoConnectRange = 8;

    public enum eConnectionType
    {
        Link,
        AutoConnect
    }

    public static class Connection
    {
        public Connection(WispNode inNode, eConnectionType inConnection)
        {
            node = new WeakReference<>(inNode);
            connectionType = inConnection;
            nodePos = inNode.pos;
        }
        public Connection(BlockPos nodePos, eConnectionType inConnection)
        {
            connectionType = inConnection;
            this.nodePos = nodePos;
        }

        public WeakReference<WispNode> node;
        public final eConnectionType connectionType;
        public final BlockPos nodePos;
    }

    private BlockPos pos;
    public int autoConnectRange = 0;
    public ArrayList<Connection> inactiveConnections;
    public ArrayList<Connection> connectedNodes;

    //networkConnection points to the node it is connected to the network with
    public WispNetwork connectedNetwork;
    public Connection networkConnectionNode;//will be null if connected directly to the network

    protected WispNode()
    {
        //expect the pos and dim to be set shortly
        inactiveConnections = new ArrayList<>();
        connectedNodes = new ArrayList<>();
    }
    public WispNode(BlockPos pos)
    {
        this.pos = pos;
        inactiveConnections = new ArrayList<>();
        connectedNodes = new ArrayList<>();
    }

    public void SetAutoConnectRange(int range)
    {
        if(range > MaxAutoConnectRange)
        {
            throw new RuntimeException(String.format("Setting auto connect range to {}. Max is {}", range, MaxAutoConnectRange));
        }
        autoConnectRange = range;
    }

    public BlockPos GetPos()
    {
        return pos;
    }
    public int GetAutoConnectRange()
    {
        return autoConnectRange;
    }

    //Can return null even if connected to a network, if the network isn't loaded
    public WispNetwork GetConnectedNetwork()
    {
        return connectedNetwork;
    }

    public boolean IsConnectedToANetwork()
    {
        return connectedNetwork != null;
    }

    public void ConnectToWispNetwork(WispNetwork wispNetwork)
    {
        connectedNetwork = wispNetwork;
    }
    public void DisconnectFromWispNetwork(WispNetwork wispNetwork)
    {
    }

    public void RemoveFromWorld(Level level)
    {
    }

    //called when a node in the same or neighbouring chunk is loaded
    public void NotifyNearbyNodeLoad(WispNode loadedNode)
    {
        for(Iterator<Connection> it = inactiveConnections.iterator(); it.hasNext();)
        {
            Connection inactiveConnection = it.next();
            if(inactiveConnection.nodePos.equals(loadedNode.pos))
            {
                inactiveConnection.node = new WeakReference<>(loadedNode);
                connectedNodes.add(inactiveConnection);

                it.remove();
                return;
            }
        }
    }

    //called when this chunk or a neighbouring chunk has finished loading
    public boolean NotifyNearbyChunkFinishedLoad(ChunkPos chunkPos)
    {
        boolean lostNetworkConnection = false;
        //remove any inactive connections to a chunk that has finished loading, node must have vanished
        for(Iterator<Connection> it = inactiveConnections.iterator(); it.hasNext();)
        {
            Connection inactiveConnection = it.next();
            if(Utils.GetChunkPos(inactiveConnection.nodePos).equals(chunkPos))
            {
                it.remove();
                if(inactiveConnection == networkConnectionNode)
                {
                    networkConnectionNode = null;
                    lostNetworkConnection = true;
                }
            }
        }
        return lostNetworkConnection;
    }

    public void NotifyNearbyChunkUnload(ChunkPos chunkPos)
    {
        for(Iterator<Connection> it = connectedNodes.iterator(); it.hasNext();)
        {
            Connection activeConnection = it.next();
            if(Utils.GetChunkPos(activeConnection.nodePos).equals(chunkPos))
            {
                //this node is going inactive
                activeConnection.node = null;
                inactiveConnections.add(activeConnection);
                it.remove();
            }
        }
    }

    public boolean CanConnectToPos(Level world, Vec3 target, int connectionRange)
    {
        double rangeSq = (connectionRange * connectionRange) + 0.01;

        Vec3 startPos = Vec3.atCenterOf(pos);
        if(target.distanceToSqr(startPos) > rangeSq)
        {
            //too far
            return false;
        }

        //ToDo: Do this correctly with the proper model
        Vec3 direction = target.subtract(startPos).normalize();
        Vec3 thisStartPos = startPos.add(direction.scale(0.4));//start it off the centre, but still within the block

        BlockHitResult result = world.clip(new ClipContext(thisStartPos, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));

        if(result.getType() == HitResult.Type.MISS || result.getBlockPos().equals(target))
        {
            return true;
        }

        return false;
    }

    public void EnsureConnection(WispNode node, eConnectionType type)
    {
        for(Connection existingConnection : connectedNodes)
        {
            if(existingConnection.nodePos.equals(node.pos))
            {
                //connection exists
                return;
            }
        }
        //no need to check inactive connections I don't think
        connectedNodes.add(new Connection(node, type));
        node.connectedNodes.add(new Connection(this, type));
    }

    public void AddConnectionAndNetworkConnection(WispNode node, eConnectionType type)
    {
        Connection nodeConnection = new Connection(node, type);
        connectedNetwork = node.connectedNetwork;
        networkConnectionNode = nodeConnection;

        connectedNodes.add(nodeConnection);
        node.connectedNodes.add(new Connection(this, type));
    }

    public CompoundTag Save()
    {
        return Write(this, new CompoundTag());
    }

    public static CompoundTag Write(WispNode node, CompoundTag nbt)
    {
        nbt.put("pos", NbtUtils.writeBlockPos(node.pos));
        nbt.putInt("autoConnectRange", node.autoConnectRange);

        ListTag nodeConnections = new ListTag();
        for(WispNode.Connection connection : node.connectedNodes)
        {
            CompoundTag connectionNBT = new CompoundTag();
            connectionNBT.putInt("type", connection.connectionType.ordinal());
            if(connection == node.networkConnectionNode)
            {
                connectionNBT.putBoolean("isNetworkConnection", true);
            }
            connectionNBT.put("pos", NbtUtils.writeBlockPos(connection.nodePos));

            nodeConnections.add(connectionNBT);
        }
        for(WispNode.Connection connection : node.inactiveConnections)
        {
            CompoundTag connectionNBT = new CompoundTag();
            connectionNBT.putInt("type", connection.connectionType.ordinal());
            if(connection == node.networkConnectionNode)
            {
                connectionNBT.putBoolean("isNetworkConnection", true);
            }
            connectionNBT.put("pos", NbtUtils.writeBlockPos(connection.nodePos));

            nodeConnections.add(connectionNBT);
        }
        nbt.put("nodes", nodeConnections);

        if(node.connectedNetwork != null)
        {
            nbt.putString("ConnectedNetworkDim", node.connectedNetwork.GetDim().toString());
            nbt.put("ConnectedNetworkPos", NbtUtils.writeBlockPos(node.connectedNetwork.GetPos()));
        }


        return nbt;
    }

    public void Load(CompoundTag nbt)
    {
        pos = NbtUtils.readBlockPos(nbt.getCompound("pos"));
        autoConnectRange = nbt.getInt("autoConnectRange");

        if(nbt.contains("ConnectedNetworkDim") && nbt.contains("ConnectedNetworkPos"))
        {
            ResourceLocation dim = ResourceLocation.tryParse(nbt.getString("ConnectedNetworkDim"));
            BlockPos networkPos = NbtUtils.readBlockPos(nbt.getCompound("ConnectedNetworkPos"));
            connectedNetwork = GlobalWispData.GetWispNetwork(dim, networkPos);
        }

        ListTag nodeConnectionsNBT = nbt.getList("nodes", nbt.getId());
        for(int i = 0; i < nodeConnectionsNBT.size(); ++i)
        {
            CompoundTag connectionNBT = nodeConnectionsNBT.getCompound(i);

            eConnectionType connectionType = eConnectionType.values()[connectionNBT.getInt("type")];
            BlockPos connectionNodePos = NbtUtils.readBlockPos(connectionNBT.getCompound("pos"));

            Connection nodeConnectionData = new Connection(connectionNodePos, connectionType);
            inactiveConnections.add(nodeConnectionData);

            if(connectionNBT.contains("isNetworkConnection"))
            {
                boolean isNetworkConnection = connectionNBT.getBoolean("isNetworkConnection");
                if(isNetworkConnection)
                {
                    networkConnectionNode = nodeConnectionData;
                }
            }
        }
    }

    public static WispNode ReadNode(CompoundTag nbt)
    {
        WispNode node = new WispNode();
        node.Load(nbt);
        return node;
    }

}
