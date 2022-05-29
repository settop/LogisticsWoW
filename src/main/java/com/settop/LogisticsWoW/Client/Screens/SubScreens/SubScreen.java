package com.settop.LogisticsWoW.Client.Screens.SubScreens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.GUI.SubMenus.SubMenu;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public abstract class SubScreen extends GuiComponent
{
    protected int guiLeft;
    protected int guiTop;

    private SubMenu container;
    private MultiScreen<?> parentScreen;
    private List<AbstractWidget> widgets;
    protected boolean active = false;

    protected SubScreen(SubMenu container, MultiScreen<?> parentScreen)
    {
        this.container = container;
        this.parentScreen = parentScreen;
        widgets = new ArrayList<>();
    }

    public void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {}
    public void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {}
    public abstract void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks);
    public void init(int guiLeft, int guiTop)
    {
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
    }
    public void onClose() {}

    public SubMenu GetSubContainer() { return container; }
    public MultiScreen<?> GetParentScreen() { return parentScreen; }

    protected <T extends AbstractWidget> T AddWidget(T button)
    {
        widgets.add(button);
        return button;
    }

    public List<AbstractWidget> GetWidgets() { return widgets; }

    public void SetActive(boolean active)
    {
        if(this.active != active)
        {
            this.active = active;
            for(AbstractWidget widget : GetWidgets())
            {
                widget.active = active;
                widget.visible = active;
            }
        }
    }
}
