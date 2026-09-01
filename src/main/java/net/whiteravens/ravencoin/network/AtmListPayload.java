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
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.whiteravens.ravencoin.RavenCoin;

/**
 * One page of the ATM, filled in by the server.
 *
 * <p>The rank ladder and the leaderboard both live on the server and neither is
 * worth syncing continuously — a player looks at them for a few seconds each
 * time they visit the bank. So the screen asks with {@link AtmRequestPayload}
 * and gets one of these back, and what it draws is a list of finished lines
 * rather than a model it would have to interpret. That keeps the ladder's rules
 * — who owns what, what needs what first — in the one place that knows them.
 *
 * <p>A row carries {@link Component}s rather than finished strings. It is
 * written on the server and read by a client that may be running a different
 * language, and the server has no idea which — resolving the translation here
 * would send every Polish player English. The component travels untranslated
 * and the client's own language file finishes it, which is what every chat
 * message in the game already does.
 *
 * @param page what was asked for
 * @param rows the lines to draw, in order
 */
public record AtmListPayload(Page page, List<Row> rows) implements CustomPacketPayload {
    /** More than any bank screen will ever show, and small enough to be harmless. */
    private static final int MAX_ROWS = 32;

    /** Rank ids only; a leaderboard row is never actionable. */
    private static final int MAX_ACTION = 64;

    public static final Type<AtmListPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "atm_list"));

    @SuppressWarnings("EnumOrdinal")
    public static final StreamCodec<ByteBuf, AtmListPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            payload -> payload.page().ordinal(),
            Row.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)),
            AtmListPayload::rows,
            AtmListPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static AtmListPayload decode(int ordinal, List<Row> rows) {
        Page[] pages = Page.values();
        if (ordinal < 0 || ordinal >= pages.length) {
            throw new DecoderException("Unknown ATM page " + ordinal);
        }
        return new AtmListPayload(pages[ordinal], rows);
    }

    /**
     * One line of a list.
     *
     * @param label   the left half — a rank's name, or a place and a player
     * @param detail  the right half — a price, or a balance
     * @param action  rank id to buy when the row's button is pressed, or empty
     *                for a row that is only there to be read
     * @param enabled whether that button can be pressed; a rank already owned,
     *                or one whose rung below is missing, is shown and refused
     */
    public record Row(Component label, Component detail, String action, boolean enabled) {
        public static final StreamCodec<ByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
                ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC,
                Row::label,
                ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC,
                Row::detail,
                ByteBufCodecs.stringUtf8(MAX_ACTION),
                Row::action,
                ByteBufCodecs.BOOL,
                Row::enabled,
                Row::new);

        public boolean actionable() {
            return this.enabled && !this.action.isEmpty();
        }
    }
}
