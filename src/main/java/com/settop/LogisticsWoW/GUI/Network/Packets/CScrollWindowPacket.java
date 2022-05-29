package com.settop.LogisticsWoW.GUI.Network.Packets;

import net.minecraft.network.FriendlyByteBuf;

public class CScrollWindowPacket
{
    /** The id of the window which was clicked. 0 for player inventory. */
    private final int windowId;
    /** Id of the hovered slot */
    private final int slotId;
    /** The amount the scroll wheel moved  */
    private final float delta;

    public CScrollWindowPacket(int windowId, int slotId, float delta)
    {
        this.windowId = windowId;
        this.slotId = slotId;
        this.delta = delta;
    }

    public static CScrollWindowPacket decode(FriendlyByteBuf buf)
    {
        int windowID = buf.readInt();
        int slotId = buf.readInt();
        float delta = buf.readFloat();

        CScrollWindowPacket retval = new CScrollWindowPacket(windowID, slotId, delta);

        return retval;
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeInt(windowId);
        buf.writeInt(slotId);
        buf.writeFloat(delta);
    }

    public int GetWindowID()
    {
        return windowId;
    }
    public int GetSlotID()
    {
        return slotId;
    }
    public float GetDelta() { return delta; }
}
