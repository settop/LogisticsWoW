package com.settop.LogisticsWoW.Utils;


import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class FakeInventory implements Container
{
    public static final int MAX_STACK = 1 << 30;

    private final NonNullList<ItemStack> stacks;
    public final boolean includeCounts;

    public FakeInventory(int size, boolean includeCounts)
    {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
        this.includeCounts = includeCounts;
    }

    @Override
    public int getContainerSize()
    {
        return stacks.size();
    }

    @Override
    public boolean isEmpty()
    {
        for(ItemStack item : stacks)
        {
            if(!item.isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int index)
    {
        if(index < stacks.size())
        {
            return stacks.get(index);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int index, int count)
    {
        if(index < stacks.size())
        {
            ItemStack items = stacks.get(index);
            if(count < items.getCount())
            {
                items.setCount(items.getCount() - count);
                stacks.set(index, items);
            }
            else
            {
                stacks.set(index, ItemStack.EMPTY);
            }
            setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int index)
    {
        if(index < stacks.size())
        {
            stacks.set(index, ItemStack.EMPTY);
            setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, @NotNull ItemStack stack)
    {
        if(index < stacks.size())
        {
            stacks.set(index, stack.copy());
            if (!includeCounts)
            {
                stacks.get(index).setCount(1);
            }
            setChanged();
        }
    }

    @Override
    public void setChanged()
    {

    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return true;
    }

    @Override
    public void clearContent()
    {
        for(int i = 0; i < stacks.size(); ++i)
        {
            stacks.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    public CompoundTag Save(CompoundTag tag)
    {
        return ContainerHelper.saveAllItems(tag, stacks);
    }

    public void Load(CompoundTag tag)
    {
        ContainerHelper.loadAllItems(tag, stacks);
    }
}
