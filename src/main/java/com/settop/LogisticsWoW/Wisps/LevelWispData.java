package com.settop.LogisticsWoW.Wisps;

import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.world.ChunkDataEvent;
import com.settop.LogisticsWoW.Utils.Utils;
import com.settop.LogisticsWoW.WispNetwork.WispNetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

public class LevelWispData extends SavedData
{
    static public final String NAME = "WispData";

    public final ResourceLocation dim;

    public LevelWispData(ResourceLocation dim)
    {
        super();
        this.dim = dim;
    }

    public HashMap<ChunkPos, ChunkWisps> chunkData = new HashMap<>();
    public final ArrayList<WispNetwork> wispNetworks = new ArrayList<>();

    public ChunkWisps EnsureChunkWisps(BlockPos blockPos)
    {
        ChunkPos chunkPos = Utils.GetChunkPos(blockPos);
        return chunkData.computeIfAbsent(chunkPos, (key)->new ChunkWisps());
    }
    public ChunkWisps GetChunkWisps(BlockPos blockPos)
    {
        ChunkPos chunkPos = Utils.GetChunkPos(blockPos);
        return chunkData.get(chunkPos);
    }
    public ChunkWisps GetChunkWisps(ChunkPos chunkPos)
    {
        return chunkData.get(chunkPos);
    }

    public void load(CompoundTag nbt)
    {
        if(nbt.contains("networks"))
        {
            ListTag networksNBT = nbt.getList("networks", nbt.getId());
            for(int i = 0; i < networksNBT.size(); ++i)
            {
                CompoundTag networkNBT = networksNBT.getCompound(i);
                if(!networkNBT.contains("pos"))
                {
                    continue;
                }
                BlockPos pos = NbtUtils.readBlockPos(networkNBT.getCompound("pos"));

                boolean dataRead = false;
                for(WispNetwork existingNetwork : wispNetworks)
                {
                    if(existingNetwork.pos.equals(pos))
                    {
                        dataRead = true;
                        existingNetwork.read(networkNBT);
                        break;
                    }
                }
                if(!dataRead)
                {
                    WispNetwork network = WispNetwork.CreateAndRead(dim, pos, networkNBT);
                    if(network != null)
                    {
                       wispNetworks.add(network);
                    }
                }
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag compound)
    {
        ListTag networksNBT = new ListTag();

        for(WispNetwork wispNetwork : wispNetworks)
        {
            CompoundTag networkNBT = new CompoundTag();
            networkNBT.put("pos", NbtUtils.writeBlockPos(wispNetwork.pos));
            networksNBT.add(wispNetwork.write(networkNBT));
        }
        compound.put("networks", networksNBT);

        return compound;
    }

    @Override
    public boolean isDirty()
    {
        return true;
    }

    public void OnChunkSave(ChunkDataEvent.Save saveEvent)
    {
        ChunkWisps chunkWisps = chunkData.get(saveEvent.getChunk().getPos());
        if(chunkWisps != null)
        {
            CompoundTag data = saveEvent.getData();
            CompoundTag chunkSave = chunkWisps.save();
            if(chunkSave != null)
            {
                data.put(LogisticsWoW.MOD_ID, chunkSave);
            }
        }
    }

    private void NotifyChunkOfNeighbourChunkLoad(ChunkWisps chunkToNotify, ChunkPos loadedChunkPos ,ChunkWisps loadedChunk)
    {
        //This isn't as bad as it looks, most of these arrays are going to be small or empty with one having most of the entries
        Consumer<? super WispNode> notifier = (node)->
        {
            for(WispNode loadedNode : loadedChunk.unclaimedWispConnectionNodes){node.NotifyNearbyNodeLoad(loadedNode);}
            for(WispNode loadedNode : loadedChunk.unregisteredWispConnectionNodes){node.NotifyNearbyNodeLoad(loadedNode);}
            for(WispNode loadedNode : loadedChunk.registeredWispConnectionNodes){node.NotifyNearbyNodeLoad(loadedNode);}

            for(WispNode loadedWisp : loadedChunk.unclaimedWispsInChunk){node.NotifyNearbyNodeLoad(loadedWisp);}
            for(WispNode loadedWisp : loadedChunk.unregisteredWispsInChunk){node.NotifyNearbyNodeLoad(loadedWisp);}
            for(WispNode loadedWisp : loadedChunk.registeredWispsInChunk){node.NotifyNearbyNodeLoad(loadedWisp);}
        };
        chunkToNotify.unclaimedWispConnectionNodes.forEach(notifier);
        chunkToNotify.unregisteredWispConnectionNodes.forEach(notifier);
        chunkToNotify.registeredWispConnectionNodes.forEach(notifier);

        chunkToNotify.unclaimedWispsInChunk.forEach(notifier);
        chunkToNotify.unregisteredWispsInChunk.forEach(notifier);
        chunkToNotify.registeredWispsInChunk.forEach(notifier);

        Consumer<? super WispNode> loadFinishedNotifier = (node)->
        {
            node.NotifyNearbyChunkFinishedLoad(loadedChunkPos);
            node.NotifyNearbyChunkFinishedLoad(loadedChunkPos);
            node.NotifyNearbyChunkFinishedLoad(loadedChunkPos);

            node.NotifyNearbyChunkFinishedLoad(loadedChunkPos);
            node.NotifyNearbyChunkFinishedLoad(loadedChunkPos);
            node.NotifyNearbyChunkFinishedLoad(loadedChunkPos);
        };

        chunkToNotify.unclaimedWispConnectionNodes.forEach(loadFinishedNotifier);
        chunkToNotify.unregisteredWispConnectionNodes.forEach(loadFinishedNotifier);
        chunkToNotify.registeredWispConnectionNodes.forEach(loadFinishedNotifier);

        chunkToNotify.unclaimedWispsInChunk.forEach(loadFinishedNotifier);
        chunkToNotify.unregisteredWispsInChunk.forEach(loadFinishedNotifier);
        chunkToNotify.registeredWispsInChunk.forEach(loadFinishedNotifier);
    }

    public void OnChunkLoad(ChunkDataEvent.Load loadEvent)
    {
        if(!loadEvent.getData().contains(LogisticsWoW.MOD_ID))
        {
            return;
        }
        CompoundTag chunkLoad = loadEvent.getData().getCompound(LogisticsWoW.MOD_ID);

        ChunkWisps newChunkWisps = new ChunkWisps();
        newChunkWisps.load(((Level)loadEvent.getWorld()).dimension().location(), chunkLoad);

        chunkData.put(loadEvent.getChunk().getPos(), newChunkWisps);

        NotifyChunkOfNeighbourChunkLoad(newChunkWisps, loadEvent.getChunk().getPos(), newChunkWisps);
        for(int x = -1; x <= 1; ++x)
        for(int z = -1; z <= 1; ++z)
        {
            if(x == 0 && z == 0) continue;

            ChunkPos neighbourChunkPos = new ChunkPos(x, z);
            ChunkWisps neighbourWisps = chunkData.get(neighbourChunkPos);
            if(neighbourWisps != null)
            {
                NotifyChunkOfNeighbourChunkLoad(neighbourWisps, loadEvent.getChunk().getPos(), newChunkWisps);
                NotifyChunkOfNeighbourChunkLoad(newChunkWisps, neighbourChunkPos, neighbourWisps);
            }
        }
    }
}
