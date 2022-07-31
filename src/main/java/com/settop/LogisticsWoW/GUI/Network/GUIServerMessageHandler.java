package com.settop.LogisticsWoW.GUI.Network;

import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.LogicalSide;
import com.settop.LogisticsWoW.GUI.BasicWispMenu;
import com.settop.LogisticsWoW.GUI.MultiScreenMenu;
import com.settop.LogisticsWoW.GUI.Network.Packets.*;
import com.settop.LogisticsWoW.GUI.SubMenus.StorageEnhancementSubMenu;
import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class GUIServerMessageHandler
{
    public static void OnMessageReceived(final CContainerTabSelected message, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER)
        {
            LogisticsWoW.LOGGER.warn("ContainerTabSelected received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null)
        {
            LogisticsWoW.LOGGER.warn("EntityPlayerMP was null when ContainerTabSelected was received");
        }

        ctx.enqueueWork(() -> ProcessMessage(message, sendingPlayer));
    }

    private static void ProcessMessage(final CContainerTabSelected message, ServerPlayer sendingPlayer)
    {
        if(sendingPlayer.containerMenu == null)
        {
            LogisticsWoW.LOGGER.warn("Player does not have open container for ContainerTabSelected");
            return;
        }

        if(sendingPlayer.containerMenu.containerId != message.GetWindowID())
        {
            LogisticsWoW.LOGGER.warn("Player open container does not match window id for ContainerTabSelected");
            return;
        }

        if(sendingPlayer.containerMenu instanceof BasicWispMenu)
        {
            BasicWispMenu basicWispMenu = (BasicWispMenu)sendingPlayer.containerMenu;
            basicWispMenu.SelectTab(message.GetTabID());
        }
        else
        {
            LogisticsWoW.LOGGER.error("Player open container is not a known container type for OpenEnhancementSubContainerPacket");
        }
    }

    public static void OnMessageReceived(final CSubContainerDirectionChange message, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER)
        {
            LogisticsWoW.LOGGER.warn("ProviderContainerDirectionChange received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null)
        {
            LogisticsWoW.LOGGER.warn("EntityPlayerMP was null when ProviderContainerDirectionChange was received");
        }

        ctx.enqueueWork(() -> ProcessMessage(message, sendingPlayer));
    }

    private static void ProcessMessage(final CSubContainerDirectionChange message, ServerPlayer sendingPlayer)
    {
        if(sendingPlayer.containerMenu == null)
        {
            LogisticsWoW.LOGGER.warn("Player does not have open container for SubContainerDirectionChange");
            return;
        }

        if(sendingPlayer.containerMenu.containerId != message.GetWindowID())
        {
            LogisticsWoW.LOGGER.warn("Player open container does not match window id for SubContainerDirectionChange");
            return;
        }

        if(!(sendingPlayer.containerMenu instanceof MultiScreenMenu))
        {
            LogisticsWoW.LOGGER.error("Player open container is not a known container type for SubContainerDirectionChange");
            return;
        }

        MultiScreenMenu multiScreenContainer = (MultiScreenMenu)sendingPlayer.containerMenu;
        if(message.GetSubWindowID() == MultiScreenMenu.TEMP_MENU_ID)
        {
            SubMenu subContainer = multiScreenContainer.GetTempSubMenu();
            if(subContainer == null)
            {
                LogisticsWoW.LOGGER.error("Invalid temp sub window for SubContainerDirectionChange");
                return;
            }

        }
        else
        {
            List<SubMenu> subContainers = multiScreenContainer.GetSubMenus();

            if(message.GetSubWindowID() >= subContainers.size())
            {
                LogisticsWoW.LOGGER.error("Invalid sub window id for SubContainerDirectionChange");
                return;
            }

            SubMenu subContainer = subContainers.get(message.GetSubWindowID());
            {
                LogisticsWoW.LOGGER.error("Unknown sub container type for SubContainerDirectionChange");
            }
        }
    }

    public static void OnMessageReceived(final CScrollWindowPacket message, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER)
        {
            LogisticsWoW.LOGGER.warn("CScrollWindowPacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null)
        {
            LogisticsWoW.LOGGER.warn("EntityPlayerMP was null when CScrollWindowPacket was received");
        }

        ctx.enqueueWork(() -> ProcessMessage(message, sendingPlayer));
    }

    private static void ProcessMessage(final CScrollWindowPacket message, ServerPlayer sendingPlayer)
    {
        if(sendingPlayer.containerMenu == null)
        {
            LogisticsWoW.LOGGER.warn("Player does not have open container for CScrollWindowPacket");
            return;
        }

        if(sendingPlayer.containerMenu.containerId != message.GetWindowID())
        {
            LogisticsWoW.LOGGER.warn("Player open container does not match window id for CScrollWindowPacket");
            return;
        }

        if(!(sendingPlayer.containerMenu instanceof MultiScreenMenu))
        {
            LogisticsWoW.LOGGER.error("Player open container is not a known container type for CScrollWindowPacket");
            return;
        }

        MultiScreenMenu multiScreenContainer = (MultiScreenMenu)sendingPlayer.containerMenu;
        multiScreenContainer.mouseScrolled(message.GetSlotID(), 0, 0, message.GetDelta());
    }


    public static void OnMessageReceived(final CSubWindowPropertyUpdatePacket message, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER)
        {
            LogisticsWoW.LOGGER.warn("SubWindowPropertyUpdatePacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null)
        {
            LogisticsWoW.LOGGER.warn("EntityPlayerMP was null when SubWindowPropertyUpdatePacket was received");
        }

        ctx.enqueueWork(() -> ProcessMessage(message, sendingPlayer));
    }

    private static void ProcessMessage(final CSubWindowPropertyUpdatePacket message, ServerPlayer sendingPlayer)
    {
        if(sendingPlayer.containerMenu == null)
        {
            LogisticsWoW.LOGGER.warn("Player does not have open container for SubWindowPropertyUpdatePacket");
            return;
        }

        if(sendingPlayer.containerMenu.containerId != message.GetWindowID())
        {
            LogisticsWoW.LOGGER.warn("Player open container does not match window id for SubWindowPropertyUpdatePacket");
            return;
        }

        if(!(sendingPlayer.containerMenu instanceof MultiScreenMenu))
        {
            LogisticsWoW.LOGGER.error("Player open container is not a known container type for SubWindowPropertyUpdatePacket");
            return;
        }

        MultiScreenMenu multiScreenContainer = (MultiScreenMenu)sendingPlayer.containerMenu;
        if(message.GetSubWindowID() == MultiScreenMenu.TEMP_MENU_ID)
        {
            SubMenu subContainer = multiScreenContainer.GetTempSubMenu();
            if(subContainer == null)
            {
                LogisticsWoW.LOGGER.error("Invalid temp sub window for SubWindowPropertyUpdatePacket");
                return;
            }
            subContainer.HandlePropertyUpdate(message.GetPropertyID(), message.GetValue());
        }
        else
        {
            List<SubMenu> subContainers = multiScreenContainer.GetSubMenus();

            if(message.GetSubWindowID() >= subContainers.size())
            {
                LogisticsWoW.LOGGER.error("Invalid sub window id for SubWindowPropertyUpdatePacket");
                return;
            }

            SubMenu subContainer = subContainers.get(message.GetSubWindowID());
            if(subContainer == null)
            {
                LogisticsWoW.LOGGER.error("Invalid sub window for SubWindowPropertyUpdatePacket");
                return;
            }

            subContainer.HandlePropertyUpdate(message.GetPropertyID(), message.GetValue());
        }
    }

    public static void OnMessageReceived(final CSubWindowStringPropertyUpdatePacket message, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER)
        {
            LogisticsWoW.LOGGER.warn("SubWindowStringPropertyUpdatePacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null)
        {
            LogisticsWoW.LOGGER.warn("EntityPlayerMP was null when SubWindowStringPropertyUpdatePacket was received");
        }

        ctx.enqueueWork(() -> ProcessMessage(message, sendingPlayer));
    }

    private static void ProcessMessage(final CSubWindowStringPropertyUpdatePacket message, ServerPlayer sendingPlayer)
    {
        if(sendingPlayer.containerMenu == null)
        {
            LogisticsWoW.LOGGER.warn("Player does not have open container for SubWindowStringPropertyUpdatePacket");
            return;
        }

        if(sendingPlayer.containerMenu.containerId != message.GetWindowID())
        {
            LogisticsWoW.LOGGER.warn("Player open container does not match window id for SubWindowStringPropertyUpdatePacket");
            return;
        }

        if(!(sendingPlayer.containerMenu instanceof MultiScreenMenu))
        {
            LogisticsWoW.LOGGER.error("Player open container is not a known container type for SubWindowStringPropertyUpdatePacket");
            return;
        }

        MultiScreenMenu multiScreenContainer = (MultiScreenMenu)sendingPlayer.containerMenu;
        if(message.GetSubWindowID() == MultiScreenMenu.TEMP_MENU_ID)
        {
            SubMenu subContainer = multiScreenContainer.GetTempSubMenu();
            if(subContainer == null)
            {
                LogisticsWoW.LOGGER.error("Invalid temp sub window for SubWindowStringPropertyUpdatePacket");
                return;
            }
            subContainer.HandleStringPropertyUpdate(message.GetPropertyID(), message.GetValue());
        }
        else
        {
            List<SubMenu> subContainers = multiScreenContainer.GetSubMenus();

            if(message.GetSubWindowID() >= subContainers.size())
            {
                LogisticsWoW.LOGGER.error("Invalid sub window id for SubWindowStringPropertyUpdatePacket");
                return;
            }

            SubMenu subContainer = subContainers.get(message.GetSubWindowID());
            if(subContainer == null)
            {
                LogisticsWoW.LOGGER.error("Invalid sub window for SubWindowStringPropertyUpdatePacket");
                return;
            }

            subContainer.HandleStringPropertyUpdate(message.GetPropertyID(), message.GetValue());
        }
    }

}
