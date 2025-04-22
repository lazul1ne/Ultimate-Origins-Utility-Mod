package xyz.lazuline;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.lazuline.utils.*;

public class UltimateOriginsUtilityMod implements ModInitializer {
	public static final String MOD_ID = "uo-utils";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing " + MOD_ID);

		try {
			// Load configurations first
			UOConfig.load(); // Initialize migration config
			BlockUtils.loadConfig();
			InventoryUtils.loadConfig();

			// Register command and event handlers
			registerCommands();
			registerEventHandlers();
			//new BlockIDFinder();

			startBlockIDFinder();

			LOGGER.info(MOD_ID + " initialization completed successfully");
		} catch (Exception e) {
			LOGGER.error("Failed to initialize " + MOD_ID + ": " + e.getMessage(), e);
		}
	}

	private void processWorldMigrations(MinecraftServer server) {
		if (!UOConfig.isMigrationEnabled()) {
			LOGGER.info("Migration is disabled in config, skipping world migrations");
			return;
		}

		LOGGER.info("Processing world migrations (Migration enabled in config)");
		try {
			for (ServerWorld world : server.getWorlds()) {
				LOGGER.info("Processing world: {}", world.getRegistryKey());
				BlockUtils.replaceAllBlocksInWorld(world);
			}
		} catch (Exception e) {
			LOGGER.error("Error processing world migrations", e);
		}
	}
	public void startBlockIDFinder(){
		if (UOConfig.isBlockIdFinderEnabled()) {
			BlockIDFinder.register();
		}
	}
	private void processMigrationForPlayer(ServerPlayerEntity player) {
		if (!UOConfig.isMigrationEnabled()) {
			return;
		}


		player.sendMessage(Text.literal(
				"§6[UO-Utils] §eMigration Feature Active:\n" +
						"§7This feature helps fix known issues present when updating from 1.19.2 to 1.20.1.\n" +
						"§7It runs once per join and is recommended for:\n" +
						"§7• Large worlds/servers\n" +
						"§7• Multiple world saves\n\n" +
						"§7To disable this feature and message, run: §f/uo-utils migration false"
		));

		try {
			LOGGER.info("Processing migrations for player: {}", player.getName().getString());
			// Make sure we process blocks first as they might affect storage/inventory
			BlockUtils.replaceAllBlocksAroundPlayer(player);
			InventoryUtils.loadConfig();
			StorageUtils.replaceAllAroundPlayer(player);
			InventoryUtils.replaceAll(player);
		} catch (Exception e) {
			LOGGER.error("Error processing migrations for player: {}", player.getName().getString(), e);
		}
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("uo-utils")
					.requires(source -> source.hasPermissionLevel(3))
					.then(CommandManager.literal("migration")
							.then(CommandManager.argument("enabled", BoolArgumentType.bool())
									.executes(context -> {
										boolean enabled = BoolArgumentType.getBool(context, "enabled");
										boolean wasEnabled = UOConfig.isMigrationEnabled();
										UOConfig.setMigrationEnabled(enabled);

										ServerCommandSource source = context.getSource();
										source.sendFeedback(
												(Text.literal("UO-Utils migration " +
														(enabled ? "enabled" : "disabled"))),
														true
										);

										// Only process if enabling migration or if it was disabled and now enabled
										if (enabled) {
											processWorldMigrations(source.getServer());
											if (source.isExecutedByPlayer()) {
												InventoryUtils.loadConfig();
												BlockUtils.loadConfig();
												processMigrationForPlayer(source.getPlayer());
											}
										}

										return Command.SINGLE_SUCCESS;
									})
							)
							// Add a subcommand to force execution without changing the enabled state
							.then(CommandManager.literal("run")
									.executes(context -> {
										ServerCommandSource source = context.getSource();
										source.sendFeedback(
												(Text.literal("UO-Utils forcing migration run...")),
												true
										);

										processWorldMigrations(source.getServer());
										if (source.isExecutedByPlayer()) {
											InventoryUtils.loadConfig();
											BlockUtils.loadConfig();
											processMigrationForPlayer(source.getPlayer());
										}

										source.sendFeedback(
												(Text.literal("UO-Utils migration run completed")),
												true
										);

										return Command.SINGLE_SUCCESS;
									})
							)
					)
			);
		});
	}

	private void registerEventHandlers() {
		// Player join event handler
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (UOConfig.isMigrationEnabled()) {
				InventoryUtils.loadConfig();
				BlockUtils.loadConfig();
				processMigrationForPlayer(handler.player);
			}
		});

		// Server start event handler
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (UOConfig.isMigrationEnabled()) {
				LOGGER.info("Migration enabled in config, processing on server start");
				InventoryUtils.loadConfig();
				BlockUtils.loadConfig();
				processWorldMigrations(server);
			}
		});

		// World load event handler - ensure blocks are processed when worlds are loaded
		ServerWorldEvents.LOAD.register((server, world) -> {
			if (UOConfig.isMigrationEnabled()) {
				LOGGER.info("Migration enabled in config, processing newly loaded world: {}",
						world.getRegistryKey());
				InventoryUtils.loadConfig();
				BlockUtils.loadConfig();
				BlockUtils.replaceAllBlocksInWorld(world);
			}
		});
	}
}