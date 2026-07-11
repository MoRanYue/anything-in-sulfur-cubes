package io.github.moranyue.anythinginsulfurcubes.listener;

import io.github.moranyue.anythinginsulfurcubes.AnythingInSulfurCubesBootstrap;
import net.minecraft.world.entity.EquipmentSlot;
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
import org.bukkit.inventory.ItemStack;

/**
 * Handles player interactions with Sulfur Cubes.
 * <ul>
 *   <li><b>With an item in hand:</b> bypasses the vanilla
 *       {@code sulfur_cube_swallowable} tag check and directly calls
 *       NMS {@code SulfurCube.equipItem()} to place the block.</li>
 *   <li><b>With empty hand:</b> opens the container GUI if the cube
 *       is carrying a chest, shulker box, or other container item.</li>
 * </ul>
 */
public class SulfurCubeListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        handleInteraction(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        handleInteraction(event.getPlayer(), event.getRightClicked(), event);
    }

    private void handleInteraction(Player player, Entity clicked, Object event) {
        if (clicked.getType() != EntityType.SULFUR_CUBE) return;

        // Access NMS SulfurCube
        CraftEntity craftEntity = (CraftEntity) clicked;
        if (!(craftEntity.getHandle() instanceof SulfurCube nmsCube)) return;

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.isEmpty() || itemInHand.getType().isAir()) {
            // Empty hand → try to open container (chest, shulker box, etc.)
            net.minecraft.world.item.ItemStack nmsBodyItem = nmsCube.getItemBySlot(EquipmentSlot.BODY);
            if (!nmsBodyItem.isEmpty()) {
                boolean opened = ContainerOpenHelper.tryOpenContainer(player, nmsCube, nmsBodyItem);
                if (opened) {
                    if (event instanceof PlayerInteractEntityEvent e) {
                        e.setCancelled(true);
                    } else if (event instanceof PlayerInteractAtEntityEvent e) {
                        e.setCancelled(true);
                    }
                }
            }
            return;
        }

        // Item in hand → check if it has an archetype mapping
        String itemName = itemInHand.getType().getKey().getKey();
        if (!AnythingInSulfurCubesBootstrap.hasArchetype(itemName)) return;

        // Cancel vanilla handling
        if (event instanceof PlayerInteractEntityEvent e) {
            e.setCancelled(true);
        } else if (event instanceof PlayerInteractAtEntityEvent e) {
            e.setCancelled(true);
        }

        // Call equipItem to place the block in the cube
        net.minecraft.world.item.ItemStack nmsStack = net.minecraft.world.item.ItemStack.fromBukkitCopy(itemInHand);
        boolean success = nmsCube.equipItem(nmsStack);

        if (success) {
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
