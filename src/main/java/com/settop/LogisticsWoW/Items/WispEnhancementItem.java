package com.settop.LogisticsWoW.Items;

import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import com.settop.LogisticsWoW.Wisps.Enhancements.EnhancementTypes;
import com.settop.LogisticsWoW.Wisps.Enhancements.IEnhancement;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class WispEnhancementItem extends Item
{
    public class CapabilityProviderEnhancement implements ICapabilitySerializable<CompoundTag>
    {
        public CapabilityProviderEnhancement()
        {
            enhancement = type.GetFactory().Create();
        }

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing)
        {
            return LogisticsWoW.Capabilities.CAPABILITY_ENHANCEMENT.orEmpty(capability, LazyOptional.of(()->enhancement));
        }

        @Override
        public CompoundTag serializeNBT()
        {
            return enhancement.SerializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt)
        {
            enhancement.DeserializeNBT(nbt);
        }

        private final IEnhancement enhancement;
    }

    private final EnhancementTypes type;

    public WispEnhancementItem(EnhancementTypes enhancementType)
    {
        super(new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_MISC));
        type = enhancementType;
    }

    public EnhancementTypes GetType() { return type; }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt)
    {
        return new CapabilityProviderEnhancement();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level worldIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn)
    {
        boolean sneakPressed = Screen.hasShiftDown();
        if(sneakPressed)
        {
            stack.getCapability(LogisticsWoW.Capabilities.CAPABILITY_ENHANCEMENT).ifPresent(enhancement->enhancement.AddTooltip(tooltip, flagIn));
        }
    }
}

