/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.inventory.util;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.DoubleSidedInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.BeaconContainer;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.item.inventory.BlockCarrier;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.inventory.container.AbstractFurnaceContainerAccessor;
import org.spongepowered.common.accessor.inventory.container.BeaconContainerAccessor;
import org.spongepowered.common.accessor.inventory.container.BrewingStandContainerAccessor;
import org.spongepowered.common.accessor.inventory.container.ContainerAccessor;
import org.spongepowered.common.accessor.inventory.container.CraftingResultSlotAccessor;
import org.spongepowered.common.accessor.inventory.container.DispenserContainerAccessor;
import org.spongepowered.common.accessor.inventory.container.HopperContainerAccessor;
import org.spongepowered.common.accessor.inventory.container.HorseInventoryContainerAccessor;
import org.spongepowered.common.accessor.inventory.container.MerchantContainerAccessor;
import org.spongepowered.common.accessor.inventory.container.AbstractRepairContainerAccessor;
import org.spongepowered.common.bridge.inventory.InventoryBridge;
import org.spongepowered.common.bridge.inventory.container.ContainerBridge;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.inventory.SpongeLocationCarrier;
import org.spongepowered.common.inventory.SpongeBlockEntityCarrier;
import org.spongepowered.common.inventory.custom.CustomContainer;
import org.spongepowered.common.inventory.lens.CompoundSlotLensProvider;
import org.spongepowered.common.inventory.lens.Lens;
import org.spongepowered.common.inventory.lens.impl.CompoundLens;
import org.spongepowered.common.inventory.lens.impl.DelegatingLens;
import org.spongepowered.common.inventory.lens.impl.comp.CraftingInventoryLens;
import org.spongepowered.common.inventory.lens.impl.comp.GridInventoryLens;
import org.spongepowered.common.inventory.lens.impl.comp.PrimaryPlayerInventoryLens;
import org.spongepowered.common.inventory.lens.impl.minecraft.PlayerInventoryLens;
import org.spongepowered.common.inventory.lens.impl.minecraft.SingleGridLens;
import org.spongepowered.common.inventory.lens.impl.minecraft.container.ContainerLens;
import org.spongepowered.common.inventory.lens.impl.slot.SlotLensProvider;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public final class ContainerUtil {

    private static final Random RANDOM = new Random();

    private ContainerUtil() {
    }

    // Note this is likely not doable throughout the implementation, only in certain cases

    public static Container fromNative(final net.minecraft.inventory.container.Container container) {
        return (Container) container;
    }

    // Note, this really cannot be guaranteed to work
    public static net.minecraft.inventory.container.Container toNative(final Container container) {
        return (net.minecraft.inventory.container.Container) container;
    }

     /**
     * Replacement helper method for {@code InventoryHelperMixin#spongeDropInventoryItems(World, double, double, double, IInventory)}
     * to perform cause tracking related drops. This is specific for blocks, not for any other cases.
     *
     * @param worldServer The world server
     * @param x the x position
     * @param y the y position
     * @param z the z position
     * @param inventory The inventory to drop items from
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void performBlockInventoryDrops(final ServerWorld worldServer, final double x, final double y, final double z, final IInventory inventory) {
        final PhaseContext<@NonNull ?> context = PhaseTracker.getInstance().getPhaseContext();
        if (context.doesBlockEventTracking()) {
            // this is where we could perform item stack pre-merging.
            // TODO - figure out how inventory drops will work?
            for (int j = 0; j < inventory.getContainerSize(); j++) {
                final net.minecraft.item.ItemStack itemStack = inventory.getItem(j);
                if (!itemStack.isEmpty()) {
                    final float f = ContainerUtil.RANDOM.nextFloat() * 0.8F + 0.1F;
                    final float f1 = ContainerUtil.RANDOM.nextFloat() * 0.8F + 0.1F;
                    final float f2 = ContainerUtil.RANDOM.nextFloat() * 0.8F + 0.1F;

                    while (!itemStack.isEmpty())
                    {
                        final int i = ContainerUtil.RANDOM.nextInt(21) + 10;

                        final ItemEntity entityitem = new ItemEntity(worldServer, x + f, y + f1, z + f2, itemStack.split(i));

                        entityitem.setDeltaMovement(
                            ContainerUtil.RANDOM.nextGaussian() * 0.05,
                            ContainerUtil.RANDOM.nextGaussian() * 0.05 + 0.2,
                            ContainerUtil.RANDOM.nextGaussian() * 0.05);
                        worldServer.addFreshEntity(entityitem);
                    }
                }
            }
            return;
        }
        // Finally, just default to spawning the entities normally, regardless of the case.
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            final net.minecraft.item.ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                InventoryHelper.dropItemStack(worldServer, x, y, z, itemStack);
            }
        }
    }

    private static class CraftingInventoryData {
        @Nullable private Integer out;
        @Nullable private Integer base;
        @Nullable private CraftingInventory grid;
    }

    /**
     * Generates a fallback lens for given Container
     *
     * @param container The Container to generate a lens for
     * @param slots The slots of the Container
     * @return The generated fallback lens
     */
    @SuppressWarnings("unchecked") public static Lens generateLens(final net.minecraft.inventory.container.Container container, final SlotLensProvider slots) {
        // Get all inventories viewed in the Container & count slots & retain order
        final Map<Optional<IInventory>, List<Slot>> viewed = container.slots.stream()
                .collect(Collectors.groupingBy(i -> Optional.<IInventory>ofNullable(i.container), LinkedHashMap::new, Collectors.toList()));
        int index = 0; // Count the index
        final CraftingInventoryData crafting = new CraftingInventoryData();
        int chestHeight = 0;
        final List<Lens> lenses = new ArrayList<>();
        for (final Map.Entry<Optional<IInventory>, List<Slot>> entry : viewed.entrySet()) {
            final List<Slot> slotList = entry.getValue();
            final int slotCount = slotList.size();
            final IInventory subInventory = entry.getKey().orElse(null);
            // Generate Lens based on existing InventoryAdapter
            Lens lens = ContainerUtil.generateAdapterLens(slots, index, crafting, slotList, subInventory);
            // Inventory size <> Lens size
            if (lens.slotCount() != slotCount) {
                CompoundSlotLensProvider slotProvider = new CompoundSlotLensProvider().add(((InventoryBridge) subInventory).bridge$getAdapter());
                CompoundLens.Builder lensBuilder = CompoundLens.builder();
                for (Slot slot : slotList) {
                    lensBuilder.add(((InventoryBridge) slot).bridge$getAdapter().inventoryAdapter$getRootLens());
                }
                lens = lensBuilder.build(slotProvider);
            }
            lenses.add(lens);
            index += slotCount;

            // Count height of 9 width grid
            if (chestHeight != -1) {
                if (lens instanceof DelegatingLens) {
                    Lens delegated = ((DelegatingLens) lens).getDelegate();
                    if (delegated instanceof PrimaryPlayerInventoryLens) {
                        delegated = ((PrimaryPlayerInventoryLens) delegated).getFullGrid();
                    }
                    if (delegated instanceof SingleGridLens) {
                        delegated = delegated.getSpanningChildren().get(0);
                    }
                    if (delegated instanceof GridInventoryLens) {
                        if (((GridInventoryLens) delegated).getWidth() == 9) {
                            chestHeight += ((GridInventoryLens) delegated).getHeight();
                        } else {
                            chestHeight = -1;
                        }
                    } else {
                        chestHeight = -1;
                    }
                } else {
                    chestHeight = -1;
                }
            }
        }

        final List<Lens> additional = new ArrayList<>();
        try {
            if (crafting.out != null && crafting.base != null && crafting.grid != null) {
                additional.add(new CraftingInventoryLens(crafting.out, crafting.base, crafting.grid.getWidth(), crafting.grid.getHeight(), slots));
            } else if (crafting.base != null && crafting.grid != null) {
                additional.add(new GridInventoryLens(crafting.base, crafting.grid.getWidth(), crafting.grid.getHeight(), slots));
            }
        } catch (Exception e) {
            SpongeCommon
                .getLogger().error("Error while creating CraftingInventoryLensImpl or GridInventoryLensImpl for " + container.getClass().getName(), e);
        }
        if (chestHeight > 0) { // Add container grid for chest/double chest
            additional.add(new GridInventoryLens(0, 9, chestHeight, slots));
        }


        // Lens containing/delegating to other lenses
        return new ContainerLens(container.slots.size(), (Class<? extends Inventory>) container.getClass(), slots, lenses, additional);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Lens generateAdapterLens(final SlotLensProvider slots, final int index,
            final org.spongepowered.common.inventory.util.ContainerUtil.CraftingInventoryData crafting, final List<Slot> slotList, @Nullable final IInventory subInventory) {

        Lens lens = ((InventoryBridge) subInventory).bridge$getAdapter().inventoryAdapter$getRootLens();
        if (lens instanceof PlayerInventoryLens) {
            if (slotList.size() == 36) {
                return new DelegatingLens(index, new PrimaryPlayerInventoryLens(0, slots, true), slots);
            }
            return lens;
        }
        // For Crafting Result we need the Slot to get Filter logic
        if (subInventory instanceof CraftResultInventory) {
            final Slot slot = slotList.get(0);
            if (slot instanceof CraftingResultSlotAccessor) {
                crafting.out = index;
                if (crafting.base == null) {
                    // In case we do not find the InventoryCrafting later assume it is directly after the SlotCrafting
                    // e.g. for IC2 ContainerIndustrialWorkbench
                    crafting.base = index + 1;
                    crafting.grid = ((CraftingResultSlotAccessor) slot).accessor$craftSlots();
                }
            }
        }
        if (subInventory instanceof CraftingInventory) {
            crafting.base = index;
            crafting.grid = ((CraftingInventory) subInventory);
        }
        return new DelegatingLens(index, slotList, lens, slots);
    }

    @Nullable
    public static Carrier getCarrier(final Container container) {
        if (container instanceof BlockCarrier) {
            return ((BlockCarrier) container);
        }
        if (container instanceof CustomContainer) {
            return ((CustomContainer) container).inv.getCarrier();
        } else if (container instanceof ChestContainer) {
            final IInventory inventory = ((ChestContainer) container).getContainer();
            if (inventory instanceof Carrier) {
                if (inventory instanceof ChestTileEntity) {
                    return (Carrier) inventory;
                } else if (inventory instanceof DoubleSidedInventory) {
                    return ((BlockCarrier) inventory);
                }
            }
            return ContainerUtil.carrierOrNull(inventory);
        } else if (container instanceof HopperContainerAccessor) {
            return ContainerUtil.carrierOrNull(((HopperContainerAccessor) container).accessor$hopper());
        } else if (container instanceof DispenserContainerAccessor) {
            return ContainerUtil.carrierOrNull(((DispenserContainerAccessor) container).accessor$dispenser());
        } else if (container instanceof AbstractFurnaceContainerAccessor) {
            return ContainerUtil.carrierOrNull(((AbstractFurnaceContainerAccessor) container).accessor$container());
        } else if (container instanceof BrewingStandContainerAccessor) {
            return ContainerUtil.carrierOrNull(((BrewingStandContainerAccessor) container).accessor$brewingStand());
        } else if (container instanceof BeaconContainer) {
            return new SpongeBlockEntityCarrier(((BeaconContainerAccessor) container).accessor$access().evaluate(World::getBlockEntity).orElse(null), container);
        } else if (container instanceof HorseInventoryContainerAccessor) {
            return (Carrier) ((HorseInventoryContainerAccessor) container).accessor$horse();
        } else if (container instanceof MerchantContainerAccessor && ((MerchantContainerAccessor) container).accessor$trader() instanceof Carrier) {
            return (Carrier) ((MerchantContainerAccessor) container).accessor$trader();
        } else if (container instanceof AbstractRepairContainerAccessor) {
            final PlayerEntity player = ((AbstractRepairContainerAccessor) container).accessor$player();
            if (player instanceof ServerPlayerEntity) {
                return (Carrier) player;
            }
        }

        // Fallback: Try to find a Carrier owning the first Slot of the Container
        if (container instanceof ContainerAccessor) {
            for (final Slot slot : ((ContainerAccessor) container).accessor$slots()) {
                // Slot Inventory is a Carrier?
                if (slot.container instanceof Carrier) {
                    return ((Carrier) slot.container);
                }
                // Slot Inventory is a TileEntity
                if (slot.container instanceof TileEntity) {
                    return new SpongeBlockEntityCarrier((TileEntity) slot.container, container);
                }
            }
        }
        final ServerLocation loc = ((ContainerBridge) container).bridge$getOpenLocation();
        if (loc != null) {
            return new SpongeLocationCarrier(loc, container);
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Carrier carrierOrNull(final IInventory inventory) {
        if (inventory instanceof Carrier) {
            return (Carrier) inventory;
        }
        if (inventory instanceof CarriedInventory) {
            final Optional<Carrier> carrier = ((CarriedInventory) inventory).getCarrier();
            return carrier.orElse(null);
        }
        return null;
    }

}
