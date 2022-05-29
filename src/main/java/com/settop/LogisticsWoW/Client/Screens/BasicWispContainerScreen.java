package com.settop.LogisticsWoW.Client.Screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.client.Minecraft;
import com.settop.LogisticsWoW.GUI.Network.Packets.CContainerTabSelected;
import com.settop.LogisticsWoW.GUI.SubMenus.PlayerInventorySubMenu;
import com.settop.LogisticsWoW.GUI.BasicWispMenu;
import com.settop.LogisticsWoW.Wisps.BasicWispContents;
import com.settop.LogisticsWoW.Wisps.Enhancements.EnhancementTypes;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class BasicWispContainerScreen extends MultiScreen<BasicWispMenu> implements Button.OnPress
{
    static class TabButton extends Button
    {
        public static final GuiPart ACTIVE_TAB = new GuiPart(64, 0, 32, 15);
        public static final GuiPart HOVERED_TAB = new GuiPart(96, 0, 32, 15);
        public static final GuiPart INACTIVE_TAB = new GuiPart(0, 0, 32, 16);

        public TabButton(int x, int y, int width, int height, TextComponent title, OnPress pressedAction)
        {
            super(x, y, width, height, title, pressedAction);
        }
        public TabButton(int x, int y, int width, int height, TextComponent title, OnPress pressedAction, OnTooltip onTooltip)
        {
            super(x, y, width, height, title, pressedAction, onTooltip);
        }

        @Override
        public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            RenderSystem.setShaderTexture(0, GUI_PARTS_TEXTURE);
            GuiPart renderedPart = active ? (isHoveredOrFocused() ? HOVERED_TAB : ACTIVE_TAB) : INACTIVE_TAB;

            this.blit(matrixStack, x, y, renderedPart.uStart, renderedPart.vStart, renderedPart.width, renderedPart.height);
            if(!active)
            {
                this.fillGradient(matrixStack, x + 3, y + renderedPart.height, x + renderedPart.width - 3, y + renderedPart.height + 2, BG_COLOUR, BG_COLOUR);
            }
            if (isHoveredOrFocused())
            {
                renderToolTip(matrixStack, mouseX, mouseY);
            }
        }
    }

    private ArrayList<Button> tabs;
    private BasicWispContents wispContents;

    public BasicWispContainerScreen(BasicWispMenu container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
        wispContents = container.GetWispContents();

        // Set the width and height of the gui.  Should match the size of the texture!
        imageWidth = 166;
        imageHeight = BasicWispMenu.PLAYER_INVENTORY_YPOS + PLAYER_INVENTORY.height;
    }

    @Override
    public void onPress(Button pressedButton)
    {
        for(int i = 0; i < tabs.size(); ++i)
        {
            if(tabs.get(i) == pressedButton)
            {
                LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer( new CContainerTabSelected( this.menu.containerId, i ));
                break;
            }
        }
    }

    //render

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        UpdateButtons();

        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY)
    {
        final float LABEL_XPOS = 5;
        final float FONT_Y_SPACING = 12;

        final float PLAYER_INV_LABEL_YPOS = BasicWispMenu.PLAYER_INVENTORY_YPOS - FONT_Y_SPACING;
        font.draw(matrixStack, playerInventoryTitle,                  ///    this.font.drawString
                LABEL_XPOS, PLAYER_INV_LABEL_YPOS, Color.darkGray.getRGB());

        super.renderLabels(matrixStack, mouseX, mouseY);
    }


    //drawGuiContainerBackgroundLayer
    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_PARTS_TEXTURE);

        PlayerInventorySubMenu playerContainer = menu.GetPlayerInventorySubMenu();

        this.fillGradient(matrixStack, leftPos, topPos, leftPos + this.imageWidth, topPos + this.imageHeight, BG_COLOUR, BG_COLOUR);
        RenderPlayerInv(matrixStack, playerContainer.GetXPos(), playerContainer.GetYPos());

        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);

        RenderBorder(this, matrixStack, leftPos, topPos, getBlitOffset(), this.imageWidth, this.imageHeight);
    }

    //Setup GUI
    @Override
    protected void init()
    {

        tabs = new ArrayList<>();

        //add the tabs from right to left to ensure that tooltips render over correctly

       for(int i = 0; i < EnhancementTypes.NUM; ++i)
        {
            final int j = EnhancementTypes.NUM - 1 - i;
            tabs.add(addRenderableWidget( new TabButton(0, 0, 32, 20, new TextComponent(""), this,
                    (Button button, PoseStack matrix, int mouseX, int mouseY)->this.renderTooltip(matrix, new TranslatableComponent(EnhancementTypes.values()[j].GetName()), mouseX, mouseY))));
        }
       tabs.add(addRenderableWidget( new TabButton(0, 0, 32, 20, new TextComponent(""), this,
            (Button button, PoseStack matrix, int mouseX, int mouseY)->this.renderTooltip(matrix, new TranslatableComponent("logwow.wisp_contents"), mouseX, mouseY)) ));

        Collections.reverse(tabs);

        super.init();

        UpdateButtons();
    }

    private void UpdateButtons()
    {
        int xPos = leftPos;
        for(int i = 0; i < tabs.size(); ++i)
        {
            Button button = tabs.get(i);
            if(menu.IsTabActive(i))
            {
                button.visible = true;
                button.active = !menu.IsTabSelected(i);

                button.x = xPos;
                button.y = topPos - button.getHeight();

                xPos += button.getWidth();
            }
            else
            {
                button.visible = false;
                button.active = false;
            }
        }
    }

}
