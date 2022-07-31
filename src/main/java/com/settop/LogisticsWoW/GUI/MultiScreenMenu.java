package com.settop.LogisticsWoW.GUI;

import com.google.common.collect.Lists;
import com.settop.LogisticsWoW.GUI.Network.Packets.SWindowStringPropertyPacket;
import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.StringReferenceHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MultiScreenMenu extends AbstractContainerMenu
{
    public static int TEMP_MENU_ID = -1;

    private static class DisabledSlot extends Slot
    {
        private static final SimpleContainer disabledContainer = new SimpleContainer(1);
        public DisabledSlot()
        {
            super(disabledContainer, 0, 0 ,0);
        }
        @Override
        public boolean isActive()
        {
            return false;
        }
        @Override
        public boolean hasItem() { return false; }
        @Override
        public boolean allowModification(@NotNull Player player) { return false; }
        @Override
        public @NotNull ItemStack getItem() { return ItemStack.EMPTY; }
    }
    private final DataSlot dummyDataSlot = DataSlot.standalone();
    private final StringReferenceHolder dummyStringRef = StringReferenceHolder.single();

    private final boolean clientSide;
    private final List<SubMenu> subMenus = new ArrayList<>();
    private SubMenu tempSubMenu = null;
    private final List<StringReferenceHolder> trackedStringReferences = Lists.newArrayList();
    private final ServerPlayer player;
    private int permanentSlotCount = 0;
    private int permanentDataCount = 0;
    private int permanentStringCount = 0;

    private static class QueuedSlot
    {
        final int index;
        final ItemStack stack;
        private QueuedSlot(int index, ItemStack stack)
        {
            this.index = index;
            this.stack = stack;
        }
    }
    private final ArrayList<QueuedSlot> queuedTempSlots;
    private static class QueuedData
    {
        final int index;
        final int value;
        private QueuedData(int index, int value)
        {
            this.index = index;
            this.value = value;
        }
    }
    private final ArrayList<QueuedData> queuedTempData;

    protected MultiScreenMenu(@Nullable MenuType<?> type, int id, Player player)
    {
        super(type, id);
        this.player = (ServerPlayer)player;
        clientSide = player == null;
        if(clientSide)
        {
            queuedTempSlots = new ArrayList<>();
            queuedTempData = new ArrayList<>();
        }
        else
        {
            queuedTempSlots = null;
            queuedTempData = null;
        }
    }

    protected void SetSubMenus(List<SubMenu> subContainers)
    {
        for(SubMenu subContainer : subContainers)
        {
            AddSubMenu(subContainer);
        }
    }

    private void AddSubMenu(SubMenu container)
    {
        container.SetParent( new WeakReference<>(this), subMenus.size() );

        if(tempSubMenu != null)
        {
            throw new RuntimeException("All permanent sub menus must be registered before any temporary ones");
        }
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
            ++permanentStringCount;
        }

        subMenus.add(container);
    }

    protected void ReserveTempSubMenuSlots(int numSlots, int numDataSlots, int numStringRefs)
    {
        permanentSlotCount = slots.size();
        permanentDataCount = dataSlots.size();
        permanentStringCount = trackedStringReferences.size();
        for(int i = 0; i < numSlots; ++i)
        {
            addSlot(new DisabledSlot());
        }
        for(int i = 0; i < numDataSlots; ++i)
        {
            addDataSlot(dummyDataSlot);
        }
        for(int i = 0; i < numStringRefs; ++i)
        {
            trackString(dummyStringRef);
        }
        broadcastChanges();
    }

    protected void AddTempSubMenu(SubMenu container)
    {
        assert permanentSlotCount + container.inventorySlots.size() <= slots.size();
        assert permanentDataCount + container.trackedIntData.size() <= dataSlots.size();
        assert permanentStringCount + container.trackedStringData.size() <= trackedStringReferences.size();
        container.SetParent( new WeakReference<>(this), TEMP_MENU_ID);
        tempSubMenu = container;
        int tempSlotIndex = permanentSlotCount;
        for(Slot subSlot : container.inventorySlots)
        {
            subSlot.index = tempSlotIndex;
            slots.set(tempSlotIndex, subSlot);
            ++tempSlotIndex;
        }
        int tempDataIndex = permanentDataCount;
        for(DataSlot dataSlot : container.trackedIntData)
        {
            dataSlots.set(tempDataIndex, dataSlot);
            ++tempDataIndex;
        }

        int tempStringIndex = permanentStringCount;
        for(StringReferenceHolder strRef : container.trackedStringData)
        {
            trackedStringReferences.set(tempStringIndex, strRef);
            ++tempStringIndex;
        }

        if(clientSide)
        {
            assert queuedTempSlots != null;
            for(QueuedSlot queuedSlot : queuedTempSlots)
            {
                setItem(queuedSlot.index, getStateId(), queuedSlot.stack);
            }
            queuedTempSlots.clear();

            assert queuedTempData != null;
            for(QueuedData queuedData : queuedTempData)
            {
                setData(queuedData.index, queuedData.value);
            }
            queuedTempData.clear();
        }
        else
        {
            broadcastFullState();
        }
        tempSubMenu.OnDataRefresh();
    }

    protected void RemoveTempSubMenu(SubMenu container)
    {
        assert tempSubMenu == container;
        container.OnClose();
        tempSubMenu = null;
        for(int i = permanentSlotCount; i < slots.size(); ++i)
        {
            Slot dummySlot = new DisabledSlot();
            dummySlot.index = i;
            slots.set(i, dummySlot);
        }
        for(int i = permanentDataCount; i < dataSlots.size(); ++i)
        {
            dataSlots.set(i, dummyDataSlot);
        }
        for(int i = permanentStringCount; i < trackedStringReferences.size(); ++i)
        {
            trackedStringReferences.set(i, dummyStringRef);
        }
    }

    @Override
    public void setItem(int slot, int state, @NotNull ItemStack stack)
    {
        if(clientSide)
        {
            if(slot >= permanentSlotCount && tempSubMenu == null)
            {
                //queue it for later
                assert queuedTempSlots != null;
                queuedTempSlots.add(new QueuedSlot(slot, stack));
                return;
            }
        }
        super.setItem(slot, state, stack);
    }

    @Override
    public void setData(int id, int data)
    {
        if(clientSide)
        {
            if(id >= permanentDataCount && tempSubMenu == null)
            {
                //queue it for later
                assert queuedTempData != null;
                queuedTempData.add(new QueuedData(id, data));
                return;
            }
        }
        super.setData(id, data);
        for(SubMenu subContainer : subMenus)
        {
            subContainer.OnDataRefresh();
        }
        if(tempSubMenu != null)
        {
            tempSubMenu.OnDataRefresh();
        }
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
    public void broadcastFullState()
    {
        super.broadcastFullState();
        for(int i = 0; i < trackedStringReferences.size(); ++i)
        {
            StringReferenceHolder strRef = trackedStringReferences.get(i);
            if(player != null)
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
        if(tempSubMenu != null)
        {
            tempSubMenu.OnClose();
        }
        super.removed(playerIn);
    }

    public List<SubMenu> GetSubMenus() { return subMenus; }
    public SubMenu GetTempSubMenu() { return tempSubMenu; }

    protected void moveItemStackToFakeSlots(ItemStack stack, int slotStart, int slotEnd)
    {
        for(int i = slotStart; i < slotEnd; ++i)
        {
            Slot targetSlot = slots.get(i);
            if(targetSlot instanceof FakeSlot)
            {
                if(targetSlot.isActive() && targetSlot.getItem().isEmpty())
                {
                    targetSlot.set(stack);
                    return;
                }
            }
        }
    }

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
        Slot clickedSlot = slotId >= 0 ? getSlot(slotId) : null;
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
        Slot hoveredSlot = slotID >= 0 ? getSlot(slotID) : null;
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
