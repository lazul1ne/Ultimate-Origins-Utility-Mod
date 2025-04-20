package xyz.lazuline.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import xyz.lazuline.UltimateOriginsUtilityMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

// Utilities for replacing blocks in loaded chunks using JSON config
// Keeps inventory and NBT data for block entities
public class BlockUtils {
    private static final String CONFIG_PATH = "config/uo-block-replace.json";
    private static Map<String, String> blockMap = Collections.emptyMap();

    // Loads block replacement config, creates sample if missing
    public static void loadConfig() {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            try {
                File configDir = file.getParentFile();
                if (configDir != null && !configDir.exists()) configDir.mkdirs();
                Map<String, String> sample = new HashMap<>();
                sample.put("minecraft:chest", "minecraft:barrel");
                sample.put("minecraft:shulker_box", "minecraft:ender_chest");
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(new Gson().toJson(sample));
                }
                UltimateOriginsUtilityMod.LOGGER.info("uo-block-replace.json config created at " + file.getAbsolutePath());
                blockMap = sample;
            } catch (IOException e) {
                UltimateOriginsUtilityMod.LOGGER.error("Failed to create uo-block-replace.json: " + e.getMessage());
                blockMap = Collections.emptyMap();
            }
        } else {
            try (FileReader reader = new FileReader(file)) {
                Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                blockMap = new Gson().fromJson(reader, mapType);
                if (blockMap == null) blockMap = Collections.emptyMap();
                UltimateOriginsUtilityMod.LOGGER.info("uo-block-replace.json loaded with " + blockMap.size() + " entries");
            } catch (IOException e) {
                UltimateOriginsUtilityMod.LOGGER.error("Failed to load uo-block-replace.json: " + e.getMessage());
                blockMap = Collections.emptyMap();
            }
        }
    }

    // Returns unmodifiable block replacement map
    public static Map<String, String> getBlockMap() {
        return blockMap;
    }

    // Replaces matching blocks in loaded chunks, keeps NBT data
    public static void replaceAllBlocksInWorld(ServerWorld world) {
        if (blockMap == null || blockMap.isEmpty()) return;

        ServerChunkManager chunkManager = world.getChunkManager();
        int replacedCount = 0;

        // Get all loaded chunks by checking chunks in reasonable range
        int radius = Math.max(32, world.getServer().getPlayerManager().getViewDistance() * 2);

        // Process spawn area chunks first
        BlockPos spawnPos = world.getSpawnPos();
        int spawnChunkX = spawnPos.getX() >> 4;
        int spawnChunkZ = spawnPos.getZ() >> 4;

        // Process chunks around each player
        List<ServerPlayerEntity> players = world.getPlayers();
        List<ChunkPos> chunksToCheck = new ArrayList<>();

        // Add spawn chunks
        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 16; z++) {
                chunksToCheck.add(new ChunkPos(spawnChunkX + x, spawnChunkZ + z));
            }
        }

        // Add chunks around each player
        for (ServerPlayerEntity player : players) {
            int playerChunkX = player.getBlockPos().getX() >> 4;
            int playerChunkZ = player.getBlockPos().getZ() >> 4;
            int viewDistance = world.getServer().getPlayerManager().getViewDistance();

            for (int x = -viewDistance; x <= viewDistance; x++) {
                for (int z = -viewDistance; z <= viewDistance; z++) {
                    chunksToCheck.add(new ChunkPos(playerChunkX + x, playerChunkZ + z));
                }
            }
        }

        // If no players, expand spawn chunk radius
        if (players.isEmpty()) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    chunksToCheck.add(new ChunkPos(spawnChunkX + x, spawnChunkZ + z));
                }
            }
        }

        // Debug logging
        UltimateOriginsUtilityMod.LOGGER.info("Checking " + chunksToCheck.size() + " potential chunks in world " +
                world.getRegistryKey().getValue());

        // Process chunks
        int checkedCount = 0;
        int loadedCount = 0;

        for (ChunkPos chunkPos : chunksToCheck.stream().distinct().collect(Collectors.toList())) {
            checkedCount++;

            // Only process if chunk is loaded
            if (world.isChunkLoaded(chunkPos.x, chunkPos.z)) {
                loadedCount++;
                WorldChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);

                if (chunk != null) {
                    int startX = chunk.getPos().getStartX();
                    int startZ = chunk.getPos().getStartZ();
                    int endX = chunk.getPos().getEndX();
                    int endZ = chunk.getPos().getEndZ();
                    int minY = world.getBottomY();
                    int maxY = world.getTopY();

                    for (int x = startX; x <= endX; x++) {
                        for (int z = startZ; z <= endZ; z++) {
                            for (int y = minY; y < maxY; y++) {
                                BlockPos pos = new BlockPos(x, y, z);
                                BlockState state = world.getBlockState(pos);
                                String oldId = Registries.BLOCK.getId(state.getBlock()).toString();
                                String newId = blockMap.get(oldId);
                                if (newId != null) {
                                    Block newBlock = Registries.BLOCK.get(new Identifier(newId));
                                    if (newBlock != null && newBlock != state.getBlock()) {
                                        // Handle block entities
                                        BlockEntity be = world.getBlockEntity(pos);
                                        NbtCompound nbt = null;
                                        String oldBeId = null;
                                        if (be != null) {
                                            nbt = be.createNbtWithId();
                                            oldBeId = nbt.getString("id");
                                            world.removeBlockEntity(pos);
                                        }

                                        world.setBlockState(pos, newBlock.getDefaultState());

                                        // Restore NBT if new block is also a block entity
                                        BlockEntity newBe = world.getBlockEntity(pos);
                                        if (nbt != null && newBe != null) {
                                            // Update NBT "id" if block entity type changed
                                            String newBeId = BlockEntityType.getId(newBe.getType()).toString();
                                            if (!oldBeId.equals(newBeId)) {
                                                nbt.putString("id", newBeId);
                                            }
                                            newBe.readNbt(nbt);
                                            newBe.markDirty();
                                        }
                                        replacedCount++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        UltimateOriginsUtilityMod.LOGGER.info("BlockReplacementUtils: checked " + checkedCount +
                " chunks, found " + loadedCount + " loaded chunks, replaced " +
                replacedCount + " blocks in world '" +
                world.getRegistryKey().getValue() + "' with " +
                blockMap.size() + " mappings.");
    }
}