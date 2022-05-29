package com.settop.LogisticsWoW.Client.Renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.settop.LogisticsWoW.BlockEntities.WispCoreBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.EmptyModelData;
import com.settop.LogisticsWoW.Client.Client;

import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class WispCoreTileRenderer implements BlockEntityRenderer<WispCoreBlockEntity>
{
    public static class Provider implements BlockEntityRendererProvider<WispCoreBlockEntity>
    {
        @Override
        public BlockEntityRenderer<WispCoreBlockEntity> create(Context context)
        {
            return new WispCoreTileRenderer(context);
        }
    }

    BlockEntityRendererProvider.Context renderContext;

    public WispCoreTileRenderer(BlockEntityRendererProvider.Context context)
    {
        renderContext = context;
    }

    private BakedModel ringModels[] = null;

    @Override
    public void render(WispCoreBlockEntity tileEntityIn, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn)
    {
        if(ringModels == null)
        {
            ringModels = new BakedModel[3];
            ringModels[0] = Minecraft.getInstance().getModelManager().getModel(Client.WISP_CORE_RING_0);
            ringModels[1] = Minecraft.getInstance().getModelManager().getModel(Client.WISP_CORE_RING_1);
            ringModels[2] = Minecraft.getInstance().getModelManager().getModel(Client.WISP_CORE_RING_2);
        }

        if(!tileEntityIn.IsMultiblockComplete())
        {
            return;
        }

        VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderType.solid());

        tileEntityIn.renderTimer += partialTicks * 0.05f;

        float bobAmplitude = 0.03f;
        float bobCycleTime = 10.f;

        for(int i = 0; i < 3; ++i)
        {
            matrixStackIn.pushPose();

            float bob = bobAmplitude * (3 - i) * (float)Math.sin((tileEntityIn.renderTimer + bobCycleTime * i * 0.1f) * (float)Math.PI * 2.f / bobCycleTime );
            matrixStackIn.translate(0.f, bob, 0.f);

            matrixStackIn.translate(0.5f, 0.5f, 0.5f );
            matrixStackIn.scale(1.5f, 1.5f, 1.5f);


            matrixStackIn.mulPose(new Quaternion(Vector3f.YP, tileEntityIn.renderTimer * 0.1f, false));
            if(i < 3) matrixStackIn.mulPose(new Quaternion(Vector3f.ZP, tileEntityIn.renderTimer * 0.37f, false));
            if(i < 2) matrixStackIn.mulPose(new Quaternion(Vector3f.XP, tileEntityIn.renderTimer * 0.61f, false));
            if(i < 1) matrixStackIn.mulPose(new Quaternion(Vector3f.YP, tileEntityIn.renderTimer, false));

            switch(i)
            {
                case 0:
                    matrixStackIn.mulPose(new Quaternion(Vector3f.YP, 90.f, true));
                    break;
                case 1:
                    break;
                default:
                    matrixStackIn.mulPose(new Quaternion(Vector3f.XP, 90.f, true));
                    break;
            }
            BlockRenderDispatcher dispatcher = renderContext.getBlockRenderDispatcher();

            dispatcher.getModelRenderer().renderModel(matrixStackIn.last(), ivertexbuilder, null, ringModels[i],
                    1.f, 1.f, 1.f, combinedLightIn, combinedOverlayIn, EmptyModelData.INSTANCE);

            matrixStackIn.popPose();
        }
    }

}
