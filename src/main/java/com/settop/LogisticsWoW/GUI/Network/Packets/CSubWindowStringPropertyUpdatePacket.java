package com.settop.LogisticsWoW.GUI.Network.Packets;

import net.minecraft.network.FriendlyByteBuf;

public class CSubWindowStringPropertyUpdatePacket
{
    private final int windowID;
    private final int subWindowID;
    private final int propertyID;
    private final String value;

    public CSubWindowStringPropertyUpdatePacket(int windowID, int subWindowID, int propertyID, String value)
    {
        this.windowID = windowID;
        this.subWindowID = subWindowID;
        this.propertyID = propertyID;
        this.value = value;
    }

    public static CSubWindowStringPropertyUpdatePacket decode(FriendlyByteBuf buf)
    {
        int windowID = buf.readInt();
        int subWindowID = buf.readInt();
        int propertyID = buf.readInt();
        String value = buf.readUtf();

        CSubWindowStringPropertyUpdatePacket retval = new CSubWindowStringPropertyUpdatePacket(windowID, subWindowID, propertyID, value);

        return retval;
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeInt(windowID);
        buf.writeInt(subWindowID);
        buf.writeInt(propertyID);
        buf.writeUtf(value);
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
    public String GetValue()
    {
        return value;
    }
}
