package com.settop.LogisticsWoW.Wisps;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class WispFactory
{
    @Nonnull
    public static WispBase CreateNewWisp(String type, ResourceLocation dim, BlockPos inPos)
    {
        switch (type)
        {
            case WispConstants.BASIC_WISP: return new BasicWisp(inPos);
            default: throw new RuntimeException(type + " is not a valid wisp type");
        }
    }

    @Nonnull
    public static WispBase LoadWisp(ResourceLocation dim, CompoundTag nbt)
    {
        String type = nbt.getString(WispConstants.WISP_TYPE_KEY);
        WispBase wisp = null;
        switch (type)
        {
            case WispConstants.BASIC_WISP: wisp = new BasicWisp(); break;
            default: throw new RuntimeException(type + " is not a valid wisp type");
        }
        wisp.Load(dim, nbt);
        return wisp;
    }
}
