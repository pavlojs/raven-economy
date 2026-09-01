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
 * "Sell me this rank."
 *
 * <p>Carries the rank's id and nothing else — not its price, which the server
 * reads out of the ladder. A packet that named its own price would be a packet
 * that let a client set one.
 *
 * @param rank the ladder id, as the row's button was given it
 */
public record AtmRankBuyPayload(String rank) implements CustomPacketPayload {
    /** The same ceiling the shop's rank field uses. */
    private static final int MAX_RANK_LENGTH = 64;

    public static final Type<AtmRankBuyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "atm_rank_buy"));

    public static final StreamCodec<ByteBuf, AtmRankBuyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_RANK_LENGTH), AtmRankBuyPayload::rank, AtmRankBuyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
