package io.github.moranyue.anythinginsulfurcubes.listener;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.bukkit.craftbukkit.entity.CraftPlayer;

/**
 * Handles opening the GUI for sulfur cubes carrying container items
 * (chests, shulker boxes, etc.) when interacted with an empty hand.
 */
public final class ContainerOpenHelper {

    private ContainerOpenHelper() {}

    /**
     * Attempts to open the container GUI for a sulfur cube's body item.
     *
     * @param bukkitPlayer the Bukkit player
     * @param nmsCube      the NMS SulfurCube
     * @param bodyItem     the NMS ItemStack from the body slot
     * @return true if a container GUI was opened
     */
    public static boolean tryOpenContainer(
            org.bukkit.entity.Player bukkitPlayer,
            net.minecraft.world.entity.monster.cubemob.SulfurCube nmsCube,
            ItemStack bodyItem) {
        if (bodyItem.isEmpty())
            return false;

        if (bodyItem.is(Items.ENDER_CHEST)) {
            nmsCube.playSound(
                    SoundEvents.ENDER_CHEST_OPEN,
                    0.5F,
                    1.0F);

            return openEnderChest(bukkitPlayer);
        }
        else if (bodyItem.is(Items.CHEST) || bodyItem.is(Items.TRAPPED_CHEST)) {
            nmsCube.playSound(
                    SoundEvents.CHEST_OPEN,
                    0.5F,
                    1.0F);
        }
        else if (bodyItem.is(Items.BARREL)) {
            nmsCube.playSound(
                    SoundEvents.BARREL_OPEN,
                    0.5F,
                    1.0F);
        }
        else if (bodyItem.is(ItemTags.SHULKER_BOXES)) {
            nmsCube.playSound(
                    SoundEvents.SHULKER_BOX_OPEN,
                    0.5F,
                    1.0F);
        }
        else if (bodyItem.is(Items.HOPPER)) {
            return openHopper(bukkitPlayer, nmsCube, bodyItem);
        }
        else if (bodyItem.is(Items.DROPPER) || bodyItem.is(Items.DISPENSER)) {
            return openDispenserOrDropper(bukkitPlayer, nmsCube, bodyItem);
        }

        ItemContainerContents contents = bodyItem.get(DataComponents.CONTAINER);
        if (contents == null)
            return false;

        Player nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();

        int size = 27;
        int rows = 3;
        // Create container and populate with current items
        SaveOnCloseContainer container = new SaveOnCloseContainer(size, bodyItem.copy(), nmsCube);
        NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        contents.copyInto(items);
        for (int i = 0; i < size; i++) {
            container.setItem(i, items.get(i));
        }

        Component title = bodyItem.getHoverName();
        MenuType<?> menuType = getMenuType(rows);
        if (menuType == null)
            return false;

        Component finalTitle = title;
        nmsPlayer.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return finalTitle;
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int containerId, Inventory inv, Player player) {
                return new ChestMenu(menuType, containerId, inv, container, rows);
            }
        });

        return true;
    }
    
    private static boolean openEnderChest(org.bukkit.entity.Player bukkitPlayer) {
        Player nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();

        nmsPlayer.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.enderchest");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int containerId,
                    Inventory playerInventory,
                    Player player) {

                return ChestMenu.threeRows(
                        containerId,
                        playerInventory,
                        player.getEnderChestInventory()
                );
            }
        });

        return true;
    }

    private static boolean openHopper(
            org.bukkit.entity.Player bukkitPlayer,
            net.minecraft.world.entity.monster.cubemob.SulfurCube nmsCube,
            ItemStack bodyItem
    ) {
        Player nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();

        ItemContainerContents contents =
                bodyItem.getOrDefault(
                        DataComponents.CONTAINER,
                        ItemContainerContents.EMPTY
                );

        int size = 5;

        SaveOnCloseContainer container =
                new SaveOnCloseContainer(
                        size,
                        bodyItem.copy(),
                        nmsCube
                );

        NonNullList<ItemStack> items =
                NonNullList.withSize(
                        size,
                        ItemStack.EMPTY
                );

        contents.copyInto(items);

        for (int i = 0; i < size; i++) {
            container.setItem(
                    i,
                    items.get(i)
            );
        }

        nmsPlayer.openMenu(
                new net.minecraft.world.MenuProvider() {

                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("container.hopper");
                    }


                    @Override
                    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                            int containerId,
                            Inventory inventory,
                            Player player
                    ) {

                        return new net.minecraft.world.inventory.HopperMenu(
                                containerId,
                                inventory,
                                container
                        );
                    }
                }
        );

        return true;
    }

    private static boolean openDispenserOrDropper(
            org.bukkit.entity.Player bukkitPlayer,
            net.minecraft.world.entity.monster.cubemob.SulfurCube nmsCube,
            ItemStack bodyItem
    ) {
        Player nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();

        ItemContainerContents contents =
                bodyItem.getOrDefault(
                        DataComponents.CONTAINER,
                        ItemContainerContents.EMPTY
                );

        int size = 9;
        SaveOnCloseContainer container =
                new SaveOnCloseContainer(
                        size,
                        bodyItem.copy(),
                        nmsCube
                );

        NonNullList<ItemStack> items =
                NonNullList.withSize(
                        size,
                        ItemStack.EMPTY
                );

        contents.copyInto(items);

        for (int i = 0; i < size; i++) {
            container.setItem(
                    i,
                    items.get(i)
            );
        }

        Component title = bodyItem.getHoverName();
        nmsPlayer.openMenu(
                new net.minecraft.world.MenuProvider() {

                    @Override
                    public Component getDisplayName() {
                        return title;
                    }


                    @Override
                    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                            int containerId,
                            Inventory inventory,
                            Player player
                    ) {

                        return new net.minecraft.world.inventory.DispenserMenu(
                                containerId,
                                inventory,
                                container
                        );
                    }
                }
        );


        return true;
    }

    private static MenuType<?> getMenuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3; // Default: 27 slots for chest/shulker
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> null;
        };
    }

    /**
     * A SimpleContainer that saves its contents back to the item
     * and updates the sulfur cube's equipment when the container is closed.
     */
    private static class SaveOnCloseContainer extends SimpleContainer {
        private final ItemStack bodyItem;
        private final net.minecraft.world.entity.monster.cubemob.SulfurCube nmsCube;

        SaveOnCloseContainer(int size, ItemStack bodyItem,
                             net.minecraft.world.entity.monster.cubemob.SulfurCube nmsCube) {
            super(size);
            this.bodyItem = bodyItem;
            this.nmsCube = nmsCube;
        }

        @Override
        public void stopOpen(net.minecraft.world.entity.ContainerUser containerUser) {
            // Save items back to the item's CONTAINER component
            NonNullList<ItemStack> savedItems = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < getContainerSize(); i++) {
                savedItems.set(i, getItem(i));
            }
            bodyItem.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(savedItems));

            // Update the sulfur cube's body equipment
            nmsCube.setItemSlot(EquipmentSlot.BODY, bodyItem);

            super.stopOpen(containerUser);
        }
    }
}
