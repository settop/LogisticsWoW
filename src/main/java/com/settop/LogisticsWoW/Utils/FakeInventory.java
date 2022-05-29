package com.settop.LogisticsWoW.Utils;


import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class FakeInventory implements Container
{
    public static final int MAX_STACK = 1 << 30;

    private NonNullList<ItemStack> stacks;
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
    public ItemStack getItem(int index)
    {
        if(index < stacks.size())
        {
            return stacks.get(index);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count)
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
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index)
    {
        if(index < stacks.size())
        {
            stacks.set(index, ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack)
    {
        if(index < stacks.size())
        {
            stacks.set(index, stack.copy());
            if (!includeCounts)
            {
                stacks.get(index).setCount(1);
            }
        }
    }

    @Override
    public void setChanged()
    {

    }

    @Override
    public boolean stillValid(Player player)
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
