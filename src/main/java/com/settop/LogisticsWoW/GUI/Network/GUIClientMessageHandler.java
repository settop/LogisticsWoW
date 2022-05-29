package com.settop.LogisticsWoW.GUI.Network;

import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.fml.LogicalSide;
import com.settop.LogisticsWoW.GUI.MultiScreenMenu;
import com.settop.LogisticsWoW.GUI.Network.Packets.SWindowStringPropertyPacket;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GUIClientMessageHandler
{

    public static void OnMessageReceived(final SWindowStringPropertyPacket message, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.CLIENT)
        {
            LogisticsWoW.LOGGER.warn("SWindowStringPropertyPacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        ctx.enqueueWork(() -> ProcessMessage(message));
    }

    private static void ProcessMessage(final SWindowStringPropertyPacket message)
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player.containerMenu == null)
        {
            LogisticsWoW.LOGGER.warn("Player does not have open container for SWindowStringPropertyPacket");
            return;
        }

        if(player.containerMenu.containerId != message.GetWindowID())
        {
            LogisticsWoW.LOGGER.warn("Player open container does not match window id for SWindowStringPropertyPacket");
            return;
        }

        if(!(player.containerMenu instanceof MultiScreenMenu))
        {
            LogisticsWoW.LOGGER.error("Player open container is not a known container type for SWindowStringPropertyPacket");
            return;
        }

        MultiScreenMenu multiScreenContainer = (MultiScreenMenu)player.containerMenu;

        multiScreenContainer.updateTrackedString(message.GetPropertyID(), message.GetValue());
    }
}
