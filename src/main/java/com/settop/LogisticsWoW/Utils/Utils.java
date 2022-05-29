package com.settop.LogisticsWoW.Utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class Utils
{
    public static void SpawnAsEntity(Level worldIn, BlockPos pos, ItemStack stack)
    {
        if (!worldIn.isClientSide && !stack.isEmpty())
        {
            float f = 0.5F;
            double d0 = (double)(worldIn.random.nextFloat() * 0.5F) + 0.25D;
            double d1 = (double)(worldIn.random.nextFloat() * 0.5F) + 0.25D;
            double d2 = (double)(worldIn.random.nextFloat() * 0.5F) + 0.25D;
            ItemEntity itementity = new ItemEntity(worldIn, (double)pos.getX() + d0, (double)pos.getY() + d1, (double)pos.getZ() + d2, stack);
            itementity.setDefaultPickUpDelay();
            worldIn.addFreshEntity(itementity);
        }
    }

    public static ChunkPos GetChunkPos(BlockPos blockPos)
    {
        return new ChunkPos(blockPos.getX() >> 4, blockPos.getZ() >> 4);
    }
}
