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
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.whiteravens.ravencoin.RavenCoin;

/**
 * What a player asked the ATM to do.
 *
 * <p>Carries an intent and a number and nothing else. It does not say what the
 * balance should become, because a client is never told a result it could have
 * chosen — the server reads the ledger, decides, and the screen finds out when
 * the balance it is watching changes.
 *
 * @param action  bank coins or draw them out
 * @param amount  how much, in whole coins
 */
public record AtmActionPayload(Action action, long amount) implements CustomPacketPayload {
    public enum Action {
        DEPOSIT,
        WITHDRAW
    }

    public static final Type<AtmActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "atm_action"));

    public static final StreamCodec<ByteBuf, AtmActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            payload -> payload.action().ordinal(),
            ByteBufCodecs.VAR_LONG,
            AtmActionPayload::amount,
            AtmActionPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Rebuilds a payload from the wire.
     *
     * <p>The ordinal is range-checked rather than indexed straight into
     * {@code values()}: this arrives from a client, and an out-of-range index
     * would be an {@link ArrayIndexOutOfBoundsException} thrown deep inside the
     * netty pipeline rather than a clean protocol error.
     */
    private static AtmActionPayload decode(int ordinal, long amount) {
        Action[] actions = Action.values();
        if (ordinal < 0 || ordinal >= actions.length) {
            throw new DecoderException("Unknown ATM action " + ordinal);
        }
        return new AtmActionPayload(actions[ordinal], amount);
    }
}
