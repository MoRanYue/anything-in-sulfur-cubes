package io.github.moranyue.anythinginsulfurcubes.scheduler;

import io.github.moranyue.anythinginsulfurcubes.AnythingInSulfurCubesPlugin;
import io.github.moranyue.anythinginsulfurcubes.config.PluginConfig;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.SulfurCube;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

/**
 * Replicates the creakingheart.mcfunction + creakinghearttick.mcfunction logic.
 * <p>
 * Every tick, checks Sulfur Cubes containing creaking_heart in armor.body.
 * Uses a per-entity random timer (40-100 ticks) to periodically play
 * the block.creaking_heart.idle sound.
 */
public class CreakingHeartBehavior extends AbstractTickBehavior {

    private static final Material CREAKING_HEART = Material.CREAKING_HEART;
    private static final NamespacedKey TIMER_KEY = new NamespacedKey("anythinginsulfurcubes", "creaking_heart_timer");

    private static final Sound CREAKING_HEART_SOUND = Sound.sound()
        .type(Key.key("minecraft:block.creaking_heart.idle"))
        .source(Sound.Source.BLOCK)
        .volume(1.0f)
        .pitch(1.0f)
        .build();

    private final Random random = new Random();

    public CreakingHeartBehavior(AnythingInSulfurCubesPlugin plugin, PluginConfig config) {
        super(plugin, config);
    }

    @Override
    protected void tick(World world) {
        if (!config.isCreakingHeartEnabled()) return;

        int timerMin = config.getCreakingHeartTimerMin();
        int timerMax = config.getCreakingHeartTimerMax();

        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof SulfurCube cube)) continue;
            if (!cube.isValid()) continue;

            EntityEquipment equipment = cube.getEquipment();
            if (equipment == null) continue;

            // Check if the sulfur cube has creaking_heart in its body slot
            if (equipment.getItem(EquipmentSlot.BODY).getType() != CREAKING_HEART) continue;

            PersistentDataContainer pdc = cube.getPersistentDataContainer();

            // Get current timer value, default to a random initial value
            int timer = pdc.getOrDefault(TIMER_KEY, PersistentDataType.INTEGER, -1);
            if (timer < 0) {
                // Initialize with random value between min and max
                timer = timerMin + random.nextInt(timerMax - timerMin + 1);
                pdc.set(TIMER_KEY, PersistentDataType.INTEGER, timer);
                continue;
            }

            // Decrement timer
            timer--;
            if (timer <= 0) {
                // Play the eerie sound at the cube's location using Adventure API
                world.playSound(CREAKING_HEART_SOUND, cube.getX(), cube.getY(), cube.getZ());

                // Reset timer to a new random value
                timer = timerMin + random.nextInt(timerMax - timerMin + 1);
            }

            pdc.set(TIMER_KEY, PersistentDataType.INTEGER, timer);
        }
    }

    @Override
    public String getBehaviorName() {
        return "CreakingHeart";
    }
}
