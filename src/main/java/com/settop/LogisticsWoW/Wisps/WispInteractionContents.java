package com.settop.LogisticsWoW.Wisps;

import com.settop.LogisticsWoW.Items.WispEnhancementItem;
import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import com.settop.LogisticsWoW.Wisps.Enhancements.IEnhancement;

import java.util.ArrayList;

public class WispInteractionContents implements Container
{
    public interface OnEnhancementChanged
    {
        void OnEnhancementChange(int index, IEnhancement previousEnhancement, IEnhancement nextEnhancement);
    }

    private final ItemStackHandler wispContents;
    private OnEnhancementChanged listener;

    public WispInteractionContents(int size)
    {
        wispContents = new ItemStackHandler(size);
    }

    public void SetListener(OnEnhancementChanged changeListener)
    {
        listener = changeListener;
        if(listener != null)
        {
            for(int i = 0; i < wispContents.getSlots(); ++i)
            {
                ItemStack stack = wispContents.getStackInSlot(i);
                if(stack != null)
                {
                    LazyOptional<IEnhancement> enhancement = stack.getCapability(LogisticsWoW.Capabilities.CAPABILITY_ENHANCEMENT);
                    if(enhancement.isPresent())
                    {
                        listener.OnEnhancementChange(i, null, enhancement.resolve().get());
                    }
                }
            }
        }
    }

    @Override
    public int getContainerSize()
    {
        return wispContents.getSlots();
    }

    @Override
    public boolean isEmpty()
    {
        for (int i = 0; i < wispContents.getSlots(); ++i)
        {
            if (!wispContents.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index)
    {
        return wispContents.getStackInSlot(index);
    }

    @Override
    public ItemStack removeItem(int index, int count)
    {
        ItemStack retItem = wispContents.extractItem(index, count, false);
        UpdateEnhancement(index, retItem, wispContents.getStackInSlot(index));
        return retItem;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index)
    {
        //UpdateEnhancement(index, wispContents.getStackInSlot(index), null);
        int maxPossibleItemStackSize = wispContents.getSlotLimit(index);
        return wispContents.extractItem(index, maxPossibleItemStackSize, false);
    }

    @Override
    public void setItem(int index, ItemStack stack)
    {
        UpdateEnhancement(index, wispContents.getStackInSlot(index), stack);
        wispContents.setStackInSlot(index, stack);
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
        for (int i = 0; i < wispContents.getSlots(); ++i)
        {
            wispContents.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public ListTag write()
    {
        ListTag nbtTagList = new ListTag();
        for(int i = 0; i < wispContents.getSlots(); ++i)
        {
            if (!wispContents.getStackInSlot(i).isEmpty())
            {
                CompoundTag compoundnbt = new CompoundTag();
                compoundnbt.putByte("Slot", (byte)i);
                wispContents.getStackInSlot(i).save(compoundnbt);
                nbtTagList.add(compoundnbt);
            }
        }
        return nbtTagList;
    }

    public void read(CompoundTag nbt, String invName)
    {
        if(!nbt.contains(invName))
        {
            return;
        }
        ListTag listNBT = nbt.getList(invName, nbt.getId());
        for(int i = 0; i < listNBT.size(); ++i)
        {
            CompoundTag compoundnbt = listNBT.getCompound(i);
            int j = compoundnbt.getByte("Slot");
            ItemStack itemstack = ItemStack.of(compoundnbt);
            if (!itemstack.isEmpty())
            {
                wispContents.setStackInSlot(j, itemstack);
            }
            UpdateEnhancement(j, null, itemstack);
        }
    }

    private void UpdateEnhancement(int index, ItemStack previousStack, ItemStack newStack)
    {
        IEnhancement previousEnhancement = null;
        IEnhancement newEnhancement = null;

        if(previousStack != null  && previousStack.getItem() instanceof WispEnhancementItem)
        {
            previousEnhancement = previousStack.getCapability(LogisticsWoW.Capabilities.CAPABILITY_ENHANCEMENT).resolve().get();
        }
        if(newStack != null && newStack.getItem() instanceof WispEnhancementItem)
        {
            newEnhancement = newStack.getCapability(LogisticsWoW.Capabilities.CAPABILITY_ENHANCEMENT).resolve().get();
        }

        if(listener != null)
        {
            listener.OnEnhancementChange(index, previousEnhancement, newEnhancement);
        }
    }

    public boolean HasEnhancement(int slotIndex)
    {
        return slotIndex < wispContents.getSlots() && wispContents.getStackInSlot(slotIndex).getItem() instanceof WispEnhancementItem;
    }

    public IEnhancement GetEnhancement(int slotIndex)
    {
        if(!HasEnhancement(slotIndex))
        {
            return null;
        }

        ItemStack itemStack = wispContents.getStackInSlot(slotIndex);
        return itemStack.getCapability(LogisticsWoW.Capabilities.CAPABILITY_ENHANCEMENT).resolve().get();
    }
}
