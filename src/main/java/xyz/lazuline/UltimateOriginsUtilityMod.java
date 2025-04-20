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
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing " + MOD_ID);

		try {
			// Load configurations first
			BlockUtils.loadConfig();
			InventoryUtils.loadConfig();

			// Register event handlers
			registerEventHandlers();

			LOGGER.info(MOD_ID + " initialization completed successfully");
		} catch (Exception e) {
			LOGGER.error("Failed to initialize " + MOD_ID + ": " + e.getMessage(), e);
		}

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			try {
				ServerPlayerEntity player = handler.player;
				LOGGER.info("Processing blocks around player: " + player.getName().getString());

				// Process blocks around player
				BlockUtils.replaceAllBlocksAroundPlayer(player);

				LOGGER.info("Completed block processing for player: " + player.getName().getString());
			} catch (Exception e) {
				LOGGER.error("Error processing player join events: " + e.getMessage(), e);
			}
		});

		ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
			try {
				LOGGER.info("Server started - processing world blocks");

				for (ServerWorld world : server.getWorlds()) {
					LOGGER.debug("Processing blocks in world: " + world.getRegistryKey().getValue());
					BlockUtils.replaceAllBlocksInWorld(world);
				}

				LOGGER.info("Completed world block processing");
			} catch (Exception e) {
				LOGGER.error("Error processing server start events: " + e.getMessage(), e);
			}
		});
	}

	private void registerEventHandlers() {
		// Player join event handler
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			try {
				ServerPlayerEntity player = handler.player;
				LOGGER.info("Processing inventory replacements for player: " + player.getName().getString());

				// Run inventory replacements
				InventoryUtils.replaceAll(player);

				// Process storage blocks around player
				StorageUtils.replaceAllAroundPlayer(player);

				LOGGER.info("Completed inventory processing for player: " + player.getName().getString());
			} catch (Exception e) {
				LOGGER.error("Error processing player join events: " + e.getMessage(), e);
			}
		});

		// Server start event handler
		ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
			try {
				LOGGER.info("Server started - processing world replacements");

				int worldsProcessed = 0;
				for (ServerWorld world : server.getWorlds()) {
					LOGGER.debug("Processing world: " + world.getRegistryKey().getValue());
					// Add your world processing logic here if needed
					// Currently commented out:
					BlockUtils.replaceAllBlocksInWorld(world);
					// StorageUtils.replaceAllInWorld(world);
					worldsProcessed++;
				}

				LOGGER.info("Completed world processing for " + worldsProcessed + " dimensions");
			} catch (Exception e) {
				LOGGER.error("Error processing server start events: " + e.getMessage(), e);
			}
		});
	}
}