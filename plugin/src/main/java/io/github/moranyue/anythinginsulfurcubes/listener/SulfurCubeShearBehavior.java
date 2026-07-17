package io.github.moranyue.anythinginsulfurcubes.listener;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

public class SulfurCubeShearBehavior implements DispenseItemBehavior {
    private final DispenseItemBehavior original = DispenserBlock.DISPENSER_REGISTRY.get(Items.SHEARS);

    public SulfurCubeShearBehavior() {
    }

    @Override
    public ItemStack dispense(BlockSource source, ItemStack dispensed) {
        ServerLevel level = source.level();

        BlockPos dispenserPos = source.pos();
        Direction direction = source.state().getValue(DispenserBlock.FACING);
        BlockPos target = dispenserPos.relative(direction);
        for (SulfurCube nmsCube : level.getEntitiesOfClass(
                SulfurCube.class,
                new AABB(target).inflate(0.5)
        )) {
            net.minecraft.world.item.ItemStack nmsBodyItem = nmsCube.getItemBySlot(EquipmentSlot.BODY);
            if (nmsBodyItem.isEmpty()) {
                continue;
            }
            
            if (SulfurCubeListener.CHEST_CONTAINERS.contains(nmsBodyItem.getItem())) {
                ItemContainerContents contents = nmsBodyItem.get(DataComponents.CONTAINER);
                if (contents == null) {
                    continue;
                }

                NonNullList<net.minecraft.world.item.ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
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

        return original.dispense(source, dispensed);
    }
}