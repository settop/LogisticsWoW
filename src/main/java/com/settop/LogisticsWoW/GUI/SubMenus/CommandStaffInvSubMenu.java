package com.settop.LogisticsWoW.GUI.SubMenus;

import com.settop.LogisticsWoW.Client.Client;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.SubScreen;
import com.settop.LogisticsWoW.GUI.ActivatableSlotItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;

public class CommandStaffInvSubMenu extends SubMenu
{
    public CommandStaffInvSubMenu(IItemHandler staffInv, int xPos, int yPos)
    {
        super(xPos, yPos);

        int slotIndex = 0;
        for(int i = 0; i < staffInv.getSlots(); ++i)
        {
            int row = i / Client.PLAYER_INVENTORY_COLUMN_COUNT;
            int column = i % Client.PLAYER_INVENTORY_COLUMN_COUNT;
            this.inventorySlots.add(new ActivatableSlotItemHandler(staffInv, slotIndex++,  xPos + column * Client.SLOT_X_SPACING, yPos + row * Client.SLOT_Y_SPACING));
        }
    }

    @Override
    public SubScreen CreateScreen(MultiScreen<?> parentScreen)
    {
        return null;
    }
}
