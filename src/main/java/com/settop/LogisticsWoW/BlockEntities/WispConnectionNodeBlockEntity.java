package com.settop.LogisticsWoW.BlockEntities;


import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Wisps.GlobalWispData;
import com.settop.LogisticsWoW.Wisps.WispNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WispConnectionNodeBlockEntity extends BlockEntity
{
    private WispNode node;
    private int autoConnectRange = 8;
    private boolean unloading = false;

    public WispConnectionNodeBlockEntity(BlockPos pos, BlockState state)
    {
        super(LogisticsWoW.BlockEntities.WISP_CONNECTION_NODE_TILE_ENTITY.get(), pos, state);
    }

    public void onPlace()
    {
        if(!level.isClientSide)
        {
            if (node == null)
            {
                LogisticsWoW.LOGGER.warn("WispConnectionNodeBlockEntity::onPlace called but has no node");
                return;
            }
            try
            {
                GlobalWispData.TryAndConnectNodeToANetwork(level, node, autoConnectRange);
            }
            catch (Exception ex)
            {
                LogisticsWoW.LOGGER.error("WispConnectionNodeBlockEntity::onPlace exception {}", ex.getMessage());
            }
        }

    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        if(!level.isClientSide)
        {
            try
            {
                node = GlobalWispData.CreateOrClaimNode(level, getBlockPos());
            }
            catch (Exception ex)
            {
                LogisticsWoW.LOGGER.error("WispConnectionNodeBlockEntity::onLoad exception {}", ex.getMessage());
            }
        }
    }

    @Override
    public void onChunkUnloaded()
    {
        super.onChunkUnloaded();
        unloading = true;
        if(node != null)
        {
            //ChunkWispData.RemoveNode(world, node);
        }
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        if(!level.isClientSide && !unloading)
        {
            if (node != null)
            {
                GlobalWispData.RemoveNode(level, node);
            }
        }
    }

    /*
    @Override
    public void clearRemoved()
    {
        super.clearRemoved();
        if(!level.isClientSide)
        {
            try
            {
                node = GlobalWispData.CreateOrClaimNode(level, getBlockPos());
            }
            catch (Exception ex)
            {
                LogisticsWoW.LOGGER.warn("WispConnectionNodeBlockEntity::onLoad exception {}", ex.getMessage());
            }
        }
    }
     */
}
