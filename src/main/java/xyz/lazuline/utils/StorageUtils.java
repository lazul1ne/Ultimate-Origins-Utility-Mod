package xyz.lazuline.utils;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import xyz.lazuline.UltimateOriginsUtilityMod;
import xyz.lazuline.utils.InventoryUtils;

import java.util.Map;

public class StorageUtils {
    public static void replaceAllInWorld(ServerWorld world) {
        Map<String, String> mappings = InventoryUtils.getItemMap();
        if (mappings == null || mappings.isEmpty()) return;

        ServerChunkManager chunkManager = world.getChunkManager();

// Iterates over chunk in a specific range (loaded areas)
        for (int chunkX = -16; chunkX <= 16; chunkX++) { // Adjust the range as its needed
            for (int chunkZ = -16; chunkZ <= 16; chunkZ++) {
                // Check if the chunk is loaded and ticking  (would probably crash if we didnt)
                if (chunkManager.isChunkLoaded(chunkX, chunkZ) &&
                        chunkManager.isTickingFutureReady(ChunkPos.toLong(chunkX, chunkZ))) {

                    WorldChunk chunk = chunkManager.getWorldChunk(chunkX, chunkZ);
                    if (chunk != null) {
                        for (BlockEntity be : chunk.getBlockEntities().values()) {
                            if (be instanceof Inventory inv) {
                                boolean dirty = replaceInInventory(inv, mappings);
                                if (dirty) be.markDirty();
                            }
                        }
                    }
                }
            }
        }

        // todo: add checks for items in item frames and dropped on the ground but probably in a seperate file.



        UltimateOriginsUtilityMod.LOGGER.info("processed world '" +
                world.getRegistryKey().getValue() + "' with " + mappings.size() + " mappings.");
    }

    private static boolean replaceInInventory(Inventory inv, Map<String, String> mappings) {
        boolean changed = false;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            ItemStack repl = getReplacedStack(stack, mappings);
            if (repl != null) {
                inv.setStack(i, repl);
                changed = true;
            }
        }
        return changed;
    }

    private static ItemStack getReplacedStack(ItemStack stack, Map<String, String> mappings) {
        if (stack.isEmpty()) return null;
        String oldId = Registries.ITEM.getId(stack.getItem()).toString();
        String newId = mappings.get(oldId);
        if (newId == null) return null;

        Item newItem = Registries.ITEM.get(new Identifier(newId));
        if (newItem == null) return null;

        ItemStack out = new ItemStack(newItem, stack.getCount());
        if (stack.hasNbt()) out.setNbt(stack.getNbt().copy());
        return out;
    }
}