package com.settop.LogisticsWoW.Client.Screens.Popups;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

import java.util.ArrayList;
import java.util.List;

public abstract class ScreenPopup extends AbstractContainerEventHandler implements Widget, NarratableEntry
{
    public final int x, y;
    public final int width, height;
    private ArrayList<GuiEventListener> guiEventListeners;

    public ScreenPopup(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        guiEventListeners = new ArrayList<>();
    }

    @Override
    public List<? extends GuiEventListener> children()
    {
        return guiEventListeners;
    }


    public <T extends GuiEventListener> T AddListener(T listener)
    {
        guiEventListeners.add(listener);
        return listener;
    }

    //public abstract void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks);

    @Override
    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return x < mouseX && mouseX < x + width &&
                y < mouseY && mouseY < y + height;
    }

    public void OnOpen() {}
    public void OnClose() {}
}
