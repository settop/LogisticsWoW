package com.settop.LogisticsWoW.Items;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import com.settop.LogisticsWoW.Wisps.GlobalWispData;
import com.settop.LogisticsWoW.Wisps.WispBase;
import com.settop.LogisticsWoW.Wisps.WispConstants;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

import static net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

public class BasicWispItem extends Item
{
    public BasicWispItem()
    {
        super(new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_MISC));
    }

    // adds 'tooltip' text
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn)
    {
        tooltip.add(new TextComponent("Sneak right click on a block to insert wisp"));
        tooltip.add(new TextComponent("Can be enchanted"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level world = context.getLevel();
        if(!context.getPlayer().isCrouching())
        {
            return InteractionResult.PASS;
        }
        BlockEntity tileEntity = world.getBlockEntity( context.getClickedPos() );
        if(tileEntity == null)
        {
            return InteractionResult.PASS;
        }
        LazyOptional<IItemHandler> itemHandler = tileEntity.getCapability(ITEM_HANDLER_CAPABILITY);
        LazyOptional<IFluidHandler> fluidHandler = tileEntity.getCapability(FLUID_HANDLER_CAPABILITY);
        if(itemHandler.isPresent() || fluidHandler.isPresent())
        {
            if(!world.isClientSide())
            {
                ItemStack wispItemStack = context.getItemInHand();
                //server side only work
                Tuple<WispBase, Boolean> blocksWisp = GlobalWispData.CreateOrClaimWisp(WispConstants.BASIC_WISP, world, context.getClickedPos(), wispItemStack.getTag());
                if(blocksWisp.getB())
                {
                    //we just added it, so remove one from the stack
                    wispItemStack.shrink(1);
                }
                Player player = context.getPlayer();
                if (!(player instanceof ServerPlayer))
                    return InteractionResult.FAIL;  // should always be true, but just in case...
                ServerPlayer serverPlayerEntity = (ServerPlayer) player;
                NetworkHooks.openGui(serverPlayerEntity, blocksWisp.getA(),  (packetBuffer) -> blocksWisp.getA().ContainerExtraDataWriter(packetBuffer));
            }
            return InteractionResult.CONSUME;
        }
        else
        {
            return InteractionResult.PASS;
        }
    }
}
