package com.settop.LogisticsWoW.Client.Screens.Popups;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.EmptyModelData;
import com.settop.LogisticsWoW.Client.Screens.MultiScreen;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class SideSelectionPopup extends ScreenPopup
{
    private static final String[] BUTTON_NAMES = {"B", "T", "N", "S", "W", "E"};
    public class FaceButton extends AbstractButton
    {
        private final Direction direction;

        public FaceButton(int x, int y, int width, int height, Direction direction)
        {
            super(x, y, width, height, null);
            this.direction = direction;
        }

        @Override
        public void onPress()
        {
            if(direction == selectedDirection)
            {
                selectedDirection = null;
                OnSelectedDirectionChange(null);
            }
            else
            {
                selectedDirection = direction;
                OnSelectedDirectionChange(direction);
            }
        }

        public void renderButton(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            boolean isDown = selectedDirection == direction;

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
                MultiScreen.GuiPart buttonPart = isHoveredOrFocused() ? MultiScreen.BUTTON_HOVERED : MultiScreen.BUTTON;

                matrixStack.pushPose();
                matrixStack.translate(this.x, this.y, 0);
                matrixStack.scale((float)BUTTON_SIZE / buttonPart.width, (float)BUTTON_SIZE / buttonPart.height, 1.f);

                RenderSystem.setShaderTexture(0, MultiScreen.GUI_PARTS_TEXTURE);
                blit(matrixStack, 0, 0, buttonPart.uStart, buttonPart.vStart, buttonPart.width, buttonPart.height );

                matrixStack.translate(8.f,5.f, getBlitOffset() + 1.f);
                float textScale = 3.f;
                matrixStack.scale(textScale, textScale, 1.f);
                Minecraft.getInstance().font.draw(matrixStack, BUTTON_NAMES[direction.get3DDataValue()], 0, 0, 0x0f0f0f);

                matrixStack.popPose();
                this.renderBg(matrixStack, minecraft, mouseX, mouseY);
            }

            if(isDown)
            {
                RenderSystem.setShaderTexture(0, MultiScreen.GUI_PARTS_TEXTURE);
                MultiScreen.GuiPart overlayPart = isExtraction ? MultiScreen.OVERLAY_ORANGE : MultiScreen.OVERLAY_BLUE;
                blit(matrixStack, this.x, this.y, overlayPart.uStart, overlayPart.vStart, overlayPart.width, overlayPart.height );
            }
        }

        @Override
        public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput)
        {
        }
    }

    static public final int BUTTON_SIZE = 20;

    static public final Vec3i[] OFFSETS = {
            /*Down*/new Vec3i(0, BUTTON_SIZE, 0),
            /*Up*/new Vec3i(0, -BUTTON_SIZE, 0),
            /*North*/new Vec3i(0, 0, 0),
            /*South*/new Vec3i(BUTTON_SIZE, BUTTON_SIZE, 0),
            /*East*/new Vec3i(-BUTTON_SIZE, 0, 0),
            /*West*/new Vec3i(BUTTON_SIZE, 0, 0)
    };

    private final BlockState blockState;
    private final boolean isExtraction;
    public Direction selectedDirection;
    public List<FaceButton> faceButtons;

    public SideSelectionPopup(BlockState blockState, boolean isExtraction, int x, int y)
    {
        super(x,y, BUTTON_SIZE * 3, BUTTON_SIZE * 3);

        this.blockState = blockState;
        this.isExtraction = isExtraction;
        faceButtons = new ArrayList<>();
        selectedDirection = null;
        for(int i = 0; i < 6; ++i)
        {
            FaceButton faceButton = new FaceButton(x + OFFSETS[i].getX() + BUTTON_SIZE, y + OFFSETS[i].getY() + BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, Direction.from3DDataValue(i));
            faceButtons.add( AddListener(faceButton) );
        }
    }

    @Override
    public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.fillGradient(matrixStack, x, y, x + width, y + height, MultiScreen.BG_COLOUR, MultiScreen.BG_COLOUR);
        for(FaceButton faceButton : faceButtons)
        {
            faceButton.setBlitOffset(getBlitOffset() + 1);
            faceButton.render(matrixStack, mouseX, mouseY, partialTicks);
        }
        MultiScreen.RenderBorder(this, matrixStack, x, y, getBlitOffset(), width, height);
    }

    public void SetSelectedDirection(Direction direction)
    {
        selectedDirection = direction;
    }

    public Direction GetSelectedDirection()
    {
        return selectedDirection;
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
    public @NotNull NarrationPriority narrationPriority()
    {
        return NarrationPriority.FOCUSED;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput p_169152_)
    {

    }

    public abstract void OnSelectedDirectionChange(Direction newDirection);
}
