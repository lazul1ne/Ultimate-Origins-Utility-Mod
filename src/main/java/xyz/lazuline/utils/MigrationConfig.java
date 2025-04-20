package xyz.lazuline.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import xyz.lazuline.UltimateOriginsUtilityMod;

import java.io.*;

public class MigrationConfig {
    private static final String CONFIG_PATH = "config/uo-utils.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static MigrationConfig INSTANCE;

    public boolean migration = true;

    public static MigrationConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static MigrationConfig load() {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            MigrationConfig config = new MigrationConfig();
            config.save();
            INSTANCE = config;
            return config;
        }

        try (FileReader reader = new FileReader(file)) {
            MigrationConfig config = GSON.fromJson(reader, MigrationConfig.class);
            if (config == null) {
                config = new MigrationConfig();
            }
            INSTANCE = config;
            return config;
        } catch (IOException e) {
            UltimateOriginsUtilityMod.LOGGER.error("Failed to load migration config: {}", e.getMessage());
            INSTANCE = new MigrationConfig();
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
            UltimateOriginsUtilityMod.LOGGER.error("Failed to save migration config: {}", e.getMessage());
        }
    }

    public static boolean isMigrationEnabled() {
        return getInstance().migration;
    }

    public static void setMigrationEnabled(boolean enabled) {
        getInstance().migration = enabled;
        getInstance().save();
    }
}