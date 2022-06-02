package com.settop.LogisticsWoW.Wisps;

import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ChunkWisps
{
    //unclaimed refers tp objects that have been loaded, but not yet been claimed by the corresponding block entity
    //unregistered refers to objects that have been claimed, but not are not part of a network
    //registered refers to objects that are part of a network

    public ArrayList<WispBase> unregisteredWispsInChunk = new ArrayList<>();
    public ArrayList<WispNode> unregisteredWispConnectionNodes = new ArrayList<>();

    public CompoundTag save()
    {
        //don't save unclaimed, something must have happened to remove their corresponding block entity

        if(unregisteredWispsInChunk.isEmpty() && unregisteredWispConnectionNodes.isEmpty())
        {
            //nothing to save
            return null;
        }
        CompoundTag nbt = new CompoundTag();

        ListTag wisps = new ListTag();
        for( WispBase wisp : unregisteredWispsInChunk )
        {
            if(wisp.claimed)
            {
                CompoundTag wispNBT = wisp.Save();
                wisps.add(wispNBT);
            }
            else
            {
                LogisticsWoW.LOGGER.error("Unclaimed wisp in chunk on save");
            }
        }
        nbt.put("wisps", wisps);

        ListTag nodes = new ListTag();
        for( WispNode node : unregisteredWispConnectionNodes )
        {
            if(node.claimed)
            {
                CompoundTag nodeNBT = node.Save();
                nodes.add(nodeNBT);
            }
            else
            {
                LogisticsWoW.LOGGER.error("Unclaimed node in chunk on save");
            }
        }
        nbt.put("nodes", nodes);

        return nbt;
    }

    public void load(ResourceLocation dim, CompoundTag nbt)
    {
        if(!unregisteredWispsInChunk.isEmpty() || !unregisteredWispConnectionNodes.isEmpty())
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
                unregisteredWispsInChunk.add(loadedWisp);
            }
        }
        if(nbt.contains("nodes"))
        {
            ListTag nodes = nbt.getList("nodes", nbt.getId());
            for(int i = 0; i < nodes.size(); ++i)
            {
                unregisteredWispConnectionNodes.add(WispNode.ReadNode(nodes.getCompound(i)));
            }
        }
    }
}
