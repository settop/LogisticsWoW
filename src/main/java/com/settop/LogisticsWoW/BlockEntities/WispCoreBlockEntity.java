package com.settop.LogisticsWoW.BlockEntities;

import com.settop.LogisticsWoW.Blocks.WispCore;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import com.settop.LogisticsWoW.Wisps.GlobalWispData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;

public class WispCoreBlockEntity extends BlockEntity
{
    /*
    static public final ResourceLocation[] RING_BLOCK_TAGS =
            {
                    new ResourceLocation("forge", "storage_blocks/gold"),
                    new ResourceLocation("forge", "storage_blocks/quartz")
            };
     */
    //filled in in LogisticsWoW::setup
    static public ArrayList<TagKey<Block>> RING_BLOCK_TAGS;

    public float renderTimer = 0.f;

    private boolean multiBlockComplete = false;
    private boolean cachedMultiblockComplete = false;
    private BlockState[][][] ringOriginalBlocks;
    private WispNetwork network;

    private boolean needToLoadNetwork = false;

    public WispCoreBlockEntity(BlockPos pos, BlockState state)
    {
        super( LogisticsWoW.BlockEntities.WISP_CORE_TILE_ENTITY.get(), pos, state );
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        super.handleUpdateTag(tag);
        cachedMultiblockComplete = false;
    }

    public boolean IsMultiblockComplete()
    {
        if(!cachedMultiblockComplete)
        {
            cachedMultiblockComplete = true;
            multiBlockComplete = getBlockState().getValue(WispCore.TYPE) == WispCore.WispCoreType.CORE_COMPLETE;
        }
        return multiBlockComplete;
    }

    private static final int[][][] LAYOUT =
    {
            {
                    { -1,  0, -1 },
                    {  0,  1,  0 },
                    { -1,  0, -1 }
            },
            {
                    {  0,  1,  0 },
                    {  1, -1,  1 },
                    {  0,  1,  0 }
            },
            {
                    { -1,  0, -1 },
                    {  0,  2,  0 },
                    { -1,  0, -1 }
            }
    };

    public void CheckMultiBlockForm()
    {
        BlockPos myBlockPos = getBlockPos();

        if(IsMultiblockComplete())
        {
            //check the corners are still empty
            for (int y = -1; y <= 1; y+=2)
                for (int x = -1; x <= 1; x+=2)
                    for (int z = -1; z <= 1; z+=2)
                    {
                        BlockPos testPos = myBlockPos.offset(x, y, z);
                        BlockState blockState = level.getBlockState(testPos);
                        if (!blockState.isAir())
                        {
                            BreakMultiBlock(testPos);
                            return;
                        }
                    }

        }
        else
        {

            for (int y = -1; y <= 1; ++y)
                for (int x = -1; x <= 1; ++x)
                    for (int z = -1; z <= 1; ++z)
                    {
                        if (x == 0 && y == 0 && z == 0) continue;

                        BlockPos testPos = myBlockPos.offset(x, y, z);
                        BlockState blockState = level.getBlockState(testPos);

                        int blockType = LAYOUT[y + 1][x + 1][z + 1];
                        if (blockType == -1)
                        {
                            //needs to be air
                            if (!blockState.isAir())
                            {
                                return;
                            }
                        }
                        else
                        {
                            //needs to be the right block
                            if (blockState.getTags().noneMatch((tag) -> tag == RING_BLOCK_TAGS.get(blockType)))
                            {
                                return;
                            }
                        }
                    }

            //we passed all the checks to become a multiblock
            FormMultiBlock();
        }
    }

    private void FormMultiBlock()
    {
        BlockPos myBlockPos = getBlockPos();
        WispCore coreBlock = (WispCore)LogisticsWoW.Blocks.WISP_CORE.get();

        ringOriginalBlocks = new BlockState[3][3][3];

        level.setBlockAndUpdate(myBlockPos, coreBlock.getBlockState(WispCore.WispCoreType.CORE_COMPLETE));

        for(int y = -1; y <=1; ++y)
            for(int x = -1; x <=1; ++x)
                for(int z = -1; z <=1; ++z)
                {
                    if (x == 0 && y == 0 && z == 0) continue;

                    BlockPos setPos = myBlockPos.offset(x, y, z);
                    ringOriginalBlocks[y+1][x+1][z+1] = level.getBlockState(setPos);

                    int blockType = LAYOUT[y+1][x+1][z+1];
                    if (blockType == -1)
                    {
                        level.setBlockAndUpdate(setPos, Blocks.AIR.defaultBlockState());
                    }
                    else
                    {
                        level.setBlockAndUpdate(setPos, coreBlock.getBlockState(WispCore.WispCoreType.RING));
                    }
                }

        if(!level.isClientSide)
        {
            network = GlobalWispData.CreateOrClaimWispNetwork(level, getBlockPos());
            GlobalWispData.TryAndConnectNetworkToNodes(level, network);
        }
        setChanged();
    }

    public BlockState BreakMultiBlock(BlockPos brokenPos)
    {
        BlockPos myBlockPos = getBlockPos();
        WispCore coreBlock = (WispCore)LogisticsWoW.Blocks.WISP_CORE.get();
        BlockPos offset = brokenPos.subtract(myBlockPos);

        level.setBlockAndUpdate(myBlockPos, coreBlock.getBlockState(WispCore.WispCoreType.CORE));

        BlockState returnBlockState = null;

        for(int y = -1; y <=1; ++y)
            for(int x = -1; x <=1; ++x)
                for(int z = -1; z <=1; ++z)
                {
                    if (x == 0 && y == 0 && z == 0) continue;

                    if(offset.getX() == x && offset.getY() == y && offset.getZ() == z)
                    {
                        //this is the block being broken
                        returnBlockState = ringOriginalBlocks[y+1][x+1][z+1];
                    }
                    else
                    {
                        BlockPos setPos = getBlockPos().offset(x, y, z);
                        level.setBlockAndUpdate(setPos, ringOriginalBlocks[y+1][x+1][z+1]);
                    }

                }

        if(!level.isClientSide)
        {
            GlobalWispData.RemoveWispNetwork(level, network);
            network = null;
        }
        ringOriginalBlocks = null;
        setChanged();
        return returnBlockState;
    }

    @Override
    public void setChanged()
    {
        super.setChanged();
        cachedMultiblockComplete = false;
    }

    @Override
    public void setBlockState(BlockState blockState)
    {
        super.setBlockState(blockState);
        cachedMultiblockComplete = false;
    }

    @Override
    public void saveAdditional(CompoundTag compound)
    {
        super.saveAdditional(compound);
        if(ringOriginalBlocks != null)
        {
            CompoundTag originalBlockNBT = new CompoundTag();

            for(int y = -1; y <=1; ++y)
            {
                CompoundTag yNBT = new CompoundTag();
                for (int x = -1; x <= 1; ++x)
                {
                    CompoundTag xNBT = new CompoundTag();
                    for (int z = -1; z <= 1; ++z)
                    {
                        BlockState blockState = ringOriginalBlocks[y+1][x+1][z+1];
                        if(blockState != null)
                        {
                            xNBT.put(String.valueOf(z), NbtUtils.writeBlockState(blockState));
                        }
                    }
                    yNBT.put(String.valueOf(x), xNBT);
                }
                originalBlockNBT.put( String.valueOf(y), yNBT );
            }
            compound.put("RingOriginalBlocks", originalBlockNBT);
        }

        compound.putBoolean("HasNetwork", network != null);
    }

    @Override
    public void load(CompoundTag nbt)
    {
        super.load(nbt);

        if(nbt.contains("RingOriginalBlocks"))
        {
            CompoundTag originalBlockNBT = nbt.getCompound("RingOriginalBlocks");
            ringOriginalBlocks = new BlockState[3][3][3];
            for(int y = -1; y <=1; ++y)
            {
                CompoundTag yNBT = originalBlockNBT.getCompound( String.valueOf(y) );
                for (int x = -1; x <= 1; ++x)
                {
                    CompoundTag xNBT = yNBT.getCompound( String.valueOf(x) );
                    for (int z = -1; z <= 1; ++z)
                    {
                        ringOriginalBlocks[y+1][x+1][z+1] = NbtUtils.readBlockState(xNBT.getCompound(String.valueOf(z)));
                    }
                }
            }
        }
        else
        {
            ringOriginalBlocks = null;
        }

        needToLoadNetwork = nbt.getBoolean("HasNetwork");
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        if(!level.isClientSide && needToLoadNetwork)
        {
            needToLoadNetwork = false;
            network = GlobalWispData.CreateOrClaimWispNetwork(level, getBlockPos());
        }
    }


    @OnlyIn(Dist.CLIENT)
    @Override
    public AABB getRenderBoundingBox()
    {
        return new AABB(getBlockPos().offset(-1, -1, -1), getBlockPos().offset(2, 2, 2));
    }
}
