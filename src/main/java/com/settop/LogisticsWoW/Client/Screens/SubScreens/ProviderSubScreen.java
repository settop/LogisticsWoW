package com.settop.LogisticsWoW.Client.Screens.SubScreens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
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
import com.settop.LogisticsWoW.GUI.SubMenus.ProviderEnhancementSubMenu;
import com.settop.LogisticsWoW.Wisps.Enhancements.ProviderEnhancement;

import java.util.ArrayList;

@OnlyIn(Dist.CLIENT)
public class ProviderSubScreen extends SubScreen
{

    public class SideConfigButton extends SmallButton
    {
        public SideConfigButton(int x, int y)
        {
            super(x, y, new TextComponent(""));
        }

        @Override
        public void onPress()
        {
            GetParentScreen().OpenPopup(sideSelectionPopup);
        }

        @Override
        public void renderToolTip(PoseStack matrixStack, int mouseX, int mouseY)
        {
            GetParentScreen().renderTooltip(matrixStack, new TranslatableComponent("logwow.side_config"), mouseX, mouseY);
        }

        @Override
        public void RenderOverlay(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            MultiScreen.GuiPart overlayPart = MultiScreen.OVERLAY_SIDE_CONFIG;
            blit(matrixStack, 0, 0, overlayPart.uStart, overlayPart.vStart, overlayPart.width, overlayPart.height );
        }
    }
    public class WhitelistToggle extends SmallButton
    {
        public WhitelistToggle(int x, int y)
        {
            super(x, y, new TextComponent(""));
        }

        @Override
        public void onPress()
        {
            ProviderEnhancementSubMenu providerContainer = (ProviderEnhancementSubMenu)GetSubContainer();
            boolean newEnabled = !providerContainer.GetWhitelistEnabled();
            providerContainer.SetWhitelistEnabled(newEnabled);

            LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubWindowPropertyUpdatePacket(GetParentScreen().getMenu().containerId, GetSubContainer().GetSubWindowID(), ProviderEnhancementSubMenu.WHITELIST_PROPERTY_ID,  newEnabled ? 1 : 0));
        }

        @Override
        public void renderToolTip(PoseStack matrixStack, int mouseX, int mouseY)
        {
            ProviderEnhancementSubMenu providerContainer = (ProviderEnhancementSubMenu)GetSubContainer();
            GetParentScreen().renderTooltip(matrixStack, new TranslatableComponent(providerContainer.GetWhitelistEnabled() ? "logwow.whitelist" : "logwow.blacklist"), mouseX, mouseY);
        }

        @Override
        public void RenderOverlay(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            ProviderEnhancementSubMenu providerContainer = (ProviderEnhancementSubMenu)GetSubContainer();
            MultiScreen.GuiPart overlayPart = providerContainer.GetWhitelistEnabled() ? MultiScreen.OVERLAY_WHITELIST : MultiScreen.OVERLAY_BLACKLIST;
            blit(matrixStack, 0, 0, overlayPart.uStart, overlayPart.vStart, overlayPart.width, overlayPart.height );
        }
    }

    public class FilterTypeCycle extends SmallButton
    {
        public FilterTypeCycle(int x, int y)
        {
            super(x, y, null);
        }

        @Override
        public void onPress()
        {
            ProviderEnhancementSubMenu providerContainer = (ProviderEnhancementSubMenu)GetSubContainer();

            int nextValue = providerContainer.GetFilterType().ordinal() + 1;
            if(nextValue >= ProviderEnhancement.eFilterType.values().length)
            {
                nextValue = 0;
            }
            providerContainer.SetFilterType(ProviderEnhancement.eFilterType.values()[nextValue]);
            LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubWindowPropertyUpdatePacket(GetParentScreen().getMenu().containerId, GetSubContainer().GetSubWindowID(), ProviderEnhancementSubMenu.FILTER_TYPE_PROPERTY_ID,  nextValue));
        }

        @Override
        public void renderToolTip(PoseStack matrixStack, int mouseX, int mouseY)
        {
            ProviderEnhancementSubMenu providerContainer = (ProviderEnhancementSubMenu)GetSubContainer();
            TranslatableComponent text;
            switch(providerContainer.GetFilterType())
            {
                default:
                case Item:
                    text = new TranslatableComponent("logwow.item_filter");
                    break;
                case Tag:
                    text = new TranslatableComponent("logwow.tag_filter");
                    break;
            }
            GetParentScreen().renderTooltip(matrixStack, text, mouseX, mouseY);
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
            ProviderEnhancementSubMenu providerContainer = (ProviderEnhancementSubMenu)GetSubContainer();
            return providerContainer.GetFilterTags();
        }

        @Override
        protected void UpdateTagList(ArrayList<String> updatedList)
        {
            ProviderEnhancementSubMenu providerContainer = (ProviderEnhancementSubMenu)GetSubContainer();
            String tagList = providerContainer.SetFilterTags(updatedList);
            LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubWindowStringPropertyUpdatePacket(GetParentScreen().getMenu().containerId, GetSubContainer().GetSubWindowID(), ProviderEnhancementSubMenu.FILTER_TAGS_STRING_PROPERTY_ID,  tagList));

        }

        @Override
        public void updateNarration(NarrationElementOutput narration)
        {
            narration.add(NarratedElementType.TITLE, getMessage());
        }
    }

    private SideSelectionPopup sideSelectionPopup;
    private SideConfigButton sideSelectionButton;
    private WhitelistToggle whitelistToggle;
    private FilterTypeCycle filterTypeCycle;
    private FilterTags tagSelection;


    public ProviderSubScreen(ProviderEnhancementSubMenu container, MultiScreen<?> parentScreen)
    {
        super(container, parentScreen);
    }

    @Override
    public void init(int guiLeft, int guiTop)
    {
        super.init(guiLeft, guiTop);

        int xPos = GetSubContainer().GetXPos();
        int yPos = GetSubContainer().GetYPos();

        ProviderEnhancementSubMenu providerContainer = (ProviderEnhancementSubMenu)GetSubContainer();

        int tagSelectionXOffset = xPos + 21;
        tagSelection = AddWidget(new FilterTags
                (
                        Minecraft.getInstance().font,
                        guiLeft + tagSelectionXOffset, guiTop + yPos,
                        GetParentScreen().getXSize() - tagSelectionXOffset, 80,
                        new TextComponent(""),
                        providerContainer.GetTagFetchHelperSlot()
                ));

        sideSelectionButton = AddWidget(new SideConfigButton(guiLeft + xPos, guiTop + yPos));
        whitelistToggle = AddWidget(new WhitelistToggle(guiLeft + xPos, guiTop + yPos + 8));
        filterTypeCycle = AddWidget(new FilterTypeCycle(guiLeft + xPos, guiTop + yPos + 16));

        final int popupXOffset = 6;
        final int popupYOffset = 6;

        sideSelectionPopup = new SideSelectionPopup
                (
                        providerContainer.GetBlockState(),
                        providerContainer.GetDirectionsProvided(),
                        providerContainer.GetParentContainer().containerId,
                        providerContainer.GetSubWindowID(),
                        sideSelectionButton.x + sideSelectionButton.getWidth() + popupXOffset,
                        sideSelectionButton.y + popupYOffset
                );

    }

    @Override
    public void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY)
    {
        for(int r = 0; r < ProviderEnhancement.FILTER_NUM_ROWS; ++r)
        {
            MultiScreen.RenderSlotRowBackground(this, matrixStack, guiLeft + ProviderEnhancementSubMenu.FILTER_SLOT_X, guiTop + ProviderEnhancementSubMenu.FILTER_SLOT_Y + r * Client.SLOT_Y_SPACING, getBlitOffset(), ProviderEnhancement.FILTER_NUM_COLUMNS);
        }

        ProviderEnhancementSubMenu providerContainer = (ProviderEnhancementSubMenu)GetSubContainer();
        if(providerContainer.GetFilterType() == ProviderEnhancement.eFilterType.Tag)
        {
            MultiScreen.RenderSlotRowBackground(this, matrixStack, guiLeft + ProviderEnhancementSubMenu.TAG_FETCH_HELPER_SLOT_X, guiTop + ProviderEnhancementSubMenu.TAG_FETCH_HELPER_SLOT_Y, getBlitOffset(), 1);
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
    }

    @Override
    public void SetActive(boolean active)
    {
        if(this.active != active)
        {
            this.active = active;

            sideSelectionButton.active =  sideSelectionButton.visible = active;
            whitelistToggle.active =  whitelistToggle.visible = active;
            filterTypeCycle.active =  filterTypeCycle.visible = active;
        }

        ProviderEnhancementSubMenu providerContainer = (ProviderEnhancementSubMenu)GetSubContainer();
        tagSelection.active =  tagSelection.visible = active && (providerContainer.GetFilterType() == ProviderEnhancement.eFilterType.Tag);
    }
}
