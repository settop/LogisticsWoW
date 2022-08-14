package com.settop.LogisticsWoW.Wisps.Enhancements;


import com.settop.LogisticsWoW.GUI.SubMenus.ItemExtractionSubMenu;
import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import com.settop.LogisticsWoW.Utils.ItemFilter;
import com.settop.LogisticsWoW.WispNetwork.*;
import com.settop.LogisticsWoW.WispNetwork.Tasks.ItemTransferTask;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ItemExtractionEnhancement extends ExtractionEnhancement
{
    public static class Factory implements IEnhancement.IFactory
    {
        @Override
        public IEnhancement Create()
        {
            return new ItemExtractionEnhancement();
        }
    }

    public static final Factory FACTORY = new Factory();

    //data player can tweak
    private final ItemFilter filter = new ItemFilter();

    @Override
    protected eExtractionState TickExtraction(int currentTick)
    {
        //first find an item we are not extracting that we want to extract
        eExtractionState fallbackState = eExtractionState.ASLEEP;
        ReservableInventory inv = GetAttachedInteractionNode().GetReservableInventory(GetInvAccessDirection());
        inv.RefreshCacheThisTick(currentTick);
        for(int i = 0; i < inv.GetSimulatedInv().getSlots(); ++i)
        {
            ItemStack slotStack = inv.GetSimulatedInv().extractItem(i, Integer.MAX_VALUE, true);
            if(slotStack.isEmpty())
            {
                continue;
            }
            if(!filter.MatchesFilter(slotStack))
            {
                continue;
            }

            fallbackState = eExtractionState.NO_DESTINATION;

            CarryWisp reservedCarryWisp = GetAttachedInteractionNode().connectedNetwork.TryReserveCarryWisp(GetAttachedInteractionNode(), slotStack.getCount());
            if(reservedCarryWisp == null)
            {
                continue;
            }
            ItemStack carryStack = slotStack.copy();
            carryStack.setCount(Math.min(slotStack.getCount(), reservedCarryWisp.GetCarryCapacity()));
            ItemResource testStack = new ItemResource(carryStack);
            SourcedReservation insertReservation = GetAttachedInteractionNode().connectedNetwork.GetItemManagement().ReserveSpaceInBestSink(testStack, Integer.MIN_VALUE, Integer.MIN_VALUE, true );

            if(insertReservation == null)
            {
                reservedCarryWisp.ReleaseReservation();
                continue;
            }
            Reservation selfExtractReservation = inv.ReserveExtraction(testStack, insertReservation.reservation.reservedCount);
            if(selfExtractReservation == null)
            {
                insertReservation.reservation.SetInvalid();
                reservedCarryWisp.ReleaseReservation();
                continue;
            }
            reservedCarryWisp.Claim();

            GetAttachedInteractionNode().connectedNetwork.StartTask(new ItemTransferTask
                    (
                            testStack,
                            reservedCarryWisp,
                            new SourcedReservation(selfExtractReservation, GetAttachedInteractionNode(), GetInvAccessDirection()),
                            insertReservation
                    ));
            return eExtractionState.ACTIVE;
        }
        return fallbackState;
    }

    @Override
    public SubMenu CreateSubMenu(int xPos, int yPos, BlockState blockState, BlockEntity blockEntity, WispInteractionNodeBase parentWisp)
    {
        return new ItemExtractionSubMenu(this, xPos, yPos, blockState, blockEntity, parentWisp);
    }

    @Override
    public String GetName()
    {
        return "item.logwow.wisp_item_extraction_enhancement";
    }

    @Override
    public void AddTooltip(@NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn)
    {
        filter.AddTooltip(tooltip, flagIn);
    }

    @Override
    public CompoundTag SerializeNBT()
    {
        CompoundTag nbt = super.SerializeNBT();

        nbt.put("filter", filter.SerializeNBT());

        return nbt;
    }

    @Override
    public void DeserializeNBT(CompoundTag nbt)
    {
        if(nbt == null)
        {
            return;
        }
        super.DeserializeNBT(nbt);
        filter.DeserializeNBT(nbt.getCompound("filter"));
    }

    public ItemFilter GetFilter()
    {
        return filter;
    }
}
