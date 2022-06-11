package com.settop.LogisticsWoW.Wisps.Enhancements;

import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import com.settop.LogisticsWoW.Wisps.WispBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IEnhancement
{
    interface IFactory
    {
        IEnhancement Create();
        SubMenu CreateSubMenu(int xPos, int yPos, BlockState blockState, BlockEntity blockEntity);
    }

    CompoundTag SerializeNBT();
    void DeserializeNBT(CompoundTag nbt);
    EnhancementTypes GetType();
    void AddTooltip(@NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn);
    void Setup(WispBase parentWisp, BlockEntity blockEntity);
}
