package com.settop.LogisticsWoW.WispNetwork;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.HashMap;

public class SimulatedInventory extends InvWrapper
{
    private static class SimulatedContainer implements Container
    {
        private final IItemHandler wrappedItemHandler;
        private final HashMap<Integer, ItemStack> modifiedSlots = new HashMap<>();
        public SimulatedContainer(IItemHandler wrappedItemHandler)
        {
            this.wrappedItemHandler = wrappedItemHandler;
        }

        public void Reset()
        {
            modifiedSlots.clear();
        }

        @Override
        public int getContainerSize()
        {
            return wrappedItemHandler.getSlots();
        }


        @Override
        public boolean isEmpty()
        {
            for (int i = 0; i < wrappedItemHandler.getSlots(); i++)
            {
                ItemStack modifiedStack = modifiedSlots.get(i);
                if(modifiedStack != null && !modifiedStack.isEmpty())
                {
                    return false;
                }
                else if(!wrappedItemHandler.getStackInSlot(i).isEmpty())
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        public @NotNull ItemStack getItem(int slot)
        {
            ItemStack modifiedSlotStack = modifiedSlots.get(slot);
            if(modifiedSlotStack != null)
            {
                return modifiedSlotStack;
            }
            else
            {
                return wrappedItemHandler.getStackInSlot(slot);
            }
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int count)
        {
            if(count == 0)
            {
                return ItemStack.EMPTY;
            }
            ItemStack wrappedStack = wrappedItemHandler.getStackInSlot(slot);
            ItemStack modifiedSlotStack = modifiedSlots.get(slot);

            if(modifiedSlotStack == null)
            {
                if(wrappedStack.isEmpty())
                {
                    return wrappedStack;
                }
                modifiedSlotStack = wrappedStack.copy();
                modifiedSlots.put(slot, modifiedSlotStack);
            }
            assert wrappedStack.sameItem(modifiedSlotStack);

            int removedCount = Math.min(count, modifiedSlotStack.getCount());

            ItemStack retStack = modifiedSlotStack.copy();
            modifiedSlotStack.setCount(modifiedSlotStack.getCount() - removedCount);
            retStack.setCount(removedCount);
            return retStack;
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot)
        {
            return removeItem(slot, Integer.MAX_VALUE);
        }

        @Override
        public void setItem(int slot, @NotNull ItemStack stack)
        {
            modifiedSlots.put(slot, stack);
        }

        @Override
        public void setChanged()
        {

        }

        @Override
        public boolean stillValid(@NotNull Player ignored)
        {
            return true;
        }

        @Override
        public void clearContent()
        {

        }
    }

    public SimulatedInventory(IItemHandler wrappedItemHandler)
    {
        super(new SimulatedContainer(wrappedItemHandler));
    }

    public void Reset()
    {
        SimulatedContainer container = (SimulatedContainer)getInv();
        container.Reset();
    }

    //turn off the simulation insertion here
    //since we are going to cache the inserts/extracts
    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate)
    {
        return super.insertItem(slot, stack, false);
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return super.extractItem(slot, amount, false);
    }

    @Override
    public int getSlotLimit(int slot)
    {
        SimulatedContainer container = (SimulatedContainer)getInv();
        return container.wrappedItemHandler.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack)
    {
        SimulatedContainer container = (SimulatedContainer)getInv();
        return container.wrappedItemHandler.isItemValid(slot, stack);
    }
}
