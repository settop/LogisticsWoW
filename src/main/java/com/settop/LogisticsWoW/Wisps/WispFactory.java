package com.settop.LogisticsWoW.Wisps;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class WispFactory
{
    @Nonnull
    public static WispInteractionNodeBase CreateNewWisp(String type, ResourceLocation dim, BlockPos inPos)
    {
        switch (type)
        {
            case WispConstants.WISP_INTERACTION_NODE: return new WispInteractionNode(inPos);
            default: throw new RuntimeException(type + " is not a valid wisp type");
        }
    }

    @Nonnull
    public static WispInteractionNodeBase LoadWisp(ResourceLocation dim, CompoundTag nbt)
    {
        String type = nbt.getString(WispConstants.WISP_TYPE_KEY);
        WispInteractionNodeBase wisp = null;
        switch (type)
        {
            case WispConstants.WISP_INTERACTION_NODE: wisp = new WispInteractionNode(); break;
            default: throw new RuntimeException(type + " is not a valid wisp type");
        }
        wisp.Load(dim, nbt);
        return wisp;
    }
}
