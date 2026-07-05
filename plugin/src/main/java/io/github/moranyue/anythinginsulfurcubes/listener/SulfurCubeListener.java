package io.github.moranyue.anythinginsulfurcubes.listener;

import io.github.moranyue.anythinginsulfurcubes.AnythingInSulfurCubesBootstrap;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles player interactions with Sulfur Cubes.
 * <p>
 * Vanilla Minecraft uses {@code isSwallowableItem()} which checks the
 * {@code minecraft:sulfur_cube_swallowable} item tag. This listener
 * bypasses that check by cancelling the vanilla interaction and
 * directly calling NMS {@code SulfurCube.equipItem()}.
 * <p>
 * Which items are acceptable is determined by the bootstrap-phase
 * archetype registry (see {@link AnythingInSulfurCubesBootstrap#hasArchetype}),
 * not by a runtime config file.
 */
public class SulfurCubeListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        handleInteraction(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        handleInteraction(event.getPlayer(), event.getRightClicked(), event);
    }

    private void handleInteraction(Player player, Entity clicked, Object event) {
        // Check if the clicked entity is a SulfurCube
        if (clicked.getType() != EntityType.SULFUR_CUBE) {
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.isEmpty() || itemInHand.getType().isAir()) {
            return;
        }

        // Check if item has an archetype mapping in the bootstrap registry
        String itemName = itemInHand.getType().getKey().getKey(); // e.g. "cactus", "oak_log"
        if (!AnythingInSulfurCubesBootstrap.hasArchetype(itemName)) {
            return;
        }

        // Cancel the event to prevent vanilla handling (bypasses sulfur_cube_swallowable tag)
        if (event instanceof PlayerInteractEntityEvent e) {
            e.setCancelled(true);
        } else if (event instanceof PlayerInteractAtEntityEvent e) {
            e.setCancelled(true);
        }

        // Access NMS SulfurCube via CraftBukkit
        CraftEntity craftEntity = (CraftEntity) clicked;
        if (!(craftEntity.getHandle() instanceof SulfurCube nmsCube)) {
            return;
        }

        // Use equipItem directly — it handles all logic internally:
        // - baby check → returns false
        // - hasBodyItem && same type → returns false
        // - hasBodyItem && different type → drops existing, places new
        // - no body item → places new
        net.minecraft.world.item.ItemStack nmsStack = net.minecraft.world.item.ItemStack.fromBukkitCopy(itemInHand);
        boolean success = nmsCube.equipItem(nmsStack);

        if (success) {
            // Only consume item in survival/adventure mode (not creative/spectator)
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE
                && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                if (itemInHand.getAmount() <= 1) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                    player.getInventory().setItemInMainHand(itemInHand);
                }
            }
        }
    }
}
