package io.github.moranyue.anythinginsulfurcubes.scheduler;

import io.github.moranyue.anythinginsulfurcubes.AnythingInSulfurCubesPlugin;
import io.github.moranyue.anythinginsulfurcubes.config.PluginConfig;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.SulfurCube;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Replicates the potentsulfur.mcfunction + potentsulfurtick.mcfunction logic.
 * <p>
 * Every tick, checks Sulfur Cubes containing potent_sulfur in armor.body.
 * Applies nausea effect to nearby living entities within the configured range.
 */
public class PotentSulfurBehavior extends AbstractTickBehavior {

    private static final Material POTENT_SULFUR = Material.POTENT_SULFUR;

    public PotentSulfurBehavior(AnythingInSulfurCubesPlugin plugin, PluginConfig config) {
        super(plugin, config);
    }

    @Override
    protected void tick(World world) {
        if (!config.isPotentSulfurEnabled()) return;

        double range = config.getPotentSulfurRange();
        int durationTicks = config.getPotentSulfurDuration() * 20; // seconds to ticks
        int amplifier = config.getPotentSulfurAmplifier();
        PotionEffect nausea = new PotionEffect(PotionEffectType.NAUSEA, durationTicks, amplifier, true, false, true);

        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof SulfurCube cube)) continue;
            if (!cube.isValid()) continue;

            EntityEquipment equipment = cube.getEquipment();
            if (equipment == null) continue;

            // Check if the sulfur cube has potent_sulfur in its body slot
            if (equipment.getItem(EquipmentSlot.BODY).getType() != POTENT_SULFUR) continue;

            // Apply nausea to nearby living entities
            List<Entity> nearby = cube.getNearbyEntities(range, range, range);
            for (Entity nearbyEntity : nearby) {
                if (nearbyEntity instanceof LivingEntity living) {
                    living.addPotionEffect(nausea);
                }
            }
        }
    }

    @Override
    public String getBehaviorName() {
        return "PotentSulfur";
    }
}
