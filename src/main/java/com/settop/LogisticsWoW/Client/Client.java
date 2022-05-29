package com.settop.LogisticsWoW.Client;

import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.Wisps.GlobalWispData;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LogisticsWoW.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
@OnlyIn(Dist.CLIENT)
public class Client
{
    public static final ResourceLocation WISP_CORE_RING_0 = new ResourceLocation(LogisticsWoW.MOD_ID, "block/wisp_core_ring_0");
    public static final ResourceLocation WISP_CORE_RING_1 = new ResourceLocation(LogisticsWoW.MOD_ID, "block/wisp_core_ring_1");
    public static final ResourceLocation WISP_CORE_RING_2 = new ResourceLocation(LogisticsWoW.MOD_ID, "block/wisp_core_ring_2");

    public static final ResourceLocation WISP_CORE_RING_TEX = new ResourceLocation(LogisticsWoW.MOD_ID, "blocks/wisp_core_ring_test");


    public static final int SLOT_X_SPACING = 18;
    public static final int SLOT_Y_SPACING = 18;
    public static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    @SubscribeEvent
    public static void RenderWorldLastEvent(RenderLevelLastEvent evt)
    {
        //this will only work on single player
        final int renderType = 2;
        switch (renderType)
        {
            case 0:
                break;
            case 1:
                GlobalWispData.RenderConnections(evt);
                break;
            case 2:
                GlobalWispData.RenderConnections(evt);
                break;
        }
    }
}
