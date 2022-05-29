package com.settop.LogisticsWoW.Items;

import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import com.settop.LogisticsWoW.Wisps.Enhancements.EnhancementTypes;
import com.settop.LogisticsWoW.Wisps.Enhancements.IEnhancement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WispEnhancementItem extends Item
{
    public class CapabilityProviderEnhancement implements ICapabilitySerializable<CompoundTag>
    {
        public CapabilityProviderEnhancement()
        {
            enhancement = type.GetFactory().Create();
        }

        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing)
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

        private IEnhancement enhancement;
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
}

