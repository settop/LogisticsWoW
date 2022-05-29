package com.settop.LogisticsWoW.Client.Screens.SubScreens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;
import net.minecraft.world.inventory.Slot;

import java.awt.*;

public class WispContentsSubScreen extends SubScreen
{
    public WispContentsSubScreen(SubMenu container, MultiScreen<?> parentScreen)
    {
        super(container, parentScreen);
    }
    @Override
    public void init(int guiLeft, int guiTop)
    {
        super.init(guiLeft, guiTop);
    }

    @Override
    public void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Slot baseSlot = GetSubContainer().inventorySlots.get(0);
        MultiScreen.RenderSlotRowBackground(this, matrixStack, guiLeft + baseSlot.x, guiTop + baseSlot.y, getBlitOffset(), GetSubContainer().inventorySlots.size());
    }

    @Override
    public void renderLabels(PoseStack matrixStack, int mouseX, int mouseY)
    {
        final float LABEL_XPOS = 5;
        final float LABEL_YPOS = 4;

        Minecraft.getInstance().font.draw(matrixStack, GetParentScreen().getTitle(),
                LABEL_XPOS, LABEL_YPOS, Color.darkGray.getRGB());  //this.font.drawString;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
    }
}
