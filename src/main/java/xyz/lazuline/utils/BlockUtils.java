package xyz.lazuline.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import xyz.lazuline.UltimateOriginsUtilityMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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

            sample.put("charm:azalea_chest", "minecraft:chest");
            sample.put("charm:azalea_barrel", "minecraft:barrel");

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

        ServerWorld world = (ServerWorld) player.getWorld();

        // Player position calculations
        int playerX = player.getBlockPos().getX();
        int playerZ = player.getBlockPos().getZ();
        int playerChunkX = playerX >> 4;
        int playerChunkZ = playerZ >> 4;
        int viewDistance = world.getServer().getPlayerManager().getViewDistance();

        UltimateOriginsUtilityMod.LOGGER.info(
                "Processing blocks around player {} at position (x={}, z={}) [chunk: {}, {}] with view distance {}",
                player.getName().getString(), playerX, playerZ, playerChunkX, playerChunkZ, viewDistance
        );
        AtomicInteger blocksProcessed = new AtomicInteger();
        AtomicInteger chunksProcessed = new AtomicInteger();
        AtomicInteger blocksReplaced = new AtomicInteger();

        // Process chunks in player's view distance
        for (int dx = -viewDistance; dx <= viewDistance; dx++) {
            for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                int targetChunkX = playerChunkX + dx;
                int targetChunkZ = playerChunkZ + dz;

                // Skip chunks outside of render distance
                if (dx * dx + dz * dz > viewDistance * viewDistance) {
                    continue;
                }

                WorldChunk chunk = world.getChunk(targetChunkX, targetChunkZ);
                if (chunk != null && chunk.getStatus() == ChunkStatus.FULL) {
                    chunksProcessed.incrementAndGet();

                    // Process block entities first
                    chunk.getBlockEntities().forEach((pos, be) -> {
                        blocksProcessed.incrementAndGet();
                        if (processBlock(world, pos)) {
                            blocksReplaced.incrementAndGet();
                        }
                    });

                    // Scan chunk for target blocks
                    processBlocksInChunk(world, chunk, blocksProcessed, blocksReplaced);
                }
            }
        }

        UltimateOriginsUtilityMod.LOGGER.info(String.format(
                "Processed %d chunks, %d blocks around player %s. Replaced %d blocks.",
                chunksProcessed.get(), blocksProcessed.get(), player.getName().getString(), blocksReplaced.get()
        ));
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

        UltimateOriginsUtilityMod.LOGGER.info("Processing spawn chunks in world: {}", world.getRegistryKey());

        // Process spawn area
        AtomicInteger blocksProcessed = new AtomicInteger();
        AtomicInteger chunksProcessed = new AtomicInteger();
        AtomicInteger blocksReplaced = new AtomicInteger();
        int spawnRadius = 8;

        for (int dx = -spawnRadius; dx <= spawnRadius; dx++) {
            for (int dz = -spawnRadius; dz <= spawnRadius; dz++) {
                int targetChunkX = spawnChunkX + dx;
                int targetChunkZ = spawnChunkZ + dz;

                if (dx * dx + dz * dz > spawnRadius * spawnRadius) {
                    continue;
                }

                WorldChunk chunk = world.getChunk(targetChunkX, targetChunkZ);
                if (chunk != null && chunk.getStatus() == ChunkStatus.FULL) {
                    chunksProcessed.incrementAndGet();

                    List<BlockPos> positions = new ArrayList<>(chunk.getBlockEntities().keySet());
                    positions.forEach(pos -> {
                        blocksProcessed.incrementAndGet();
                        if (processBlock(world, pos)) {
                            blocksReplaced.incrementAndGet();
                        }
                    });

                    processBlocksInChunk(world, chunk, blocksProcessed, blocksReplaced);
                }
            }
        }

        UltimateOriginsUtilityMod.LOGGER.info(String.format(
                "Processed %d chunks, %d blocks in spawn area. Replaced %d blocks.",
                chunksProcessed.get(), blocksProcessed.get(), blocksReplaced.get()
        ));

        // Process player areas
        for (ServerPlayerEntity player : world.getPlayers()) {
            replaceAllBlocksAroundPlayer(player);
        }
    }

    private static void processBlocksInChunk(ServerWorld world, WorldChunk chunk, AtomicInteger blocksProcessed, AtomicInteger blocksReplaced) {
        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();
        int endX = chunk.getPos().getEndX();
        int endZ = chunk.getPos().getEndZ();
        int minY = world.getBottomY();
        int maxY = world.getTopY();

        // Focus on target blocks for efficiency
        Collection<Block> blocksToLookFor = new HashSet<>();
        for (String blockId : blockMap.keySet()) {
            try {
                Block block = Registry.BLOCK.get(new Identifier(blockId));
                if (block != null) {
                    blocksToLookFor.add(block);
                }
            } catch (Exception e) {
                UltimateOriginsUtilityMod.LOGGER.error("Invalid block ID in config: " + blockId, e);
            }
        }

        // Targeted block scanning
        if (!blocksToLookFor.isEmpty()) {
            for (int x = startX; x <= endX; x++) {
                for (int z = startZ; z <= endZ; z++) {
                    for (int y = minY; y < maxY; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = chunk.getBlockState(pos);

                        if (blocksToLookFor.contains(state.getBlock())) {
                            blocksProcessed.incrementAndGet();
                            if (processBlock(world, pos)) {
                                blocksReplaced.incrementAndGet();
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean processBlock(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        String oldId = Registry.BLOCK.getId(state.getBlock()).toString();
        String newId = blockMap.get(oldId);

        if (newId == null) {
            return false;
        }

        Block newBlock = Registry.BLOCK.get(new Identifier(newId));
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
                UltimateOriginsUtilityMod.LOGGER.debug("Replacing block entity {} at {} with {}",
                        oldId, pos, newId);
                world.removeBlockEntity(pos);
            } else {
                UltimateOriginsUtilityMod.LOGGER.debug("Replacing block {} at {} with {}",
                        oldId, pos, newId);
            }

            boolean result = world.setBlockState(pos, newBlock.getDefaultState());

            // Restore NBT data
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

            return result;
        } catch (Exception e) {
            UltimateOriginsUtilityMod.LOGGER.error(
                    String.format("Failed to replace block at %s: %s -> %s",
                            pos, oldId, newId), e
            );
            return false;
        }
    }
}