package com.settop.LogisticsWoW.Blocks;

import com.settop.LogisticsWoW.BlockEntities.WispConnectionNodeBlockEntity;
import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Material;
import org.jetbrains.annotations.Nullable;

public class WispConnectionNode extends DirectionalBlock implements EntityBlock
{

    public WispConnectionNode()
    {
        super(Block.Properties.of( Material.METAL )
                .strength( 4.f, 10.f )
                .noCollission() );

        registerDefaultState
                (
                        stateDefinition.any()
                                .setValue(FACING, Direction.DOWN)
                );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new WispConnectionNodeBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack item)
    {
        super.setPlacedBy(level, pos, state, placer, item);

        level.getBlockEntity(pos, LogisticsWoW.BlockEntities.WISP_CONNECTION_NODE_TILE_ENTITY.get()).ifPresent(WispConnectionNodeBlockEntity::onPlace);
    }
}
