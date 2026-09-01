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
 * How many lots the customer pressed buy for.
 *
 * <p>Which shop is not on the wire. The only shop a player can buy from is the
 * one whose screen they have open, and the server already knows which that is.
 *
 * @param fromAccount whether to pay out of the buyer's account rather than
 *                    their pockets. Only means anything where the price is
 *                    RavenCoin; the shop decides that, not the screen.
 * @param lots how many times to run the trade
 */
public record ShopBuyPayload(int lots, boolean fromAccount) implements CustomPacketPayload {
    public static final Type<ShopBuyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "shop_buy"));

    public static final StreamCodec<ByteBuf, ShopBuyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ShopBuyPayload::lots,
            ByteBufCodecs.BOOL,
            ShopBuyPayload::fromAccount,
            ShopBuyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
