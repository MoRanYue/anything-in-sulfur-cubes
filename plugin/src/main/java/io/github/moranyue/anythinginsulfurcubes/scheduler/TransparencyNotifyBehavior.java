package io.github.moranyue.anythinginsulfurcubes.scheduler;

import io.github.moranyue.anythinginsulfurcubes.AnythingInSulfurCubesPlugin;
import io.github.moranyue.anythinginsulfurcubes.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SulfurCube;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Replicates the transparencynotify.mcfunction + transparencynotifytick.mcfunction logic.
 * <p>
 * Once per server lifetime, notifies online players when Sulfur Cubes contain transparent blocks
 * (glass, stained glass, ice, slime_block, honey_block) about the transparency fix resource pack.
 * Uses the main world's PDC as a global flag so it only triggers once.
 */
public class TransparencyNotifyBehavior extends AbstractTickBehavior {

    private static final NamespacedKey NOTIFIED_KEY = new NamespacedKey("anythinginsulfurcubes", "transparency_notified");

    private static final Component NOTIFICATION_MESSAGE = Component.text()
        .append(Component.text(
            "Transparent Blocks in Sulfur Cubes have a graphical bug. " +
            "You can download the following resource pack to fix this. " +
            "(It will make all Sulfur Cubes look a bit worse though. " +
            "This is not fixable due to vanilla limitations)\n" +
            "This message will not be displayed again in this world.",
            NamedTextColor.YELLOW
        ))
        .append(Component.newline())
        .append(Component.text("Click here to download the pack", NamedTextColor.BLUE))
        .clickEvent(ClickEvent.openUrl("https://modrinth.com/resourcepack/sulfur-cube-transparency-fix"))
        .build();

    private boolean notified = false;

    public TransparencyNotifyBehavior(AnythingInSulfurCubesPlugin plugin, PluginConfig config) {
        super(plugin, config);
    }

    @Override
    protected void tick(World world) {
        if (!config.isTransparencyNotifyEnabled()) return;
        if (notified) return;

        // Check the global flag from the main world's PDC
        // (runs on the main world's region thread since we process per-world)
        World mainWorld = plugin.getServer().getWorlds().getFirst();
        if (!world.equals(mainWorld)) return; // Only process on the main world

        PersistentDataContainer globalPdc = mainWorld.getPersistentDataContainer();
        if (globalPdc.has(NOTIFIED_KEY)) {
            notified = true;
            return;
        }

        boolean found = false;

        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof SulfurCube cube)) continue;
            if (!cube.isValid()) continue;

            EntityEquipment equipment = cube.getEquipment();
            if (equipment == null) continue;

            String bodyMaterial = equipment.getItem(EquipmentSlot.BODY).getType().name();

            // Check if the body item is a transparent block
            if (config.isTransparencyBlock(bodyMaterial)) {
                found = true;
                break;
            }
        }

        if (!found) return;

        // Notify all online players once
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(NOTIFICATION_MESSAGE);
        }

        // Mark as notified globally
        globalPdc.set(NOTIFIED_KEY, PersistentDataType.BOOLEAN, true);
        notified = true;
    }

    @Override
    public String getBehaviorName() {
        return "TransparencyNotify";
    }
}
