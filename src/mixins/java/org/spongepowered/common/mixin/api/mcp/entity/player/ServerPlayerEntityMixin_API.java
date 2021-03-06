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
package org.spongepowered.common.mixin.api.mcp.entity.player;

import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.SChangeBlockPacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.network.play.server.SPlaySoundPacket;
import net.minecraft.network.play.server.SSendResourcePackPacket;
import net.minecraft.network.play.server.SStopSoundPacket;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.network.play.server.SWorldBorderPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.server.ServerBossInfo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Server;
import org.spongepowered.api.advancement.Advancement;
import org.spongepowered.api.advancement.AdvancementProgress;
import org.spongepowered.api.advancement.AdvancementTree;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.sound.music.MusicDisc;
import org.spongepowered.api.entity.living.player.CooldownTracker;
import org.spongepowered.api.entity.living.player.PlayerChatRouter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.living.player.tab.TabList;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.network.ServerPlayerConnection;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.resourcepack.ResourcePack;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.world.WorldBorder;
import org.spongepowered.api.world.WorldType;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.SpongeServer;
import org.spongepowered.common.accessor.world.border.WorldBorderAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.advancements.PlayerAdvancementsBridge;
import org.spongepowered.common.bridge.entity.player.ServerPlayerEntityBridge;
import org.spongepowered.common.bridge.scoreboard.ServerScoreboardBridge;
import org.spongepowered.common.effect.particle.SpongeParticleHelper;
import org.spongepowered.common.effect.record.SpongeMusicDisc;
import org.spongepowered.common.entity.player.tab.SpongeTabList;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.resourcepack.SpongeResourcePack;
import org.spongepowered.common.util.BookUtil;
import org.spongepowered.common.util.NetworkUtil;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin_API extends PlayerEntityMixin_API implements ServerPlayer {

    // @formatter:off
    @Shadow @Final public MinecraftServer server;
    @Shadow @Final private PlayerAdvancements advancements;
    @Shadow public ServerPlayNetHandler connection;

    @Shadow public abstract net.minecraft.world.server.ServerWorld shadow$getLevel();
    // @formatter:on

    private final TabList api$tabList = new SpongeTabList((ServerPlayerEntity) (Object) this);
    @Nullable private PlayerChatRouter api$chatRouter;
    @Nullable private WorldBorder api$worldBorder;

    @Override
    public ServerWorld getWorld() {
        return (ServerWorld) this.shadow$getLevel();
    }

    @Override
    public void spawnParticles(final ParticleEffect particleEffect, final Vector3d position, final int radius) {
        if (this.impl$isFake) {
            return;
        }
        Objects.requireNonNull(particleEffect, "particleEffect");
        Objects.requireNonNull(position, "position");
        if (radius <= 0) {
            throw new IllegalArgumentException("The radius has to be greater then zero!");
        }
        final List<IPacket<?>> packets = SpongeParticleHelper.toPackets(particleEffect, position);

        if (!packets.isEmpty()) {
            if (position.sub(this.shadow$getX(), this.shadow$getY(), this.shadow$getZ()).lengthSquared() < (long) radius * (long) radius) {
                for (final IPacket<?> packet : packets) {
                    this.connection.send(packet);
                }
            }
        }
    }

    @Override
    public User getUser() {
        return ((ServerPlayerEntityBridge) this).bridge$getUser();
    }

    @Override
    public boolean isOnline() {
        if (this.impl$isFake) {
            return true;
        }
        return this.server.getPlayerList().getPlayer(this.uuid) == (ServerPlayerEntity) (Object) this;
    }

    @Override
    public GameProfile getProfile() {
        return ((ServerPlayerEntityBridge) this).bridge$getUser().getProfile();
    }

    @Override
    public void sendWorldType(final WorldType worldType) {
        if (this.impl$isFake) {
            return;
        }
        ((ServerPlayerEntityBridge) this).bridge$sendViewerEnvironment((net.minecraft.world.DimensionType) Objects.requireNonNull(worldType,
                "worldType"));
    }

    @Override
    public void spawnParticles(final ParticleEffect particleEffect, final Vector3d position) {
        if (this.impl$isFake) {
            return;
        }
        this.spawnParticles(particleEffect, position, Integer.MAX_VALUE);
    }

    @Override
    public ServerPlayerConnection getConnection() {
        return (ServerPlayerConnection) this.connection;
    }

    /**
     * @author Minecrell - August 22nd, 2016
     * @reason Use InetSocketAddress#getHostString() where possible (instead of
     *     inspecting SocketAddress#toString()) to support IPv6 addresses
     */
    @Overwrite
    public String getIpAddress() {
        return NetworkUtil.getHostString(this.connection.connection.getRemoteAddress());
    }

    @Override
    public String getIdentifier() {
        return ((ServerPlayerEntityBridge) this).bridge$getUser().getIdentifier();
    }

    @Override
    public void setScoreboard(final Scoreboard scoreboard) {
        Objects.requireNonNull(scoreboard, "scoreboard");

        ((ServerScoreboardBridge) ((ServerPlayerEntityBridge) this).bridge$getScoreboard()).bridge$removePlayer((ServerPlayerEntity) (Object) this, true);
        ((ServerPlayerEntityBridge) this).bridge$replaceScoreboard(scoreboard);
        ((ServerScoreboardBridge) ((ServerPlayerEntityBridge) this).bridge$getScoreboard()).bridge$addPlayer((ServerPlayerEntity) (Object) this, true);
    }

    @Override
    public Component getTeamRepresentation() {
        return SpongeAdventure.asAdventure(this.shadow$getName());
    }

    @Override
    public Scoreboard getScoreboard() {
        return ((ServerPlayerEntityBridge) this).bridge$getScoreboard();
    }

    @Override
    public boolean kick() {
        return this.kick(Component.translatable("disconnect.disconnected"));
    }

    @Override
    public boolean kick(final Component message) {
        return ((ServerPlayerEntityBridge) this).bridge$kick(Objects.requireNonNull(message, "message"));
    }

    @Override
    public void playMusicDisc(final Vector3i position, final MusicDisc recordType) {
        this.connection.send(SpongeMusicDisc.createPacket(Objects.requireNonNull(position, "position"), Objects.requireNonNull(recordType, "recordType")));
    }

    @Override
    public void stopMusicDisc(final Vector3i position) {
        this.connection.send(SpongeMusicDisc.createPacket(position, null));
    }

    @Override
    public void sendResourcePack(final ResourcePack pack) {
        this.connection.send(new SSendResourcePackPacket(((SpongeResourcePack) Objects.requireNonNull(pack, "pack")).getUrlString(), pack.getHash().orElse("")));
    }

    @Override
    public TabList getTabList() {
        return this.api$tabList;
    }

    @Override
    public boolean hasPlayedBefore() {
        final Instant instant = ((SpongeServer) this.shadow$getServer()).getPlayerDataManager().getFirstJoined(this.getUniqueId()).get();
        final Instant toTheMinute = instant.truncatedTo(ChronoUnit.MINUTES);
        final Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        final Duration timeSinceFirstJoined = Duration.of(now.minusMillis(toTheMinute.toEpochMilli()).toEpochMilli(), ChronoUnit.MINUTES);
        return timeSinceFirstJoined.getSeconds() > 0;
    }

    @Override
    public void sendBlockChange(final int x, final int y, final int z, final BlockState state) {
        this.connection.send(new SChangeBlockPacket(new BlockPos(x, y, z), (net.minecraft.block.BlockState) state));
    }

    @Override
    public void resetBlockChange(final int x, final int y, final int z) {
        this.connection.send(new SChangeBlockPacket(this.shadow$getCommandSenderWorld(), new BlockPos(x, y, z)));
    }

    @Override
    public boolean respawn() {
        if (this.impl$isFake) {
            return false;
        }
        if (this.shadow$getHealth() > 0.0F) {
            return false;
        }
        this.connection.player = this.server.getPlayerList().respawn((ServerPlayerEntity) (Object) this, false);
        return true;
    }

    @Override
    public PlayerChatRouter getChatRouter() {
        if (this.api$chatRouter == null) {
            this.api$chatRouter = (player, message) -> ((Server) this.server).sendMessage(player,
                    Component.translatable("chat.type.text", SpongeAdventure.asAdventure(this.shadow$getDisplayName()), message));
        }
        return this.api$chatRouter;
    }

    @Override
    public void setChatRouter(final PlayerChatRouter router) {
        this.api$chatRouter = Objects.requireNonNull(router, "router");
    }

    @Override
    public PlayerChatEvent simulateChat(final Component message, final Cause cause) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(cause, "cause");

        final PlayerChatRouter originalRouter = this.getChatRouter();
        final PlayerChatEvent event = SpongeEventFactory.createPlayerChatEvent(cause, originalRouter, Optional.of(originalRouter), message, message);
        if (!SpongeCommon.postEvent(event)) {
            event.getChatRouter().ifPresent(channel -> channel.chat(this, event.getMessage()));
        }
        return event;
    }

    @Override
    public Optional<WorldBorder> getWorldBorder() {
        return Optional.ofNullable(this.api$worldBorder);
    }

    @Override
    public CooldownTracker getCooldownTracker() {
        return (CooldownTracker) this.shadow$getCooldowns();
    }

    @Override
    public AdvancementProgress getProgress(final Advancement advancement) {
        return (AdvancementProgress) this.advancements.getOrStartProgress((net.minecraft.advancements.Advancement) Objects.requireNonNull(advancement, "advancement"));
    }

    @Override
    public Collection<AdvancementTree> getUnlockedAdvancementTrees() {
        if (this.impl$isFake) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(((PlayerAdvancementsBridge) this.advancements).bridge$getAdvancementTrees());
    }

    @Override
    public void setWorldBorder(final @Nullable WorldBorder border) {
        if (this.impl$isFake) {
            return;
        }
        if (this.api$worldBorder == border) {
            return; //do not fire an event since nothing would have changed
        }
        if (!SpongeCommon.postEvent(SpongeEventFactory.createChangeWorldBorderEventTargetPlayer(PhaseTracker.getCauseStackManager().getCurrentCause(),
                Optional.ofNullable(this.api$worldBorder), this, Optional.ofNullable(border)))) {
            if (this.api$worldBorder != null) { //is the world border about to be unset?
                ((WorldBorderAccessor) this.api$worldBorder).accessor$listeners().remove(
                        ((ServerPlayerEntityBridge) this).bridge$getWorldBorderListener()); //remove the listener, if so
            }
            this.api$worldBorder = border;
            if (this.api$worldBorder != null) {
                ((net.minecraft.world.border.WorldBorder) this.api$worldBorder).addListener(
                        ((ServerPlayerEntityBridge) this).bridge$getWorldBorderListener());
                this.connection.send(
                        new SWorldBorderPacket((net.minecraft.world.border.WorldBorder) this.api$worldBorder,
                                SWorldBorderPacket.Action.INITIALIZE));
            } else { //unset the border if null
                this.connection.send(
                        new SWorldBorderPacket(this.shadow$getCommandSenderWorld().getWorldBorder(), SWorldBorderPacket.Action.INITIALIZE));
            }
        }
    }

    @Override
    protected Set<Value.Immutable<?>> api$getVanillaValues() {
        final Set<Value.Immutable<?>> values = super.api$getVanillaValues();

        // Humanoid
        values.add(this.foodLevel().asImmutable());
        values.add(this.exhaustion().asImmutable());
        values.add(this.saturation().asImmutable());
        values.add(this.gameMode().asImmutable());

        // Player
        values.add(this.firstJoined().asImmutable());
        values.add(this.lastPlayed().asImmutable());
        values.add(this.sleepingIgnored().asImmutable());
        values.add(this.hasViewedCredits().asImmutable());

        // If getSpectatingEntity returns this player, then we are not spectating any other entity, so spectatorTarget would be an Optional.empty()
        this.spectatorTarget().map(Value::asImmutable).ifPresent(values::add);

        return values;
    }

    // Audience

    @Override
    public void sendMessage(final Identity identity, final Component message, final MessageType type) {
        if (this.impl$isFake) {
            return;
        }
        this.connection.send(new SChatPacket(SpongeAdventure.asVanilla(Objects.requireNonNull(message, "message")),
                SpongeAdventure.asVanilla(Objects.requireNonNull(type, "type")), Objects.requireNonNull(identity, "identity").uuid()));
    }

    @Override
    public void sendActionBar(final Component message) {
        if (this.impl$isFake) {
            return;
        }
        this.connection.send(new STitlePacket(STitlePacket.Type.ACTIONBAR, SpongeAdventure.asVanilla(Objects.requireNonNull(message, "message"))));
    }

    @Override
    public void sendPlayerListHeader(final Component header) {
        this.api$tabList.setHeader(Objects.requireNonNull(header, "header"));
    }

    @Override
    public void sendPlayerListFooter(final Component footer) {
        this.api$tabList.setFooter(Objects.requireNonNull(footer, "footer"));
    }

    @Override
    public void sendPlayerListHeaderAndFooter(final Component header, final Component footer) {
        this.api$tabList.setHeaderAndFooter(Objects.requireNonNull(header, "header"), Objects.requireNonNull(footer, "footer"));
    }

    @Override
    public void showTitle(final Title title) {
        if (this.impl$isFake) {
            return;
        }
        final Title.Times times = Objects.requireNonNull(title, "title").times();
        if (times != null) {
            this.connection.send(new STitlePacket(this.api$durationToTicks(times.fadeIn()), this.api$durationToTicks(times.stay()), this.api$durationToTicks(times.fadeOut())));
        }
        this.connection.send(new STitlePacket(STitlePacket.Type.SUBTITLE, SpongeAdventure.asVanilla(title.subtitle())));
        this.connection.send(new STitlePacket(STitlePacket.Type.TITLE, SpongeAdventure.asVanilla(title.title())));
    }

    @Override
    public void clearTitle() {
        if (this.impl$isFake) {
            return;
        }
        this.connection.send(new STitlePacket(STitlePacket.Type.CLEAR, null));
    }

    @Override
    public void resetTitle() {
        if (this.impl$isFake) {
            return;
        }
        this.connection.send(new STitlePacket(STitlePacket.Type.RESET, null));
    }

    @Override
    public void showBossBar(final BossBar bar) {
        if (this.impl$isFake) {
            return;
        }
        final ServerBossInfo vanilla = SpongeAdventure.asVanillaServer(Objects.requireNonNull(bar, "bar"));
        vanilla.addPlayer((ServerPlayerEntity) (Object) this);
    }

    @Override
    public void hideBossBar(final BossBar bar) {
        if (this.impl$isFake) {
            return;
        }
        final ServerBossInfo vanilla = SpongeAdventure.asVanillaServer(Objects.requireNonNull(bar, "bar"));
        vanilla.removePlayer((ServerPlayerEntity) (Object) this);
    }

    @Override
    public void playSound(final Sound sound) {
        this.playSound(Objects.requireNonNull(sound, "sound"), this.shadow$getX(), this.shadow$getY(), this.shadow$getZ());
    }

    @Override
    public void playSound(final Sound sound, final double x, final double y, final double z) {
        if (this.impl$isFake) {
            return;
        }
        final Optional<SoundEvent> event = Registry.SOUND_EVENT.getOptional(SpongeAdventure.asVanilla(Objects.requireNonNull(sound, "sound").name()));
        if (event.isPresent()) {
            // Check if the event is registered
            this.connection.send(new SPlaySoundEffectPacket(event.get(), SpongeAdventure.asVanilla(sound.source()), x, y, z, sound.volume(), sound.pitch()));
        } else {
            // Otherwise send it as a custom sound
            this.connection.send(new SPlaySoundPacket(SpongeAdventure.asVanilla(sound.name()), SpongeAdventure.asVanilla(sound.source()),
                    new net.minecraft.util.math.vector.Vector3d(x, y, z), sound.volume(), sound.pitch()));
        }
    }

    @Override
    public void stopSound(final SoundStop stop) {
        if (this.impl$isFake) {
            return;
        }
        this.connection.send(new SStopSoundPacket(SpongeAdventure.asVanillaNullable(Objects.requireNonNull(stop, "stop").sound()), SpongeAdventure.asVanillaNullable(stop.source())));
    }

    @Override
    public void openBook(@NonNull final Book book) {
        if (this.impl$isFake) {
            return;
        }
        BookUtil.fakeBookView(Objects.requireNonNull(book, "book"), Collections.singletonList(this));
    }

    private int api$durationToTicks(final Duration duration) {
        return (int) (duration.toMillis() / 50L);
    }
}
