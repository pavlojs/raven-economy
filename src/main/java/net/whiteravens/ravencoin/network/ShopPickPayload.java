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
 * "Copy what I am holding into the goods slot, or the price slot."
 *
 * <p>Carries no item. The server reads its own copy of the player's cursor,
 * which is the difference between picking up a diamond and telling the server
 * you have one.
 *
 * @param forPrice true for the price slot, false for the goods slot
 */
public record ShopPickPayload(boolean forPrice) implements CustomPacketPayload {
    public static final Type<ShopPickPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "shop_pick"));

    public static final StreamCodec<ByteBuf, ShopPickPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, ShopPickPayload::forPrice, ShopPickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
