package com.settop.LogisticsWoW.GUI.SubMenus;

import com.settop.LogisticsWoW.Client.Client;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.SubScreen;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.WispContentsSubScreen;
import com.settop.LogisticsWoW.GUI.WispEnhancementSlot;
import com.settop.LogisticsWoW.Wisps.WispInteractionContents;

public class WispContentsMenu extends SubMenu
{
    public WispContentsMenu(WispInteractionContents inWispContents, int xPos, int yPos)
    {
        super(xPos, yPos);
        for(int i = 0; i < inWispContents.getContainerSize(); ++i)
        {
            inventorySlots.add(new WispEnhancementSlot(inWispContents, i, xPos + Client.SLOT_X_SPACING * i, yPos));
        }
    }

    @Override
    public SubScreen CreateScreen(MultiScreen<?> parentScreen)
    {
        return new WispContentsSubScreen(this, parentScreen);
    }
}
