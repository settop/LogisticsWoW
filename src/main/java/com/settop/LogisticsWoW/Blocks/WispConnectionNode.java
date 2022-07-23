package com.settop.LogisticsWoW.Blocks;

import com.settop.LogisticsWoW.BlockEntities.WispConnectionNodeBlockEntity;
import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WispConnectionNode extends DirectionalBlock implements EntityBlock, SimpleWaterloggedBlock
{
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    protected final VoxelShape[] collisionShapeByDirection;

    public WispConnectionNode()
    {
        super(Block.Properties.of( Material.METAL )
                .strength( 4.f, 10.f ));

        registerDefaultState
                (
                        stateDefinition.any()
                                .setValue(FACING, Direction.DOWN)
                                .setValue(WATERLOGGED, false)
                );
        collisionShapeByDirection = makeShapes();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState()
                .setValue(FACING, context.getClickedFace().getOpposite())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return new WispConnectionNodeBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack item)
    {
        super.setPlacedBy(level, pos, state, placer, item);

        level.getBlockEntity(pos, LogisticsWoW.BlockEntities.WISP_CONNECTION_NODE_TILE_ENTITY.get()).ifPresent(WispConnectionNodeBlockEntity::onPlace);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState blockState, @NotNull BlockGetter blockGetter, @NotNull BlockPos pos, @NotNull CollisionContext collisionContext)
    {
        return collisionShapeByDirection[blockState.getValue(FACING).get3DDataValue()];
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState blockState, @NotNull BlockGetter blockGetter, @NotNull BlockPos pos, @NotNull CollisionContext collisionContext)
    {
        return collisionShapeByDirection[blockState.getValue(FACING).get3DDataValue()];
    }

    protected VoxelShape[] makeShapes()
    {
        VoxelShape[] shapes = new VoxelShape[6];

        for(int i = 0; i < 6; ++i)
        {
            Direction direction = Direction.from3DDataValue(i);
            Direction.AxisDirection axisDirection = direction.getAxisDirection();
            Vec3i up = Direction.fromAxisAndDirection(direction.getAxis(), Direction.AxisDirection.POSITIVE).getNormal();
            Vec3i side = new Vec3i(1, 1, 1).subtract(up);

            Vec3i baseMin = side.multiply(2);
            Vec3i baseMax = side.multiply(14);
            if(axisDirection == Direction.AxisDirection.POSITIVE)
            {
                baseMin = baseMin.offset(up.multiply(14));
                baseMax = baseMax.offset(up.multiply(16));
            }
            else
            {
                baseMax = baseMax.offset(up.multiply(2));
            }
            VoxelShape voxelshapeBase = Block.box(baseMin.getX(), baseMin.getY(), baseMin.getZ(), baseMax.getX(), baseMax.getY(), baseMax.getZ());

            Vec3i midMin = side.multiply(6);
            Vec3i midMax = side.multiply(10);
            if(axisDirection == Direction.AxisDirection.POSITIVE)
            {
                midMin = midMin.offset(up.multiply(10));
                midMax = midMax.offset(up.multiply(14));
            }
            else
            {
                midMin = midMin.offset(up.multiply(2));
                midMax = midMax.offset(up.multiply(6));
            }
            VoxelShape voxelshapeMid = Block.box(midMin.getX(), midMin.getY(), midMin.getZ(), midMax.getX(), midMax.getY(), midMax.getZ());

            shapes[i] = Shapes.or(voxelshapeBase, voxelshapeMid);
        }

        return shapes;
    }

    @Override
    public @NotNull BlockState updateShape(BlockState blockState, @NotNull Direction updateDirection, @NotNull BlockState otherBlockState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos otherPos)
    {
        if (blockState.getValue(WATERLOGGED))
        {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return super.updateShape(blockState, updateDirection, otherBlockState, level, pos, otherPos);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState blockState)
    {
        return blockState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState blockState, Rotation rotationType)
    {
        Direction newFacingDirection = rotationType.rotate(blockState.getValue(FACING));
        blockState.setValue(FACING, newFacingDirection);
        return blockState;
    }

    @Override
    public @NotNull BlockState mirror(@NotNull BlockState blockState, @NotNull Mirror mirrorType)
    {
        Direction newFacingDirection = mirrorType.mirror(blockState.getValue(FACING));
        blockState.setValue(FACING, newFacingDirection);
        return blockState;
    }
}
