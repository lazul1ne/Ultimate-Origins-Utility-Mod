package xyz.lazuline;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.item.Item;

import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

/*public class UoUtilsDataGen implements net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint {
   @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider((dataOutput, registryLookup) -> new StrippedLogsTagProvider(dataOutput, registryLookup));
    }

    private static class StrippedLogsTagProvider extends FabricTagProvider<Item> {
        public StrippedLogsTagProvider(net.fabricmc.fabric.api.datagen.v1.FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
            super(dataOutput, RegistryKeys.ITEM, registryLookup);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup registries) {
            TagKey<Item> STRIPPED_LOGS = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "stripped_logs"));

            for (Item item : Registries.ITEM) {
                Identifier id = Registries.ITEM.getId(item);
                String path = id.getPath();
                if (path.startsWith("stripped_") && path.endsWith("_log")) {
                    getOrCreateTagBuilder(STRIPPED_LOGS).add(item);
                }
            }
        }
    }
}*/