package com.settop.LogisticsWoW.Wisps.Enhancements;

import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IEnhancement
{
    public static int MAX_NUM_GUI_SLOTS = 33;
    public static int MAX_NUM_GUI_DATA = 2;
    public static int MAX_NUM_GUI_STRINGS = 1;

    interface IFactory
    {
        IEnhancement Create();
    }

    CompoundTag SerializeNBT();
    void DeserializeNBT(CompoundTag nbt);
    SubMenu CreateSubMenu(int xPos, int yPos, BlockState blockState, BlockEntity blockEntity, WispInteractionNodeBase parentWisp);
    String GetName();
    void AddTooltip(@NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn);
    void Setup(WispInteractionNodeBase parentWisp);
    void OnConnectToNetwork();
    void OnDisconnectFromNetwork();
    WispInteractionNodeBase GetAttachedInteractionNode();
}
