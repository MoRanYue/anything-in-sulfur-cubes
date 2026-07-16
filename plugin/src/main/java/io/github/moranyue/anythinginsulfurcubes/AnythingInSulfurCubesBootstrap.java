package io.github.moranyue.anythinginsulfurcubes;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.SulfurCubeArchetypeKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import net.kyori.adventure.key.Key;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.TagEntry;
import org.bukkit.inventory.ItemType;
import net.kyori.adventure.key.Key;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.bukkit.entity.SulfurCube;
import java.util.*;

/**
 * Bootstrap for Anything in Sulfur Cubes.
 * <p>
 * Runs during server startup (before plugin enable) to register lifecycle
 * event handlers that modify the sulfur cube archetype registry entries.
 * <p>
 * This adds the plugin's custom block-to-archetype mappings into the
 * vanilla archetype item sets, ensuring the sulfur cube applies correct
 * physical properties (bounciness, friction, etc.) for custom blocks.
 */
public class AnythingInSulfurCubesBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        // Register compose event handler for sulfur cube archetype registry
        // The compose event fires after all vanilla archetype entries are loaded.
        // We use entryAdd events to modify each vanilla archetype's item set.
        var entryAdd = RegistryEvents.SULFUR_CUBE_ARCHETYPE.entryAdd();

        // For each archetype, add our custom items to its item set
        for (var entry : ARCHETYPE_ITEMS.entrySet()) {
            String archetypeName = entry.getKey();
            TypedKey<SulfurCube.Archetype> archetypeKey = getArchetypeKey(archetypeName);
            if (archetypeKey == null) continue;

            Set<String> itemNames = entry.getValue();
            RegistryKeySet<org.bukkit.inventory.ItemType> itemSet = createItemKeySet(itemNames);
            if (itemSet == null) continue;

            context.getLifecycleManager().registerEventHandler(
                entryAdd.newHandler(event -> event.builder().items(itemSet))
                    .filter(archetypeKey)
            );
        }

        context.getLifecycleManager().registerEventHandler(
            LifecycleEvents.TAGS.preFlatten(RegistryKey.ITEM),
            event -> {
                var registrar = event.registrar();

                List<TagEntry<ItemType>> entries = new ArrayList<>();

                for (Set<String> itemNames : ARCHETYPE_ITEMS.values()) {
                    for (String itemName : itemNames) {
                        entries.add(
                            TagEntry.valueEntry(
                                TypedKey.create(RegistryKey.ITEM, Key.key("minecraft", itemName))
                            )
                        );
                    }
                }

                registrar.addToTag(
                    TagKey.create(RegistryKey.ITEM, "minecraft:sulfur_cube_swallowable"),
                    entries
                );
            }
        );
    }

    @Override
    public org.bukkit.plugin.java.JavaPlugin createPlugin(io.papermc.paper.plugin.bootstrap.PluginProviderContext context) {
        return new AnythingInSulfurCubesPlugin();
    }

    /**
     * Creates a RegistryKeySet of ItemType keys from a set of item names.
     */
    private RegistryKeySet<org.bukkit.inventory.ItemType> createItemKeySet(Set<String> itemNames) {
        List<TypedKey<org.bukkit.inventory.ItemType>> keys = new ArrayList<>();
        for (String name : itemNames) {
            keys.add(TypedKey.create(RegistryKey.ITEM, Key.key("minecraft", name)));
        }
        return RegistrySet.keySet(RegistryKey.ITEM, keys);
    }

    /**
     * Gets the TypedKey for an archetype by name.
     */
    private TypedKey<SulfurCube.Archetype> getArchetypeKey(String name) {
        return switch (name) {
            case "REGULAR" -> SulfurCubeArchetypeKeys.REGULAR;
            case "BOUNCY" -> SulfurCubeArchetypeKeys.BOUNCY;
            case "SLOW_BOUNCY" -> SulfurCubeArchetypeKeys.SLOW_BOUNCY;
            case "LIGHT" -> SulfurCubeArchetypeKeys.LIGHT;
            case "FAST_FLAT" -> SulfurCubeArchetypeKeys.FAST_FLAT;
            case "SLOW_FLAT" -> SulfurCubeArchetypeKeys.SLOW_FLAT;
            case "FAST_SLIDING" -> SulfurCubeArchetypeKeys.FAST_SLIDING;
            case "SLOW_SLIDING" -> SulfurCubeArchetypeKeys.SLOW_SLIDING;
            case "STICKY" -> SulfurCubeArchetypeKeys.STICKY;
            case "HIGH_RESISTANCE" -> SulfurCubeArchetypeKeys.HIGH_RESISTANCE;
            case "HOT" -> SulfurCubeArchetypeKeys.HOT;
            case "EXPLOSIVE" -> SulfurCubeArchetypeKeys.EXPLOSIVE;
            default -> null;
        };
    }

    // ===== Archetype-to-item mappings (same defaults as config.yml) =====

    private static final Map<String, Set<String>> ARCHETYPE_ITEMS = new LinkedHashMap<>();

    static {
        // Regular archetype
        add("REGULAR", "cactus", "potent_sulfur", "creaking_heart", "glass", "tinted_glass",
            "gravel", "red_sand", "sand", "dirt", "coarse_dirt", "dirt_path", "farmland",
            "grass_block", "mud", "muddy_mangrove_roots", "packed_mud", "podzol", "rooted_dirt",
            "clay", "coal_block", "bone_block", "scaffolding", "calibrated_sculk_sensor",
            "crafter", "dispenser", "dropper", "dragon_egg", "enchanting_table", "end_portal_frame",
            "ender_chest", "furnace", "grindstone", "jigsaw", "piston", "redstone_block",
            "repeating_command_block", "respawn_anchor", "sculk", "sculk_catalyst", "sculk_sensor",
            "sculk_shrieker", "sculk_vein", "smoker", "sniffer_egg", "structure_block",
            "suspicious_gravel", "suspicious_sand", "trial_spawner", "vault", "decorated_pot",
            "chain_command_block", "command_block",
            "infested_chiseled_stone_bricks", "infested_cobblestone", "infested_cracked_stone_bricks",
            "infested_deepslate", "infested_mossy_stone_bricks", "infested_stone", "infested_stone_bricks");

        // All stained glass as regular
        for (String color : new String[]{"black", "blue", "brown", "cyan", "gray", "green",
            "light_blue", "light_gray", "lime", "magenta", "orange", "pink", "purple", "red",
            "white", "yellow"}) {
            add("REGULAR", color + "_stained_glass");
        }

        // Concrete powder as regular
        for (String color : new String[]{"black", "blue", "brown", "cyan", "gray", "green",
            "light_blue", "light_gray", "lime", "magenta", "orange", "pink", "purple", "red",
            "white", "yellow"}) {
            add("REGULAR", color + "_concrete_powder");
        }

        // Bouncy archetype
        for (String wood : new String[]{"acacia", "birch", "cherry", "dark_oak", "jungle",
            "mangrove", "oak", "pale_oak", "spruce"}) {
            add("BOUNCY", wood + "_log", wood + "_planks", wood + "_wood",
                "stripped_" + wood + "_log", "stripped_" + wood + "_wood");
        }
        add("BOUNCY", "bamboo_block", "bamboo_mosaic",
            "crimson_hyphae", "crimson_planks", "crimson_stem",
            "stripped_crimson_hyphae", "stripped_crimson_stem",
            "stripped_warped_hyphae", "stripped_warped_stem",
            "warped_hyphae", "warped_planks", "warped_stem",
            "bee_nest", "beehive", "bookshelf", "cartography_table",
            "chiseled_bookshelf", "composter", "crafting_table",
            "fletching_table", "jukebox", "lectern", "loom", "note_block");

        // Light archetype
        for (String color : new String[]{"black", "blue", "brown", "cyan", "gray", "green",
            "light_blue", "light_gray", "lime", "magenta", "orange", "pink", "purple", "red",
            "white", "yellow"}) {
            add("LIGHT", color + "_wool");
        }
        add("LIGHT", "acacia_leaves", "azalea_leaves", "birch_leaves", "cherry_leaves",
            "dark_oak_leaves", "flowering_azalea_leaves", "jungle_leaves", "mangrove_leaves",
            "mangrove_roots", "oak_leaves", "pale_oak_leaves", "spruce_leaves");

        // Slow bouncy
        add("SLOW_BOUNCY",
            "stone", "andesite", "diorite", "granite", "cobblestone", "cobbled_deepslate",
            "deepslate", "deepslate_bricks", "deepslate_tiles", "polished_deepslate",
            "bricks", "stone_bricks", "chiseled_stone_bricks", "cracked_stone_bricks",
            "mossy_cobblestone", "mossy_stone_bricks", "smooth_stone",
            "sandstone", "cut_sandstone", "chiseled_sandstone",
            "red_sandstone", "cut_red_sandstone", "chiseled_red_sandstone",
            "prismarine", "prismarine_bricks", "dark_prismarine",
            "nether_bricks", "red_nether_bricks", "cracked_nether_bricks", "chiseled_nether_bricks",
            "end_stone", "end_stone_bricks", "obsidian", "crying_obsidian",
            "purpur_block", "purpur_pillar",
            "quartz_block", "quartz_bricks", "quartz_pillar", "smooth_quartz",
            "calcite", "tuff", "tuff_bricks", "chiseled_tuff", "chiseled_tuff_bricks", "polished_tuff",
            "sulfur", "sulfur_bricks", "chiseled_sulfur", "polished_sulfur",
            "cinnabar", "cinnabar_bricks", "chiseled_cinnabar", "polished_cinnabar",
            "basalt", "polished_basalt", "smooth_basalt",
            "blackstone", "polished_blackstone", "polished_blackstone_bricks",
            "chiseled_polished_blackstone", "cracked_polished_blackstone_bricks", "gilded_blackstone",
            "cracked_deepslate_bricks", "cracked_deepslate_tiles",
            "netherrack", "nether_wart_block", "warped_wart_block", "shroomlight",
            "glowstone", "sea_lantern", "redstone_lamp", "observer",
            "mud_bricks", "polished_andesite", "polished_diorite", "polished_granite",
            "dripstone_block", "magma_block",
            "coal_ore", "deepslate_coal_ore", "deepslate_diamond_ore", "deepslate_emerald_ore",
            "deepslate_lapis_ore", "deepslate_redstone_ore", "nether_quartz_ore",
            "diamond_block", "emerald_block", "lapis_block",
            "amethyst_block");

        // Terracotta and concrete (slow bouncy)
        add("SLOW_BOUNCY", "terracotta");
        for (String color : new String[]{"black", "blue", "brown", "cyan", "gray", "green",
            "light_blue", "light_gray", "lime", "magenta", "orange", "pink", "purple", "red",
            "white", "yellow"}) {
            add("SLOW_BOUNCY", color + "_terracotta", color + "_concrete", color + "_glazed_terracotta");
        }

        // Fast flat
        add("FAST_FLAT",
            "brain_coral_block", "bubble_coral_block", "fire_coral_block", "horn_coral_block",
            "tube_coral_block", "dead_brain_coral_block", "dead_bubble_coral_block",
            "dead_fire_coral_block", "dead_horn_coral_block", "dead_tube_coral_block",
            "moss_block", "pale_moss_block", "melon", "pumpkin", "carved_pumpkin",
            "jack_o_lantern", "dried_kelp_block", "hay_block", "sponge", "wet_sponge",
            "ochre_froglight", "pearlescent_froglight", "verdant_froglight",
            "resin_block", "resin_bricks", "chiseled_resin_bricks",
            "chest", "trapped_chest", "copper_chest",
            "exposed_copper_chest", "oxidized_copper_chest", "weathered_copper_chest",
            "waxed_copper_chest", "waxed_exposed_copper_chest",
            "waxed_oxidized_copper_chest", "waxed_weathered_copper_chest", "barrel");
        // Shulker boxes (fast flat)
        add("FAST_FLAT", "shulker_box");
        for (String color : new String[]{"black", "blue", "brown", "cyan", "gray", "green",
            "light_blue", "light_gray", "lime", "magenta", "orange", "pink", "purple", "red",
            "white", "yellow"}) {
            add("FAST_FLAT", color + "_shulker_box");
        }

        // Slow flat
        // Note: copper_block has _block, but exposed/weathered/oxidized copper don't!
        add("SLOW_FLAT",
            "copper_block", "chiseled_copper", "cut_copper", "copper_bulb",
            "copper_ore", "deepslate_copper_ore", "deepslate_iron_ore", "deepslate_gold_ore",
            "iron_ore", "gold_ore", "nether_gold_ore",
            "iron_block", "gold_block", "netherite_block",
            "raw_iron_block", "raw_gold_block", "raw_copper_block",
            "ancient_debris",
            "anvil", "chipped_anvil", "damaged_anvil", "blast_furnace",
            "beacon", "cauldron", "cobweb", "lodestone", "hopper",
            "smithing_table", "target");
        // Exposed/weathered/oxidized copper entries do NOT have _block suffix
        for (String ox : new String[]{"exposed_", "oxidized_", "weathered_"}) {
            add("SLOW_FLAT", ox + "copper", ox + "chiseled_copper",
                ox + "cut_copper", ox + "copper_bulb");
        }
        // Waxed variants: waxed_copper HAS _block; waxed_exposed/oxidized/weathered do NOT
        add("SLOW_FLAT", "waxed_copper_block", "waxed_chiseled_copper",
            "waxed_cut_copper", "waxed_copper_bulb");
        for (String ox : new String[]{"waxed_exposed_", "waxed_oxidized_", "waxed_weathered_"}) {
            add("SLOW_FLAT", ox + "copper", ox + "chiseled_copper",
                ox + "cut_copper", ox + "copper_bulb");
        }

        // Fast sliding
        add("FAST_SLIDING", "blue_ice", "ice", "packed_ice", "snow_block", "copper_grate");
        for (String ox : new String[]{"exposed_", "oxidized_", "weathered_", "waxed_",
            "waxed_exposed_", "waxed_oxidized_", "waxed_weathered_"}) {
            add("FAST_SLIDING", ox + "copper_grate");
        }

        // Slow sliding
        add("SLOW_SLIDING", "brown_mushroom_block", "red_mushroom_block",
            "mushroom_stem", "mycelium");

        // Sticky
        add("STICKY", "honey_block", "honeycomb_block", "slime_block", "sticky_piston");

        // High resistance
        add("HIGH_RESISTANCE", "bedrock", "barrier", "reinforced_deepslate",
            "spawner", "test_instance_block");

        // Hot
        add("HOT", "magma_block");

        // Explosive
        add("EXPLOSIVE", "tnt");
    }

    private static void add(String archetype, String... items) {
        ARCHETYPE_ITEMS.computeIfAbsent(archetype, k -> new LinkedHashSet<>()).addAll(Arrays.asList(items));
    }

    /**
     * Checks whether a given item name (lowercase, e.g. "cactus", "glass") has
     * an archetype mapping registered by this bootstrap.
     * <p>
     * Used by {@link io.github.moranyue.anythinginsulfurcubes.listener.SulfurCubeListener}
     * to determine if a player-held item can be placed into a sulfur cube.
     *
     * @param itemName the Minecraft item ID in lowercase (e.g. "cactus", "oak_log")
     * @return true if the item has an archetype mapping
     */
    public static boolean hasArchetype(String itemName) {
        for (Set<String> items : ARCHETYPE_ITEMS.values()) {
            if (items.contains(itemName)) {
                return true;
            }
        }
        return false;
    }
}
