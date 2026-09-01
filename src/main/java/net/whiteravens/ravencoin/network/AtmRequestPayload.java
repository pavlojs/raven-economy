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
 * "Show me this page."
 *
 * <p>Sent when a page that needs the server's own state is opened, and again
 * when something on it has just changed — buying a rank is the case that
 * matters, because the row you pressed has to stop saying its price and start
 * saying it is yours.
 *
 * @param page which list to fill in
 */
public record AtmRequestPayload(Page page) implements CustomPacketPayload {
    public static final Type<AtmRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "atm_request"));

    @SuppressWarnings("EnumOrdinal")
    public static final StreamCodec<ByteBuf, AtmRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, payload -> payload.page().ordinal(), AtmRequestPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static AtmRequestPayload decode(int ordinal) {
        Page[] pages = Page.values();
        if (ordinal < 0 || ordinal >= pages.length) {
            throw new DecoderException("Unknown ATM page " + ordinal);
        }
        return new AtmRequestPayload(pages[ordinal]);
    }
}
