package io.github.moranyue.anythinginsulfurcubes.archetype;

import io.github.moranyue.anythinginsulfurcubes.AnythingInSulfurCubesPlugin;
import org.bukkit.NamespacedKey;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates Minecraft item tag JSON files for sulfur cube archetypes.
 * <p>
 * This replicates the functionality of the original data pack's
 * {@code data/<namespace>/tags/item/sulfur_cube_archetype/<archetype>.json} files.
 * <p>
 * The plugin writes these tags into its own built-in datapack so that
 * sulfur cubes in vanilla Minecraft can accept additional blocks beyond
 * the default set.
 */
public class ArchetypeTagGenerator {

    private static final String DATAPACK_NAME = "anything-in-sulfur-cubes-plugin";
    private static final int PACK_FORMAT = 101;

    private final AnythingInSulfurCubesPlugin plugin;
    private final Map<String, Set<String>> archetypeBlocks;

    /**
     * @param plugin          the plugin instance
     * @param archetypeBlocks map of archetype key -> set of block IDs (e.g. "REGULAR" -> {"minecraft:cactus", ...})
     */
    public ArchetypeTagGenerator(AnythingInSulfurCubesPlugin plugin, Map<String, Set<String>> archetypeBlocks) {
        this.plugin = plugin;
        this.archetypeBlocks = archetypeBlocks;
    }

    /**
     * Writes the built-in datapack to the server's datapacks directory.
     * This datapack contains item tag files that tell Minecraft which
     * blocks sulfur cubes can absorb and which archetype they map to.
     */
    public void generateDatapack() {
        Path datapacksDir = plugin.getServer().getWorlds().getFirst().getWorldFolder()
            .toPath().resolve("datapacks");
        Path packDir = datapacksDir.resolve(DATAPACK_NAME);

        try {
            // Clean up any previous version
            deleteDirectory(packDir);

            // Write pack.mcmeta
            Path mcmetaPath = packDir.resolve("pack.mcmeta");
            Files.createDirectories(mcmetaPath.getParent());
            String mcmeta = """
                {
                  "pack": {
                    "description": "Anything in Sulfur Cubes - Block Archetype Tags",
                    "pack_format": %d,
                    "min_format": %d,
                    "max_format": %d
                  }
                }
                """.formatted(PACK_FORMAT, PACK_FORMAT - 1, PACK_FORMAT + 1);
            Files.writeString(mcmetaPath, mcmeta);

            // Write each archetype tag file
            for (Map.Entry<String, Set<String>> entry : archetypeBlocks.entrySet()) {
                String archetype = entry.getKey().toLowerCase();
                Set<String> blocks = entry.getValue();

                if (blocks.isEmpty()) continue;

                Path tagFile = packDir.resolve("data")
                    .resolve("minecraft")
                    .resolve("tags")
                    .resolve("item")
                    .resolve("sulfur_cube_archetype")
                    .resolve(archetype + ".json");

                Files.createDirectories(tagFile.getParent());

                List<String> sortedBlocks = new ArrayList<>(blocks);
                Collections.sort(sortedBlocks);

                StringBuilder json = new StringBuilder();
                json.append("{\n");
                json.append("  \"values\": [\n");
                for (int i = 0; i < sortedBlocks.size(); i++) {
                    json.append("    \"").append(sortedBlocks.get(i)).append("\"");
                    if (i < sortedBlocks.size() - 1) {
                        json.append(",");
                    }
                    json.append("\n");
                }
                json.append("  ]\n");
                json.append("}\n");

                Files.writeString(tagFile, json.toString());
            }

            plugin.getLogger().info("Generated built-in datapack '" + DATAPACK_NAME
                + "' with " + archetypeBlocks.size() + " archetype tag files.");

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to generate sulfur cube archetype datapack: " + e.getMessage());
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            try (var stream = Files.walk(path)) {
                stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            // ignore
                        }
                    });
            }
        }
    }

    /**
     * Builds the archetype-to-blocks map from the plugin config's block archetype mappings.
     *
     * @param plugin   the plugin instance
     * @param rawMaps  raw material name -> archetype name mappings (all uppercase)
     * @return map of archetype key -> set of minecraft:block_id strings
     */
    public static Map<String, Set<String>> buildMapFromConfig(
            AnythingInSulfurCubesPlugin plugin,
            Map<String, String> rawMaps) {
        Map<String, Set<String>> result = new HashMap<>();

        for (Map.Entry<String, String> entry : rawMaps.entrySet()) {
            String materialName = entry.getKey().toLowerCase();
            String archetype = entry.getValue();

            if (archetype == null || archetype.isEmpty()) continue;

            String blockId = "minecraft:" + materialName;
            result.computeIfAbsent(archetype.toUpperCase(), k -> new HashSet<>()).add(blockId);
        }

        return result;
    }
}
