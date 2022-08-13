package com.settop.LogisticsWoW.Client.Screens.SubScreens;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.settop.LogisticsWoW.Client.Client;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.Client.Screens.Widgets.SmallButton;
import com.settop.LogisticsWoW.Client.Screens.Widgets.TagListSelection;
import com.settop.LogisticsWoW.GUI.FakeSlot;
import com.settop.LogisticsWoW.GUI.Network.Packets.CSubWindowPropertyUpdatePacket;
import com.settop.LogisticsWoW.GUI.Network.Packets.CSubWindowStringPropertyUpdatePacket;
import com.settop.LogisticsWoW.GUI.SubMenus.ItemFilterSubMenu;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.Constants;
import com.settop.LogisticsWoW.Utils.ItemFilter;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@OnlyIn(Dist.CLIENT)
public class ItemFilterSubScreen extends SubScreen
{
    public class WhitelistToggle extends SmallButton
    {
        public WhitelistToggle(int x, int y)
        {
            super(x, y, 16, null);
        }

        @Override
        public void onPress()
        {
            ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();

            boolean nextValue = !filterContainer.GetIsWhitelist();
            filterContainer.SetWhitelist(nextValue);
            LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubWindowPropertyUpdatePacket(GetParentScreen().getMenu().containerId, GetSubContainer().GetSubWindowID(), ItemFilterSubMenu.WHITELIST_PROPERTY_ID,  nextValue ? 1 : 0));
        }

        @Override
        public void renderToolTip(@NotNull PoseStack matrixStack, int mouseX, int mouseY)
        {
            ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();
            boolean isWhitelist = filterContainer.GetIsWhitelist();
            GetParentScreen().renderTooltip(matrixStack, new TranslatableComponent(isWhitelist ? "logwow.whitelist" : "logwow.blacklist"), mouseX, mouseY);
        }

        @Override
        public void RenderOverlay(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();
            boolean isWhitelist = filterContainer.GetIsWhitelist();
            MultiScreen.GuiPart overlayPart = isWhitelist ? MultiScreen.OVERLAY_WHITELIST : MultiScreen.OVERLAY_BLACKLIST;
            blit(matrixStack, 0, 0, overlayPart.uStart, overlayPart.vStart, overlayPart.width, overlayPart.height );
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
            ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();

            int nextValue = filterContainer.GetFilterType().ordinal() + 1;
            if(nextValue >= Constants.eFilterType.values().length)
            {
                nextValue = 0;
            }
            filterContainer.SetFilterType(Constants.eFilterType.values()[nextValue]);
            LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubWindowPropertyUpdatePacket(GetParentScreen().getMenu().containerId, GetSubContainer().GetSubWindowID(), ItemFilterSubMenu.FILTER_TYPE_PROPERTY_ID,  nextValue));

            polymorphicFilterButton.active = polymorphicFilterButton.visible = active && filterContainer.GetFilterType() == Constants.eFilterType.Type;
        }

        @Override
        public void renderToolTip(@NotNull PoseStack matrixStack, int mouseX, int mouseY)
        {
            ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();
            TranslatableComponent text = switch (filterContainer.GetFilterType())
                    {
                        case Type -> new TranslatableComponent("logwow.item_filter");
                        case Tag -> new TranslatableComponent("logwow.tag_filter");
                        case Default -> new TranslatableComponent("logwow.default_store");
                    };
            GetParentScreen().renderTooltip(matrixStack, text, mouseX, mouseY);
        }

        @Override
        public void RenderOverlay(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();
            Item renderItem = switch (filterContainer.GetFilterType())
                    {
                        case Type -> Items.IRON_INGOT;
                        case Tag -> Items.NAME_TAG;
                        case Default -> Items.AIR;
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

    public class MatchNBTToggle extends SmallButton
    {
        public MatchNBTToggle(int x, int y)
        {
            super(x, y, 16, null);
        }

        @Override
        public void onPress()
        {
            ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();

            boolean nextValue = !filterContainer.GetMatchNBT();
            filterContainer.SetMatchNBT(nextValue);
            LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubWindowPropertyUpdatePacket(GetParentScreen().getMenu().containerId, GetSubContainer().GetSubWindowID(), ItemFilterSubMenu.MATCH_NBT_PROPERTY_ID,  nextValue ? 1 : 0));
        }

        @Override
        public void renderToolTip(@NotNull PoseStack matrixStack, int mouseX, int mouseY)
        {
            ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();
            boolean matchNBT = filterContainer.GetMatchNBT();
            GetParentScreen().renderTooltip(matrixStack, new TranslatableComponent(matchNBT ? "logwow.matchnbt" : "logwow.nomatchnbt"), mouseX, mouseY);
        }

        @Override
        public void RenderOverlay(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            /*
            ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();
            boolean isWhitelist = filterContainer.GetIsWhitelist();
            MultiScreen.GuiPart overlayPart = isWhitelist ? MultiScreen.OVERLAY_WHITELIST : MultiScreen.OVERLAY_BLACKLIST;
            blit(matrixStack, 0, 0, overlayPart.uStart, overlayPart.vStart, overlayPart.width, overlayPart.height );

             */
        }
    }

    public class PolymorphicFilterButton extends SmallButton
    {
        private int lastTextWidth = 8;

        public PolymorphicFilterButton(int x, int y)
        {
            super(x, y, 16, new TextComponent(""));
        }

        @Override
        public void onPress()
        {
            LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubWindowPropertyUpdatePacket(GetParentScreen().getMenu().containerId, GetSubContainer().GetSubWindowID(), ItemFilterSubMenu.POLYMORPHIC_PROPERTY_ID,  1));
        }

        @Override
        public void renderToolTip(@NotNull PoseStack matrixStack, int mouseX, int mouseY)
        {
            GetParentScreen().renderTooltip(matrixStack, new TranslatableComponent("logwow.filter_add_contents"), mouseX, mouseY);
        }

        @Override
        public void RenderOverlay(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
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
            ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();
            return filterContainer.GetFilterTags();
        }

        @Override
        protected void UpdateTagList(ArrayList<String> updatedList)
        {
            ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();
            String tagList = filterContainer.SetFilterTags(updatedList);
            LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubWindowStringPropertyUpdatePacket(GetParentScreen().getMenu().containerId, GetSubContainer().GetSubWindowID(), ItemFilterSubMenu.FILTER_TAGS_STRING_PROPERTY_ID,  tagList));

        }

        @Override
        public void updateNarration(NarrationElementOutput narration)
        {
            narration.add(NarratedElementType.TITLE, getMessage());
        }
    }

    public static final int BUTTON_Y_OFFSET = 16;

    private final int numButtonPositionsToSkip;
    private FilterTypeCycle filterTypeCycle;
    private WhitelistToggle whitelistToggle;
    private MatchNBTToggle matchNBTToggle;
    private PolymorphicFilterButton polymorphicFilterButton;
    private FilterTags tagSelection;


    public ItemFilterSubScreen(ItemFilterSubMenu container, MultiScreen<?> parentScreen, int numButtonPositionsToSkip)
    {
        super(container, parentScreen);
        this.numButtonPositionsToSkip = numButtonPositionsToSkip;
    }

    @Override
    public void init(int guiLeft, int guiTop)
    {
        super.init(guiLeft, guiTop);

        int xPos = GetSubContainer().GetXPos();
        int yPos = GetSubContainer().GetYPos() + BUTTON_Y_OFFSET * numButtonPositionsToSkip;

        ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();

        int tagSelectionXOffset = xPos + 21;
        tagSelection = AddWidget(new FilterTags
                (
                        Minecraft.getInstance().font,
                        guiLeft + tagSelectionXOffset, guiTop + GetSubContainer().GetYPos(),
                        GetParentScreen().getXSize() - tagSelectionXOffset, 80,
                        new TextComponent(""),
                        filterContainer.GetTagFetchHelperSlot()
                ));

        filterTypeCycle = AddWidget(new FilterTypeCycle(guiLeft + xPos, guiTop + yPos));
        yPos += BUTTON_Y_OFFSET;

        if(!filterContainer.hideWhitelistAndNBT)
        {
            whitelistToggle = AddWidget(new WhitelistToggle(guiLeft + xPos, guiTop + yPos));
            yPos += BUTTON_Y_OFFSET;
            matchNBTToggle = AddWidget(new MatchNBTToggle(guiLeft + xPos, guiTop + yPos));
            yPos += BUTTON_Y_OFFSET;
        }

        polymorphicFilterButton = AddWidget(new PolymorphicFilterButton(guiLeft + xPos, guiTop + GetSubContainer().GetYPos() + 56));
    }

    @Override
    public void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY)
    {
        ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();

        if(filterContainer.GetFilterType() == Constants.eFilterType.Type)
        {
            for(int r = 0; r < ItemFilter.FILTER_NUM_ROWS; ++r)
            {
                MultiScreen.RenderSlotRowBackground(this, matrixStack, guiLeft + ItemFilterSubMenu.FILTER_SLOT_X, guiTop + ItemFilterSubMenu.FILTER_SLOT_Y + r * Client.SLOT_Y_SPACING, getBlitOffset(), ItemFilter.FILTER_NUM_COLUMNS);
            }
        }
        else if(filterContainer.GetFilterType() == Constants.eFilterType.Tag)
        {
            MultiScreen.RenderSlotRowBackground(this, matrixStack, guiLeft + ItemFilterSubMenu.TAG_FETCH_HELPER_SLOT_X, guiTop + ItemFilterSubMenu.TAG_FETCH_HELPER_SLOT_Y, getBlitOffset(), 1);
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
    }

    @Override
    public void SetActive(boolean active)
    {
        ItemFilterSubMenu filterContainer = (ItemFilterSubMenu)GetSubContainer();
        if(this.active != active)
        {
            this.active = active;

            filterTypeCycle.active = filterTypeCycle.visible = active;
        }

        polymorphicFilterButton.active = polymorphicFilterButton.visible = active && filterContainer.GetFilterType() == Constants.eFilterType.Type;
        tagSelection.active = tagSelection.visible = active && (filterContainer.GetFilterType() == Constants.eFilterType.Tag);

        if(!filterContainer.hideWhitelistAndNBT)
        {
            whitelistToggle.active = whitelistToggle.visible = active && filterContainer.GetFilterType() != Constants.eFilterType.Default;
            matchNBTToggle.active = matchNBTToggle.visible = active && filterContainer.GetFilterType() == Constants.eFilterType.Type;;
        }
    }
}
