package com.settop.LogisticsWoW.Client.Screens.Popups;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.EmptyModelData;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import com.settop.LogisticsWoW.GUI.Network.Packets.CSubContainerDirectionChange;
import com.settop.LogisticsWoW.Utils.BoolArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SideSelectionPopup extends ScreenPopup
{

    public class FaceButton extends AbstractButton
    {
        private final Direction direction;
        private final BlockState blockState;
        private BoolArray setDirections;
        private final int windowID;
        private final int subWindowID;

        public FaceButton(BlockState blockState, BoolArray setDirections, int windowID, int subWindowID, int x, int y, int width, int height, Direction direction)
        {
            super(x, y, width, height, null);
            this.direction = direction;
            this.blockState = blockState;
            this.setDirections = setDirections;
            this.windowID = windowID;
            this.subWindowID = subWindowID;
        }

        @Override
        public void onPress()
        {
            int d = direction.ordinal();
            boolean isDown = setDirections.GetBool(direction.ordinal());
            boolean nextIsDown = !isDown;
            setDirections.SetBool(d, nextIsDown);

            LogisticsWoW.MULTI_SCREEN_CHANNEL.sendToServer(new CSubContainerDirectionChange(windowID, subWindowID, direction, nextIsDown));
        }

        public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            boolean isDown = setDirections.get(direction.ordinal()) != 0;

            Minecraft minecraft = Minecraft.getInstance();
            BakedModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);

            Random random = new Random();
            random.setSeed(42);
            List<BakedQuad> quads = blockModel.getQuads(blockState, direction, random, EmptyModelData.INSTANCE);

            if(quads != null && quads.size() == 1)
            {
                TextureAtlasSprite sprite = quads.get(0).getSprite();

                RenderSystem.setShaderTexture(0, sprite.atlas().location());
                RenderSystem.enableDepthTest();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                blit(matrixStack, x, y, getBlitOffset(), 20, 20, sprite );

                this.renderBg(matrixStack, minecraft, mouseX, mouseY);
            }
            else
            {
                RenderSystem.setShaderTexture(0, MultiScreen.GUI_PARTS_TEXTURE);
                RenderSystem.enableDepthTest();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

                MultiScreen.GuiPart sidePart = MultiScreen.BUTTON_DIRECTIONS[direction.get3DDataValue()];

                blit(matrixStack, this.x, this.y, sidePart.uStart, sidePart.vStart, sidePart.width, sidePart.height );
                this.renderBg(matrixStack, minecraft, mouseX, mouseY);
            }

            if(isDown)
            {
                RenderSystem.setShaderTexture(0, MultiScreen.GUI_PARTS_TEXTURE);
                MultiScreen.GuiPart overlayPart = MultiScreen.OVERLAY_BLUE;
                blit(matrixStack, this.x, this.y, overlayPart.uStart, overlayPart.vStart, overlayPart.width, overlayPart.height );
            }
        }

        @Override
        public void updateNarration(NarrationElementOutput p_169152_)
        {
        }
    }

    static public final int BUTTON_SIZE = 20;

    static public final Vec3i OFFSETS[] = {
            /*Down*/new Vec3i(0, BUTTON_SIZE, 0),
            /*Up*/new Vec3i(0, -BUTTON_SIZE, 0),
            /*North*/new Vec3i(0, 0, 0),
            /*South*/new Vec3i(BUTTON_SIZE, BUTTON_SIZE, 0),
            /*East*/new Vec3i(-BUTTON_SIZE, 0, 0),
            /*West*/new Vec3i(BUTTON_SIZE, 0, 0)
    };

    public List<FaceButton> faceButtons;

    public SideSelectionPopup(BlockState blockState, BoolArray setDirections, int windowID, int subWindowID, int x, int y)
    {
        super(x,y, BUTTON_SIZE * 3, BUTTON_SIZE * 3);

        faceButtons = new ArrayList<>();
        for(int i = 0; i < 6; ++i)
        {
            FaceButton faceButton = new FaceButton( blockState, setDirections, windowID, subWindowID, x + OFFSETS[i].getX() + BUTTON_SIZE, y + OFFSETS[i].getY() + BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, Direction.from3DDataValue(i));
            faceButtons.add( AddListener(faceButton) );
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.fillGradient(matrixStack, x, y, x + width, y + height, MultiScreen.BG_COLOUR, MultiScreen.BG_COLOUR);
        for(FaceButton faceButton : faceButtons)
        {
            faceButton.setBlitOffset(getBlitOffset() + 1);
            faceButton.render(matrixStack, mouseX, mouseY, partialTicks);
        }
        MultiScreen.RenderBorder(this, matrixStack, x, y, getBlitOffset(), width, height);
    }

    @Override
    public void OnOpen()
    {
    }

    @Override
    public void OnClose()
    {
    }

    @Override
    public NarrationPriority narrationPriority()
    {
        return NarrationPriority.FOCUSED;
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_)
    {

    }
}
