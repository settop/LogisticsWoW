package com.settop.LogisticsWoW.GUI.Network.Packets;


import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;

public class CSubContainerDirectionChange
{
    private final int windowID;
    private final int subWindowID;
    private final Direction direction;
    private final boolean isSet;

    public CSubContainerDirectionChange(int inWindowID, int inSubWindowID, Direction inDirection, boolean inIsSet)
    {
        windowID = inWindowID;
        subWindowID = inSubWindowID;
        direction = inDirection;
        isSet = inIsSet;
    }

    public static CSubContainerDirectionChange decode(FriendlyByteBuf buf)
    {
        int windowID = buf.readInt();
        int subWindowID = buf.readInt();
        int direction = buf.readInt();
        boolean isSet = buf.readBoolean();

        CSubContainerDirectionChange retval = new CSubContainerDirectionChange(windowID, subWindowID, Direction.from3DDataValue(direction), isSet);

        return retval;
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeInt(windowID);
        buf.writeInt(subWindowID);
        buf.writeInt(direction.get3DDataValue());
        buf.writeBoolean(isSet);
    }

    public int GetWindowID()
    {
        return windowID;
    }
    public int GetSubWindowID(){ return subWindowID; }
    public Direction GetDirection()
    {
        return direction;
    }
    public boolean GetIsSet()
    {
        return isSet;
    }
}
