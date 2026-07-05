package io.github.moranyue.anythinginsulfurcubes.scheduler;

import io.github.moranyue.anythinginsulfurcubes.AnythingInSulfurCubesPlugin;
import io.github.moranyue.anythinginsulfurcubes.config.PluginConfig;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.SulfurCube;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

/**
 * Replicates the cactus.mcfunction + cactustick.mcfunction logic.
 * <p>
 * Every tick, checks all Sulfur Cubes whose armor.body contains cactus.
 * Damages nearby living entities within the configured range.
 */
public class CactusBehavior extends AbstractTickBehavior {

    private static final Material CACTUS = Material.CACTUS;

    public CactusBehavior(AnythingInSulfurCubesPlugin plugin, PluginConfig config) {
        super(plugin, config);
    }

    @Override
    protected void tick(World world) {
        if (!config.isCactusEnabled()) return;

        double range = config.getCactusRange();
        double damage = config.getCactusDamage();

        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof SulfurCube cube)) continue;
            if (!cube.isValid()) continue;

            EntityEquipment equipment = cube.getEquipment();
            if (equipment == null) continue;

            // Check if the sulfur cube has cactus in its body slot
            if (equipment.getItem(EquipmentSlot.BODY).getType() != CACTUS) continue;

            // Damage nearby living entities (excluding the cube itself)
            // Uses DamageType.CACTUS instead of entity attack to match the original
            // datapack behavior: 'damage @s 1 cactus' uses the cactus damage type which
            // affects all entities including players. Entity attack damage (the default
            // when passing an Entity source to damage()) may be blocked by game mechanics
            // such as PvP settings, game mode checks, or invulnerability ticks against
            // non-hostile mob attacks.
            DamageSource cactusDamageSource = DamageSource.builder(DamageType.CACTUS).build();
            List<Entity> nearby = cube.getNearbyEntities(range, range, range);
            for (Entity nearbyEntity : nearby) {
                if (nearbyEntity instanceof LivingEntity living) {
                    living.damage(damage, cactusDamageSource);
                }
            }
        }
    }

    @Override
    public String getBehaviorName() {
        return "Cactus";
    }
}
