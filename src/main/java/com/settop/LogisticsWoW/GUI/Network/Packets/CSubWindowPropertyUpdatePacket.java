package com.settop.LogisticsWoW.GUI.Network.Packets;

import net.minecraft.network.FriendlyByteBuf;

public class CSubWindowPropertyUpdatePacket
{
    private final int windowID;
    private final int subWindowID;
    private final int propertyID;
    private final int value;

    public CSubWindowPropertyUpdatePacket(int windowID, int subWindowID, int propertyID, int value)
    {
        this.windowID = windowID;
        this.subWindowID = subWindowID;
        this.propertyID = propertyID;
        this.value = value;
    }

    public static CSubWindowPropertyUpdatePacket decode(FriendlyByteBuf buf)
    {
        int windowID = buf.readInt();
        int subWindowID = buf.readInt();
        int propertyID = buf.readInt();
        int value = buf.readInt();

        CSubWindowPropertyUpdatePacket retval = new CSubWindowPropertyUpdatePacket(windowID, subWindowID, propertyID, value);

        return retval;
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeInt(windowID);
        buf.writeInt(subWindowID);
        buf.writeInt(propertyID);
        buf.writeInt(value);
    }

    public int GetWindowID()
    {
        return windowID;
    }
    public int GetSubWindowID(){ return subWindowID; }
    public int GetPropertyID()
    {
        return propertyID;
    }
    public int GetValue()
    {
        return value;
    }
}
