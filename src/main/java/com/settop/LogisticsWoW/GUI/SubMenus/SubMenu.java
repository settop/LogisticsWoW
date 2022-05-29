package com.settop.LogisticsWoW.GUI.SubMenus;

import com.google.common.collect.Lists;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.SubScreen;
import com.settop.LogisticsWoW.GUI.IActivatableSlot;
import com.settop.LogisticsWoW.GUI.MultiScreenMenu;
import com.settop.LogisticsWoW.Utils.StringReferenceHolder;

import java.lang.ref.WeakReference;
import java.util.List;

public abstract class SubMenu
{
    private WeakReference<MultiScreenMenu> parentContainer;
    private int subWindowId = -1;
    protected boolean isActive = true;

    private int xPos;
    private int yPos;

    public final List<Slot> inventorySlots = Lists.newArrayList();
    public final List<DataSlot> trackedIntData = Lists.newArrayList();
    public final List<StringReferenceHolder> trackedStringData = Lists.newArrayList();

    protected SubMenu(int xPos, int yPos)
    {
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public void SetParent(WeakReference<MultiScreenMenu> parentContainer, int subWindowId)
    {
        this.parentContainer = parentContainer;
        this.subWindowId = subWindowId;
    }

    public void SetActive(boolean active)
    {
        isActive = active;
        for(Slot slot : inventorySlots)
        {
            if(slot instanceof IActivatableSlot)
            {
                ((IActivatableSlot)slot).SetActive(active);
            }
        }
    }

    public MultiScreenMenu GetParentContainer() { return parentContainer.get(); }
    public int GetSubWindowID() { return subWindowId; }

    protected DataSlot addDataSlot(DataSlot dataSlot)
    {
        trackedIntData.add(dataSlot);
        return dataSlot;
    }

    protected void addDataSlots(ContainerData dataContainer)
    {
        for(int i = 0; i < dataContainer.getCount(); ++i)
        {
            this.addDataSlot(DataSlot.forContainer(dataContainer, i));
        }
    }

    protected StringReferenceHolder trackStr(StringReferenceHolder strIn)
    {
        trackedStringData.add(strIn);
        return strIn;
    }

    public void HandlePropertyUpdate(int propertyId, int value)
    {
    }

    public void HandleStringPropertyUpdate(int propertyId, String value)
    {
    }

    public void OnClose()
    {
    }

    public boolean IsActive()
    {
        return isActive;
    }

    public int GetXPos(){ return xPos; }
    public int GetYPos(){ return yPos; }


    @OnlyIn(Dist.CLIENT)
    abstract public SubScreen CreateScreen(MultiScreen<?> parentScreen);
}
