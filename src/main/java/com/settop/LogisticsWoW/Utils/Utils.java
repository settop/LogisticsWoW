package com.settop.LogisticsWoW.Utils;

import com.mojang.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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

    public static float clamp(float v, float min, float max)
    {
        return Math.min(Math.max(v, min), max);
    }
    public static double clamp(double v, double min, double max)
    {
        return Math.min(Math.max(v, min), max);
    }
    public static int clamp(int v, int min, int max)
    {
        return Math.min(Math.max(v, min), max);
    }

    public static BlockPos clamp(BlockPos v, BlockPos min, BlockPos max)
    {
        return new BlockPos
                (
                        clamp(v.getX(), min.getX(), max.getX()),
                        clamp(v.getY(), min.getY(), max.getY()),
                        clamp(v.getZ(), min.getZ(), max.getZ())
                );
    }
    public static BlockPos clamp(BlockPos v, int min, int max)
    {
        return new BlockPos
                (
                        clamp(v.getX(), min, max),
                        clamp(v.getY(), min, max),
                        clamp(v.getZ(), min, max)
                );
    }

    public static Vector3f clamp(Vector3f v, Vector3f min, Vector3f max)
    {
        return new Vector3f
                (
                        clamp(v.x(), min.x(), max.x()),
                        clamp(v.y(), min.y(), max.y()),
                        clamp(v.z(), min.z(), max.z())
                );
    }

    public static Vec3 clamp(Vec3 v, Vec3 min, Vec3 max)
    {
        return new Vec3
                (
                        clamp(v.x(), min.x(), max.x()),
                        clamp(v.y(), min.y(), max.y()),
                        clamp(v.z(), min.z(), max.z())
                );
    }

    public static Vec3 clamp(Vec3 v, double min, double max)
    {
        return new Vec3
                (
                        clamp(v.x(), min, max),
                        clamp(v.y(), min, max),
                        clamp(v.z(), min, max)
                );
    }

    public static Tuple<Vec3, Vec3> GetClosestBlockPositions(BlockPos a, BlockPos b)
    {
        Vec3 aVec = Vec3.atCenterOf(a);
        Vec3 bVec = Vec3.atCenterOf(b);

        Vec3 offset = clamp(bVec.subtract(aVec), -0.5, 0.5);

        return new Tuple<>(aVec.add(offset), bVec.subtract(offset));
    }
}
