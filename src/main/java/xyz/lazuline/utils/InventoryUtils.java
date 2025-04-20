package xyz.lazuline.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.entity.player.PlayerEntity;
import xyz.lazuline.UltimateOriginsUtilityMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InventoryUtils {
    private static final Gson GSON = new Gson();
    private static final String CONFIG_FILE = "config/uo-utils.json";
    private static Map<String, String> itemMap = Collections.emptyMap();

    // Loads the config file, makes a new one if it cant find it.
    public static void loadConfig() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            try {
                File configDir = file.getParentFile();
                if (configDir != null && !configDir.exists()) {
                    configDir.mkdirs();
                }
                // Write placeholder so it doesn't crash
                Map<String, String> sample = new HashMap<>();
                sample.put("minecraft:bedrock", "minecraft:dirt");
                sample.put("minecraft:stick", "minecraft:stick");
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(GSON.toJson(sample));
                }
                UltimateOriginsUtilityMod.LOGGER.info("uo-utils.json config created at " + file.getAbsolutePath());
                itemMap = sample;
            } catch (IOException e) {
                UltimateOriginsUtilityMod.LOGGER.error("Failed to create uo-utils.json: " + e.getMessage());
                itemMap = Collections.emptyMap();
            }
        } else {
            try (FileReader reader = new FileReader(file)) {
                Type mapType = new TypeToken<Map<String, String>>(){}.getType();
                itemMap = GSON.fromJson(reader, mapType);
                if (itemMap == null) itemMap = Collections.emptyMap();
                UltimateOriginsUtilityMod.LOGGER.info("uo-utils.json loaded with " + itemMap.size() + " entries");
            } catch (IOException | JsonSyntaxException e) {
                UltimateOriginsUtilityMod.LOGGER.error("Failed to load uo-utils.json: " + e.getMessage());
                itemMap = Collections.emptyMap();
            }
        }
    }
    public static Map<String, String> getItemMap() {
        return itemMap;
    }

    // replaces the items using the config
    public static void replaceAll(PlayerEntity player) {
        if (itemMap.isEmpty()) return;
        loadConfig();

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                Identifier oldId = Registries.ITEM.getId(stack.getItem());
                String newIdStr = itemMap.get(oldId.toString());
                if (newIdStr != null) {
                    Item newItem = Registries.ITEM.get(new Identifier(newIdStr));
                    if (newItem == null) continue; // Skip invalid items
                    ItemStack newStack = new ItemStack(newItem, stack.getCount());
                    if (stack.hasNbt()) newStack.setNbt(stack.getNbt().copy());
                    player.getInventory().setStack(i, newStack);
                }
            }
        }
    }
}