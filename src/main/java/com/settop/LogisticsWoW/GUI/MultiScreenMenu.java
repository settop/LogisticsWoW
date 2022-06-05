package com.settop.LogisticsWoW.GUI;

import com.google.common.collect.Lists;
import com.settop.LogisticsWoW.GUI.Network.Packets.SWindowStringPropertyPacket;
import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.StringReferenceHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MultiScreenMenu extends AbstractContainerMenu
{
    private final List<SubMenu> subMenus = new ArrayList<>();
    private final List<StringReferenceHolder> trackedStringReferences = Lists.newArrayList();
    private final ServerPlayer player;

    protected MultiScreenMenu(@Nullable MenuType<?> type, int id, Player player)
    {
        super(type, id);
        this.player = (ServerPlayer)player;
    }

    protected void SetSubContainers(List<SubMenu> subContainers)
    {
        for(SubMenu subContainer : subContainers)
        {
            AddSubContainer(subContainer);
        }
    }

    private void AddSubContainer(SubMenu container)
    {
        container.SetParent( new WeakReference<>(this), subMenus.size() );

        for(Slot subSlot : container.inventorySlots)
        {
            addSlot(subSlot);
        }
        for(DataSlot dataSlot : container.trackedIntData)
        {
            addDataSlot(dataSlot);
        }
        for(StringReferenceHolder strRef : container.trackedStringData)
        {
            trackString(strRef);
        }

        subMenus.add(container);
    }

    protected StringReferenceHolder trackString(StringReferenceHolder strIn)
    {
        this.trackedStringReferences.add(strIn);
        return strIn;
    }

    public void updateTrackedString(int id, String value)
    {
        if(id >= 0 && id < trackedStringReferences.size())
        {
            trackedStringReferences.get(id).set(value);
        }
        else
        {
            LogisticsWoW.LOGGER.warn("Invalid id for tracked string update");
        }
    }

    @Override
    public boolean stillValid(@NotNull Player playerIn)
    {
        return player == null || player == playerIn;
    }

    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();

        for(int i = 0; i < trackedStringReferences.size(); ++i)
        {
            StringReferenceHolder strRef = trackedStringReferences.get(i);
            if(strRef.isDirty() && player != null)
            {
                strRef.clearDirty();
                LogisticsWoW.MULTI_SCREEN_CHANNEL.send(PacketDistributor.PLAYER.with(()->player), new SWindowStringPropertyPacket( containerId, i, strRef.get() ));
            }
        }
    }

    @Override
    public void removed(@NotNull Player playerIn)
    {
        for(SubMenu subContainer : subMenus)
        {
            subContainer.OnClose();
        }
        super.removed(playerIn);
    }

    public List<SubMenu> GetSubMenus() { return subMenus; }


    @Override
    public boolean canDragTo(@NotNull Slot slotIn)
    {
        return !(slotIn instanceof FakeSlot);
    }

    private void FakeSlotClick(int slotId, int dragType, ClickType clickTypeIn, Player player)
    {
        FakeSlot clickedSlot = (FakeSlot)slots.get(slotId);
        switch (clickTypeIn)
        {
            case PICKUP:
            {
                if(clickedSlot.getItem().getItem() == getCarried().getItem())
                {
                    int countChange = 0;
                    if(dragType == 0)
                    {
                        //left click
                        countChange = getCarried().getCount();
                    }
                    else if(dragType == 1)
                    {
                        //right click
                        countChange = -getCarried().getCount();
                    }

                    int newCount = clickedSlot.getItem().getCount() + countChange;
                    if(newCount <= 0)
                    {
                        clickedSlot.set(ItemStack.EMPTY);
                    }
                    else
                    {
                        clickedSlot.getItem().setCount(Math.min(clickedSlot.getMaxStackSize(), newCount));
                    }
                }
                else if(getCarried().isEmpty())
                {
                    if(!clickedSlot.getItem().isEmpty())
                    {
                        int countChange = 0;
                        if(dragType == 0)
                        {
                            //left click
                            countChange = 1;
                        }
                        else if(dragType == 1)
                        {
                            //right click
                            countChange = -1;
                        }

                        int newCount = clickedSlot.getItem().getCount() + countChange;
                        if(newCount <= 0)
                        {
                            clickedSlot.set(ItemStack.EMPTY);
                        }
                        else
                        {
                            clickedSlot.getItem().setCount(Math.min(clickedSlot.getMaxStackSize(), newCount));
                        }
                    }
                }
                else
                {
                    if(dragType == 0)
                    {
                        ItemStack stack = getCarried().copy();
                        stack.setCount(Math.min(stack.getCount(), clickedSlot.getMaxStackSize()));
                        clickedSlot.set(stack);
                    }
                }
                return;
            }
            case QUICK_MOVE:
            {
                int newCount = 0;
                if(dragType == 0)
                {
                    newCount = clickedSlot.getItem().getCount() * 2;
                }
                else if(dragType == 1)
                {
                    newCount = clickedSlot.getItem().getCount() / 2;
                }

                if(newCount <= 0)
                {
                    clickedSlot.set(ItemStack.EMPTY);
                }
                else
                {
                    clickedSlot.getItem().setCount(Math.min(clickedSlot.getMaxStackSize(), newCount));
                }
                return;
            }
            case CLONE:
            {
                clickedSlot.set(ItemStack.EMPTY);
                return;
            }
            default:
                break;
        }
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickTypeIn, @NotNull Player player)
    {
        Slot clickedSlot = slotId >= 0 ? slots.get(slotId) : null;
        if (clickedSlot instanceof FakeSlot)
        {
            FakeSlotClick(slotId, dragType, clickTypeIn, player);
        }
        else
        {
            super.clicked(slotId, dragType, clickTypeIn, player);
        }
    }

    public boolean mouseScrolled(int slotID, double mouseX, double mouseY, double delta)
    {
        Slot hoveredSlot = slotID >= 0 ? slots.get(slotID) : null;
        if(hoveredSlot instanceof FakeSlot)
        {
            ItemStack stack = hoveredSlot.getItem();

            int newCount = stack.getCount() + (int)delta;
            if(newCount <= 0)
            {
                hoveredSlot.set(ItemStack.EMPTY);
            }
            else
            {
                hoveredSlot.getItem().setCount(Math.min(hoveredSlot.getMaxStackSize(), newCount));
            }

            return true;
        }
        return false;
    }
}
