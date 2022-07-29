package com.settop.LogisticsWoW.WispNetwork.Tasks;

import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public abstract class ResourceHoldingTask implements SerialisableWispTask
{
    public ResourceHoldingTask()
    {

    }

    public abstract ItemStack GetHeldResourceAsItemStack();
}
