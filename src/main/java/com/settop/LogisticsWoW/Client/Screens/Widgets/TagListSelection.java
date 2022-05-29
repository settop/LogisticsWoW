package com.settop.LogisticsWoW.Client.Screens.Widgets;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.apache.commons.lang3.StringUtils;
import com.settop.LogisticsWoW.GUI.FakeSlot;
import com.settop.LogisticsWoW.Utils.FakeInventory;

import java.util.ArrayList;


public abstract class TagListSelection extends AbstractWidget
{
    public static final int LINE_HEIGHT = 10;
    public static final int DEFAULT_TEXT_COLOUR = 0xE0E0E0;
    public static final int SUGGESTED_TEXT_COLOUR = 0x707070;
    public static final int TEXT_REGION_BORDER_SIZE = 4;
    public static final int POSSIBLE_TAG_GAP = 4;

    protected EditBox textEntry;

    protected final Font font;
    protected int fullHeight;
    protected float tagEntryFailedTimer = 0.f;
    private int selectedTagIndex = -1;
    private FakeSlot itemTagFetchSlot;
    private ItemStack previousTagFetchItem;
    private ArrayList<String> possibleTags = new ArrayList<>();

    protected abstract ArrayList<String> GetTagList();
    protected abstract void UpdateTagList(ArrayList<String> updatedList);

    public TagListSelection(Font font, int x, int y, int width, int height, Component title, FakeSlot tagFetchSlot)
    {
        super(x, y, width, height, title);
        textEntry = new EditBox(font, x, y, width, LINE_HEIGHT, title);
        this.font = font;
        fullHeight = height;

        itemTagFetchSlot = tagFetchSlot;
    }

    public static boolean IsValidTag(String str)
    {
        String[] splitString = StringUtils.split(str, ':');

        if(splitString.length != 2)
        {
            return false;
        }

        try
        {
            ResourceLocation tagId = new ResourceLocation(splitString[0], splitString[1]);
            return ForgeRegistries.ITEMS.tags().isKnownTagName(ItemTags.create(tagId));
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers)
    {
        return textEntry.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if(!this.visible || !this.active)
        {
            return false;
        }

        if(textEntry.mouseClicked(mouseX, mouseY, button))
        {
            return true;
        }

        ArrayList<String> tagList = GetTagList();

        int listXPos = this.x + TEXT_REGION_BORDER_SIZE;

        int listYStartPossibleTags = this.y + LINE_HEIGHT + TEXT_REGION_BORDER_SIZE;
        int listYEndPossibleTags = listYStartPossibleTags + possibleTags.size() * LINE_HEIGHT;
        int listYStartTagList = listYEndPossibleTags + (possibleTags.isEmpty() ? 0 : POSSIBLE_TAG_GAP);
        int listYEndTagList = listYStartTagList + tagList.size() * LINE_HEIGHT;

        int xEnd = listXPos + textEntry.getInnerWidth();
        int yEndMax = this.y + fullHeight - TEXT_REGION_BORDER_SIZE;

        if(listXPos <= mouseX && mouseX <= xEnd &&
                listYStartPossibleTags <= mouseY && mouseY <= yEndMax)
        {
            if(listYStartPossibleTags <= mouseY && mouseY < listYEndPossibleTags)
            {
                int selectedPossibleTag = (int)((mouseY - listYStartPossibleTags) / LINE_HEIGHT);
                if(selectedPossibleTag >= 0 && selectedPossibleTag < possibleTags.size())
                {
                    tagList.add(possibleTags.get(selectedPossibleTag));
                    possibleTags.remove(selectedPossibleTag);
                    UpdateTagList(tagList);
                }
                selectedTagIndex = -1;
                textEntry.setValue("");
                return true;
            }
            else if(listYStartTagList <= mouseY && mouseY < listYEndTagList)
            {
                int newlySelectedTagIndex = (int)((mouseY - listYStartTagList) / LINE_HEIGHT);
                if(newlySelectedTagIndex != selectedTagIndex && newlySelectedTagIndex >= 0 && newlySelectedTagIndex < tagList.size())
                {
                    selectedTagIndex = newlySelectedTagIndex;
                    textEntry.setValue(tagList.get(selectedTagIndex));
                }
                else
                {
                    selectedTagIndex = -1;
                    textEntry.setValue("");
                }
            }
            return true;
        }
        else
        {
            selectedTagIndex = -1;
            textEntry.setValue("");
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        boolean consumeKey = false;
        if(textEntry.canConsumeInput())
        {
            if(keyCode >= 65 && keyCode <= 90)
            {
                //make sure that any keybinds on letters don't do anything while text is focused
                consumeKey = true;
            }
            else if(keyCode == 257)
            {
                //enter pressed
                consumeKey = true;

                String tag = textEntry.getValue();
                if(IsValidTag(tag))
                {
                    ArrayList<String> tagList = GetTagList();
                    if(selectedTagIndex >= 0 && selectedTagIndex < tagList.size())
                    {
                        tagList.set(selectedTagIndex, tag);
                        selectedTagIndex = -1;
                    }
                    else
                    {
                        tagList.add(tag);
                    }
                    textEntry.setValue("");
                    UpdateTagList(tagList);
                }
                else
                {
                    tagEntryFailedTimer = 1.5f;
                }
            }
        }
        else if(keyCode == 261 && selectedTagIndex != -1)
        {
            ArrayList<String> tagList = GetTagList();
            tagList.remove(selectedTagIndex);
            selectedTagIndex = -1;
            textEntry.setValue("");
            UpdateTagList(tagList);
            return true;
        }
        return textEntry.keyPressed(keyCode, scanCode, modifiers) || consumeKey;
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        if (!visible)
        {
            return;
        }

        tagEntryFailedTimer -= (partialTicks / 20.f);
        if(tagEntryFailedTimer > 0.f)
        {
            int failedColour = FastColor.ARGB32.color(255, 224, 0, 0);
            if(tagEntryFailedTimer > 0.5f)
            {
                textEntry.setTextColor(failedColour);
            }
            else
            {
                //fade back to normal
                float blend = tagEntryFailedTimer / 0.5f;
                int r = (int)(FastColor.ARGB32.red(DEFAULT_TEXT_COLOUR) * (1.f - blend) + FastColor.ARGB32.red(failedColour) * blend);
                int g = (int)(FastColor.ARGB32.green(DEFAULT_TEXT_COLOUR) * (1.f - blend) + FastColor.ARGB32.green(failedColour) * blend);
                int b = (int)(FastColor.ARGB32.blue(DEFAULT_TEXT_COLOUR) * (1.f - blend) + FastColor.ARGB32.blue(failedColour) * blend);

                textEntry.setTextColor(FastColor.ARGB32.color(255, r, g, b));
            }
        }
        else
        {
            textEntry.setTextColor(DEFAULT_TEXT_COLOUR);
        }

        fill(matrixStack, this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.fullHeight + 1, -6250336);
        fill(matrixStack, this.x, this.y, this.x + this.width, this.y + this.fullHeight, -16777216);

        ArrayList<String> tagList = GetTagList();

        int listXPos = this.x + TEXT_REGION_BORDER_SIZE;
        int listYPos = this.y + LINE_HEIGHT + TEXT_REGION_BORDER_SIZE;

        int xEnd = listXPos + textEntry.getInnerWidth();
        int yEnd = listYPos + fullHeight - LINE_HEIGHT - 2 * TEXT_REGION_BORDER_SIZE;

        Window mainWindow = Minecraft.getInstance().getWindow();
        double scaleFactor = mainWindow.getGuiScale();

        int scissorWidth = (int)(textEntry.getInnerWidth() * scaleFactor);
        int scissorHeight = (int)((fullHeight - LINE_HEIGHT - 2 * TEXT_REGION_BORDER_SIZE) * scaleFactor);
        int scissorX = (int)(listXPos * scaleFactor);
        int scissorY = mainWindow.getScreenHeight() - (int)(listYPos * scaleFactor) - scissorHeight;
        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);

        ItemStack tagFetchItem = itemTagFetchSlot != null ? itemTagFetchSlot.getItem() : null;
        if(previousTagFetchItem != tagFetchItem)
        {
            previousTagFetchItem = tagFetchItem;
            possibleTags.clear();
            if(tagFetchItem != null)
            {
                tagFetchItem.getTags().forEach((TagKey<Item> tag)->
                {
                    String tagStr = tag.location().getNamespace() + ':' + tag.location().getPath();
                    if(!tagList.contains(tagStr))
                    {
                        possibleTags.add(tagStr);
                    }
                });
            }
        }

        if(!possibleTags.isEmpty())
        {
            for(String possibleTag : possibleTags)
            {
                if(listXPos <= mouseX && mouseX <= xEnd &&
                        listYPos <= mouseY && mouseY <= listYPos + LINE_HEIGHT)
                {
                    fill(matrixStack, listXPos, listYPos, xEnd, listYPos + LINE_HEIGHT, FastColor.ARGB32.color(128, 224, 224, 224));
                }
                this.font.draw(matrixStack, FormattedCharSequence.forward(possibleTag, Style.EMPTY), (float)listXPos, (float)listYPos, SUGGESTED_TEXT_COLOUR);
                listYPos += LINE_HEIGHT;
            }
            int finalOffset = POSSIBLE_TAG_GAP + LINE_HEIGHT;
            listYPos += POSSIBLE_TAG_GAP;
            listYPos -= finalOffset / 2;
            this.font.draw(matrixStack, FormattedCharSequence.forward("-----------------------------------", Style.EMPTY), (float)listXPos, (float)listYPos, DEFAULT_TEXT_COLOUR);
            listYPos += finalOffset / 2;
        }

        for(int i = 0; i < tagList.size(); ++i)
        {
            if(i == selectedTagIndex)
            {
                fill(matrixStack, listXPos, listYPos, xEnd, listYPos + LINE_HEIGHT, FastColor.ARGB32.color(192, 224, 224, 224));
            }
            else if(listXPos <= mouseX && mouseX <= xEnd &&
                    listYPos <= mouseY && mouseY <= listYPos + LINE_HEIGHT)
            {
                fill(matrixStack, listXPos, listYPos, xEnd, listYPos + LINE_HEIGHT, FastColor.ARGB32.color(128, 224, 224, 224));
            }


            this.font.draw(matrixStack, FormattedCharSequence.forward(tagList.get(i), Style.EMPTY), (float)listXPos, (float)listYPos, DEFAULT_TEXT_COLOUR);
            listYPos += LINE_HEIGHT;
        }

        RenderSystem.disableScissor();

        textEntry.renderButton(matrixStack, mouseX, mouseY, partialTicks);
    }
}
