package io.github.moranyue.anythinginsulfurcubes.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import io.github.moranyue.anythinginsulfurcubes.AnythingInSulfurCubesPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manages the plugin configuration (config.yml).
 * <p>
 * Contains block-to-archetype mappings and behavior settings
 * that replicate the functionality from the original data pack's archetypes.json5.
 */
public class PluginConfig {

    private static final String CONFIG_FILE_NAME = "config.yml";

    private final AnythingInSulfurCubesPlugin plugin;
    private FileConfiguration config;

    // Behavior settings
    private boolean cactusEnabled = true;
    private double cactusDamage = 1.0;
    private double cactusRange = 1.35;

    private boolean creakingHeartEnabled = true;
    private int creakingHeartTimerMin = 40;
    private int creakingHeartTimerMax = 100;

    private boolean potentSulfurEnabled = true;
    private int potentSulfurDuration = 5; // seconds
    private int potentSulfurAmplifier = 1;
    private double potentSulfurRange = 1.35;

    private boolean transparencyNotifyEnabled = true;

    // Block-to-archetype mapping (Material name -> archetype key)
    private final Map<String, String> blockArchetypes = new HashMap<>();

    // Set of transparent block materials for notification
    private Set<String> transparencyBlocks;

    public PluginConfig(AnythingInSulfurCubesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads (or reloads) the configuration from config.yml.
     * Falls back to defaults embedded in the plugin JAR.
     */
    public void loadConfig() {
        // Save default config if not present
        plugin.saveDefaultConfig();
        // Reload from file
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // We also read our bundled defaults to ensure all keys exist
        mergeDefaults();

        // Read behavior settings
        cactusEnabled = config.getBoolean("behaviors.cactus.enabled", true);
        cactusDamage = config.getDouble("behaviors.cactus.damage", 1.0);
        cactusRange = config.getDouble("behaviors.cactus.range", 1.35);

        creakingHeartEnabled = config.getBoolean("behaviors.creaking-heart.enabled", true);
        creakingHeartTimerMin = config.getInt("behaviors.creaking-heart.timer-min", 40);
        creakingHeartTimerMax = config.getInt("behaviors.creaking-heart.timer-max", 100);

        potentSulfurEnabled = config.getBoolean("behaviors.potent-sulfur.enabled", true);
        potentSulfurDuration = config.getInt("behaviors.potent-sulfur.duration-seconds", 5);
        potentSulfurAmplifier = config.getInt("behaviors.potent-sulfur.amplifier", 1);
        potentSulfurRange = config.getDouble("behaviors.potent-sulfur.range", 1.35);

        transparencyNotifyEnabled = config.getBoolean("behaviors.transparency-notify.enabled", true);

        // Read block-archetype mappings
        blockArchetypes.clear();
        if (config.isConfigurationSection("block-archetypes")) {
            for (String key : config.getConfigurationSection("block-archetypes").getKeys(false)) {
                String value = config.getString("block-archetypes." + key);
                if (value != null && !value.isEmpty()) {
                    blockArchetypes.put(key.toUpperCase(), value.toUpperCase());
                }
            }
        }

        // Read transparency blocks list
        transparencyBlocks = Set.copyOf(config.getStringList("transparency-blocks")
                .stream()
                .map(String::toUpperCase)
                .toList());
    }

    /**
     * Merges defaults from the bundled config.yml into the user's config.
     */
    private void mergeDefaults() {
        // Ensure all default block-archetype keys exist
        Map<String, String> defaults = getDefaultBlockArchetypes();
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            String path = "block-archetypes." + entry.getKey().toLowerCase();
            if (!config.contains(path)) {
                config.set(path, entry.getValue().toLowerCase());
            }
        }

        // Ensure all default behavior keys exist
        if (!config.contains("behaviors.cactus.enabled")) config.set("behaviors.cactus.enabled", true);
        if (!config.contains("behaviors.cactus.damage")) config.set("behaviors.cactus.damage", 1.0);
        if (!config.contains("behaviors.cactus.range")) config.set("behaviors.cactus.range", 1.35);
        if (!config.contains("behaviors.creaking-heart.enabled")) config.set("behaviors.creaking-heart.enabled", true);
        if (!config.contains("behaviors.creaking-heart.timer-min")) config.set("behaviors.creaking-heart.timer-min", 40);
        if (!config.contains("behaviors.creaking-heart.timer-max")) config.set("behaviors.creaking-heart.timer-max", 100);
        if (!config.contains("behaviors.potent-sulfur.enabled")) config.set("behaviors.potent-sulfur.enabled", true);
        if (!config.contains("behaviors.potent-sulfur.duration-seconds")) config.set("behaviors.potent-sulfur.duration-seconds", 5);
        if (!config.contains("behaviors.potent-sulfur.amplifier")) config.set("behaviors.potent-sulfur.amplifier", 1);
        if (!config.contains("behaviors.potent-sulfur.range")) config.set("behaviors.potent-sulfur.range", 1.35);
        if (!config.contains("behaviors.transparency-notify.enabled")) config.set("behaviors.transparency-notify.enabled", true);

        // Default transparency blocks
        if (!config.contains("transparency-blocks")) {
            config.set("transparency-blocks", getDefaultTransparencyBlocks());
        }

        // Save changes back
        plugin.saveConfig();
    }

    // --- Getters ---

    public boolean isCactusEnabled() { return cactusEnabled; }
    public double getCactusDamage() { return cactusDamage; }
    public double getCactusRange() { return cactusRange; }

    public boolean isCreakingHeartEnabled() { return creakingHeartEnabled; }
    public int getCreakingHeartTimerMin() { return creakingHeartTimerMin; }
    public int getCreakingHeartTimerMax() { return creakingHeartTimerMax; }

    public boolean isPotentSulfurEnabled() { return potentSulfurEnabled; }
    public int getPotentSulfurDuration() { return potentSulfurDuration; }
    public int getPotentSulfurAmplifier() { return potentSulfurAmplifier; }
    public double getPotentSulfurRange() { return potentSulfurRange; }

    public boolean isTransparencyNotifyEnabled() { return transparencyNotifyEnabled; }

    /**
     * Returns the raw block archetype mappings (material name -> archetype key, all uppercase).
     * Used by {@link io.github.moranyue.anythinginsulfurcubes.archetype.ArchetypeTagGenerator}
     * to generate the built-in datapack.
     */
    public Map<String, String> getBlockArchetypes() {
        return Map.copyOf(blockArchetypes);
    }

    /**
     * Gets the archetype key for a given Material.
     *
     * @param material the material to look up
     * @return the archetype key, or empty string if not found (block not absorbable)
     */
    public String getArchetype(Material material) {
        return blockArchetypes.getOrDefault(material.name(), "");
    }

    public boolean isTransparencyBlock(String materialName) {
        return transparencyBlocks.contains(materialName.toUpperCase());
    }

    // --- Default values ---

    private List<String> getDefaultTransparencyBlocks() {
        return List.of(
            "glass",
            "white_stained_glass", "orange_stained_glass", "magenta_stained_glass",
            "light_blue_stained_glass", "yellow_stained_glass", "lime_stained_glass",
            "pink_stained_glass", "gray_stained_glass", "light_gray_stained_glass",
            "cyan_stained_glass", "purple_stained_glass", "blue_stained_glass",
            "brown_stained_glass", "green_stained_glass", "red_stained_glass",
            "black_stained_glass",
            "ice",
            "slime_block",
            "honey_block"
        );
    }

    private Map<String, String> getDefaultBlockArchetypes() {
        Map<String, String> map = new HashMap<>();
        // This is derived from archetypes.json5 - all blocks with archetypes assigned
        // Leaves
        map.put("ACACIA_LEAVES", "LIGHT");
        map.put("AZALEA_LEAVES", "LIGHT");
        map.put("BIRCH_LEAVES", "LIGHT");
        map.put("CHERRY_LEAVES", "LIGHT");
        map.put("DARK_OAK_LEAVES", "LIGHT");
        map.put("FLOWERING_AZALEA_LEAVES", "LIGHT");
        map.put("JUNGLE_LEAVES", "LIGHT");
        map.put("MANGROVE_LEAVES", "LIGHT");
        map.put("MANGROVE_ROOTS", "LIGHT");
        map.put("OAK_LEAVES", "LIGHT");
        map.put("PALE_OAK_LEAVES", "LIGHT");
        map.put("SPRUCE_LEAVES", "LIGHT");

        // Wool (light archetype)
        map.put("BLACK_WOOL", "LIGHT");
        map.put("BLUE_WOOL", "LIGHT");
        map.put("BROWN_WOOL", "LIGHT");
        map.put("CYAN_WOOL", "LIGHT");
        map.put("GRAY_WOOL", "LIGHT");
        map.put("GREEN_WOOL", "LIGHT");
        map.put("LIGHT_BLUE_WOOL", "LIGHT");
        map.put("LIGHT_GRAY_WOOL", "LIGHT");
        map.put("LIME_WOOL", "LIGHT");
        map.put("MAGENTA_WOOL", "LIGHT");
        map.put("ORANGE_WOOL", "LIGHT");
        map.put("PINK_WOOL", "LIGHT");
        map.put("PURPLE_WOOL", "LIGHT");
        map.put("RED_WOOL", "LIGHT");
        map.put("WHITE_WOOL", "LIGHT");
        map.put("YELLOW_WOOL", "LIGHT");

        // Wood logs/planks/wood (bouncy archetype)
        for (String wood : new String[]{"ACACIA", "BIRCH", "CHERRY", "DARK_OAK", "JUNGLE", "MANGROVE", "OAK", "PALE_OAK", "SPRUCE"}) {
            map.put(wood + "_LOG", "BOUNCY");
            map.put(wood + "_PLANKS", "BOUNCY");
            map.put(wood + "_WOOD", "BOUNCY");
            map.put("STRIPPED_" + wood + "_LOG", "BOUNCY");
            map.put("STRIPPED_" + wood + "_WOOD", "BOUNCY");
        }
        // Non-wood bouncy
        map.put("BAMBOO_BLOCK", "BOUNCY");
        map.put("BAMBOO_MOSAIC", "BOUNCY");
        map.put("CRIMSON_HYPHAE", "BOUNCY");
        map.put("CRIMSON_PLANKS", "BOUNCY");
        map.put("CRIMSON_STEM", "BOUNCY");
        map.put("STRIPPED_CRIMSON_HYPHAE", "BOUNCY");
        map.put("STRIPPED_CRIMSON_STEM", "BOUNCY");
        map.put("STRIPPED_WARPED_HYPHAE", "BOUNCY");
        map.put("STRIPPED_WARPED_STEM", "BOUNCY");
        map.put("WARPED_HYPHAE", "BOUNCY");
        map.put("WARPED_PLANKS", "BOUNCY");
        map.put("WARPED_STEM", "BOUNCY");
        map.put("BEE_NEST", "BOUNCY");
        map.put("BEEHIVE", "BOUNCY");
        map.put("BOOKSHELF", "BOUNCY");
        map.put("CARTOGRAPHY_TABLE", "BOUNCY");
        map.put("CHISELED_BOOKSHELF", "BOUNCY");
        map.put("COMPOSTER", "BOUNCY");
        map.put("CRAFTING_TABLE", "BOUNCY");
        map.put("FLETCHING_TABLE", "BOUNCY");
        map.put("JUKEBOX", "BOUNCY");
        map.put("LECTERN", "BOUNCY");
        map.put("LOOM", "BOUNCY");
        map.put("NOTE_BLOCK", "BOUNCY");

        // Regular archetype
        map.put("CACTUS", "REGULAR");
        map.put("POTENT_SULFUR", "REGULAR");
        map.put("CREAKING_HEART", "REGULAR");
        map.put("GLASS", "REGULAR");
        map.put("TINTED_GLASS", "REGULAR");
        map.put("GRAVEL", "REGULAR");
        map.put("RED_SAND", "REGULAR");
        map.put("SAND", "REGULAR");
        map.put("DIRT", "REGULAR");
        map.put("COARSE_DIRT", "REGULAR");
        map.put("DIRT_PATH", "REGULAR");
        map.put("FARMLAND", "REGULAR");
        map.put("GRASS_BLOCK", "REGULAR");
        map.put("MUD", "REGULAR");
        map.put("MUDDY_MANGROVE_ROOTS", "REGULAR");
        map.put("PACKED_MUD", "REGULAR");
        map.put("PODZOL", "REGULAR");
        map.put("ROOTED_DIRT", "REGULAR");
        map.put("CLAY", "REGULAR");
        map.put("COAL_BLOCK", "REGULAR");
        map.put("BONE_BLOCK", "REGULAR");
        map.put("SCAFFOLDING", "REGULAR");
        map.put("CALIBRATED_SCULK_SENSOR", "REGULAR");
        map.put("CRAFTER", "REGULAR");
        map.put("DISPENSER", "REGULAR");
        map.put("DROPPER", "REGULAR");
        map.put("DRAGON_EGG", "REGULAR");
        map.put("ENCHANTING_TABLE", "REGULAR");
        map.put("END_PORTAL_FRAME", "REGULAR");
        map.put("ENDER_CHEST", "REGULAR"); // container
        map.put("FURNACE", "REGULAR");
        map.put("GRINDSTONE", "REGULAR");
        map.put("JIGSAW", "REGULAR");
        map.put("PISTON", "REGULAR");
        map.put("REDSTONE_BLOCK", "REGULAR");
        map.put("REPEATING_COMMAND_BLOCK", "REGULAR");
        map.put("RESPAWN_ANCHOR", "REGULAR");
        map.put("SCULK", "REGULAR");
        map.put("SCULK_CATALYST", "REGULAR");
        map.put("SCULK_SENSOR", "REGULAR");
        map.put("SCULK_SHRIEKER", "REGULAR");
        map.put("SCULK_VEIN", "REGULAR");
        map.put("SMOKER", "REGULAR");
        map.put("SNIFFER_EGG", "REGULAR");
        map.put("STRUCTURE_BLOCK", "REGULAR");
        map.put("SUSPICIOUS_GRAVEL", "REGULAR");
        map.put("SUSPICIOUS_SAND", "REGULAR");
        map.put("TRIAL_SPAWNER", "REGULAR");
        map.put("VAULT", "REGULAR");
        map.put("DECORATED_POT", "REGULAR");
        map.put("CHAIN_COMMAND_BLOCK", "REGULAR");
        map.put("COMMAND_BLOCK", "REGULAR");
        map.put("RED_SAND", "REGULAR");
        map.put("SAND", "REGULAR");
        // All stained glass as regular
        for (String color : new String[]{"BLACK", "BLUE", "BROWN", "CYAN", "GRAY", "GREEN", "LIGHT_BLUE", "LIGHT_GRAY", "LIME", "MAGENTA", "ORANGE", "PINK", "PURPLE", "RED", "WHITE", "YELLOW"}) {
            map.put(color + "_STAINED_GLASS", "REGULAR");
        }

        // Slow bouncy archetype
        map.put("STONE", "SLOW_BOUNCY");
        map.put("ANDESITE", "SLOW_BOUNCY");
        map.put("DIORITE", "SLOW_BOUNCY");
        map.put("GRANITE", "SLOW_BOUNCY");
        map.put("COBBLESTONE", "SLOW_BOUNCY");
        map.put("COBBLED_DEEPSLATE", "SLOW_BOUNCY");
        map.put("DEEPSLATE", "SLOW_BOUNCY");
        map.put("DEEPSLATE_BRICKS", "SLOW_BOUNCY");
        map.put("DEEPSLATE_TILES", "SLOW_BOUNCY");
        map.put("POLISHED_DEEPSLATE", "SLOW_BOUNCY");
        map.put("BRICKS", "SLOW_BOUNCY");
        map.put("STONE_BRICKS", "SLOW_BOUNCY");
        map.put("CHISELED_STONE_BRICKS", "SLOW_BOUNCY");
        map.put("CRACKED_STONE_BRICKS", "SLOW_BOUNCY");
        map.put("MOSSY_COBBLESTONE", "SLOW_BOUNCY");
        map.put("MOSSY_STONE_BRICKS", "SLOW_BOUNCY");
        map.put("SMOOTH_STONE", "SLOW_BOUNCY");
        map.put("SANDSTONE", "SLOW_BOUNCY");
        map.put("CUT_SANDSTONE", "SLOW_BOUNCY");
        map.put("CHISELED_SANDSTONE", "SLOW_BOUNCY");
        map.put("RED_SANDSTONE", "SLOW_BOUNCY");
        map.put("CUT_RED_SANDSTONE", "SLOW_BOUNCY");
        map.put("CHISELED_RED_SANDSTONE", "SLOW_BOUNCY");
        map.put("PRISMARINE", "SLOW_BOUNCY");
        map.put("PRISMARINE_BRICKS", "SLOW_BOUNCY");
        map.put("DARK_PRISMARINE", "SLOW_BOUNCY");
        map.put("NETHER_BRICKS", "SLOW_BOUNCY");
        map.put("RED_NETHER_BRICKS", "SLOW_BOUNCY");
        map.put("CRACKED_NETHER_BRICKS", "SLOW_BOUNCY");
        map.put("CHISELED_NETHER_BRICKS", "SLOW_BOUNCY");
        map.put("END_STONE", "SLOW_BOUNCY");
        map.put("END_STONE_BRICKS", "SLOW_BOUNCY");
        map.put("OBSIDIAN", "SLOW_BOUNCY");
        map.put("CRYING_OBSIDIAN", "SLOW_BOUNCY");
        map.put("PURPUR_BLOCK", "SLOW_BOUNCY");
        map.put("PURPUR_PILLAR", "SLOW_BOUNCY");
        map.put("QUARTZ_BLOCK", "SLOW_BOUNCY");
        map.put("QUARTZ_BRICKS", "SLOW_BOUNCY");
        map.put("QUARTZ_PILLAR", "SLOW_BOUNCY");
        map.put("SMOOTH_QUARTZ", "SLOW_BOUNCY");
        map.put("CALCITE", "SLOW_BOUNCY");
        map.put("TUFF", "SLOW_BOUNCY");
        map.put("TUFF_BRICKS", "SLOW_BOUNCY");
        map.put("CHISELED_TUFF", "SLOW_BOUNCY");
        map.put("CHISELED_TUFF_BRICKS", "SLOW_BOUNCY");
        map.put("POLISHED_TUFF", "SLOW_BOUNCY");
        map.put("SULFUR", "SLOW_BOUNCY");
        map.put("SULFUR_BRICKS", "SLOW_BOUNCY");
        map.put("CHISELED_SULFUR", "SLOW_BOUNCY");
        map.put("POLISHED_SULFUR", "SLOW_BOUNCY");
        map.put("CINNABAR", "SLOW_BOUNCY");
        map.put("CINNABAR_BRICKS", "SLOW_BOUNCY");
        map.put("CHISELED_CINNABAR", "SLOW_BOUNCY");
        map.put("POLISHED_CINNABAR", "SLOW_BOUNCY");
        map.put("BASALT", "SLOW_BOUNCY");
        map.put("POLISHED_BASALT", "SLOW_BOUNCY");
        map.put("SMOOTH_BASALT", "SLOW_BOUNCY");
        map.put("BLACKSTONE", "SLOW_BOUNCY");
        map.put("POLISHED_BLACKSTONE", "SLOW_BOUNCY");
        map.put("POLISHED_BLACKSTONE_BRICKS", "SLOW_BOUNCY");
        map.put("CHISELED_POLISHED_BLACKSTONE", "SLOW_BOUNCY");
        map.put("CRACKED_POLISHED_BLACKSTONE_BRICKS", "SLOW_BOUNCY");
        map.put("GILDED_BLACKSTONE", "SLOW_BOUNCY");
        map.put("CRACKED_DEEPSLATE_BRICKS", "SLOW_BOUNCY");
        map.put("CRACKED_DEEPSLATE_TILES", "SLOW_BOUNCY");
        map.put("NETHERRACK", "SLOW_BOUNCY");
        map.put("MAGMA_BLOCK", "SLOW_BOUNCY");
        map.put("SOUL_SAND", "SLOW_BOUNCY");
        map.put("SOUL_SOIL", "SLOW_BOUNCY");
        map.put("NETHER_WART_BLOCK", "SLOW_BOUNCY");
        map.put("WARPED_WART_BLOCK", "SLOW_BOUNCY");
        map.put("SHROOMLIGHT", "SLOW_BOUNCY");
        map.put("GLOWSTONE", "SLOW_BOUNCY");
        map.put("SEA_LANTERN", "SLOW_BOUNCY");
        map.put("REDSTONE_LAMP", "SLOW_BOUNCY");
        map.put("OBSERVER", "SLOW_BOUNCY");
        map.put("TERRACOTTA", "SLOW_BOUNCY");
        map.put("WHITE_TERRACOTTA", "SLOW_BOUNCY");
        map.put("ORANGE_TERRACOTTA", "SLOW_BOUNCY");
        // ... all terracotta colors
        for (String color : new String[]{"BLACK", "BLUE", "BROWN", "CYAN", "GRAY", "GREEN", "LIGHT_BLUE", "LIGHT_GRAY", "LIME", "MAGENTA", "ORANGE", "PINK", "PURPLE", "RED", "WHITE", "YELLOW"}) {
            map.put(color + "_TERRACOTTA", "SLOW_BOUNCY");
            map.put(color + "_CONCRETE", "SLOW_BOUNCY");
            map.put(color + "_GLAZED_TERRACOTTA", "SLOW_BOUNCY");
            map.put(color + "_CONCRETE_POWDER", "REGULAR");
        }
        map.put("TERRACOTTA", "SLOW_BOUNCY");
        map.put("ANCIENT_DEBRIS", "SLOW_FLAT");
        map.put("RED_NETHER_BRICKS", "SLOW_BOUNCY");
        map.put("MUD_BRICKS", "SLOW_BOUNCY");
        map.put("POLISHED_ANDESITE", "SLOW_BOUNCY");
        map.put("POLISHED_DIORITE", "SLOW_BOUNCY");
        map.put("POLISHED_GRANITE", "SLOW_BOUNCY");
        map.put("DRIPSTONE_BLOCK", "SLOW_BOUNCY");
        map.put("COAL_ORE", "SLOW_BOUNCY");
        map.put("DEEPSLATE_COAL_ORE", "SLOW_BOUNCY");
        map.put("DEEPSLATE_DIAMOND_ORE", "SLOW_BOUNCY");
        map.put("DEEPSLATE_EMERALD_ORE", "SLOW_BOUNCY");
        map.put("DEEPSLATE_LAPIS_ORE", "SLOW_BOUNCY");
        map.put("DEEPSLATE_REDSTONE_ORE", "SLOW_BOUNCY");
        map.put("NETHER_QUARTZ_ORE", "SLOW_BOUNCY");
        map.put("DIAMOND_BLOCK", "SLOW_BOUNCY");
        map.put("EMERALD_BLOCK", "SLOW_BOUNCY");
        map.put("LAPIS_BLOCK", "SLOW_BOUNCY");

        // Fast flat archetype
        map.put("BRAIN_CORAL_BLOCK", "FAST_FLAT");
        map.put("BUBBLE_CORAL_BLOCK", "FAST_FLAT");
        map.put("FIRE_CORAL_BLOCK", "FAST_FLAT");
        map.put("HORN_CORAL_BLOCK", "FAST_FLAT");
        map.put("TUBE_CORAL_BLOCK", "FAST_FLAT");
        map.put("DEAD_BRAIN_CORAL_BLOCK", "FAST_FLAT");
        map.put("DEAD_BUBBLE_CORAL_BLOCK", "FAST_FLAT");
        map.put("DEAD_FIRE_CORAL_BLOCK", "FAST_FLAT");
        map.put("DEAD_HORN_CORAL_BLOCK", "FAST_FLAT");
        map.put("DEAD_TUBE_CORAL_BLOCK", "FAST_FLAT");
        map.put("MOSS_BLOCK", "FAST_FLAT");
        map.put("PALE_MOSS_BLOCK", "FAST_FLAT");
        map.put("MELON", "FAST_FLAT");
        map.put("PUMPKIN", "FAST_FLAT");
        map.put("CARVED_PUMPKIN", "FAST_FLAT");
        map.put("JACK_O_LANTERN", "FAST_FLAT");
        map.put("DRIED_KELP_BLOCK", "FAST_FLAT");
        map.put("HAY_BLOCK", "FAST_FLAT");
        map.put("SPONGE", "FAST_FLAT");
        map.put("WET_SPONGE", "FAST_FLAT");
        map.put("OCHRE_FROGLIGHT", "FAST_FLAT");
        map.put("PEARLESCENT_FROGLIGHT", "FAST_FLAT");
        map.put("VERDANT_FROGLIGHT", "FAST_FLAT");
        map.put("RESIN_BLOCK", "FAST_FLAT");
        map.put("RESIN_BRICKS", "FAST_FLAT");
        map.put("CHISELED_RESIN_BRICKS", "FAST_FLAT");
        // All shulker boxes
        map.put("SHULKER_BOX", "FAST_FLAT");
        for (String color : new String[]{"BLACK", "BLUE", "BROWN", "CYAN", "GRAY", "GREEN", "LIGHT_BLUE", "LIGHT_GRAY", "LIME", "MAGENTA", "ORANGE", "PINK", "PURPLE", "RED", "WHITE", "YELLOW"}) {
            map.put(color + "_SHULKER_BOX", "FAST_FLAT");
        }
        // Chests/containers
        map.put("CHEST", "FAST_FLAT");
        map.put("TRAPPED_CHEST", "FAST_FLAT");
        map.put("ENDER_CHEST", "FAST_FLAT");
        map.put("COPPER_CHEST", "FAST_FLAT");
        for (String ox : new String[]{"EXPOSED_", "OXIDIZED_", "WEATHERED_", "WAXED_", "WAXED_EXPOSED_", "WAXED_OXIDIZED_", "WAXED_WEATHERED_"}) {
            map.put(ox + "COPPER_CHEST", "FAST_FLAT");
        }

        // Slow flat archetype
        map.put("COPPER_BLOCK", "SLOW_FLAT");
        map.put("CHISELED_COPPER", "SLOW_FLAT");
        map.put("CUT_COPPER", "SLOW_FLAT");
        map.put("COPPER_BULB", "SLOW_FLAT");
        map.put("COPPER_ORE", "SLOW_FLAT");
        map.put("DEEPSLATE_COPPER_ORE", "SLOW_FLAT");
        map.put("DEEPSLATE_IRON_ORE", "SLOW_FLAT");
        map.put("DEEPSLATE_GOLD_ORE", "SLOW_FLAT");
        map.put("IRON_ORE", "SLOW_FLAT");
        map.put("GOLD_ORE", "SLOW_FLAT");
        map.put("NETHER_GOLD_ORE", "SLOW_FLAT");
        map.put("IRON_BLOCK", "SLOW_FLAT");
        map.put("GOLD_BLOCK", "SLOW_FLAT");
        map.put("NETHERITE_BLOCK", "SLOW_FLAT");
        map.put("RAW_IRON_BLOCK", "SLOW_FLAT");
        map.put("RAW_GOLD_BLOCK", "SLOW_FLAT");
        map.put("RAW_COPPER_BLOCK", "SLOW_FLAT");
        map.put("ANCIENT_DEBRIS", "SLOW_FLAT");
        map.put("ANVIL", "SLOW_FLAT");
        map.put("CHIPPED_ANVIL", "SLOW_FLAT");
        map.put("DAMAGED_ANVIL", "SLOW_FLAT");
        map.put("BLAST_FURNACE", "SLOW_FLAT");
        map.put("BEACON", "SLOW_FLAT");
        map.put("CAULDRON", "SLOW_FLAT");
        map.put("COBWEB", "SLOW_FLAT");
        map.put("LODESTONE", "SLOW_FLAT");
        map.put("HOPPER", "SLOW_FLAT");
        map.put("SMITHING_TABLE", "SLOW_FLAT");
        map.put("TARGET", "SLOW_FLAT");
        // All copper variants
        for (String ox : new String[]{"EXPOSED_", "OXIDIZED_", "WEATHERED_", "WAXED_", "WAXED_EXPOSED_", "WAXED_OXIDIZED_", "WAXED_WEATHERED_"}) {
            map.put(ox + "COPPER_BLOCK", "SLOW_FLAT");
            map.put(ox + "CHISELED_COPPER", "SLOW_FLAT");
            map.put(ox + "CUT_COPPER", "SLOW_FLAT");
            map.put(ox + "COPPER_BULB", "SLOW_FLAT");
        }

        // Fast sliding
        map.put("BLUE_ICE", "FAST_SLIDING");
        map.put("ICE", "FAST_SLIDING");
        map.put("PACKED_ICE", "FAST_SLIDING");
        map.put("COPPER_GRATE", "FAST_SLIDING");
        for (String ox : new String[]{"EXPOSED_", "OXIDIZED_", "WEATHERED_", "WAXED_", "WAXED_EXPOSED_", "WAXED_OXIDIZED_", "WAXED_WEATHERED_"}) {
            map.put(ox + "COPPER_GRATE", "FAST_SLIDING");
        }
        map.put("SNOW_BLOCK", "FAST_SLIDING");

        // Slow sliding
        map.put("BROWN_MUSHROOM_BLOCK", "SLOW_SLIDING");
        map.put("RED_MUSHROOM_BLOCK", "SLOW_SLIDING");
        map.put("MUSHROOM_STEM", "SLOW_SLIDING");
        map.put("MYCELIUM", "SLOW_SLIDING");
        map.put("NETHER_WART_BLOCK", "SLOW_SLIDING");
        map.put("WARPED_WART_BLOCK", "SLOW_SLIDING");
        map.put("SHROOMLIGHT", "SLOW_SLIDING");

        // Sticky
        map.put("HONEY_BLOCK", "STICKY");
        map.put("HONEYCOMB_BLOCK", "STICKY");
        map.put("SLIME_BLOCK", "STICKY");
        map.put("STICKY_PISTON", "STICKY");

        // High resistance
        map.put("BEDROCK", "HIGH_RESISTANCE");
        map.put("BARRIER", "HIGH_RESISTANCE");
        map.put("REINFORCED_DEEPSLATE", "HIGH_RESISTANCE");
        map.put("SOUL_SAND", "HIGH_RESISTANCE");
        map.put("SOUL_SOIL", "HIGH_RESISTANCE");
        map.put("COMMAND_BLOCK", "HIGH_RESISTANCE");
        map.put("CHAIN_COMMAND_BLOCK", "HIGH_RESISTANCE");
        map.put("REPEATING_COMMAND_BLOCK", "HIGH_RESISTANCE");
        map.put("SPAWNER", "HIGH_RESISTANCE");
        map.put("TEST_INSTANCE_BLOCK", "HIGH_RESISTANCE");

        // Hot
        map.put("MAGMA_BLOCK", "HOT");

        // Explosive
        map.put("TNT", "EXPLOSIVE");

        // Infested blocks (regular)
        map.put("INFESTED_CHISELED_STONE_BRICKS", "REGULAR");
        map.put("INFESTED_COBBLESTONE", "REGULAR");
        map.put("INFESTED_CRACKED_STONE_BRICKS", "REGULAR");
        map.put("INFESTED_DEEPSLATE", "REGULAR");
        map.put("INFESTED_MOSSY_STONE_BRICKS", "REGULAR");
        map.put("INFESTED_STONE", "REGULAR");
        map.put("INFESTED_STONE_BRICKS", "REGULAR");

        return map;
    }
}
