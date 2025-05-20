package com.cobblemon.yajatkaul.mega_showdown;

import com.cobblemon.mod.common.data.CobblemonDataProvider;
import com.cobblemon.yajatkaul.mega_showdown.block.MegaOres;
import com.cobblemon.yajatkaul.mega_showdown.block.entity.ModBlockEntities;
import com.cobblemon.yajatkaul.mega_showdown.block.entity.renderer.PedestalBlockEntityRenderer;
import com.cobblemon.yajatkaul.mega_showdown.commands.MegaCommands;
import com.cobblemon.yajatkaul.mega_showdown.config.Config;
import com.cobblemon.yajatkaul.mega_showdown.datapack.DatapacksLoader;
import com.cobblemon.yajatkaul.mega_showdown.datapack.showdown.HeldItems;
import com.cobblemon.yajatkaul.mega_showdown.event.CobbleEvents;
import com.cobblemon.yajatkaul.mega_showdown.item.*;
import com.cobblemon.yajatkaul.mega_showdown.item.configActions.ConfigResults;
import com.cobblemon.yajatkaul.mega_showdown.item.inventory.ItemInventoryUtil;
import com.cobblemon.yajatkaul.mega_showdown.networking.NetworkHandler;
import com.cobblemon.yajatkaul.mega_showdown.networking.packets.MegaEvo;
import com.cobblemon.yajatkaul.mega_showdown.networking.packets.UltraTrans;
import com.cobblemon.yajatkaul.mega_showdown.screen.custom.ZygardeCubeScreen;
import com.cobblemon.yajatkaul.mega_showdown.screen.ModMenuTypes;
import com.cobblemon.yajatkaul.mega_showdown.sound.ModSounds;
import com.cobblemon.yajatkaul.mega_showdown.utility.PackRegister;
import com.cobblemon.yajatkaul.mega_showdown.utility.TeraTypeHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cobblemon.yajatkaul.mega_showdown.block.ModBlocks;
import com.cobblemon.yajatkaul.mega_showdown.datamanage.DataManage;
import com.cobblemon.yajatkaul.mega_showdown.utility.Utils;
import com.cobblemon.yajatkaul.mega_showdown.creativeTab.ModCreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.Map;

import static com.cobblemon.yajatkaul.mega_showdown.megaevo.Controls.MEGA_ITEM_KEY;
import static com.cobblemon.yajatkaul.mega_showdown.megaevo.Controls.ULTRA_KEY;

@Mod(MegaShowdown.MOD_ID)
public final class MegaShowdown {
    public static final Logger LOGGER = LoggerFactory.getLogger("Mega Showdown");
    public static final String MOD_ID = "mega_showdown";

    public static final ItemCapability<ItemInventoryUtil, Void> ITEM_STORAGE =
            ItemCapability.createVoid(
                    ResourceLocation.fromNamespaceAndPath(MegaShowdown.MOD_ID, "item_storage"),
                    ItemInventoryUtil.class
            );

    public MegaShowdown(IEventBus modEventBus, @NotNull ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);

        ModBlocks.register(modEventBus);
        MegaOres.register();
        ModSounds.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        ItemsRegistration.register(modEventBus);

        DataManage.register(modEventBus);

        ModCreativeModeTabs.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(NetworkHandler::register);

        NeoForge.EVENT_BUS.addListener(MegaCommands::register);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerDatapackRegistries);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(
                    ModBlocks.GRACIDEA_FLOWER.getId(),
                    ModBlocks.POTTED_GRACIDEA
            );
        });

        CobbleEvents.register();
        TeraTypeHelper.loadShardData();
        Utils.registerRemapping();
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                MegaShowdown.ITEM_STORAGE,
                (itemStack, ctx) -> new ItemInventoryUtil(),
                FormeChangeItems.ZYGARDE_CUBE.get()
        );
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        event.getServer().reloadableRegistries();
        Utils.registryLoader(event.getServer().registryAccess());
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack itemStack = event.getItemStack();
        Level level = event.getLevel();

        boolean consumed = ConfigResults.useItem(player, level, hand, itemStack);

        if (consumed) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        CobblemonDataProvider.INSTANCE.register(HeldItems.INSTANCE);
        DatapacksLoader.register(event);
    }


    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            NeoForge.EVENT_BUS.addListener(ClientModEvents::onClientTick);
        }

        @SubscribeEvent
        public static void onAddPackFinders(AddPackFindersEvent event) {
            PackRegister.register(event);
        }

        // Register the key binding
        @SubscribeEvent
        public static void registerBindings(RegisterKeyMappingsEvent event) {
            event.register(MEGA_ITEM_KEY.get());
            event.register(ULTRA_KEY.get());
        }

        public static void onClientTick(ClientTickEvent.Post event) {
            while (MEGA_ITEM_KEY.get().consumeClick()) {
                PacketDistributor.sendToServer(new MegaEvo("mega_evo"));
            }
            while (ULTRA_KEY.get().consumeClick()) {
                PacketDistributor.sendToServer(new UltraTrans("ultra_trans"));
            }
        }

        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.PEDESTAL_BE.get(), PedestalBlockEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event){
            event.register(ModMenuTypes.ZYGARDE_CUBE_MENU.get(), ZygardeCubeScreen::new);
        }
    }
}