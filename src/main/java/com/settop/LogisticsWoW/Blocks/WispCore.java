package com.settop.LogisticsWoW.Blocks;

import com.settop.LogisticsWoW.BlockEntities.WispCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.extensions.IForgeBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WispCore extends Block implements IForgeBlock, EntityBlock
{
    public enum WispCoreType implements StringRepresentable
    {
        CORE,
        RING,
        CORE_COMPLETE;


        @Override
        public String getSerializedName()
        {
            switch(this)
            {
                case CORE: return "core";
                case RING: return "ring";
                case CORE_COMPLETE: return "core_complete";
                default: return "";
            }
        }
    }

    public static final EnumProperty<WispCoreType> TYPE = EnumProperty.create("type", WispCoreType.class );

    public WispCore()
    {
        super( Block.Properties.of( Material.METAL )
                .strength( 4.f, 10.f )
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .isViewBlocking((BlockState state, BlockGetter getter, BlockPos pos)->state.getValue(TYPE) == WispCoreType.CORE)
        );

        registerDefaultState
                (
                        stateDefinition.any()
                                .setValue(TYPE, WispCoreType.CORE)
                );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(TYPE);
    }

    @Override
    public void setPlacedBy(@NotNull Level worldIn, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack)
    {
        super.setPlacedBy(worldIn, pos, state, placer, stack);
        if(state.getValue(TYPE) == WispCoreType.CORE)
        {
            WispCoreBlockEntity tileEntity = (WispCoreBlockEntity) worldIn.getBlockEntity(pos);
            if (tileEntity != null)
            {
                tileEntity.CheckMultiBlockForm();
                tileEntity.onPlace();
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, BlockState state)
    {
        switch(state.getValue(TYPE))
        {
            case CORE:
            case CORE_COMPLETE:
                return new WispCoreBlockEntity(pos, state);
            default:
                return null;
        }
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter getter, BlockPos pos)
    {
        return state.getValue(TYPE) != WispCoreType.CORE;
    }

    public VoxelShape getVisualShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext collisionContext)
    {
        if(state.getValue(TYPE) == WispCoreType.CORE)
        {
            return super.getVisualShape(state, getter, pos, collisionContext);
        }
        else
        {
            return Shapes.empty();
        }
    }

    public float getShadeBrightness(BlockState state, BlockGetter getter, BlockPos pos)
    {
        if(state.getValue(TYPE) == WispCoreType.CORE)
        {
            return super.getShadeBrightness(state, getter, pos);
        }
        else
        {
            return 1.f;
        }
    }



    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        switch (state.getValue(TYPE))
        {
            case CORE:
                return RenderShape.MODEL;
            case CORE_COMPLETE:
            case RING:
            default:
                return RenderShape.INVISIBLE;
        }
    }

    public BlockState getBlockState( WispCoreType type )
    {
        return defaultBlockState().setValue(TYPE, type);
    }

}
