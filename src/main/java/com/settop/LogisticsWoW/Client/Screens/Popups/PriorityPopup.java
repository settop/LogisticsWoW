package com.settop.LogisticsWoW.Client.Screens.Popups;

import com.mojang.blaze3d.vertex.PoseStack;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.Client.Screens.Widgets.NumberSpinner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;

public abstract class PriorityPopup extends ScreenPopup
{
    public class PrioritySetter extends NumberSpinner
    {
        public PrioritySetter(Font font, int x, int y, int width, int height)
        {
            super(font, true, x, y, width, height);
        }

        @Override
        public void ValueChanged(int value)
        {
            PriorityChanged(value);
        }

        @Override
        public void updateNarration(@NotNull NarrationElementOutput narrationElement)
        {
        }
    }

    public static final int Width = 100;
    public static final int Height = 32;

    private final PrioritySetter prioritySetter;

    public PriorityPopup(int x, int y)
    {
        super(x, y, Width, Height);

        prioritySetter = new PrioritySetter(Minecraft.getInstance().font, x + 2, y + 18, Width - 4, 10);
        AddListener(prioritySetter);
    }

    public void SetValue(int value)
    {
        prioritySetter.SetValue(value);
    }

    public abstract void PriorityChanged(int value);

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.fillGradient(matrixStack, x, y, x + width, y + height, MultiScreen.BG_COLOUR, MultiScreen.BG_COLOUR);
        matrixStack.pushPose();
        matrixStack.translate(0, 0, getBlitOffset() + 1);
        prioritySetter.render(matrixStack, mouseX, mouseY, partialTicks);
        matrixStack.popPose();
        MultiScreen.RenderBorder(this, matrixStack, x, y, getBlitOffset(), width, height);
    }

    @Override
    public @NotNull NarrationPriority narrationPriority()
    {
        return NarrationPriority.FOCUSED;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput p_169152_)
    {

    }
}
