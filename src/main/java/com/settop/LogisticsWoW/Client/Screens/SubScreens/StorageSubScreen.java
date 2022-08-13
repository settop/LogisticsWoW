package com.settop.LogisticsWoW.Client.Screens.SubScreens;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.settop.LogisticsWoW.Client.Screens.Popups.PriorityPopup;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.Constants;
import com.settop.LogisticsWoW.Utils.ItemFilter;
import com.settop.LogisticsWoW.Wisps.Enhancements.ItemStorageEnhancement;
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
import com.settop.LogisticsWoW.Client.Screens.Widgets.SmallButton;
import com.settop.LogisticsWoW.Client.Screens.Widgets.TagListSelection;
import com.settop.LogisticsWoW.GUI.FakeSlot;
import com.settop.LogisticsWoW.GUI.Network.Packets.CSubWindowPropertyUpdatePacket;
import com.settop.LogisticsWoW.GUI.Network.Packets.CSubWindowStringPropertyUpdatePacket;
import com.settop.LogisticsWoW.GUI.SubMenus.StorageEnhancementSubMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@OnlyIn(Dist.CLIENT)
public class StorageSubScreen extends ItemFilterSubScreen
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
        public void renderToolTip(@NotNull PoseStack matrixStack, int mouseX, int mouseY)
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

    private PriorityPopup priorityPopup;
    private PriorityButton priorityButton;


    public StorageSubScreen(StorageEnhancementSubMenu container, MultiScreen<?> parentScreen)
    {
        super(container, parentScreen, 1);
    }

    @Override
    public void init(int guiLeft, int guiTop)
    {
        super.init(guiLeft, guiTop);

        int xPos = GetSubContainer().GetXPos();
        int yPos = GetSubContainer().GetYPos();

        StorageEnhancementSubMenu providerContainer = (StorageEnhancementSubMenu)GetSubContainer();

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
    }

    @Override
    public void SetActive(boolean active)
    {
        super.SetActive(active);
        priorityButton.active = priorityButton.visible = active;
    }
}
