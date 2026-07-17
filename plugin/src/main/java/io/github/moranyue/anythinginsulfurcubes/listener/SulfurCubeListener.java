package io.github.moranyue.anythinginsulfurcubes.listener;

import io.github.moranyue.anythinginsulfurcubes.AnythingInSulfurCubesBootstrap;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.Set;

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
    public static final Set<Item> CHEST_CONTAINERS = Set.of(
                Items.CHEST,
                Items.TRAPPED_CHEST,
                Items.BARREL,
                Items.HOPPER,
                Items.DISPENSER,
                Items.DROPPER
    );

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSulfurCubeDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.SULFUR_CUBE) return;

        CraftEntity craftEntity = (CraftEntity) event.getEntity();
        if (!(craftEntity.getHandle() instanceof SulfurCube nmsCube)) return;

        net.minecraft.world.item.ItemStack nmsBodyItem = nmsCube.getItemBySlot(EquipmentSlot.BODY);
        if (nmsBodyItem.isEmpty()) {
            return;
        }
        
        if (CHEST_CONTAINERS.contains(nmsBodyItem.getItem())) {
            if (nmsCube.level() instanceof ServerLevel level) {
                ItemContainerContents contents = nmsBodyItem.get(DataComponents.CONTAINER);
                if (contents == null) {
                    return;
                }

                NonNullList<net.minecraft.world.item.ItemStack> items = NonNullList.create();
                contents.copyInto(items);
                for (net.minecraft.world.item.ItemStack stack : items) {
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(
                                level,
                                nmsCube.getX(),
                                nmsCube.getY(),
                                nmsCube.getZ(),
                                stack);
                    }
                }

                nmsBodyItem.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        handleInteraction(event.getPlayer(), event.getRightClicked(), event);
    }

    // @EventHandler(priority = EventPriority.LOWEST)
    // public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    //     if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
    //     handleInteraction(event.getPlayer(), event.getRightClicked(), event);
    // }

    private void handleInteraction(Player player, Entity clicked, Object event) {
        if (clicked.getType() != EntityType.SULFUR_CUBE || player.isSneaking()) return;

        // Access NMS SulfurCube
        CraftEntity craftEntity = (CraftEntity) clicked;
        if (!(craftEntity.getHandle() instanceof SulfurCube nmsCube)) return;

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        net.minecraft.world.item.ItemStack nmsStack = net.minecraft.world.item.ItemStack.fromBukkitCopy(itemInHand);

        ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();

        net.minecraft.world.item.ItemStack nmsBodyItem = nmsCube.getItemBySlot(EquipmentSlot.BODY);
        if (nmsBodyItem.isEmpty()) {
            return;
        }

        // p[rocess shearing for chest
        if (itemInHand.getType() == Material.SHEARS) {
            if (CHEST_CONTAINERS.contains(nmsBodyItem.getItem())) {
                if (event instanceof PlayerInteractEntityEvent e) {
                    e.setCancelled(true);
                }

                if (nmsCube.level() instanceof ServerLevel level) {
                    InteractionHand hand = switch (((PlayerInteractEntityEvent) event).getHand()) {
                        case HAND -> InteractionHand.MAIN_HAND;
                        case OFF_HAND -> InteractionHand.OFF_HAND;
                        default -> throw new IllegalArgumentException("Unexpected equipment slot");
                    };

                    ItemContainerContents contents = nmsBodyItem.get(DataComponents.CONTAINER);
                    if (contents != null) {
                        NonNullList<net.minecraft.world.item.ItemStack> items = NonNullList.create();
                        contents.copyInto(items);
                        for (net.minecraft.world.item.ItemStack stack : items) {
                            if (!stack.isEmpty()) {
                                Containers.dropItemStack(
                                        level,
                                        nmsCube.getX(),
                                        nmsCube.getY(),
                                        nmsCube.getZ(),
                                        stack);
                            }
                        }

                        nmsBodyItem.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
                    }

                    nmsCube.shear(level, SoundSource.PLAYERS, nmsStack);
                    nmsCube.gameEvent(GameEvent.SHEAR, nmsPlayer);
                    nmsStack.hurtAndBreak(1, nmsPlayer, hand);
                    CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT.trigger(nmsPlayer, nmsBodyItem, nmsCube);
                }
            }

            return;
        } else if (nmsStack.is(ItemTags.SULFUR_CUBE_SWALLOWABLE) && CHEST_CONTAINERS.contains(nmsBodyItem.getItem())) {
            if (nmsCube.level() instanceof ServerLevel level) {
                ItemContainerContents contents = nmsBodyItem.get(DataComponents.CONTAINER);
                if (contents != null) {
                    NonNullList<net.minecraft.world.item.ItemStack> items = NonNullList.create();
                    contents.copyInto(items);
                    for (net.minecraft.world.item.ItemStack stack : items) {
                        if (!stack.isEmpty()) {
                            Containers.dropItemStack(
                                    level,
                                    nmsCube.getX(),
                                    nmsCube.getY(),
                                    nmsCube.getZ(),
                                    stack);
                        }
                    }

                    nmsBodyItem.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
                }
            }
        }

        boolean opened = ContainerOpenHelper.tryOpenContainer(player, nmsCube, nmsBodyItem);
        if (opened) {
            if (event instanceof PlayerInteractEntityEvent e) {
                e.setCancelled(true);
            }
        }
    }
}
