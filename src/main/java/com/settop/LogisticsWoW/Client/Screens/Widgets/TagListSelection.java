package com.settop.LogisticsWoW.Client.Screens.Widgets;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.GUI.Network.Packets.CSubWindowPropertyUpdatePacket;
import com.settop.LogisticsWoW.GUI.SubMenus.ProviderEnhancementSubMenu;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.tags.ITagManager;
import org.apache.commons.lang3.StringUtils;
import com.settop.LogisticsWoW.GUI.FakeSlot;
import com.settop.LogisticsWoW.Utils.FakeInventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;


public abstract class TagListSelection extends AbstractWidget
{
    private class ArrowButton extends SmallButton
    {
        private final boolean isDownArrow;
        public ArrowButton(int x, int y, boolean down)
        {
            super(x, y, new TextComponent(""));
            isDownArrow = down;
        }

        @Override
        public void onPress()
        {
            if(isDownArrow)
            {
                scrollAmount += SCROLL_SCALE * 2;
            }
            else
            {
                scrollAmount -= SCROLL_SCALE * 2;
            }
        }

        @Override
        public void renderToolTip(@NotNull PoseStack matrixStack, int mouseX, int mouseY){}

        @Override
        public void RenderOverlay(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            MultiScreen.GuiPart overlayPart = MultiScreen.OVERLAY_ARROW;
            if(isDownArrow)
            {
                matrixStack.pushPose();
                matrixStack.translate(overlayPart.width, overlayPart.height, 0 );
                matrixStack.scale(-1.f, -1.f, 1.f);
            }
            blit(matrixStack, 0, 0, overlayPart.uStart, overlayPart.vStart, overlayPart.width, overlayPart.height );
            if(isDownArrow)
            {
                matrixStack.popPose();
            }
        }
    }
    public static final int LINE_HEIGHT = 10;
    public static final int DEFAULT_TEXT_COLOUR = 0xE0E0E0;
    public static final int TAG_TEXT_COLOUR = 0x7070E0;
    public static final int SUGGESTED_TEXT_COLOUR = 0x707070;
    public static final int TEXT_REGION_BORDER_SIZE = 4;
    public static final int POSSIBLE_TAG_GAP = 4;
    public static final float FONT_SCALE = 0.7f;
    public static final float SCROLL_SCALE = 5.f;

    protected EditBox textEntry;

    protected final Font font;
    protected int fullHeight;
    protected float tagEntryFailedTimer = 0.f;
    private int selectedTagIndex = -1;
    private int scrollAmount = 0;
    private final FakeSlot itemTagFetchSlot;
    private ItemStack previousTagFetchItem;
    private final ArrayList<String> possibleTags = new ArrayList<>();
    private ArrayList<String> displayPossibleTags = new ArrayList<>();
    private final ItemRenderer itemRenderer;
    private float tickItemSeedTimer = 0.f;
    private int itemSeed = 1;

    private final ArrowButton[] arrowButtons;

    protected abstract ArrayList<String> GetTagList();
    protected abstract void UpdateTagList(ArrayList<String> updatedList);

    public TagListSelection(Font font, int x, int y, int width, int height, Component title, FakeSlot tagFetchSlot)
    {
        super(x, y, width, height, title);
        textEntry = new EditBox(font, x, y, width, LINE_HEIGHT, title);
        this.font = font;
        fullHeight = height;

        itemTagFetchSlot = tagFetchSlot;
        itemRenderer = Minecraft.getInstance().getItemRenderer();

        TagRegionSize tagRegion = GetTagRegionSize();

        arrowButtons = new ArrowButton[]
                {
                        new ArrowButton(tagRegion.xEnd - 8, tagRegion.yStart, false),
                        new ArrowButton(tagRegion.xEnd - 8, tagRegion.yEnd - 8, true)
                };
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

    private static class TagRegionSize
    {
        int xStart, xEnd;
        int yStart, yEnd;
    }
    private TagRegionSize GetTagRegionSize()
    {
        TagRegionSize regionSize = new TagRegionSize();
        regionSize.xStart = this.x + TEXT_REGION_BORDER_SIZE;
        regionSize.xEnd = regionSize.xStart + textEntry.getInnerWidth();
        regionSize.yStart = this.y + LINE_HEIGHT + TEXT_REGION_BORDER_SIZE;
        regionSize.yEnd = this.y + fullHeight - TEXT_REGION_BORDER_SIZE;
        return regionSize;
    }
    private static class TagRenderYPositions
    {
        int possibleTagsStartY;
        int possibleTagsEndY;
        int tagStartY;
        int tagEndY;
    }
    private TagRenderYPositions GetTagRenderPositions(int startY, int tagListSize, float scale)
    {
        TagRenderYPositions renderPositions = new TagRenderYPositions();
        renderPositions.possibleTagsStartY = startY - (int)(scrollAmount * scale);
        renderPositions.possibleTagsEndY = renderPositions.possibleTagsStartY + (int)((displayPossibleTags.size() * LINE_HEIGHT) * scale);
        renderPositions.tagStartY = renderPositions.possibleTagsEndY + (int)((displayPossibleTags.isEmpty() ? 0 : POSSIBLE_TAG_GAP) * scale);
        renderPositions.tagEndY = renderPositions.tagStartY + (int)((tagListSize * LINE_HEIGHT) * scale);
        return renderPositions;
    }

    private int GetMaxScroll(int tagListSize)
    {
        TagRegionSize tagRegionSize = GetTagRegionSize();
        int scaledYHeight = (int)((tagRegionSize.yEnd - tagRegionSize.yStart) / FONT_SCALE);
        int tagTotalYHeight = displayPossibleTags.size() * LINE_HEIGHT +
                (displayPossibleTags.isEmpty() ? 0 : POSSIBLE_TAG_GAP) +
                tagListSize * LINE_HEIGHT;
        return Math.max(tagTotalYHeight - scaledYHeight, 0);
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

        for(ArrowButton arrowButton : arrowButtons)
        {
            if (arrowButton.active && arrowButton.visible)
            {
                if (arrowButton.mouseClicked(mouseX, mouseY, button))
                {
                    return true;
                }
            }
        }

        ArrayList<String> tagList = GetTagList();
        scrollAmount = Utils.clamp(scrollAmount, 0, GetMaxScroll(tagList.size()));

        TagRegionSize tagRegionSize = GetTagRegionSize();
        TagRenderYPositions tagYRenderPositions = GetTagRenderPositions(tagRegionSize.yStart, tagList.size(), FONT_SCALE);

        if(tagRegionSize.xStart <= mouseX && mouseX <= tagRegionSize.xEnd &&
                tagRegionSize.yStart <= mouseY && mouseY <= tagRegionSize.yEnd)
        {
            if(tagYRenderPositions.possibleTagsStartY <= mouseY && mouseY < tagYRenderPositions.possibleTagsEndY)
            {
                int selectedPossibleTag = (int)((mouseY - tagYRenderPositions.possibleTagsStartY) / (LINE_HEIGHT * FONT_SCALE));
                if(selectedPossibleTag >= 0 && selectedPossibleTag < displayPossibleTags.size())
                {
                    tagList.add(displayPossibleTags.get(selectedPossibleTag));
                    tagList.sort(String::compareToIgnoreCase);
                    displayPossibleTags.remove(selectedPossibleTag);
                    UpdateTagList(tagList);
                }
                selectedTagIndex = -1;
                textEntry.setValue("");
                return true;
            }
            else if(tagYRenderPositions.tagStartY <= mouseY && mouseY < tagYRenderPositions.tagEndY)
            {
                int newlySelectedTagIndex = (int)((mouseY - tagYRenderPositions.tagStartY) / (LINE_HEIGHT * FONT_SCALE));
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll)
    {
        TagRegionSize tagRegionSize = GetTagRegionSize();
        if(tagRegionSize.xStart <= mouseX && mouseX <= tagRegionSize.xEnd &&
                tagRegionSize.yStart <= mouseY && mouseY <= tagRegionSize.yEnd)
        {
            scrollAmount -= (int)(scroll * SCROLL_SCALE);
            return true;
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
                    if(!tagList.contains(tag))
                    {
                        if(selectedTagIndex >= 0 && selectedTagIndex < tagList.size())
                        {
                            tagList.set(selectedTagIndex, tag);
                            selectedTagIndex = -1;
                        }
                        else
                        {
                            tagList.add(tag);
                        }
                        tagList.sort(String::compareToIgnoreCase);
                        UpdateTagList(tagList);
                    }
                    textEntry.setValue("");
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
            String removed = tagList.remove(selectedTagIndex);
            if(possibleTags.contains(removed))
            {
                displayPossibleTags.add(removed);
            }
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

        tickItemSeedTimer += partialTicks;
        if(tickItemSeedTimer > 40.f)
        {
            tickItemSeedTimer -= 40.f;
            ++itemSeed;
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

        TagRegionSize tagRegionSize = GetTagRegionSize();

        int maxScroll = GetMaxScroll(tagList.size());
        scrollAmount = Utils.clamp(scrollAmount, 0, maxScroll);

        Window mainWindow = Minecraft.getInstance().getWindow();
        double scaleFactor = mainWindow.getGuiScale();

        int scissorWidth = (int)((tagRegionSize.xEnd - tagRegionSize.xStart) * scaleFactor);
        int scissorHeight = (int)((tagRegionSize.yEnd - tagRegionSize.yStart) * scaleFactor);
        int scissorX = (int)(tagRegionSize.xStart * scaleFactor);
        int scissorY = mainWindow.getScreenHeight() - (int)(tagRegionSize.yStart * scaleFactor) - scissorHeight;
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
                    possibleTags.add(tagStr);
                });
            }

            displayPossibleTags = (ArrayList<String>) possibleTags.clone();
            displayPossibleTags.removeIf(tagList::contains);
        }

        matrixStack.pushPose();
        matrixStack.translate(tagRegionSize.xStart, tagRegionSize.yStart, 0);
        matrixStack.scale(FONT_SCALE, FONT_SCALE, 1.f);

        renderTags(matrixStack, (int)((mouseX - tagRegionSize.xStart) / FONT_SCALE), (int)((mouseY - tagRegionSize.yStart) / FONT_SCALE), (int)((tagRegionSize.xEnd - tagRegionSize.xStart) / FONT_SCALE), tagList);

        RenderSystem.disableScissor();
        matrixStack.popPose();

        for(ArrowButton arrowButton : arrowButtons)
        {
            arrowButton.active = maxScroll > 0;
            if(arrowButton.active)
            {
                arrowButton.render(matrixStack, mouseX, mouseY, partialTicks);
            }
        }

        textEntry.renderButton(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void renderTags(PoseStack matrixStack, int mouseX, int mouseY, int xWidth, ArrayList<String> tagList)
    {
        int listXPos = 0;
        int listYPos = -scrollAmount;
        ITagManager<Item> itemTagManager = ForgeRegistries.ITEMS.tags();
        assert itemTagManager != null;

        Random rng = new Random(itemSeed);

        final int ItemRenderSize = LINE_HEIGHT;

        if(!displayPossibleTags.isEmpty())
        {
            for(String possibleTag : displayPossibleTags)
            {
                if(tagList.contains(possibleTag))
                {
                    continue;
                }

                boolean highlighted = false;
                if(listXPos <= mouseX && mouseX <= xWidth &&
                        listYPos < mouseY && mouseY <= listYPos + LINE_HEIGHT)
                {
                    highlighted = true;
                    fill(matrixStack, listXPos, listYPos, xWidth, listYPos + LINE_HEIGHT, FastColor.ARGB32.color(128, 224, 224, 224));
                }

                Optional<Item> itemWithTag = itemTagManager.getTag(ItemTags.create(new ResourceLocation(possibleTag))).getRandomElement(rng);
                if(itemWithTag.isPresent())
                {
                    renderItem(matrixStack, itemWithTag.get(), listXPos, listYPos, ItemRenderSize);
                }

                this.font.draw(matrixStack, FormattedCharSequence.forward(possibleTag, Style.EMPTY), (float)listXPos + ItemRenderSize, (float)listYPos, highlighted ? ~SUGGESTED_TEXT_COLOUR : SUGGESTED_TEXT_COLOUR);
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
                fill(matrixStack, listXPos, listYPos, xWidth, listYPos + LINE_HEIGHT, FastColor.ARGB32.color(192, 224, 224, 224));
            }
            else if(listXPos <= mouseX && mouseX <= xWidth &&
                    listYPos < mouseY && mouseY <= listYPos + LINE_HEIGHT)
            {
                fill(matrixStack, listXPos, listYPos, xWidth, listYPos + LINE_HEIGHT, FastColor.ARGB32.color(128, 224, 224, 224));
            }

            Optional<Item> itemWithTag = itemTagManager.getTag(ItemTags.create(new ResourceLocation(tagList.get(i)))).getRandomElement(rng);
            if(itemWithTag.isPresent())
            {
                renderItem(matrixStack, itemWithTag.get(), listXPos, listYPos, ItemRenderSize);
            }


            boolean tagIsOnItem = possibleTags.contains(tagList.get(i));

            int textColour = tagIsOnItem ? TAG_TEXT_COLOUR :DEFAULT_TEXT_COLOUR;
            this.font.draw(matrixStack, FormattedCharSequence.forward(tagList.get(i), Style.EMPTY), (float)listXPos + ItemRenderSize, (float)listYPos, textColour);
            listYPos += LINE_HEIGHT;
        }
    }

    private void renderItem(PoseStack matrixStack, Item item, int x, int y, float scale)
    {
        ItemStack itemStack = new ItemStack(item);
        BakedModel model = itemRenderer.getModel(itemStack, null, null, 0);
        MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
        boolean useFlatLighting = !model.usesBlockLight();

        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (useFlatLighting)
        {
            Lighting.setupForFlatItems();
        }

        matrixStack.pushPose();
        matrixStack.translate(x + scale * 0.5f, y + scale * 0.5f, 100.0F);
        matrixStack.scale(1.0F, -1.0F, 1.0F);
        matrixStack.scale(scale, scale, 1.f);
        itemRenderer.render(itemStack, ItemTransforms.TransformType.GUI, false, matrixStack, multibuffersource$buffersource, 15728880, OverlayTexture.NO_OVERLAY, model);
        multibuffersource$buffersource.endBatch();
        if (useFlatLighting)
        {
            Lighting.setupFor3DItems();
        }
        matrixStack.popPose();
    }
}
