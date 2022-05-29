package com.settop.LogisticsWoW.Events;


import com.settop.LogisticsWoW.BlockEntities.WispCoreBlockEntity;
import com.settop.LogisticsWoW.Blocks.WispCore;
import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber( modid = LogisticsWoW.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT )
public class BlockEventHandler
{


    @SubscribeEvent
    public static void OnWispCoreRingBlockPlace(BlockEvent.EntityPlaceEvent blockPlaced)
    {
        boolean blockCanFormMultiBlock = false;
        for(TagKey<Block> requiredTag : WispCoreBlockEntity.RING_BLOCK_TAGS)
        {
            if (blockPlaced.getPlacedBlock().getTags().anyMatch((tag)->tag == requiredTag))
            {
                blockCanFormMultiBlock = true;
                break;
            }
        }
        if(!blockCanFormMultiBlock)
        {
            return;
        }

        for(int x = -1; x <=1; ++x)
            for(int y = -1; y <=1; ++y)
                for(int z = -1; z <=1; ++z)
                {
                    if(x == 0 && y == 0 && z == 0) continue;
                    //check for a wisp core tile entity
                    BlockPos blockPos = blockPlaced.getPos().offset(x, y, z);
                    WispCoreBlockEntity tileEntity = (WispCoreBlockEntity)blockPlaced.getWorld().getBlockEntity(blockPos);
                    if(tileEntity != null)
                    {
                        tileEntity.CheckMultiBlockForm();
                        return;
                    }
                }
    }


    @SubscribeEvent
    public static void OnWispCoreRingBreak(BlockEvent.BreakEvent blockBroken)
    {
        if(blockBroken.getState().getBlock() != LogisticsWoW.Blocks.WISP_CORE.get())
        {
            return;
        }
        WispCore.WispCoreType coreType = blockBroken.getState().getValue(WispCore.TYPE);
        if(coreType == WispCore.WispCoreType.CORE)
        {
            //the core is handled normally
            return;
        }

        for(int x = -1; x <=1; ++x)
            for(int y = -1; y <=1; ++y)
                for(int z = -1; z <=1; ++z)
                {
                    if(x == 0 && y == 0 && z == 0) continue;
                    //check for a wisp core tile entity
                    BlockPos blockPos = blockBroken.getPos().offset(x, y, z);
                    WispCoreBlockEntity tileEntity = (WispCoreBlockEntity)blockBroken.getWorld().getBlockEntity(blockPos);
                    if(tileEntity != null)
                    {
                        if(coreType == WispCore.WispCoreType.RING)
                        {
                            BlockState dropBlock = tileEntity.BreakMultiBlock(blockBroken.getPos());
                            Block.dropResources(dropBlock, (Level) blockBroken.getWorld(), blockBroken.getPos(), null, blockBroken.getPlayer(), blockBroken.getPlayer().getItemInHand(InteractionHand.MAIN_HAND));
                        }
                        else
                        {
                            //do nothing for the 'air' block
                            blockBroken.setCanceled(true);
                        }
                        return;
                    }
                }

    }
}
