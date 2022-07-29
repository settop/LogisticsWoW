package com.settop.LogisticsWoW.WispNetwork;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ItemResource implements StorableResource<ItemStack>
{
    public final ItemStack stack;
    public final boolean ignoreNBT;

    public ItemResource(@NonNull ItemStack stack)
    {
        this.stack = stack.copy();
        this.ignoreNBT = false;
    }

    public ItemResource(@NonNull StoreableResourceMatcher<ItemStack> otherMatcher, int newCount)
    {
        if(!(otherMatcher instanceof ItemResource))
        {
            throw new RuntimeException("Unable to construct ItemResource from a non ItemResource resource matcher");
        }
        ItemResource other = (ItemResource)otherMatcher;
        this.stack = other.stack.copy();
        this.stack.setCount(newCount);
        this.ignoreNBT = other.ignoreNBT;
    }

    public ItemResource(@NonNull Item item, int count, boolean ignoreNBT)
    {
        this.stack = new ItemStack(item, count);
        this.ignoreNBT = ignoreNBT;
    }
    public ItemResource(CompoundTag tag)
    {
        this.stack = ItemStack.of(tag.getCompound("Stack"));
        this.ignoreNBT = tag.getBoolean("IgnoreNBT");
    }

    @Override
    public @NonNull ItemStack GetStack()
    {
        return stack;
    }

    @Override
    public int GetAmount()
    {
        return stack.getCount();
    }

    @Override
    public @NonNull Object GetType()
    {
        return stack.getItem();
    }

    @Override
    public boolean Matches(StoreableResourceMatcher<ItemStack> other)
    {
        if(other instanceof ItemResource)
        {
            ItemResource otherItemResource = (ItemResource) other;
            if(ignoreNBT || otherItemResource.ignoreNBT)
            {
                return stack.is((Item)other.GetType());
            }
            else
            {
                return ItemStack.isSameItemSameTags(stack, otherItemResource.GetStack());
            }
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean Matches(ItemStack other)
    {
        if(ignoreNBT)
        {
            return stack.is(other.getItem());
        }
        else
        {
            return ItemStack.isSameItemSameTags(stack, other);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
        {
            return true;
        }
        else if(obj instanceof ItemResource)
        {
            ItemResource other = (ItemResource)obj;
            if(ignoreNBT != other.ignoreNBT)
            {
                return false;
            }
            else if(ignoreNBT)
            {
                return stack.sameItem(other.stack);
            }
            else
            {
                return ItemStack.isSameItemSameTags(stack, other.stack);
            }
        }
        else
        {
            return false;
        }
    }

    public CompoundTag Serialize()
    {
        CompoundTag nbt = new CompoundTag();
        nbt.put("Stack", stack.serializeNBT());
        nbt.putBoolean("IgnoreNBT", ignoreNBT);
        return nbt;
    }
}
