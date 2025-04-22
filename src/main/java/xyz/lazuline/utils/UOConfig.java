package xyz.lazuline.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import xyz.lazuline.UltimateOriginsUtilityMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class UOConfig {
    private static final String CONFIG_PATH = "config/uo-utils.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static UOConfig INSTANCE;

    public boolean migration = true;
    public boolean finder = false;

    public static UOConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static UOConfig load() {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            UOConfig config = new UOConfig();
            config.save();
            INSTANCE = config;
            return config;
        }

        try (FileReader reader = new FileReader(file)) {
            UOConfig config = GSON.fromJson(reader, UOConfig.class);
            if (config == null) {
                config = new UOConfig();
            }
            INSTANCE = config;
            return config;
        } catch (IOException e) {
            UltimateOriginsUtilityMod.LOGGER.error("Failed to load config: {}", e.getMessage());
            INSTANCE = new UOConfig();
            return INSTANCE;
        }
    }

    public void save() {
        File file = new File(CONFIG_PATH);
        try {
            File configDir = file.getParentFile();
            if (configDir != null && !configDir.exists()) {
                configDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            UltimateOriginsUtilityMod.LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }

    public static boolean isMigrationEnabled() {
        return getInstance().migration;
    }
    public static boolean isBlockIdFinderEnabled() {
        return getInstance().finder;
    }

    public static void setMigrationEnabled(boolean enabled) {
        getInstance().migration = enabled;
        getInstance().save();
    }
}