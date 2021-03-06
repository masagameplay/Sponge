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
package org.spongepowered.common.mixin.core.network.handshake;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.ServerHandshakeNetHandler;
import net.minecraft.network.handshake.client.CHandshakePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.accessor.network.handshake.client.CHandshakePacketAccessor;
import org.spongepowered.common.bridge.network.NetworkManagerBridge;
import org.spongepowered.common.bridge.network.NetworkManagerHolderBridge;
import org.spongepowered.common.util.NetworkUtil;

@Mixin(ServerHandshakeNetHandler.class)
public abstract class ServerHandshakeNetHandlerMixin implements NetworkManagerHolderBridge {

    @Shadow @Final private NetworkManager connection;

    @Inject(method = "handleIntention", at = @At("HEAD"))
    private void impl$updateVersionAndHost(final CHandshakePacket packetIn, final CallbackInfo ci) {
        final NetworkManagerBridge info = (NetworkManagerBridge) this.connection;
        info.bridge$setVersion(packetIn.getProtocolVersion());
        info.bridge$setVirtualHost(NetworkUtil.cleanVirtualHost(
                ((CHandshakePacketAccessor) packetIn).accessor$hostName()), ((CHandshakePacketAccessor) packetIn).accessor$port());
    }

    @Override
    public NetworkManager bridge$getConnection() {
        return this.connection;
    }
}
