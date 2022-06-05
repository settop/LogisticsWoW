package com.settop.LogisticsWoW.Wisps;

import com.settop.LogisticsWoW.GUI.BasicWispMenu;
import com.settop.LogisticsWoW.GUI.MultiScreenMenu;
import com.settop.LogisticsWoW.Items.WispEnhancementItem;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.Utils;
import com.settop.LogisticsWoW.Wisps.Enhancements.IEnhancement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.extensions.IForgeLevel;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class BasicWisp extends WispBase
{
    private final BasicWispContents contents = new BasicWispContents(2);
    private ArrayList<IEnhancement> enhancements;


    public BasicWisp()
    {
        super();
    }

    public BasicWisp(BlockPos inPos)
    {
        super(inPos);
    }

    @Override
    public CompoundTag Save()
    {
        CompoundTag nbt = super.Save();

        if(!contents.isEmpty())
        {
            nbt.put("inv", contents.write());
        }

        return nbt;
    }

    @Override
    public void Load(ResourceLocation inDim, CompoundTag nbt)
    {
        super.Load(inDim, nbt);
        contents.read(nbt, "inv");
        UpdateFromContents();
    }

    @Override
    public String GetType()
    {
        return WispConstants.BASIC_WISP;
    }

    @Override
    public void DropItemStackIntoWorld(LevelAccessor world)
    {
        ItemStack droppedStack = new ItemStack(LogisticsWoW.Items.WISP_ITEM.get(), 1);
        if(!contents.isEmpty())
        {
            CompoundTag itemTags = new CompoundTag();
            itemTags.put("inv", contents.write());
            droppedStack.setTag(itemTags);
        }
        Utils.SpawnAsEntity((Level)world, GetPos(), droppedStack );
    }

    @Override
    public void InitFromTagData(CompoundTag tagData)
    {
        if(tagData == null)
        {
            return;
        }
        contents.read(tagData, "inv");
        UpdateFromContents();
    }

    @Override
    public void UpdateFromContents()
    {
        enhancements = new ArrayList<>();
        for(int i = 0; i < contents.getContainerSize(); ++i)
        {
            ItemStack contentsItem = contents.getItem(i);
            if(contentsItem == null || contentsItem.isEmpty())
            {
                continue;
            }

            if(contentsItem.getItem() instanceof WispEnhancementItem)
            {
                IEnhancement newEnhancement = contentsItem.getCapability(LogisticsWoW.Capabilities.CAPABILITY_ENHANCEMENT).resolve().get();
                enhancements.add(newEnhancement);
            }
            else
            {
                LogisticsWoW.LOGGER.warn("Unrecognised wisp enhancement");
            }
        }
    }

    // From MenuProvider
    @Override
    public @NotNull Component getDisplayName()
    {
        return new TranslatableComponent("container.logwow.basic_wisp_menu");
    }


    @Override
    public AbstractContainerMenu createMenu(int windowID, @NotNull Inventory playerInventory, @NotNull Player player)
    {
        return BasicWispMenu.CreateMenu(windowID, playerInventory, player, contents, this);
    }

    @Override
    public void ContainerExtraDataWriter(FriendlyByteBuf packetBuffer)
    {
        packetBuffer.writeInt(contents.getContainerSize());
        packetBuffer.writeBlockPos( GetPos() );
    }
}
