package com.settop.LogisticsWoW.Client.Screens.SubScreens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.Client.Screens.Popups.SideSelectionPopup;
import com.settop.LogisticsWoW.Client.Screens.Widgets.SmallButton;
import com.settop.LogisticsWoW.GUI.Network.Packets.CSubWindowPropertyUpdatePacket;
import com.settop.LogisticsWoW.GUI.SubMenus.ItemExtractionSubMenu;
import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Utils.Utils;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;

public class ItemExtractionSubScreen extends ItemFilterSubScreen
{
    public class OpenAccessDirectionSelection extends SmallButton
    {
        public OpenAccessDirectionSelection(int x, int y)
        {
            super(x, y, 16, null);
        }

        @Override
        public void onPress()
        {
            ItemExtractionSubMenu filterContainer = (ItemExtractionSubMenu)GetSubContainer();
            accessDirectionPopup.SetSelectedDirection(filterContainer.GetAccessDirection());
            GetParentScreen().OpenPopup(accessDirectionPopup);
        }

        @Override
        public void renderToolTip(@NotNull PoseStack matrixStack, int mouseX, int mouseY)
        {
            GetParentScreen().renderTooltip(matrixStack, new TranslatableComponent("logwow.accessdirection"), mouseX, mouseY);
        }

        @Override
        public void RenderOverlay(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            MultiScreen.GuiPart overlayPart = MultiScreen.OVERLAY_SIDE_CONFIG;
            blit(matrixStack, 0, 0, overlayPart.uStart, overlayPart.vStart, overlayPart.width, overlayPart.height );
        }
    }

    private SideSelectionPopup accessDirectionPopup;
    private OpenAccessDirectionSelection openAccessDirection;

    public ItemExtractionSubScreen(ItemExtractionSubMenu container, MultiScreen<?> parentScreen)
    {
        super(container, parentScreen, 1);
    }

    @Override
    public void init(int guiLeft, int guiTop)
    {
        super.init(guiLeft, guiTop);

        int xPos = GetSubContainer().GetXPos();
        int yPos = GetSubContainer().GetYPos();

        ItemExtractionSubMenu extractionContainer = (ItemExtractionSubMenu)GetSubContainer();

        accessDirectionPopup = new SideSelectionPopup
                (
                        extractionContainer.GetBlockState(),
                        true,
                        guiLeft + xPos + 16,
                        guiTop + yPos + 16
                )
        {
            @Override
            public void OnSelectedDirectionChange(Direction newDirection)
            {
                extractionContainer.SetAccessDirection(newDirection);
            }

            @Override
            public void OnClose()
            {
                LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubWindowPropertyUpdatePacket
                        (
                                GetParentScreen().getMenu().containerId,
                                GetSubContainer().GetSubWindowID(),
                                ItemExtractionSubMenu.DIRECTION_PROPERTY_ID,
                                Utils.DirectionToInt(extractionContainer.GetAccessDirection())
                        ));
            }
        };

        openAccessDirection = AddWidget(new OpenAccessDirectionSelection(guiLeft + xPos, guiTop + yPos ));
    }

    @Override
    public void SetActive(boolean active)
    {
        super.SetActive(active);
        openAccessDirection.active = openAccessDirection.visible = active;
    }
}
