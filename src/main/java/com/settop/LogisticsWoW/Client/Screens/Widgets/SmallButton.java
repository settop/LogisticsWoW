package com.settop.LogisticsWoW.Client.Screens.Widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public abstract class SmallButton extends Button
{
    public SmallButton(int x, int y, Component title)
    {
        super(x, y, 8, 8, title, null);
    }

    public SmallButton(int x, int y, Component title, Button.OnPress pressedAction)
    {
        super(x, y, 8, 8, title, pressedAction);
    }

    public SmallButton(int x, int y, Component title, Button.OnPress pressedAction, Button.OnTooltip onTooltip)
    {
        super(x, y, 8, 8, title, pressedAction, onTooltip);
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        matrixStack.pushPose();
        //have to deal with the backwards way minecraft handles matrices :/
        //so need to translate first
        matrixStack.translate(this.x, this.y, 0.f);
        matrixStack.scale(0.25f, 0.25f, 1.f);

        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.setShaderTexture(0, MultiScreen.GUI_PARTS_TEXTURE);

        MultiScreen.GuiPart buttonPart = isHoveredOrFocused() ? MultiScreen.BUTTON_HOVERED : MultiScreen.BUTTON;

        blit(matrixStack, 0, 0, buttonPart.uStart, buttonPart.vStart, buttonPart.width, buttonPart.height );
        RenderOverlay(matrixStack, mouseX, mouseY, partialTicks);

        matrixStack.popPose();

        if (this.isHoveredOrFocused())
        {
            this.renderToolTip(matrixStack, mouseX, mouseY);
        }
    }

    public abstract void RenderOverlay(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks);
}
