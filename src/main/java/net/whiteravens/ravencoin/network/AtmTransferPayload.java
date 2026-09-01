/*
 * Copyright 2026 pavlojs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.whiteravens.ravencoin.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.whiteravens.ravencoin.RavenCoin;

/**
 * A transfer typed at the ATM.
 *
 * <p>Separate from {@link AtmActionPayload} because it carries a name, and a
 * name is the one field on this screen the server cannot check against
 * anything it already has: banking and withdrawing act on the player who sent
 * the packet, and this acts on somebody else.
 *
 * @param player who to pay, as typed
 * @param amount how much, in whole coins
 */
public record AtmTransferPayload(String player, long amount) implements CustomPacketPayload {
    /** Room for any Minecraft name, and for the longer ones an offline server allows. */
    private static final int MAX_NAME_LENGTH = 32;

    public static final Type<AtmTransferPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "atm_transfer"));

    public static final StreamCodec<ByteBuf, AtmTransferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH),
            AtmTransferPayload::player,
            ByteBufCodecs.VAR_LONG,
            AtmTransferPayload::amount,
            AtmTransferPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
