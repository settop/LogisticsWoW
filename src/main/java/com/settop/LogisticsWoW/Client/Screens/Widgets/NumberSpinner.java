package com.settop.LogisticsWoW.Client.Screens.Widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public abstract class NumberSpinner extends AbstractWidget
{
    public static final int LINE_HEIGHT = 10;

    private final int min;
    private final int max;
    private final boolean alwaysEditable;
    private boolean clickedOnce = false;
    private float doubleClickTimer = 0.f;

    private final EditBox textBox;
    private boolean editing = false;

    public NumberSpinner(Font font, boolean alwaysEditable, int x, int y, int width, int height)
    {
        this(font, alwaysEditable, x, y, width, height, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    public NumberSpinner(Font font, boolean alwaysEditable, int x, int y, int width, int height, int min, int max)
    {
        super(x, y, width, height, new TextComponent(""));
        this.min = min;
        this.max = max;
        this.alwaysEditable = alwaysEditable;

        textBox = new EditBox(font, x, y, width, LINE_HEIGHT, getMessage());
        textBox.setValue("0");
        textBox.setEditable(alwaysEditable);
        textBox.setFilter(str->
        {
            try
            {
                if(str.isEmpty() || str.equals("-"))
                {
                    return true;
                }
                int v = Integer.parseInt(str);
                return min <= v && v <= max;
            }
            catch (NumberFormatException ex)
            {
                return false;
            }
        });
        textBox.setResponder(str->
        {
            try
            {
                int v = Integer.parseInt(str);
                ValueChanged(v);
                textBox.setEditable(alwaysEditable);
                editing = false;
            }
            catch (NumberFormatException ignored)
            {
                ValueChanged(0);
            }
        });
    }

    public abstract void ValueChanged(int value);

    public void SetValue(int v)
    {
        textBox.setValue(String.valueOf(v));
    }
    public int GetValue()
    {
        try
        {
            int v = Integer.parseInt(textBox.getValue());
            return v;
        }
        catch (NumberFormatException ignored)
        {
            return 0;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if(!this.visible || !this.active)
        {
            return false;
        }

        if(!alwaysEditable && !editing && button == 0)
        {
            if(clickedOnce)
            {
                //double clicked
                textBox.setEditable(true);
                editing = true;
            }
            else
            {
                clickedOnce = true;
                doubleClickTimer = 0.f;
            }
            return true;
        }
        return textBox.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll)
    {
        int valueChange = (int)(Screen.hasShiftDown() ? 10 * scroll : scroll);
        int newValue = GetValue() + valueChange;
        SetValue(newValue);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        return textBox.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers)
    {
        return textBox.charTyped(codePoint, modifiers);
    }

    @Override
    public void renderButton(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        doubleClickTimer += partialTicks;
        if(doubleClickTimer > 10.f)
        {
            //double click time if half a second
            clickedOnce = false;
        }

        if (!visible)
        {
            return;
        }

        textBox.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
