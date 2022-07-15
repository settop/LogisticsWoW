package com.settop.LogisticsWoW;

import com.settop.LogisticsWoW.BlockEntities.WispConnectionNodeBlockEntity;
import com.settop.LogisticsWoW.BlockEntities.WispCoreBlockEntity;
import com.settop.LogisticsWoW.GUI.BasicWispMenu;
import com.settop.LogisticsWoW.GUI.Network.GUIClientMessageHandler;
import com.settop.LogisticsWoW.GUI.Network.GUIServerMessageHandler;
import com.settop.LogisticsWoW.GUI.Network.Packets.*;
import com.settop.LogisticsWoW.Items.BasicWispItem;
import com.settop.LogisticsWoW.Items.WispEnhancementItem;
import com.settop.LogisticsWoW.WispNetwork.Tasks.TransferTask;
import com.settop.LogisticsWoW.WispNetwork.Tasks.WispTaskFactory;
import com.settop.LogisticsWoW.Wisps.Enhancements.EnhancementTypes;
import com.settop.LogisticsWoW.Wisps.Enhancements.IEnhancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.settop.LogisticsWoW.Blocks.WispConnectionNode;
import com.settop.LogisticsWoW.Blocks.WispCore;

import java.util.ArrayList;
import java.util.Optional;

import static net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER;


// The value here should match an entry in the META-INF/mods.toml file
@Mod("logwow")
public class LogisticsWoW
{
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "logwow";
    public static final String MULTI_SCREEN_CHANNEL_VERSION = "1.0.0";
    public static final SimpleChannel MULTI_SCREEN_CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(MOD_ID, "multi_screen"), () -> MULTI_SCREEN_CHANNEL_VERSION,
            MULTI_SCREEN_CHANNEL_VERSION::equals,
            MULTI_SCREEN_CHANNEL_VERSION::equals);

    public static final boolean DEBUG = true;

    public LogisticsWoW()
    {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);


        Blocks.BLOCKS.register( FMLJavaModLoadingContext.get().getModEventBus() );
        BlockEntities.BLOCK_ENTITIES.register( FMLJavaModLoadingContext.get().getModEventBus() );
        Items.ITEMS.register( FMLJavaModLoadingContext.get().getModEventBus() );

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void SetupTaskFactories()
    {
        WispTaskFactory.RegisterFactory(TransferTask.SERIALISABLE_NAME, new TransferTask.Factory());
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        WispCoreBlockEntity.RING_BLOCK_TAGS = new ArrayList<>();
        WispCoreBlockEntity.RING_BLOCK_TAGS.add(Tags.Blocks.STORAGE_BLOCKS_GOLD);
        WispCoreBlockEntity.RING_BLOCK_TAGS.add(Tags.Blocks.STORAGE_BLOCKS_QUARTZ);
        WispCoreBlockEntity.RING_BLOCK_TAGS.add(Tags.Blocks.STORAGE_BLOCKS_AMETHYST);

        SetupTaskFactories();

        MULTI_SCREEN_CHANNEL.registerMessage(1, CContainerTabSelected.class,
                CContainerTabSelected::encode, CContainerTabSelected::decode,
                GUIServerMessageHandler::OnMessageReceived,
                Optional.of(PLAY_TO_SERVER));
        MULTI_SCREEN_CHANNEL.registerMessage(2, CSubContainerDirectionChange.class,
                CSubContainerDirectionChange::encode, CSubContainerDirectionChange::decode,
                GUIServerMessageHandler::OnMessageReceived,
                Optional.of(PLAY_TO_SERVER));
        MULTI_SCREEN_CHANNEL.registerMessage(3, CScrollWindowPacket.class,
                CScrollWindowPacket::encode, CScrollWindowPacket::decode,
                GUIServerMessageHandler::OnMessageReceived,
                Optional.of(PLAY_TO_SERVER));
        MULTI_SCREEN_CHANNEL.registerMessage(4, CSubWindowPropertyUpdatePacket.class,
                CSubWindowPropertyUpdatePacket::encode, CSubWindowPropertyUpdatePacket::decode,
                GUIServerMessageHandler::OnMessageReceived,
                Optional.of(PLAY_TO_SERVER));
        MULTI_SCREEN_CHANNEL.registerMessage(5, CSubWindowStringPropertyUpdatePacket.class,
                CSubWindowStringPropertyUpdatePacket::encode, CSubWindowStringPropertyUpdatePacket::decode,
                GUIServerMessageHandler::OnMessageReceived,
                Optional.of(PLAY_TO_SERVER));
        MULTI_SCREEN_CHANNEL.registerMessage(6, SWindowStringPropertyPacket.class,
                SWindowStringPropertyPacket::encode, SWindowStringPropertyPacket::decode,
                GUIClientMessageHandler::OnMessageReceived,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static class Capabilities
    {
        public static Capability<IEnhancement> CAPABILITY_ENHANCEMENT = CapabilityManager.get(new CapabilityToken<>(){});
    }

    public static class Menus
    {
        public static MenuType<BasicWispMenu> BASIC_WISP_MENU;
    }

    @SubscribeEvent
    public void registerCaps(RegisterCapabilitiesEvent event)
    {
        event.register(IEnhancement.class);
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents
    {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent)
        {
        }

        @SubscribeEvent
        public static void onItemsRegistry(final RegistryEvent.Register<Item> itemRegistryEvent)
        {

        }

        @SubscribeEvent
        public static void registerMenus(final RegistryEvent.Register<MenuType<?>> event)
        {
            Menus.BASIC_WISP_MENU = IForgeMenuType.create(BasicWispMenu::CreateMenu);
            Menus.BASIC_WISP_MENU.setRegistryName("basic_wisp_menu");
            event.getRegistry().register(Menus.BASIC_WISP_MENU);
        }
    }

    public static class Blocks
    {
        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LogisticsWoW.MOD_ID);

        public static final RegistryObject<Block> WISP_CORE = BLOCKS.register("wisp_core", WispCore::new );
        public static final RegistryObject<Block> WISP_CONNECTION_NODE  = BLOCKS.register("wisp_connection_node", WispConnectionNode::new );
    }

    public static class BlockEntities
    {
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, LogisticsWoW.MOD_ID);

        public static final RegistryObject<BlockEntityType<WispCoreBlockEntity>> WISP_CORE_TILE_ENTITY = BLOCK_ENTITIES.register("wisp_core",
                ()->{ return BlockEntityType.Builder.of(WispCoreBlockEntity::new, Blocks.WISP_CORE.get() ).build(null); });

        public static final RegistryObject<BlockEntityType<WispConnectionNodeBlockEntity>> WISP_CONNECTION_NODE_TILE_ENTITY = BLOCK_ENTITIES.register("wisp_connection_node",
                ()->{ return BlockEntityType.Builder.of(WispConnectionNodeBlockEntity::new, Blocks.WISP_CONNECTION_NODE.get() ).build(null); });


    }



    public static class Items
    {
        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LogisticsWoW.MOD_ID);

        // Block Items
        public static final RegistryObject<Item> WISP_CORE_ITEM = ITEMS.register("wisp_core", ()->{ return new BlockItem( Blocks.WISP_CORE.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC) ); });
        public static final RegistryObject<Item> WISP_CONNECTION_NODE_ITEM = ITEMS.register("wisp_connection_node", ()->{ return new BlockItem( Blocks.WISP_CONNECTION_NODE.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC) ); });

        // Items
        public static final RegistryObject<Item> WISP_ITEM = ITEMS.register("wisp", BasicWispItem::new );
        public static final RegistryObject<Item> WISP_STORAGE_ENHANCEMENT_ITEM = ITEMS.register("wisp_storage_enhancement", () -> new WispEnhancementItem(EnhancementTypes.STORAGE) );

    }
}
