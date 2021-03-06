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
package org.spongepowered.common.mixin.core.world;

import net.kyori.adventure.bossbar.BossBar;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.BossInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.world.BossInfoBridge;

@Mixin(BossInfo.class)
public abstract class BossInfoMixin implements BossInfoBridge {

    @Shadow protected ITextComponent name;
    @Shadow protected float percent;
    @Shadow protected BossInfo.Color color;
    @Shadow protected BossInfo.Overlay overlay;
    @Shadow protected boolean darkenScreen;
    @Shadow protected boolean playBossMusic;
    @Shadow protected boolean createWorldFog;

    protected BossBar impl$adventure;

    @Override
    public void bridge$copyAndAssign(final BossBar adventure) {
        this.impl$adventure = adventure;
        this.percent = adventure.progress();
        this.darkenScreen = adventure.hasFlag(BossBar.Flag.DARKEN_SCREEN);
        this.playBossMusic = adventure.hasFlag(BossBar.Flag.PLAY_BOSS_MUSIC);
        this.createWorldFog = adventure.hasFlag(BossBar.Flag.CREATE_WORLD_FOG);
    }

    @Override
    public BossBar bridge$asAdventure() {
        if (this.impl$adventure == null) {
            this.bridge$setAdventure(BossBar.bossBar(
                SpongeAdventure.asAdventure(this.name),
                this.percent,
                SpongeAdventure.asAdventure(this.color),
                SpongeAdventure.asAdventure(this.overlay),
                SpongeAdventure.asAdventureFlags(this.darkenScreen, this.playBossMusic, this.createWorldFog)
            ));
        }
        return this.impl$adventure;
    }

    @Override
    public void bridge$setAdventure(final BossBar adventure) {
        this.impl$adventure = adventure;
    }

    @Override
    public void bridge$replacePlayer(final ServerPlayerEntity oldPlayer, final ServerPlayerEntity newPlayer) {
        // no-op
    }

    // Redirect setters
    @Redirect(method = "setName", at = @At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;name:Lnet/minecraft/util/text/ITextComponent;"))
    private void adventureName(final BossInfo $this, final ITextComponent name) {
        this.bridge$asAdventure().name(SpongeAdventure.asAdventure(name));
    }

    @Redirect(method = "setPercent", at = @At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;percent:F"))
    private void adventurePercent(final BossInfo $this, final float percent) {
        this.bridge$asAdventure().progress(percent);
    }

    @Redirect(method = "setColor", at = @At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;color:Lnet/minecraft/world/BossInfo$Color;"))
    private void adventureColor(final BossInfo $this, final BossInfo.Color color) {
        this.bridge$asAdventure().color(SpongeAdventure.asAdventure(color));
    }

    @Redirect(method = "setOverlay", at = @At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;overlay:Lnet/minecraft/world/BossInfo$Overlay;"))
    private void adventureOverlay(final BossInfo $this, final BossInfo.Overlay overlay) {
        this.bridge$asAdventure().overlay(SpongeAdventure.asAdventure(overlay));
    }

    @Redirect(method = "setDarkenScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;darkenScreen:Z"))
    private void adventureDarkenScreen(final BossInfo $this, final boolean darkenScreen) {
        if (darkenScreen) {
            this.bridge$asAdventure().addFlag(BossBar.Flag.DARKEN_SCREEN);
        } else {
            this.bridge$asAdventure().removeFlag(BossBar.Flag.DARKEN_SCREEN);
        }
    }

    @Redirect(method = "setPlayBossMusic", at = @At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;playBossMusic:Z"))
    private void adventurePlayBossMusic(final BossInfo $this, final boolean playBossMusic) {
        if (playBossMusic) {
            this.bridge$asAdventure().addFlag(BossBar.Flag.PLAY_BOSS_MUSIC);
        } else {
            this.bridge$asAdventure().removeFlag(BossBar.Flag.PLAY_BOSS_MUSIC);
        }
    }

    @Redirect(method = "setCreateWorldFog", at = @At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;createWorldFog:Z"))
    private void adventureCreateWorldFog(final BossInfo $this, final boolean createWorldFog) {
        if (createWorldFog) {
            this.bridge$asAdventure().addFlag(BossBar.Flag.CREATE_WORLD_FOG);
        } else {
            this.bridge$asAdventure().removeFlag(BossBar.Flag.CREATE_WORLD_FOG);
        }
    }

    // Redirect getters

    @Redirect(method = "getName", at = @At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;name:Lnet/minecraft/util/text/ITextComponent;"))
    private ITextComponent nameRead(final BossInfo $this) {
        return SpongeAdventure.asVanilla(this.bridge$asAdventure().name());
    }

    @Redirect(method = "getPercent", at = @At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;percent:F"))
    private float percentRead(final BossInfo $this) {
        return this.bridge$asAdventure().progress();
    }

    @Redirect(method = "getColor", at = @At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;color:Lnet/minecraft/world/BossInfo$Color;"))
    private BossInfo.Color colorRead(final BossInfo $this) {
        return SpongeAdventure.asVanilla(this.bridge$asAdventure().color());
    }

    @Redirect(method = "getOverlay", at = @At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;overlay:Lnet/minecraft/world/BossInfo$Overlay;"))
    private BossInfo.Overlay overlayRead(final BossInfo $this) {
        return SpongeAdventure.asVanilla(this.bridge$asAdventure().overlay());
    }

    @Redirect(method = "shouldDarkenScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;darkenScreen:Z"))
    private boolean darkenSkyRead(final BossInfo $this) {
        return this.bridge$asAdventure().hasFlag(BossBar.Flag.DARKEN_SCREEN);
    }

    @Redirect(method = "shouldPlayBossMusic", at =@At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;playBossMusic:Z"))
    private boolean playEndBossMusicRead(final BossInfo $this) {
        return this.bridge$asAdventure().hasFlag(BossBar.Flag.PLAY_BOSS_MUSIC);
    }

    @Redirect(method = "shouldCreateWorldFog", at = @At(value = "FIELD", target = "Lnet/minecraft/world/BossInfo;createWorldFog:Z"))
    private boolean createFogRead(final BossInfo $this) {
        return this.bridge$asAdventure().hasFlag(BossBar.Flag.CREATE_WORLD_FOG);
    }
}
