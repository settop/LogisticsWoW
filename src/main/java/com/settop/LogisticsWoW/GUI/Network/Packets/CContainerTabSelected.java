package com.settop.LogisticsWoW.GUI.Network.Packets;

import net.minecraft.network.FriendlyByteBuf;

public class CContainerTabSelected
{
    private final int windowID;
    private final int tabID;

    public CContainerTabSelected(int inWindowID, int inTabID)
    {
        windowID = inWindowID;
        tabID = inTabID;
    }

    public static CContainerTabSelected decode(FriendlyByteBuf buf)
    {
        int windowID = buf.readInt();
        int tabID = buf.readInt();

        CContainerTabSelected retval = new CContainerTabSelected(windowID, tabID);

        return retval;
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeInt(windowID);
        buf.writeInt(tabID);
    }

    public int GetWindowID()
    {
        return windowID;
    }

    public int GetTabID()
    {
        return tabID;
    }
}
