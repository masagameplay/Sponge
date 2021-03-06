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
package org.spongepowered.common.mixin.inventory.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.AbstractFurnaceContainer;
import net.minecraft.inventory.container.AbstractRepairContainer;
import net.minecraft.inventory.container.BeaconContainer;
import net.minecraft.inventory.container.BrewingStandContainer;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.DispenserContainer;
import net.minecraft.inventory.container.EnchantmentContainer;
import net.minecraft.inventory.container.HopperContainer;
import net.minecraft.inventory.container.HorseInventoryContainer;
import net.minecraft.inventory.container.MerchantContainer;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.WorkbenchContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.bridge.inventory.container.ContainerBridge;

import java.util.function.Predicate;

/**
 * This should target all vanilla Containers.
 */
@Mixin(value = {
        ChestContainer.class,
        HopperContainer.class,
        DispenserContainer.class,
        AbstractFurnaceContainer.class,
        EnchantmentContainer.class,
        AbstractRepairContainer.class,
        BrewingStandContainer.class,
        BeaconContainer.class,
        HorseInventoryContainer.class,
        MerchantContainer.class,
        PlayerContainer.class,
        WorkbenchContainer.class
})
public abstract class TraitMixin_ContainerBridge_Inventory extends Container implements ContainerBridge {

    protected TraitMixin_ContainerBridge_Inventory(@Nullable ContainerType<?> p_i50105_1_, int p_i50105_2_) {
        super(p_i50105_1_, p_i50105_2_);
    }

    // Container#canInteractWith is abstract so we have to target the Containers individually
    @Inject(method = "stillValid(Lnet/minecraft/entity/player/PlayerEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void impl$canInteractWith(final PlayerEntity playerIn, final CallbackInfoReturnable<Boolean> cir) {
        Predicate<PlayerEntity> predicate = this.bridge$getCanInteractWith();
        if (predicate != null) {
            cir.setReturnValue(predicate.test(playerIn));
        }
    }
}
