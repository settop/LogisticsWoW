package com.settop.LogisticsWoW.Client.Screens.SubScreens;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.settop.LogisticsWoW.Client.Screens.Popups.PriorityPopup;
import com.settop.LogisticsWoW.Client.Screens.Widgets.NumberSpinner;
import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.settop.LogisticsWoW.Client.Client;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.Client.Screens.Popups.SideSelectionPopup;
import com.settop.LogisticsWoW.Client.Screens.Widgets.SmallButton;
import com.settop.LogisticsWoW.Client.Screens.Widgets.TagListSelection;
import com.settop.LogisticsWoW.GUI.FakeSlot;
import com.settop.LogisticsWoW.GUI.Network.Packets.CSubWindowPropertyUpdatePacket;
import com.settop.LogisticsWoW.GUI.Network.Packets.CSubWindowStringPropertyUpdatePacket;
import com.settop.LogisticsWoW.GUI.SubMenus.StorageEnhancementSubMenu;
import com.settop.LogisticsWoW.Wisps.Enhancements.StorageEnhancement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@OnlyIn(Dist.CLIENT)
public class StorageSubScreen extends SubScreen
{
    public class PriorityButton extends SmallButton
    {
        private int lastTextWidth = 8;

        public PriorityButton(int x, int y)
        {
            super(x, y, 16, new TextComponent(""));
        }

        @Override
        public void onPress()
        {
            StorageEnhancementSubMenu providerContainer = (StorageEnhancementSubMenu)GetSubContainer();
            priorityPopup.SetValue(providerContainer.GetPriority());
            GetParentScreen().OpenPopup(priorityPopup);
        }

        @Override
        public void renderToolTip(PoseStack matrixStack, int mouseX, int mouseY)
        {
            GetParentScreen().renderTooltip(matrixStack, new TranslatableComponent("logwow.priority"), mouseX, mouseY);
        }

        @Override
        public void RenderOverlay(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            StorageEnhancementSubMenu providerContainer = (StorageEnhancementSubMenu)GetSubContainer();
            String priorityString = String.valueOf(providerContainer.GetPriority());

            matrixStack.pushPose();
            //the button size before the scale
            final int SIZE = 32;
            final int TextHeight = Minecraft.getInstance().font.lineHeight;
            //margin of 2 each side
            float xSpace = SIZE - 6;
            float scale = Math.min( xSpace / lastTextWidth, xSpace / TextHeight);
            float x = (SIZE - lastTextWidth * scale) * 0.5f;
            float y = (SIZE / 2.f) - (TextHeight * scale / 2.f);
            matrixStack.translate(x, y, 0);
            matrixStack.scale(scale, scale, 1);

            lastTextWidth = Minecraft.getInstance().font.draw(matrixStack, priorityString, 0, 0, 0x0f0f0f);
            matrixStack.popPose();
        }
    }

    public class IsDefaultToggle extends SmallButton
    {
        public IsDefaultToggle(int x, int y)
        {
            super(x, y, 16, new TextComponent(""));
        }

        @Override
        public void onPress()
        {
            StorageEnhancementSubMenu providerContainer = (StorageEnhancementSubMenu)GetSubContainer();
            boolean newEnabled = !providerContainer.GetIsDefaultStore();
            providerContainer.SetIsDefaultStore(newEnabled);

            LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubWindowPropertyUpdatePacket(GetParentScreen().getMenu().containerId, GetSubContainer().GetSubWindowID(), StorageEnhancementSubMenu.IS_DEFAULT_STORE_PROPERTY_ID,  newEnabled ? 1 : 0));

            filterTypeCycle.active = filterTypeCycle.visible = !providerContainer.GetIsDefaultStore();
            tagSelection.active = tagSelection.visible = !providerContainer.GetIsDefaultStore();
        }

        @Override
        public void renderToolTip(PoseStack matrixStack, int mouseX, int mouseY)
        {
            StorageEnhancementSubMenu providerContainer = (StorageEnhancementSubMenu)GetSubContainer();
            GetParentScreen().renderTooltip(matrixStack, new TranslatableComponent(providerContainer.GetIsDefaultStore() ? "logwow.default_store" : "logwow.non_default_store"), mouseX, mouseY);
        }

        @Override
        public void RenderOverlay(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
        }
    }

    public class FilterTypeCycle extends SmallButton
    {
        public FilterTypeCycle(int x, int y)
        {
            super(x, y, 16, null);
        }

        @Override
        public void onPress()
        {
            StorageEnhancementSubMenu providerContainer = (StorageEnhancementSubMenu)GetSubContainer();

            int nextValue = providerContainer.GetFilterType().ordinal() + 1;
            if(nextValue >= StorageEnhancement.eFilterType.values().length)
            {
                nextValue = 0;
            }
            providerContainer.SetFilterType(StorageEnhancement.eFilterType.values()[nextValue]);
            LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubWindowPropertyUpdatePacket(GetParentScreen().getMenu().containerId, GetSubContainer().GetSubWindowID(), StorageEnhancementSubMenu.FILTER_TYPE_PROPERTY_ID,  nextValue));
        }

        @Override
        public void renderToolTip(PoseStack matrixStack, int mouseX, int mouseY)
        {
            StorageEnhancementSubMenu providerContainer = (StorageEnhancementSubMenu)GetSubContainer();
            TranslatableComponent text = switch (providerContainer.GetFilterType())
            {
                case Item -> new TranslatableComponent("logwow.item_filter");
                case Tag -> new TranslatableComponent("logwow.tag_filter");
            };
            GetParentScreen().renderTooltip(matrixStack, text, mouseX, mouseY);
        }

        @Override
        public void RenderOverlay(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            StorageEnhancementSubMenu providerContainer = (StorageEnhancementSubMenu)GetSubContainer();
            Item renderItem = switch (providerContainer.GetFilterType())
            {
                case Item -> Items.IRON_INGOT;
                case Tag -> Items.NAME_TAG;
            };
            ItemStack itemStack = new ItemStack(renderItem);
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

            matrixStack.pushPose();
            matrixStack.translate(16, 16, GetParentScreen().getBlitOffset() + 100);
            matrixStack.scale(1.0F, -1.0F, 1.0F);
            matrixStack.scale(24.0F, 24.0F, 24.0F);

            MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
            BakedModel model = itemRenderer.getModel(itemStack, null, null, 0);
            boolean useFlatLighting = !model.usesBlockLight();

            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (useFlatLighting)
            {
                Lighting.setupForFlatItems();
            }

            itemRenderer.render(itemStack, ItemTransforms.TransformType.GUI, false, matrixStack, multibuffersource$buffersource, 15728880, OverlayTexture.NO_OVERLAY, model);

            multibuffersource$buffersource.endBatch();
            if (useFlatLighting)
            {
                Lighting.setupFor3DItems();
            }

            matrixStack.popPose();
        }
    }

    public class FilterTags extends TagListSelection
    {

        public FilterTags(Font font, int x, int y, int width, int height, TextComponent title, FakeSlot tagFetchSlot)
        {
            super(font, x, y, width, height, title, tagFetchSlot);
        }

        @Override
        protected ArrayList<String> GetTagList()
        {
            StorageEnhancementSubMenu providerContainer = (StorageEnhancementSubMenu)GetSubContainer();
            return providerContainer.GetFilterTags();
        }

        @Override
        protected void UpdateTagList(ArrayList<String> updatedList)
        {
            StorageEnhancementSubMenu providerContainer = (StorageEnhancementSubMenu)GetSubContainer();
            String tagList = providerContainer.SetFilterTags(updatedList);
            LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubWindowStringPropertyUpdatePacket(GetParentScreen().getMenu().containerId, GetSubContainer().GetSubWindowID(), StorageEnhancementSubMenu.FILTER_TAGS_STRING_PROPERTY_ID,  tagList));

        }

        @Override
        public void updateNarration(NarrationElementOutput narration)
        {
            narration.add(NarratedElementType.TITLE, getMessage());
        }
    }

    private PriorityPopup priorityPopup;
    private PriorityButton priorityButton;
    private IsDefaultToggle isDefaultToggle;
    private FilterTypeCycle filterTypeCycle;
    private FilterTags tagSelection;


    public StorageSubScreen(StorageEnhancementSubMenu container, MultiScreen<?> parentScreen)
    {
        super(container, parentScreen);
    }

    @Override
    public void init(int guiLeft, int guiTop)
    {
        super.init(guiLeft, guiTop);

        int xPos = GetSubContainer().GetXPos();
        int yPos = GetSubContainer().GetYPos();

        StorageEnhancementSubMenu providerContainer = (StorageEnhancementSubMenu)GetSubContainer();

        int tagSelectionXOffset = xPos + 21;
        tagSelection = AddWidget(new FilterTags
                (
                        Minecraft.getInstance().font,
                        guiLeft + tagSelectionXOffset, guiTop + yPos,
                        GetParentScreen().getXSize() - tagSelectionXOffset, 80,
                        new TextComponent(""),
                        providerContainer.GetTagFetchHelperSlot()
                ));

        priorityPopup = new PriorityPopup(guiLeft + xPos + 16, guiTop + yPos + 16)
        {
            @Override
            public void PriorityChanged(int value)
            {
                providerContainer.SetPriority(value);
            }

            @Override
            public void OnClose()
            {
                LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubWindowPropertyUpdatePacket(GetParentScreen().getMenu().containerId, GetSubContainer().GetSubWindowID(), StorageEnhancementSubMenu.PRIORITY_PROPERTY_ID, providerContainer.GetPriority()));
            }
        };

        priorityButton = AddWidget(new PriorityButton(guiLeft + xPos, guiTop + yPos ));
        isDefaultToggle = AddWidget(new IsDefaultToggle(guiLeft + xPos, guiTop + yPos + 16));
        filterTypeCycle = AddWidget(new FilterTypeCycle(guiLeft + xPos, guiTop + yPos + 32));
    }

    @Override
    public void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY)
    {
        StorageEnhancementSubMenu providerContainer = (StorageEnhancementSubMenu)GetSubContainer();
        if(!providerContainer.GetIsDefaultStore())
        {
            for(int r = 0; r < StorageEnhancement.FILTER_NUM_ROWS; ++r)
            {
                MultiScreen.RenderSlotRowBackground(this, matrixStack, guiLeft + StorageEnhancementSubMenu.FILTER_SLOT_X, guiTop + StorageEnhancementSubMenu.FILTER_SLOT_Y + r * Client.SLOT_Y_SPACING, getBlitOffset(), StorageEnhancement.FILTER_NUM_COLUMNS);
            }

            if(providerContainer.GetFilterType() == StorageEnhancement.eFilterType.Tag)
            {
                MultiScreen.RenderSlotRowBackground(this, matrixStack, guiLeft + StorageEnhancementSubMenu.TAG_FETCH_HELPER_SLOT_X, guiTop + StorageEnhancementSubMenu.TAG_FETCH_HELPER_SLOT_Y, getBlitOffset(), 1);
            }
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
    }

    @Override
    public void SetActive(boolean active)
    {
        StorageEnhancementSubMenu providerContainer = (StorageEnhancementSubMenu)GetSubContainer();
        if(this.active != active)
        {
            this.active = active;

            priorityButton.active = priorityButton.visible = active;
            isDefaultToggle.active = isDefaultToggle.visible = active;
            filterTypeCycle.active = filterTypeCycle.visible = active && !providerContainer.GetIsDefaultStore();
        }

        tagSelection.active = tagSelection.visible = active && (providerContainer.GetFilterType() == StorageEnhancement.eFilterType.Tag) && !providerContainer.GetIsDefaultStore();
    }
}
