package xyz.lazuline.utils;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import xyz.lazuline.UltimateOriginsUtilityMod;

import java.util.Arrays;
import java.util.List;

public class BlockIDFinder {
    // Define search criteria here
    private static final String TARGET_MOD = "charm"; // Set to null to search all mods, or specify a mod ID like "minecraft"
    private static final List<String> SEARCH_TERMS = Arrays.asList(
          //  "stripped_",
          //  "_log"
          //  "chest",
            "barrel"
    );

    public static void register() {
        UltimateOriginsUtilityMod.LOGGER.info("==== Start of Block ID Finder list ====");
        UltimateOriginsUtilityMod.LOGGER.info("Searching in " + (TARGET_MOD != null ? "mod: " + TARGET_MOD : "all mods"));
        int count = 0;

        for (Item item : Registry.ITEM) {
            Identifier id = Registry.ITEM.getId(item);
            String namespace = id.getNamespace();
            String path = id.getPath();

            // Skip if a target mod is specified and this item isn't from that mod
            if (TARGET_MOD != null && !namespace.equals(TARGET_MOD)) {
                continue;
            }

            // Check if the item matches all search terms
            boolean matchesAll = SEARCH_TERMS.stream()
                    .allMatch(term -> path.contains(term));

            if (matchesAll) {
                UltimateOriginsUtilityMod.LOGGER.info(namespace + ":" + path);
                count++;
            }
        }

        UltimateOriginsUtilityMod.LOGGER.info("==== End of Block Id Finder List (found: " + count + ") ====");
    }
}