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
 * The four things a market stall can be asked to do.
 *
 * <p>Kept apart from {@link ShopSettingsPayload} because each of these needs a
 * different person: the operator puts a stall on the market, anybody may take
 * one, and only the renter opens the barrel or gives it back. A settings packet that carried
 * "for rent" as a fourth field would hand the renter the switch that ends their
 * own tenancy.
 *
 * @param action what was asked for
 */
public record ShopStallPayload(Action action) implements CustomPacketPayload {
    public enum Action {
        /** Take the stall, paying the first period now. */
        RENT,
        /** Open the container the stall sells out of. */
        RESTOCK,
        /** Operator only: put this server shop on the market, or take it off. */
        TO_LET,
        /** Renter only: hand the stall back, keeping neither the rent nor the goods. */
        END
    }

    public static final Type<ShopStallPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "shop_stall"));

    // The ordinal is the wire format, which is safe for the reason the ATM's
    // action packet gives: both ends are the same jar, and decode range-checks.
    @SuppressWarnings("EnumOrdinal")
    public static final StreamCodec<ByteBuf, ShopStallPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, payload -> payload.action().ordinal(), ShopStallPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static ShopStallPayload decode(int ordinal) {
        Action[] actions = Action.values();
        if (ordinal < 0 || ordinal >= actions.length) {
            throw new DecoderException("Unknown stall action " + ordinal);
        }
        return new ShopStallPayload(actions[ordinal]);
    }
}
