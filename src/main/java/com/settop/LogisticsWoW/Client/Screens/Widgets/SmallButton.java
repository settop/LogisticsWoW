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
    private final float scale;

    public SmallButton(int x, int y, int size, Component title)
    {
        super(x, y, size, size, title, null);
        scale = size / 32.f;
    }

    public SmallButton(int x, int y, int size, Component title, Button.OnPress pressedAction)
    {
        super(x, y, size, size, title, pressedAction);
        scale = size / 32.f;
    }

    public SmallButton(int x, int y, int size, Component title, Button.OnPress pressedAction, Button.OnTooltip onTooltip)
    {
        super(x, y, size, size, title, pressedAction, onTooltip);
        scale = size / 32.f;
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        matrixStack.pushPose();
        matrixStack.translate(this.x, this.y, 0.f);
        matrixStack.scale(scale, scale, 1.f);

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
