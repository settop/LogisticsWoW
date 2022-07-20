package com.settop.LogisticsWoW.Items;

import com.settop.LogisticsWoW.GUI.CommandStaffMenu;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Wisps.GlobalWispData;
import com.settop.LogisticsWoW.Wisps.WispConstants;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Tuple;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

public class WispCommandStaff extends Item
{
    public static boolean CanPlaceInStaffInv(ItemStack stack)
    {
        return stack.getItem() instanceof WispEnhancementItem;
    }

    public static class StaffContainer extends SimpleContainer
    {
        public StaffContainer(int size)
        {
            super(size);
        }

        @Override
        public boolean canPlaceItem(int slot, @NotNull ItemStack stack)
        {
            return CanPlaceInStaffInv(stack);
        }
    }

    private static class StaffData extends InvWrapper implements ICapabilitySerializable<CompoundTag>, MenuProvider
    {
        public final StaffContainer container;

        public StaffData(StaffContainer container)
        {
            super(container);
            this.container = container;
        }

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
        {
            return ITEM_HANDLER_CAPABILITY.orEmpty(cap, LazyOptional.of(()-> this));
        }

        @Override
        public CompoundTag serializeNBT()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.put("inv", container.createTag());

            return nbt;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt)
        {
            container.fromTag(nbt.getList("inv", nbt.getId()));
        }

        @Override
        public @NotNull Component getDisplayName()
        {
            return new TranslatableComponent("container.logwow.command_staff");
        }

        @org.jetbrains.annotations.Nullable
        @Override
        public AbstractContainerMenu createMenu(int windowID, @NotNull Inventory playerInv, @NotNull Player player)
        {
            final int rows = 1;
            return CommandStaffMenu.CreateMenu(windowID, playerInv, container, rows);
        }
    }

    public final int rank;
    public final int numInvRows = 1;

    public WispCommandStaff(int rank)
    {
        super(new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_MISC));
        this.rank = rank;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt)
    {
        return new StaffData(new StaffContainer(numInvRows * 9));
    }

    // adds 'tooltip' text
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level worldIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn)
    {
        LazyOptional<IItemHandler> itemHandlerOpt = stack.getCapability(ITEM_HANDLER_CAPABILITY);
        itemHandlerOpt.ifPresent((itemHandler)->
        {
            HashMap<Item, Integer> itemCounts = new HashMap<>();
            for(int i = 0; i < itemHandler.getSlots(); ++i)
            {
                ItemStack slotStack = itemHandler.getStackInSlot(i);
                if(slotStack.isEmpty())
                {
                    continue;
                }
                Integer itemCount = itemCounts.getOrDefault(slotStack.getItem(), 0);
                itemCount = itemCount + slotStack.getCount();
                itemCounts.put(slotStack.getItem(), itemCount);
            }

            for(Map.Entry<Item, Integer> storedItem : itemCounts.entrySet())
            {
                Item item = storedItem.getKey();
                tooltip.add(item.getName(new ItemStack(item)).copy().append(" : %d".formatted(storedItem.getValue())));
            }
        });
    }

    @Override
    public boolean hasContainerItem(ItemStack stack)
    {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack itemStack)
    {
        return itemStack;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context)
    {
        Level world = context.getLevel();
        if(context.getPlayer() == null || !context.getPlayer().isCrouching())
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
                //server side only work
                Tuple<WispInteractionNodeBase, Boolean> blocksWisp = GlobalWispData.CreateOrGetWisp(WispConstants.WISP_INTERACTION_NODE, world, context.getClickedPos(), null);
                blocksWisp.getA().EnsureMinRank(rank);

                Player player = context.getPlayer();
                if (!(player instanceof ServerPlayer))
                    return InteractionResult.FAIL;  // should always be true, but just in case...
                ServerPlayer serverPlayerEntity = (ServerPlayer) player;
                WispInteractionNodeBase.MenuBuilder menuBuilder = blocksWisp.getA().GetMenuBuilder(serverPlayerEntity, context.getHand(), context.getItemInHand());
                if(menuBuilder != null)
                {
                    NetworkHooks.openGui(serverPlayerEntity, menuBuilder, menuBuilder::ContainerExtraDataWriter);
                }
            }
            return InteractionResult.CONSUME;
        }
        else
        {
            return InteractionResult.PASS;
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand)
    {
        ItemStack heldItem = player.getItemInHand(hand);
        return heldItem.getCapability(ITEM_HANDLER_CAPABILITY).map((itemHandler)->
        {
            //open inv menu
            if(itemHandler instanceof StaffData)
            {
                if(!level.isClientSide())
                {
                    NetworkHooks.openGui((ServerPlayer) player, (StaffData)itemHandler, (packetBuffer) ->
                    {
                        packetBuffer.writeInt(numInvRows);//row count
                    });
                }
                return InteractionResultHolder.success(heldItem);
            }
            else
            {
                return InteractionResultHolder.fail(heldItem);
            }
        }).orElseGet(()->super.use(level, player, hand));
    }
}
