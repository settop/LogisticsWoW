package com.settop.LogisticsWoW.Client;

import com.mojang.blaze3d.platform.ScreenManager;
import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.settop.LogisticsWoW.Client.Renderers.WispCoreTileRenderer;
import com.settop.LogisticsWoW.Client.Screens.BasicWispContainerScreen;

@Mod.EventBusSubscriber(modid = LogisticsWoW.MOD_ID, bus=Mod.EventBusSubscriber.Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class ClientStartup
{
    @SubscribeEvent
    public static void onModelRegistryEvent(ModelRegistryEvent event)
    {
        ForgeModelBakery.addSpecialModel(Client.WISP_CORE_RING_0);
        ForgeModelBakery.addSpecialModel(Client.WISP_CORE_RING_1);
        ForgeModelBakery.addSpecialModel(Client.WISP_CORE_RING_2);
    }

    @SubscribeEvent
    public static void onTextureStitchEvent(TextureStitchEvent.Pre event)
    {
        if (event.getAtlas().location() == InventoryMenu.BLOCK_ATLAS)
        {
            event.addSprite(Client.WISP_CORE_RING_TEX);
        }
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(LogisticsWoW.BlockEntities.WISP_CORE_TILE_ENTITY.get(), new WispCoreTileRenderer.Provider());
    }

    @SubscribeEvent
    public static void onClientSetupEvent(FMLClientSetupEvent event)
    {
        MenuScreens.register(LogisticsWoW.Menus.BASIC_WISP_MENU, BasicWispContainerScreen::new);
    }
}
