package com.settop.LogisticsWoW.Wisps;

import com.settop.LogisticsWoW.Utils.Utils;
import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
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

        public WeakReference<WispNode> node;//should only be null when not part of a network
        public final eConnectionType connectionType;
        public final BlockPos nodePos;
    }

    private BlockPos pos;
    public int autoConnectRange = 0;
    public ArrayList<Connection> connectedNodes;
    public boolean claimed = false;

    //networkConnection points to the node it is connected to the network with
    public WispNetwork connectedNetwork;
    public Connection networkConnectionNode;//will be null if connected directly to the network

    protected WispNode()
    {
        //expect the pos and dim to be set shortly
        connectedNodes = new ArrayList<>();
    }
    public WispNode(BlockPos pos)
    {
        this.pos = pos;
        connectedNodes = new ArrayList<>();
    }

    public void SetAutoConnectRange(int range)
    {
        if(range > MaxAutoConnectRange)
        {
            throw new RuntimeException(String.format("Setting auto connect range to %n. Max is %n", range, MaxAutoConnectRange));
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
    public boolean CanBeUsedAsNetworkConnection() { return true; }

    public void ConnectToWispNetwork(WispNetwork wispNetwork)
    {
        connectedNetwork = wispNetwork;
    }
    public void DisconnectFromWispNetwork(WispNetwork wispNetwork)
    {
        connectedNetwork = null;
        networkConnectionNode = null;
    }

    public void RemoveFromWorld(Level level)
    {
    }

    public boolean CanConnectToPos(Level world, BlockPos target, int connectionRange)
    {
        BlockPos blockOffset = target.subtract(pos);
        int minecraftDistance = Math.max(Math.abs(blockOffset.getX()), Math.max(Math.abs(blockOffset.getY()), Math.abs(blockOffset.getZ())));
        if(minecraftDistance > connectionRange)
        {
            //too far
            return false;
        }

        Tuple<Vec3, Vec3> closestPositions = Utils.GetClosestBlockPositions(pos, target);

        Vec3 direction = closestPositions.getB().subtract(closestPositions.getA()).normalize();
        //move the start pos off the block slightly to avoid a self intersection
        Vec3 thisStartPos = closestPositions.getA().add(direction.scale(0.01));

        BlockHitResult result = world.clip(new ClipContext(thisStartPos, closestPositions.getB(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));

        //success if we hit nothing, or if we hit a point that is very close the the target point
        //the hit point may not be on the actual block we are aiming at, but that is fine since we are checking points on edges precision could return either block
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(closestPositions.getB()) < 0.01;
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
            connectedNodes.add(nodeConnectionData);

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
