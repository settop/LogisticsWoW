package com.settop.LogisticsWoW.Wisps.Enhancements;

import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
}
