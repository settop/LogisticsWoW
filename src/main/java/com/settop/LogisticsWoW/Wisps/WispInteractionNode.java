package com.settop.LogisticsWoW.Wisps;

import com.settop.LogisticsWoW.GUI.BasicWispMenu;
import com.settop.LogisticsWoW.Items.WispEnhancementItem;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.Utils;
import com.settop.LogisticsWoW.WispNetwork.ReservableInventory;
import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import com.settop.LogisticsWoW.Wisps.Enhancements.IEnhancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Optional;

import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

public class WispInteractionNode extends WispInteractionNodeBase
{
    private final WispInteractionContents contents = new WispInteractionContents(2);
    private final ArrayList<IEnhancement> enhancements = new ArrayList<>();
    private BlockEntity blockEntity;
    private boolean inventoriesNeedCaching = true;
    private ReservableInventory generalReservableInventory;
    private ReservableInventory[] directionReservableInventory;


    public WispInteractionNode()
    {
        super();
    }

    public WispInteractionNode(ResourceLocation dim, BlockPos inPos)
    {
        super(dim, inPos);
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
        return WispConstants.WISP_INTERACTION_NODE;
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
        enhancements.clear();
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
                if(IsConnectedToANetwork())
                    newEnhancement.Setup(this, blockEntity);
            }
            else
            {
                LogisticsWoW.LOGGER.warn("Unrecognised wisp enhancement");
            }
        }
    }

    @Override
    public void ConnectToWispNetwork(WispNetwork wispNetwork)
    {
        super.ConnectToWispNetwork(wispNetwork);
        if(blockEntity != null)
        {
            for (IEnhancement enhancement : enhancements)
            {
                enhancement.Setup(this, blockEntity);
            }
        }
    }

    @Override
    public void DisconnectFromWispNetwork(WispNetwork wispNetwork)
    {
        super.DisconnectFromWispNetwork(wispNetwork);
        for (IEnhancement enhancement : enhancements)
        {
            enhancement.Setup(this, null);
        }
    }

    @Override
    public void SetConnectedBlockEntity(BlockEntity blockEntity)
    {
        this.blockEntity = blockEntity;

        UpdateInventories();

        if(IsConnectedToANetwork())
        {
            for (IEnhancement enhancement : enhancements)
            {
                enhancement.Setup(this, blockEntity);
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

    @Override
    public boolean IsActive()
    {
        return blockEntity != null;
    }

    @Override
    public ReservableInventory GetReservableInventory(Direction direction)
    {
        if(!IsActive())
        {
            throw new RuntimeException("GetReservableInventory cannot be called if not IsActive()");
        }
        if(inventoriesNeedCaching)
        {
            UpdateInventories();
        }
        if(direction == null)
        {
            return generalReservableInventory;
        }
        else
        {
            if(directionReservableInventory != null)
            {
                return directionReservableInventory[direction.get3DDataValue()];
            }
        }
        return null;
    }

    private void InvalidateInventories()
    {
        if(generalReservableInventory != null)
        {
            generalReservableInventory.SetInvalid();
        }
        if(directionReservableInventory != null)
        {
            for (ReservableInventory reservableInventory : directionReservableInventory)
            {
                if(reservableInventory != null)
                {
                    reservableInventory.SetInvalid();
                }
            }
        }
        generalReservableInventory = null;
        directionReservableInventory = null;
        inventoriesNeedCaching = true;
    }

    private void UpdateInventories()
    {
        InvalidateInventories();
        if(blockEntity == null)
        {
            return;
        }
        inventoriesNeedCaching = false;

        generalReservableInventory = null;
        directionReservableInventory = null;
        LazyOptional<IItemHandler> generalItemHandlerLazyOpt = blockEntity.getCapability(ITEM_HANDLER_CAPABILITY);
        if(!generalItemHandlerLazyOpt.isPresent())
        {
            //assuming that if the general item handler is not present then there are no directional ones either
            return;
        }

        Optional<IItemHandler> generalItemHandlerOptional = generalItemHandlerLazyOpt.resolve();
        if(generalItemHandlerOptional.isEmpty())
        {
            return;
        }

        IItemHandler generalItemHandler = generalItemHandlerOptional.get();
        generalItemHandlerLazyOpt.addListener((oldOpt)->InvalidateInventories());

        generalReservableInventory = new ReservableInventory(generalItemHandler);

        //this is not perfect since it's possible for different item handles to use the same slots
        //particularly the general itemhandler with the directional itemhandlers
        IItemHandler[] directionItemHandlers = new IItemHandler[6];
        directionReservableInventory = new ReservableInventory[6];
        for(int i = 0; i < 6; ++i)
        {
            LazyOptional<IItemHandler> directionalItemHandlerLazyOpt = blockEntity.getCapability(ITEM_HANDLER_CAPABILITY, Direction.from3DDataValue(i));
            directionItemHandlers[i] = directionalItemHandlerLazyOpt.resolve().orElseGet(()->null);
            if(directionItemHandlers[i] == null)
            {
                continue;
            }
            else if(directionItemHandlers[i] == generalItemHandler)
            {
                directionReservableInventory[i] = generalReservableInventory;
                continue;
            }
            for(int j = 0; j < i; ++j)
            {
                if(directionItemHandlers[i] == directionItemHandlers[j])
                {
                    directionReservableInventory[i] = directionReservableInventory[j];
                    break;
                }
            }

            if(directionReservableInventory[i] == null)
            {
                directionalItemHandlerLazyOpt.addListener((oldOpt)->InvalidateInventories());
                directionReservableInventory[i] = new ReservableInventory(directionItemHandlers[i]);
            }
        }
    }
}
