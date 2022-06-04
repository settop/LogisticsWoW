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
import net.minecraftforge.event.world.ChunkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

public class LevelWispData extends SavedData
{
    static public final String NAME = "WispData";

    public static final int InitialChunkTickUpdateWait = 10;
    public static final int MaxChunkTickUpdateWait = 40;
    public static final int AdditionalChunkTickUpdateWait = 2;

    public static class LoadingChunkData
    {
        public ChunkPos chunkPos;
        public int tickCounter;
    }
    public static class UpdatedChunk
    {
        public int tickCounter;
        public int totalTicksToWait;
    }
    public final ArrayDeque<LoadingChunkData> loadingChunks = new ArrayDeque<>();
    public final HashMap<ChunkPos, UpdatedChunk> updatedChunkTimers = new HashMap<>();

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
    public boolean HasChunkData(ChunkPos chunkPos)
    {
        return chunkData.containsKey(chunkPos);
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
                    if(existingNetwork.GetPos().equals(pos))
                    {
                        dataRead = true;
                        existingNetwork.read(networkNBT);
                        break;
                    }
                }
                if(!dataRead)
                {
                    WispNetwork network = WispNetwork.CreateAndRead(dim, pos, networkNBT);
                    wispNetworks.add(network);
                }
            }
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compound)
    {
        ListTag networksNBT = new ListTag();

        for(WispNetwork wispNetwork : wispNetworks)
        {
            CompoundTag networkNBT = new CompoundTag();
            networkNBT.put("pos", NbtUtils.writeBlockPos(wispNetwork.GetPos()));
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
    }

    public void OnChunkUnload(ChunkEvent.Unload unloadEvent)
    {
        ChunkPos chunkPos = unloadEvent.getChunk().getPos();
        chunkData.remove(chunkPos);
    }
}
