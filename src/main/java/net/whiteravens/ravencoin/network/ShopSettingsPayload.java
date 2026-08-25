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
 * Everything the settings screen can change, applied in one press.
 *
 * <p>One packet rather than one per field, so a shop is never briefly selling
 * at the old price with the new goods. The two item choices are not here —
 * those are {@link ShopPickPayload}, and they take effect as they are clicked
 * because a player needs to see what they picked.
 *
 * @param productUnits how many of the goods one trade hands over
 * @param priceUnits   how many of the price one trade costs
 * @param rank         rank id required to buy, or empty for none
 * @param showLabel    whether the sign floats above the block
 */
public record ShopSettingsPayload(int productUnits, int priceUnits, String rank, boolean showLabel)
        implements CustomPacketPayload {
    /** Long enough for any rank id, short enough that nobody can post a novel through it. */
    private static final int MAX_RANK_LENGTH = 64;

    public static final Type<ShopSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "shop_settings"));

    public static final StreamCodec<ByteBuf, ShopSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ShopSettingsPayload::productUnits,
            ByteBufCodecs.VAR_INT,
            ShopSettingsPayload::priceUnits,
            ByteBufCodecs.stringUtf8(MAX_RANK_LENGTH),
            ShopSettingsPayload::rank,
            ByteBufCodecs.BOOL,
            ShopSettingsPayload::showLabel,
            ShopSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
