package com.settop.LogisticsWoW.Wisps;

import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Iterator;

public class ChunkWisps
{
    //unclaimed refers tp objects that have been loaded, but not yet been claimed by the corresponding block entity
    //unregistered refers to objects that have been claimed, but not are not part of a network
    //registered refers to objects that are part of a network

    //nodes and wisps are stored together
    public ArrayList<WispNode> unregisteredNodes = new ArrayList<>();

    public void CheckWispsValid(LevelChunk chunk)
    {
        for(WispNode node : unregisteredNodes)
        {
            if(node instanceof WispBase)
            {
                if(chunk.getBlockEntity(node.GetPos()) != null)
                {
                    node.claimed = true;
                }
            }
        }
    }

    public boolean ClearUnclaimed()
    {
        boolean anyRemoved = false;
        for(Iterator<WispNode> it = unregisteredNodes.iterator(); it.hasNext(); )
        {
            WispNode node = it.next();
            if(!node.claimed)
            {
                for(WispNode.Connection connection : node.connectedNodes)
                {
                    WispNode otherNode = connection.node.get();
                    if(otherNode != null)
                    {
                        otherNode.connectedNodes.removeIf(otherConnection -> otherConnection.nodePos.equals(node.GetPos()));
                    }
                }
                node.connectedNodes.clear();
                it.remove();
                anyRemoved = true;
            }
        }
        return anyRemoved;
    }

    public CompoundTag save()
    {
        if(unregisteredNodes.isEmpty())
        {
            //nothing to save
            return null;
        }
        CompoundTag nbt = new CompoundTag();

        ListTag wisps = new ListTag();
        ListTag nodes = new ListTag();
        for( WispNode node : unregisteredNodes)
        {
            if(node instanceof WispBase)
            {
                CompoundTag wispNBT = node.Save();
                wisps.add(wispNBT);
            }
            else
            {
                CompoundTag nodeNBT = node.Save();
                nodes.add(nodeNBT);
            }
        }
        nbt.put("wisps", wisps);
        nbt.put("nodes", nodes);

        return nbt;
    }

    public void load(ResourceLocation dim, CompoundTag nbt)
    {
        if(!unregisteredNodes.isEmpty())
        {
            LogisticsWoW.LOGGER.warn("Duplicate load in ChunkWisps");
            return;
        }

        if(nbt.contains("wisps"))
        {
            ListTag wisps = nbt.getList("wisps", nbt.getId());
            for (int i = 0; i < wisps.size(); ++i)
            {
                WispBase loadedWisp = WispFactory.LoadWisp(dim, wisps.getCompound(i));
                unregisteredNodes.add(loadedWisp);
            }
        }
        if(nbt.contains("nodes"))
        {
            ListTag nodes = nbt.getList("nodes", nbt.getId());
            for(int i = 0; i < nodes.size(); ++i)
            {
                unregisteredNodes.add(WispNode.ReadNode(nodes.getCompound(i)));
            }
        }
    }
}
