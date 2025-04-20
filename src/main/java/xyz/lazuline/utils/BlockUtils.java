package xyz.lazuline.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
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

public class BlockUtils {
    private static final String CONFIG_PATH = "config/uo-block-replace.json";
    private static Map<String, String> blockMap = Collections.emptyMap();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void loadConfig() {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            createDefaultConfig(file);
        } else {
            loadExistingConfig(file);
        }
    }

    private static void createDefaultConfig(File file) {
        try {
            File configDir = file.getParentFile();
            if (configDir != null && !configDir.exists()) {
                configDir.mkdirs();
            }

            Map<String, String> sample = new HashMap<>();
            sample.put("minecraft:chest", "minecraft:barrel");
            sample.put("minecraft:shulker_box", "minecraft:ender_chest");

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(sample, writer);
            }

            UltimateOriginsUtilityMod.LOGGER.info("Created default config at: " + file.getAbsolutePath());
            blockMap = sample;
        } catch (IOException e) {
            UltimateOriginsUtilityMod.LOGGER.error("Failed to create config: " + e.getMessage(), e);
            blockMap = Collections.emptyMap();
        }
    }

    private static void loadExistingConfig(File file) {
        try (FileReader reader = new FileReader(file)) {
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            blockMap = GSON.fromJson(reader, mapType);

            if (blockMap == null) {
                blockMap = Collections.emptyMap();
            }

            UltimateOriginsUtilityMod.LOGGER.info("Loaded " + blockMap.size() + " block mappings from config");
        } catch (IOException e) {
            UltimateOriginsUtilityMod.LOGGER.error("Failed to load config: " + e.getMessage(), e);
            blockMap = Collections.emptyMap();
        }
    }

    public static Map<String, String> getBlockMap() {
        return Collections.unmodifiableMap(blockMap);
    }

    public static void replaceAllBlocksAroundPlayer(ServerPlayerEntity player) {
        if (blockMap.isEmpty()) {
            UltimateOriginsUtilityMod.LOGGER.warn("No block mappings found!");
            return;
        }

        ServerWorld world = player.getServerWorld();
        int playerChunkX = player.getBlockPos().getX() >> 4;
        int playerChunkZ = player.getBlockPos().getZ() >> 4;
        int viewDistance = world.getServer().getPlayerManager().getViewDistance();

        UltimateOriginsUtilityMod.LOGGER.info(String.format(
                "Processing blocks around player %s at chunk position (%d, %d) with view distance %d",
                player.getName().getString(), playerChunkX, playerChunkZ, viewDistance
        ));

        processChunksInRange(world, playerChunkX, playerChunkZ, viewDistance, true);
    }

    public static void replaceAllBlocksInWorld(ServerWorld world) {
        if (blockMap.isEmpty()) {
            UltimateOriginsUtilityMod.LOGGER.warn("No block mappings found!");
            return;
        }

        ServerChunkManager chunkManager = world.getChunkManager();
        BlockPos spawnPos = world.getSpawnPos();
        int spawnChunkX = spawnPos.getX() >> 4;
        int spawnChunkZ = spawnPos.getZ() >> 4;

        // Process spawn chunks first
        processChunksInRange(world, spawnChunkX, spawnChunkZ, 16, false);

        // Process chunks around each player
        for (ServerPlayerEntity player : world.getPlayers()) {
            int playerChunkX = player.getBlockPos().getX() >> 4;
            int playerChunkZ = player.getBlockPos().getZ() >> 4;
            int viewDistance = world.getServer().getPlayerManager().getViewDistance();

            processChunksInRange(world, playerChunkX, playerChunkZ, viewDistance, true);
        }
    }

    private static void processChunksInRange(ServerWorld world, int centerX, int centerZ, int radius, boolean isPlayer) {
        Set<ChunkPos> processedChunks = new HashSet<>();
        int replacedCount = 0;
        int chunksProcessed = 0;
        int chunksLoaded = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int chunkX = centerX + dx;
                int chunkZ = centerZ + dz;
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);

                if (processedChunks.contains(pos)) {
                    continue;
                }

                processedChunks.add(pos);
                chunksProcessed++;

                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }

                chunksLoaded++;
                WorldChunk chunk = world.getChunk(chunkX, chunkZ);
                replacedCount += processChunk(world, chunk);
            }
        }

        String location = isPlayer ? "around player" : "in spawn area";
        UltimateOriginsUtilityMod.LOGGER.info(String.format(
                "Processed %d chunks (%d loaded) %s, replaced %d blocks",
                chunksProcessed, chunksLoaded, location, replacedCount
        ));
    }

    private static int processChunk(ServerWorld world, WorldChunk chunk) {
        int replacedCount = 0;
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
                    if (processBlock(world, pos)) {
                        replacedCount++;
                    }
                }
            }
        }

        return replacedCount;
    }

    private static boolean processBlock(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        String oldId = Registries.BLOCK.getId(state.getBlock()).toString();
        String newId = blockMap.get(oldId);

        if (newId == null) {
            return false;
        }

        Block newBlock = Registries.BLOCK.get(new Identifier(newId));
        if (newBlock == null || newBlock == state.getBlock()) {
            return false;
        }

        try {
            // Handle block entity data
            BlockEntity oldBe = world.getBlockEntity(pos);
            NbtCompound nbt = null;
            String oldBeId = null;

            if (oldBe != null) {
                nbt = oldBe.createNbtWithId();
                oldBeId = nbt.getString("id");
                world.removeBlockEntity(pos);
            }

            // Set new block
            world.setBlockState(pos, newBlock.getDefaultState());

            // Restore block entity data if applicable
            if (nbt != null) {
                BlockEntity newBe = world.getBlockEntity(pos);
                if (newBe != null) {
                    String newBeId = BlockEntityType.getId(newBe.getType()).toString();
                    if (!oldBeId.equals(newBeId)) {
                        nbt.putString("id", newBeId);
                    }
                    newBe.readNbt(nbt);
                    newBe.markDirty();
                }
            }

            return true;
        } catch (Exception e) {
            UltimateOriginsUtilityMod.LOGGER.error(
                    String.format("Failed to replace block at %s: %s -> %s",
                            pos, oldId, newId), e
            );
            return false;
        }
    }
}