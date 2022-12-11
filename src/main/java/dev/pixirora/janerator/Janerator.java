package dev.pixirora.janerator;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Maps;

public class Janerator {
    private static MinecraftServer server = null;
    private static Map<ResourceKey<Level>, ChunkGenerator> generators = Maps.newHashMapWithExpectedSize(3);
    private static double log_phi = Math.log((1 + Math.sqrt(5)) / 2);

    public static boolean shouldOverride(int x, int z) {
        double angle = Math.PI / 4 * Math.log(Math.pow(x, 2) + Math.pow(z, 2)) / log_phi + Math.PI;
        double tan_angle = Math.tan(angle);
        return (x * tan_angle - z) * Math.signum(tan_angle / Math.sin(angle)) > 0;
    }

    public static boolean shouldOverride(ChunkPos chunkPos) {
        return shouldOverride(chunkPos.x, chunkPos.z);
    }

    public static void setMinecraftServer(MinecraftServer server) {
        Janerator.server = server;
    }

    public static synchronized ChunkGenerator getGenerator(ResourceKey<Level> dimension) {
        if (Janerator.generators.isEmpty()) {
            initializeGenerators();
        }

        ChunkGenerator generator = Janerator.generators.get(dimension);
        return generator != null ? generator : Janerator.generators.get(Level.OVERWORLD);
    }

    public static ChunkGenerator getGeneratorAt(
        ChunkPos chunkPos, 
        ResourceKey<Level> dimension,
        ChunkGenerator defaultGenerator
    ) {
        HashMap<ChunkGenerator, Integer> scores = Maps.newHashMap();
        ArrayList<ArrayList<? extends ChunkGenerator>> generatorMap = new ArrayList<ArrayList<? extends ChunkGenerator>>();

        ChunkGenerator modifiedGenerator = getGenerator(dimension);

        int actual_x = chunkPos.x * 16;
        int actual_z = chunkPos.z * 16;

        for (int x = actual_x; x < actual_x + 16; x++) {
            ArrayList<ChunkGenerator> generatorLine = new ArrayList<ChunkGenerator>();

            for (int z = actual_z; z < actual_z + 16; z++) {
                ChunkGenerator generatorAtPos = shouldOverride(x, z) ? modifiedGenerator : defaultGenerator;

                generatorLine.add(generatorAtPos);
                scores.put(generatorAtPos, scores.getOrDefault(generatorAtPos, 0) + 1);
            }

            generatorMap.add(generatorLine);
        }

        ChunkGenerator majorityGenerator = Collections
                .max(scores.entrySet(), (entry1, entry2) -> entry1.getValue() - entry2.getValue()).getKey();

        if (scores.keySet().size() == 1) {
            return majorityGenerator;
        } else {
            return new MultiGenerator(generatorMap, majorityGenerator);
        }
    }

    private static <T> Registry<T> getRegistry(ResourceKey<? extends Registry<? extends T>> registryKey) {
        return Janerator.server.registryAccess().registryOrThrow(registryKey);
    }

    private static void initializeGenerators() {
        Janerator.generators.put(Level.OVERWORLD, createOverworldGenerator());
        Janerator.generators.put(Level.NETHER, createNetherGenerator());
        Janerator.generators.put(Level.END, createEndGenerator());
    }

    private static FlatLevelSource createOverworldGenerator() {
        List<FlatLayerInfo> layers = new ArrayList<>();

        layers.add(new FlatLayerInfo(1, Blocks.BEDROCK));
        layers.add(new FlatLayerInfo(63, Blocks.DEEPSLATE));

        layers.add(new FlatLayerInfo(60, Blocks.STONE));
        layers.add(new FlatLayerInfo(2, Blocks.DIRT));
        layers.add(new FlatLayerInfo(1, Blocks.GRASS_BLOCK));

        return createGenerator(layers, Biomes.MUSHROOM_FIELDS);
    }

    private static FlatLevelSource createNetherGenerator() {
        List<FlatLayerInfo> layers = new ArrayList<>();

        layers.add(new FlatLayerInfo(1, Blocks.BEDROCK));

        layers.add(new FlatLayerInfo(30, Blocks.NETHERRACK));
        layers.add(new FlatLayerInfo(1, Blocks.WARPED_NYLIUM));

        return createGenerator(layers, Biomes.DEEP_DARK);
    }

    private static FlatLevelSource createEndGenerator() {
        List<FlatLayerInfo> layers = new ArrayList<>();

        layers.add(new FlatLayerInfo(1, Blocks.BEDROCK));

        layers.add(new FlatLayerInfo(59, Blocks.STONE));
        layers.add(new FlatLayerInfo(2, Blocks.DIRT));
        layers.add(new FlatLayerInfo(1, Blocks.GRASS_BLOCK));

        return createGenerator(layers, Biomes.DEEP_DARK);
    }

    private static FlatLevelSource createGenerator(List<FlatLayerInfo> layers, ResourceKey<Biome> biome) {
        List<Holder<PlacedFeature>> placedFeatures = List.of();
        Optional<HolderSet<StructureSet>> optional = Optional.of(HolderSet.direct());

        Holder<Biome> biomeHolder = Janerator.getRegistry(Registries.BIOME).getHolderOrThrow(biome);
        return new FlatLevelSource(new FlatLevelGeneratorSettings(optional, biomeHolder, placedFeatures)
                .withBiomeAndLayers(layers, optional, biomeHolder));
    }
}
