package xyz.lazuline;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.lazuline.utils.BlockUtils;
import xyz.lazuline.utils.InventoryUtils;
import xyz.lazuline.utils.StorageUtils;


public class UltimateOriginsUtilityMod implements ModInitializer {
	public static final String MOD_ID = "uo-utils";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		BlockUtils.loadConfig();
		InventoryUtils.loadConfig();

		LOGGER.info("Hello Fabric world!");
		//IdFix.registerRemapping();
		//StrippedLogFinder.register();

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			//InventoryUtils2.replaceItem(player, Items.DIAMOND, Items.EMERALD);
			InventoryUtils.replaceAll(player);
		});

		ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
			for (ServerWorld world : server.getWorlds()) {
				BlockUtils.replaceAllBlocksInWorld(world);
				StorageUtils.replaceAllInWorld(world);

			}
			LOGGER.info("World item replacement complete for all dimensions.");
		});
	}
}