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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.whiteravens.ravencoin.RavenCoin;

/**
 * What just happened, said on the screen the player is looking at.
 *
 * <p>The mod's own rule, written on {@code EconomyService} before any of this
 * existed: a command reports failure by writing a sentence into chat, which a
 * screen cannot read, cannot show in place, and cannot roll back. The ATM was
 * breaking that rule — every outcome went to chat, behind the screen, where a
 * player pressing a button in front of them would not look.
 *
 * <p>So a refused transfer says "nobody by that name has an account here" under
 * the field the name was typed into, rather than somewhere the screen is
 * covering.
 *
 * @param text  the sentence, translated on arrival like any other component
 * @param error whether it is a refusal, which decides its colour
 */
public record AtmNoticePayload(Component text, boolean error) implements CustomPacketPayload {
    public static final Type<AtmNoticePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "atm_notice"));

    public static final StreamCodec<ByteBuf, AtmNoticePayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC,
            AtmNoticePayload::text,
            ByteBufCodecs.BOOL,
            AtmNoticePayload::error,
            AtmNoticePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
