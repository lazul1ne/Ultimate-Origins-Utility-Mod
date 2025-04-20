package xyz.lazuline.utils;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import xyz.lazuline.UltimateOriginsUtilityMod;

public class StrippedLogFinder {
    public static void register() {
        UltimateOriginsUtilityMod.LOGGER.info("==== Stripped Log Candidates (for tag masterlist) ====");
        int count = 0;
        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item);
            String path = id.getPath();
            if (path.contains("stripped_") && path.contains("_log")) {
                UltimateOriginsUtilityMod.LOGGER.info(id.toString());
                count++;
            }
        }
        UltimateOriginsUtilityMod.LOGGER.info("==== End of Stripped Log List (found: " + count + ") ====");
    }
}