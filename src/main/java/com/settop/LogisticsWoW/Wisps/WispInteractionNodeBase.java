package com.settop.LogisticsWoW.Wisps;


import com.settop.LogisticsWoW.WispNetwork.ReservableInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeLevel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public abstract class WispInteractionNodeBase extends WispNode implements MenuProvider
{
    public WispInteractionNodeBase()
    {
        //expect a load to follow
    }

    public WispInteractionNodeBase(ResourceLocation dim, BlockPos inPos)
    {
        super(dim, inPos);
    }

    public void RemoveFromWorld(Level level)
    {
        super.RemoveFromWorld(level);

    }

    @Override
    public boolean CanBeUsedAsNetworkConnection()
    {
        return false;
    }

    public CompoundTag Save()
    {
        CompoundTag nbt = super.Save();
        nbt.putString(WispConstants.WISP_TYPE_KEY, GetType());

        return nbt;
    }

    public void Load(ResourceLocation inDim, CompoundTag nbt)
    {
        super.Load(inDim, nbt);
    }

    public abstract String GetType();
    public abstract void DropItemStackIntoWorld(LevelAccessor world);
    public abstract void InitFromTagData(CompoundTag tagData);
    public abstract void UpdateFromContents();
    public abstract void SetConnectedBlockEntity(BlockEntity blockEntity);
    public abstract void ContainerExtraDataWriter(FriendlyByteBuf packetBuffer);

    //active if the block this is connected to is loaded
    public abstract boolean IsActive();
    public abstract ReservableInventory GetReservableInventory(Direction direction);
    public ReservableInventory GetReservableInventory() { return GetReservableInventory(null); }
}
