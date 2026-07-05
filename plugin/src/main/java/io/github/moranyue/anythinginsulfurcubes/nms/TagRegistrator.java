package io.github.moranyue.anythinginsulfurcubes.nms;

import io.github.moranyue.anythinginsulfurcubes.AnythingInSulfurCubesPlugin;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Registers sulfur cube archetype item tags directly into the server's
 * item registry by injecting entries into both frozenTags and allTags.
 * <p>
 * Paper 26.2's MappedRegistry stores tags in two places:
 * - frozenTags (Map<TagKey, Named>) — used during registration
 * - allTags (TagSet) — used for runtime lookups
 * <p>
 * After calling bindTags() to update frozenTags, we must also rebuild
 * allTags from frozenTags and refresh tags in holders.
 * <p>
 * CRITICAL: We DO NOT re-freeze the registry after modification.
 * Setting frozen=true outside freeze() causes registry sync issues.
 * Instead, we only unfreeze temporarily and leave it unfrozen,
 * as the registry is already fully initialized and frozen.
 */
public class TagRegistrator {

    private static final String ARCHETYPE_PREFIX = "sulfur_cube_archetype/";

    private final AnythingInSulfurCubesPlugin plugin;
    private final Map<String, Set<String>> archetypeBlocks;

    public TagRegistrator(AnythingInSulfurCubesPlugin plugin, Map<String, Set<String>> archetypeBlocks) {
        this.plugin = plugin;
        this.archetypeBlocks = archetypeBlocks;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean registerTags() {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            var lookup = server.registryAccess().lookup(Registries.ITEM).orElseThrow();
            MappedRegistry<Item> registry;

            if (lookup instanceof MappedRegistry<?> mapped) {
                registry = (MappedRegistry<Item>) mapped;
            } else {
                registry = (MappedRegistry<Item>) BuiltInRegistries.ITEM;
            }

            // Unfreeze registry temporarily
            setField(registry, "frozen", false);

            // Build our custom tag entries
            Map<TagKey<Item>, List<Holder<Item>>> customTagMap = new HashMap<>();

            int totalItems = 0;
            for (Map.Entry<String, Set<String>> entry : archetypeBlocks.entrySet()) {
                String archetype = entry.getKey().toLowerCase();
                Set<String> blockIds = entry.getValue();
                if (blockIds.isEmpty()) continue;

                Identifier tagId = Identifier.parse(ARCHETYPE_PREFIX + archetype);
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);

                List<Holder<Item>> holders = new ArrayList<>();
                for (String blockId : blockIds) {
                    Identifier itemId = Identifier.parse(blockId);
                    var holderOpt = registry.get(itemId);
                    holderOpt.ifPresent(holders::add);
                }

                if (!holders.isEmpty()) {
                    customTagMap.put(tagKey, holders);
                    totalItems += holders.size();
                    plugin.getLogger().fine("Prepared tag " + tagId + " with " + holders.size() + " items");
                }
            }

            if (customTagMap.isEmpty()) {
                plugin.getLogger().warning("No custom tags to register");
                return true;
            }

            // Step 1: Bind tags — updates frozenTags
            registry.bindTags(customTagMap);

            // Step 2: Rebuild allTags from frozenTags
            rebuildAllTags(registry);

            // Step 3: Refresh tags in holders
            refreshTagsInHolders(registry);

            plugin.getLogger().info("Registered " + customTagMap.size()
                + " sulfur cube archetype tags (" + totalItems + " items)");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("NMS tag registration failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void rebuildAllTags(MappedRegistry<Item> registry) throws Exception {
        Field frozenTagsField = findField(registry.getClass(), "frozenTags");
        if (frozenTagsField == null) throw new NoSuchFieldException("frozenTags");
        frozenTagsField.setAccessible(true);
        Map frozenTags = (Map) frozenTagsField.get(registry);

        // Find TagSet.fromMap() in MappedRegistry (not DefaultedMappedRegistry)
        Class<?> tagSetClass = null;
        for (Class<?> inner : MappedRegistry.class.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("TagSet")) {
                tagSetClass = inner;
                break;
            }
        }
        if (tagSetClass == null) throw new ClassNotFoundException("TagSet");

        Method fromMapMethod = null;
        for (Method m : tagSetClass.getDeclaredMethods()) {
            if (m.getName().equals("fromMap") && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                fromMapMethod = m;
                fromMapMethod.setAccessible(true);
                break;
            }
        }
        if (fromMapMethod == null) throw new NoSuchMethodException("TagSet.fromMap()");

        Object newTagSet = fromMapMethod.invoke(null, frozenTags);

        Field allTagsField = findField(registry.getClass(), "allTags");
        if (allTagsField == null) throw new NoSuchFieldException("allTags");
        allTagsField.setAccessible(true);
        allTagsField.set(registry, newTagSet);

        plugin.getLogger().info("Rebuilt allTags TagSet from frozenTags (" + frozenTags.size() + " tags)");
    }

    private void refreshTagsInHolders(MappedRegistry<Item> registry) {
        try {
            Method refreshMethod = findMethod(registry.getClass(), "refreshTagsInHolders");
            if (refreshMethod != null) {
                refreshMethod.invoke(registry);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not refresh tags in holders: " + e.getMessage());
        }
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = findField(target.getClass(), name);
        if (field == null) throw new NoSuchFieldException(name + " in " + target.getClass().getName());
        field.setAccessible(true);
        field.set(target, value);
    }

    private Field findField(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(name); } catch (NoSuchFieldException e) { }
        }
        return null;
    }

    private Method findMethod(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name)) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }
}
