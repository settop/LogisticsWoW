package com.settop.LogisticsWoW.Client.Screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.client.Minecraft;
import com.settop.LogisticsWoW.Client.Screens.Popups.ScreenPopup;
import com.settop.LogisticsWoW.Client.Screens.SubScreens.SubScreen;
import com.settop.LogisticsWoW.GUI.MultiScreenMenu;
import com.settop.LogisticsWoW.GUI.Network.Packets.CScrollWindowPacket;
import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

import java.util.ArrayList;
import java.util.List;

public abstract class MultiScreen<T extends MultiScreenMenu> extends AbstractContainerScreen<T> implements MenuAccess<T>
{
    public static final ResourceLocation GUI_PARTS_TEXTURE = new ResourceLocation(LogisticsWoW.MOD_ID, "textures/gui/gui_parts.png");
    public static class GuiPart
    {
        public GuiPart(int uStart, int vStart, int width, int height)
        {
            this.uStart = uStart;
            this.vStart = vStart;
            this.width = width;
            this.height = height;
        }
        public int uStart, width;
        public int vStart, height;
    }
    public static final int BG_COLOUR = 0xffc6c6c6;
    public static final GuiPart PLAYER_INVENTORY = new GuiPart(0, 32, 166, 81);
    public static final GuiPart INV_SLOT = new GuiPart(2, 35, 18, 18);
    public static final GuiPart BUTTON = new GuiPart(0, 0, 32, 32);
    public static final GuiPart BUTTON_HOVERED = new GuiPart(32, 0, 32, 32);
    public static final GuiPart BUTTON_INACTIVE = new GuiPart(64, 0, 32, 32);
    public static final GuiPart BUTTON_INACTIVE_HOVERED = new GuiPart(96, 0, 32, 32);
    public static final GuiPart BORDER_TOP_LEFT = new GuiPart(0, 0, 5, 5);
    public static final GuiPart BORDER_TOP_RIGHT = new GuiPart(27, 0, 5, 5);
    public static final GuiPart BORDER_BOTTOM_LEFT = new GuiPart(0, 27, 5, 5);
    public static final GuiPart BORDER_BOTTOM_RIGHT = new GuiPart(27, 27, 5, 5);
    public static final GuiPart BORDER_TOP = new GuiPart(5, 0, 22, 5);
    public static final GuiPart BORDER_LEFT = new GuiPart(0, 5, 5, 22);
    public static final GuiPart BORDER_RIGHT = new GuiPart(27, 5, 5, 22);
    public static final GuiPart BORDER_BOTTOM = new GuiPart(5, 27, 22, 5);
    public static final GuiPart BACKGROUND = new GuiPart(5, 5, 24, 24);
    public static final GuiPart BUTTON_N = new GuiPart(0, 113, 20, 20);
    public static final GuiPart BUTTON_S = new GuiPart(0, 133, 20, 20);
    public static final GuiPart BUTTON_E = new GuiPart(20, 113, 20, 20);
    public static final GuiPart BUTTON_W = new GuiPart(20, 133, 20, 20);
    public static final GuiPart BUTTON_T = new GuiPart(40, 113, 20, 20);
    public static final GuiPart BUTTON_B = new GuiPart(40, 133, 20, 20);
    public static final GuiPart OVERLAY_ORANGE = new GuiPart(60, 113, 20, 20);
    public static final GuiPart OVERLAY_BLUE  = new GuiPart(60, 133, 20, 20);
    public static final GuiPart BUTTON_DIRECTIONS[] = {BUTTON_B, BUTTON_T, BUTTON_N, BUTTON_S, BUTTON_W, BUTTON_E};
    public static final GuiPart OVERLAY_SIDE_CONFIG = new GuiPart(128, 0 , 32, 32);
    public static final GuiPart OVERLAY_BLACKLIST = new GuiPart(160, 0 , 32, 32);
    public static final GuiPart OVERLAY_WHITELIST = new GuiPart(192, 0 , 32, 32);
    public static final GuiPart OVERLAY_ARROW = new GuiPart(224, 0 , 32, 32);

    private List<SubScreen> subScreens;
    private ArrayList<ScreenPopup> openPopups = new ArrayList<>();

    public MultiScreen(T screenContainer, Inventory inv, Component titleIn)
    {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY)
    {
        for(SubScreen subScreen : subScreens)
        {
            if(subScreen != null && subScreen.GetSubContainer().IsActive())
            {
                subScreen.renderBg(matrixStack, partialTicks, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int x, int y)
    {
        for(SubScreen subScreen : subScreens)
        {
            if(subScreen != null && subScreen.GetSubContainer().IsActive())
            {
                subScreen.renderLabels(matrixStack, x, y);
            }
        }
    }


    @Override
    protected void renderTooltip(PoseStack matrixStack, int x, int y)
    {
        for(ScreenPopup popup : openPopups)
        {
            if(popup.isMouseOver(x, y))
            {
                return;
            }
        }
        super.renderTooltip(matrixStack, x, y);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        for(SubScreen subScreen : subScreens)
        {
            if(subScreen != null)
            {
                if(subScreen.GetSubContainer().IsActive())
                {
                    subScreen.SetActive(true);
                    subScreen.render(matrixStack, mouseX, mouseY, partialTicks);
                }
                else
                {
                    subScreen.SetActive(false);
                }
            }
        }
        for(ScreenPopup popup : openPopups)
        {
            popup.render(matrixStack, mouseX, mouseY, partialTicks);
        }
    }

    public static void RenderBorder(GuiComponent gui, PoseStack matrixStack, int x, int y, int blitOffset, int width, int height)
    {
        int oldBlitOffset = gui.getBlitOffset();
        gui.setBlitOffset(blitOffset);
        RenderSystem.setShaderTexture(0, GUI_PARTS_TEXTURE);

        gui.blit(matrixStack, x - BORDER_TOP_LEFT.width, y - BORDER_TOP_LEFT.height, BORDER_TOP_LEFT.uStart, BORDER_TOP_LEFT.vStart, BORDER_TOP_LEFT.width, BORDER_TOP_LEFT.height);
        gui.blit(matrixStack, x + width, y - BORDER_TOP_RIGHT.height, BORDER_TOP_RIGHT.uStart, BORDER_TOP_RIGHT.vStart, BORDER_TOP_RIGHT.width, BORDER_TOP_RIGHT.height);
        gui.blit(matrixStack, x - BORDER_BOTTOM_LEFT.width, y + height, BORDER_BOTTOM_LEFT.uStart, BORDER_BOTTOM_LEFT.vStart, BORDER_BOTTOM_LEFT.width, BORDER_BOTTOM_LEFT.height);
        gui.blit(matrixStack, x + width, y + height, BORDER_BOTTOM_RIGHT.uStart, BORDER_BOTTOM_RIGHT.vStart, BORDER_BOTTOM_RIGHT.width, BORDER_BOTTOM_RIGHT.height);

        int numHorizontalBorders = width / BORDER_TOP.width;
        for(int i = 0; i < numHorizontalBorders; ++i)
        {
            gui.blit(matrixStack, x + BORDER_TOP.width * i, y - BORDER_TOP.height, BORDER_TOP.uStart, BORDER_TOP.vStart, BORDER_TOP.width, BORDER_TOP.height);
            gui.blit(matrixStack, x + BORDER_BOTTOM.width * i, y + height, BORDER_BOTTOM.uStart, BORDER_BOTTOM.vStart, BORDER_BOTTOM.width, BORDER_BOTTOM.height);
        }
        if(width % BORDER_TOP.width != 0)
        {
            gui.blit(matrixStack, x + width - BORDER_TOP.width, y - BORDER_TOP.height, BORDER_TOP.uStart, BORDER_TOP.vStart, BORDER_TOP.width, BORDER_TOP.height);
            gui.blit(matrixStack, x + width - BORDER_TOP.width, y + height, BORDER_BOTTOM.uStart, BORDER_BOTTOM.vStart, BORDER_BOTTOM.width, BORDER_BOTTOM.height);
        }

        int numVerticalBorders = height / BORDER_LEFT.height;
        for(int i = 0; i < numVerticalBorders; ++i)
        {
            gui.blit(matrixStack, x - BORDER_LEFT.width, y + BORDER_LEFT.height * i, BORDER_LEFT.uStart, BORDER_LEFT.vStart, BORDER_LEFT.width, BORDER_LEFT.height);
            gui.blit(matrixStack, x + width, y + BORDER_RIGHT.height * i, BORDER_RIGHT.uStart, BORDER_RIGHT.vStart, BORDER_RIGHT.width, BORDER_RIGHT.height);
        }
        if(height % BORDER_LEFT.height != 0)
        {
            gui.blit(matrixStack, x - BORDER_LEFT.width, y + height - BORDER_LEFT.height, BORDER_LEFT.uStart, BORDER_LEFT.vStart, BORDER_LEFT.width, BORDER_LEFT.height);
            gui.blit(matrixStack, x + width, y + height - BORDER_RIGHT.height, BORDER_RIGHT.uStart, BORDER_RIGHT.vStart, BORDER_RIGHT.width, BORDER_RIGHT.height);
        }
        gui.setBlitOffset(oldBlitOffset);
    }

    public static void RenderSlotRowBackground(GuiComponent gui, PoseStack matrixStack, int x, int y, int blitOffset, int numSlots)
    {
        int oldBlitOffset = gui.getBlitOffset();
        gui.setBlitOffset(blitOffset);
        RenderSystem.setShaderTexture(0, GUI_PARTS_TEXTURE);

        gui.blit(matrixStack, x - 1, y - 1, INV_SLOT.uStart, INV_SLOT.vStart, INV_SLOT.width * numSlots, INV_SLOT.height);

        gui.setBlitOffset(oldBlitOffset);
    }

    public void RenderPlayerInv(PoseStack matrixStack, int playerInvXPos, int playerInvYPos)
    {
        RenderSystem.setShaderTexture(0, GUI_PARTS_TEXTURE);

        int playerInvXOffset = -3;
        int playerInvYOffset = -4;
        this.blit(matrixStack, leftPos + playerInvXPos + playerInvXOffset, topPos + playerInvYPos + playerInvYOffset, PLAYER_INVENTORY.uStart, PLAYER_INVENTORY.vStart, PLAYER_INVENTORY.width, PLAYER_INVENTORY.height);
    }

    @Override
    protected void init()
    {
        super.init();

        subScreens = new ArrayList<>();
        for(SubMenu subContainer : menu.GetSubMenus())
        {
            SubScreen screen = subContainer.CreateScreen(this);
            subScreens.add(screen);
            if(screen != null)
            {
                screen.init(leftPos, topPos);
                for (AbstractWidget widget : screen.GetWidgets())
                {
                    addRenderableWidget(widget);
                    widget.visible = false;
                    widget.active = false;
                }
            }
        }
    }

    @Override
    public void onClose()
    {
        for(SubScreen subScreen : subScreens)
        {
            if(subScreen != null)
            {
                subScreen.onClose();
            }
        }
        super.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if(openPopups.isEmpty())
        {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        else
        {
            ScreenPopup topPopup = openPopups.get(openPopups.size() - 1);
            if(topPopup.isMouseOver(mouseX, mouseY))
            {
                return topPopup.mouseClicked(mouseX, mouseY, button);
            }
            else
            {
                //close the popup
                topPopup.OnClose();
                openPopups.remove(openPopups.size() - 1);
                removeWidget(topPopup);
                return true;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        int slotID = hoveredSlot == null ? -1 : hoveredSlot.index;
        if(getMenu().mouseScrolled(slotID, mouseX, mouseY, delta))
        {
            LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer( new CScrollWindowPacket(getMenu().containerId, slotID, (float)delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    public void OpenPopup(ScreenPopup popup)
    {
        popup.setBlitOffset(500 + 100 * openPopups.size());
        openPopups.add(popup);
        addRenderableWidget(popup);
        popup.OnOpen();

    }
}
