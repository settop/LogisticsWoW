package com.settop.LogisticsWoW.WispNetwork;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FluidResource implements StorableResource<FluidStack>
{
    private final FluidStack stack;

    public FluidResource(@NonNull FluidStack stack)
    {
        this.stack = stack;
    }

    public FluidResource(CompoundTag nbt)
    {
        stack = FluidStack.loadFluidStackFromNBT(nbt.getCompound("Stack"));
    }

    @Override
    public @NonNull FluidStack GetStack()
    {
        return stack;
    }

    @Override
    public int GetAmount()
    {
        return stack.getAmount();
    }

    @Override
    public @NonNull Object GetType()
    {
        return stack.getFluid();
    }

    @Override
    public boolean Matches(StoreableResourceMatcher<FluidStack> other)
    {
        if(other instanceof FluidResource)
        {
            FluidResource otherFluidResource = (FluidResource)other;
            return stack.isFluidEqual(otherFluidResource.GetStack());
        }
        return false;
    }

    @Override
    public boolean Matches(FluidStack other)
    {
        return stack.isFluidEqual(other);
    }

    public CompoundTag Serialize()
    {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("type", "fluid");
        nbt.put("Stack", stack.writeToNBT(new CompoundTag()));
        return nbt;
    }
}
