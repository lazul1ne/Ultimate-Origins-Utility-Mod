package xyz.lazuline.utils;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import xyz.lazuline.UltimateOriginsUtilityMod;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StorageUtils {
    public static void replaceAllAroundPlayer(ServerPlayerEntity player) {
        // Make sure we reload the config to have the latest mappings
        InventoryUtils.loadConfig();
        Map<String, String> mappings = InventoryUtils.getItemMap();

        if (mappings == null || mappings.isEmpty()) {
            UltimateOriginsUtilityMod.LOGGER.warn("No item mappings found!");
            return;
        }

        UltimateOriginsUtilityMod.LOGGER.info("Starting item replacement with " + mappings.size() + " mappings");

        ServerWorld world = (ServerWorld) player.getWorld();
        ServerChunkManager chunkManager = world.getChunkManager();

        // Get player's absolute position
        int playerX = player.getBlockPos().getX();
        int playerZ = player.getBlockPos().getZ();

        // Convert to chunk coordinates
        int playerChunkX = playerX >> 4;
        int playerChunkZ = playerZ >> 4;

        int viewDistance = world.getServer().getPlayerManager().getViewDistance();

        UltimateOriginsUtilityMod.LOGGER.info(
                "Processing chunks around player {} at position (x={}, z={}) [chunk: {}, {}] with view distance {}",
                player.getName().getString(), playerX, playerZ, playerChunkX, playerChunkZ, viewDistance
        );
        AtomicInteger inventoriesProcessed = new AtomicInteger();
        AtomicInteger inventoriesModified = new AtomicInteger();

        // Process chunks in view distance using absolute chunk coordinates
        for (int dx = -viewDistance; dx <= viewDistance; dx++) {
            for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                // Calculate absolute chunk coordinates
                int targetChunkX = playerChunkX + dx;
                int targetChunkZ = playerChunkZ + dz;

                // Skip chunks outside the circular view distance
                if (dx * dx + dz * dz > viewDistance * viewDistance) {
                    continue;
                }

                // Load the chunk explicitly
                WorldChunk chunk = world.getChunk(targetChunkX, targetChunkZ);
                if (chunk != null && chunk.getStatus() == ChunkStatus.FULL) {
                    // Get all block entities in the chunk
                    chunk.getBlockEntities().forEach((pos, be) -> {
                        if (be instanceof Inventory inv) {
                            UltimateOriginsUtilityMod.LOGGER.debug(
                                    String.format("Found inventory at %s in chunk [%d, %d]",
                                            pos.toString(), targetChunkX, targetChunkZ)
                            );

                            if (processInventory(inv, mappings)) {
                                be.markDirty();
                                inventoriesModified.getAndIncrement();
                            }
                            inventoriesProcessed.getAndIncrement();
                        }
                    });
                }
            }
        }

        UltimateOriginsUtilityMod.LOGGER.info(String.format(
                "Processed world '%s' around player %s. Found %d inventories, modified %d.",
                world.getRegistryKey().getValue(), player.getName().getString(),
                inventoriesProcessed, inventoriesModified
        ));
    }

    private static boolean processInventory(Inventory inv, Map<String, String> mappings) {
        boolean changed = false;

        for (int slot = 0; slot < inv.size(); slot++) {
            ItemStack stack = inv.getStack(slot);
            if (!stack.isEmpty()) {
                Identifier oldId = Registry.ITEM.getId(stack.getItem());
                String newIdStr = mappings.get(oldId.toString());

                if (newIdStr != null) {
                    UltimateOriginsUtilityMod.LOGGER.debug(
                            String.format("Found item to replace: %s -> %s",
                                    oldId.toString(), newIdStr)
                    );

                    try {
                        Item newItem = Registry.ITEM.get(new Identifier(newIdStr));
                        if (newItem != null) {
                            ItemStack newStack = new ItemStack(newItem, stack.getCount());
                            // Preserve NBT data if it exists
                            if (stack.hasNbt()) {
                                newStack.setNbt(stack.getNbt().copy());
                            }
                            inv.setStack(slot, newStack);
                            changed = true;
                        }
                    } catch (Exception e) {
                        UltimateOriginsUtilityMod.LOGGER.error(
                                String.format("Failed to replace item %s with %s: %s",
                                        oldId.toString(), newIdStr, e.getMessage())
                        );
                    }
                }
            }
        }

        return changed;
    }
}