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
package org.spongepowered.common.mixin.api.mcp.item.crafting;

import static org.spongepowered.common.inventory.util.InventoryUtil.toNativeInventory;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.recipe.Recipe;
import org.spongepowered.api.item.recipe.RecipeType;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.item.recipe.ingredient.IngredientUtil;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

@Mixin(IRecipe.class)
@Implements(@Interface(iface = Recipe.class, prefix = "recipe$"))
public interface IRecipeMixin_API<C extends IInventory> {

    // @formatter:off
    @Shadow ItemStack shadow$assemble(C inv);
    @Shadow net.minecraft.item.ItemStack shadow$getResultItem();
    @Shadow ResourceLocation shadow$getId();
    @Shadow boolean shadow$isSpecial();
    @Shadow boolean shadow$matches(C inv, net.minecraft.world.World worldIn);
    @Shadow NonNullList<ItemStack> shadow$getRemainingItems(C inv);
    @Shadow IRecipeType<?> shadow$getType();
    @Shadow NonNullList<net.minecraft.item.crafting.Ingredient> shadow$getIngredients();
    // @formatter:on

    @Nonnull
    default ItemStackSnapshot recipe$getExemplaryResult() {
        return ItemStackUtil.snapshotOf(this.shadow$getResultItem());
    }

    default boolean recipe$isValid(@Nonnull Inventory inv, @Nonnull ServerWorld world) {
        return this.shadow$matches(toNativeInventory(inv), (net.minecraft.world.World) world);
    }

    @Nonnull
    default ItemStackSnapshot recipe$getResult(@Nonnull Inventory inv) {
        return ItemStackUtil.snapshotOf(this.shadow$assemble(toNativeInventory(inv)));
    }

    @Nonnull
    default List<ItemStackSnapshot> recipe$getRemainingItems(@Nonnull Inventory inv) {
        return this.shadow$getRemainingItems(toNativeInventory(inv)).stream()
                .map(ItemStackUtil::snapshotOf)
                .collect(Collectors.toList());
    }

    @Intrinsic
    default List<Ingredient> recipe$getIngredients() {
        return this.shadow$getIngredients().stream().map(IngredientUtil::fromNative).collect(Collectors.toList());
    }

    @Intrinsic
    default boolean recipe$isDynamic() {
        return this.shadow$isSpecial();
    }

    default RecipeType<? extends Recipe> recipe$getType() {
        return (RecipeType) this.shadow$getType();
    }
}
