package com.settop.LogisticsWoW.WispNetwork.Tasks;

import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public abstract class ItemHoldingTask implements SerialisableWispTask
{
    protected ItemStack heldItemStack = ItemStack.EMPTY;

    public ItemHoldingTask()
    {

    }

    @Override
    public CompoundTag SerialiseNBT(WispNetwork network)
    {
        CompoundTag nbt = SerialisableWispTask.super.SerialiseNBT(network);
        if(!heldItemStack.isEmpty())
        {
            nbt.put("heldItem", heldItemStack.serializeNBT());
        }
        return nbt;
    }

    @Override
    public void DeserialiseNBT(WispNetwork network, CompoundTag nbt)
    {
        if(nbt.contains("heldItem"))
        {
            heldItemStack = ItemStack.of(nbt.getCompound("heldItem"));
        }
    }
}
