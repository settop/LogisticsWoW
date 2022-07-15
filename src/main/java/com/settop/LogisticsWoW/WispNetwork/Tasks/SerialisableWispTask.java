package com.settop.LogisticsWoW.WispNetwork.Tasks;

import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;

public interface SerialisableWispTask extends WispTask
{
    String GetSerialisableName();
    default CompoundTag SerialiseNBT(WispNetwork network)
    {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("type", GetSerialisableName());
        return nbt;
    }

    void DeserialiseNBT(WispNetwork network, CompoundTag nbt);
}
