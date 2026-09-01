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

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.whiteravens.ravencoin.economy.Account;
import net.whiteravens.ravencoin.economy.Amounts;
import net.whiteravens.ravencoin.economy.EconomyService;
import net.whiteravens.ravencoin.economy.LedgerEntry;
import net.whiteravens.ravencoin.rank.Playtime;
import net.whiteravens.ravencoin.rank.Rank;
import net.whiteravens.ravencoin.rank.RankService;

/**
 * What the ATM's two list pages contain.
 *
 * <p>Built on the server, for one player, at the moment they ask. Which rungs of
 * the ladder are yours and which are still locked is not a property of the
 * ladder — it is a property of you and the ladder together — so there is nothing
 * here a client could have worked out for itself, and nothing worth syncing
 * between visits to the bank.
 */
public final class AtmPages {
    /** As many places as the screen has room for. {@code /rc top} still shows ten. */
    private static final int LEADERBOARD_SIZE = 8;

    public static AtmListPayload build(ServerPlayer player, Page page) {
        return new AtmListPayload(page, switch (page) {
            case RANKS -> ranks(player);
            case TOP -> top(player);
            case HISTORY -> history(player);
        });
    }

    /**
     * The ladder as it stands for this player.
     *
     * <p>Every rung is listed, including the ones that cannot be bought, because
     * a ladder with the locked rungs hidden looks like a ladder that ends where
     * you are standing. What changes is whether the row has a button.
     */
    private static List<AtmListPayload.Row> ranks(ServerPlayer player) {
        if (!RankService.permissionsAvailable()) {
            return List.of(notice("commands.ravencoin.rank.error.no_permissions"));
        }
        List<Rank> ladder = RankService.ranks();
        if (ladder.isEmpty()) {
            return List.of(notice("commands.ravencoin.rank.list.empty"));
        }

        List<AtmListPayload.Row> rows = new ArrayList<>(ladder.size());
        for (Rank rank : ladder) {
            Component name = Component.literal(rank.name());
            if (RankService.owns(player, rank)) {
                rows.add(new AtmListPayload.Row(
                        name,
                        Component.translatable("screen.ravencoin.atm.rank.owned").withStyle(ChatFormatting.DARK_GREEN),
                        "",
                        false));
                continue;
            }

            // Only locked if the rung below is actually missing: a prerequisite
            // already held is no longer a reason not to sell.
            Rank required = RankService.prerequisite(rank).orElse(null);

            if (rank.earned()) {
                // No price is printed for a rank that is never sold. "0 RC" on a
                // row with a button would read as an invitation.
                rows.add(new AtmListPayload.Row(
                        name,
                        Component.translatable(
                                        "screen.ravencoin.atm.rank.earned",
                                        Playtime.format(rank.playtimeMinutes()),
                                        Playtime.format(Playtime.minutes(player)))
                                .withStyle(ChatFormatting.DARK_GRAY),
                        "",
                        false));
                continue;
            }

            if (required != null && !RankService.owns(player, required)) {
                rows.add(new AtmListPayload.Row(
                        name,
                        Component.translatable("screen.ravencoin.atm.rank.locked", required.name())
                                .withStyle(ChatFormatting.DARK_GRAY),
                        "",
                        false));
                continue;
            }

            rows.add(new AtmListPayload.Row(
                    name,
                    Component.translatable("screen.ravencoin.shop.coins", Amounts.format(rank.price()))
                            .withStyle(ChatFormatting.DARK_GRAY),
                    rank.id(),
                    RankService.enabled()));
        }
        return rows;
    }

    /**
     * The richest accounts.
     *
     * <p>Banked balances, like {@code /rc top} — and like it, blind to the coins
     * in anybody's pockets. That is the right list for a bank screen to show:
     * this is the bank's own ranking of its own accounts, and the honest whole-
     * holdings figure is what {@code /rc eco total} is for.
     */
    private static List<AtmListPayload.Row> top(ServerPlayer player) {
        List<Account> accounts = EconomyService.leaderboard(player.server, LEADERBOARD_SIZE);
        if (accounts.isEmpty()) {
            return List.of(notice("commands.ravencoin.top.empty"));
        }
        List<AtmListPayload.Row> rows = new ArrayList<>(accounts.size());
        for (int place = 0; place < accounts.size(); place++) {
            Account account = accounts.get(place);
            boolean you = account.id().equals(player.getUUID());
            Component name = Component.literal((place + 1) + ". " + account.name());
            rows.add(new AtmListPayload.Row(
                    you ? name.copy().withStyle(ChatFormatting.DARK_AQUA) : name,
                    Component.translatable("screen.ravencoin.shop.coins", Amounts.format(account.balance()))
                            .withStyle(ChatFormatting.DARK_GRAY),
                    "",
                    false));
        }
        return rows;
    }

    /**
     * This account's own statement.
     *
     * <p>How long ago each line was is worked out here rather than sent as a
     * timestamp for the client to subtract. A client's clock is its own, and a
     * statement that said "in 40 minutes" because somebody's laptop is slow
     * would be a bug report about the bank rather than about the clock.
     */
    private static List<AtmListPayload.Row> history(ServerPlayer player) {
        List<LedgerEntry> lines = EconomyService.history(player.server, player.getUUID());
        if (lines.isEmpty()) {
            return List.of(notice("screen.ravencoin.atm.history.empty"));
        }
        long now = System.currentTimeMillis();
        List<AtmListPayload.Row> rows = new ArrayList<>(lines.size());
        for (LedgerEntry line : lines) {
            Component what = line.other().isEmpty()
                    ? Component.translatable(line.kind().key())
                    : Component.translatable(line.kind().key() + ".from", line.other());
            rows.add(new AtmListPayload.Row(
                    Component.empty().append(ago(now - line.when())).append(" ").append(what),
                    Component.translatable(
                                    line.kind().incoming()
                                            ? "screen.ravencoin.atm.history.in"
                                            : "screen.ravencoin.atm.history.out",
                                    Amounts.format(line.amount()))
                            .withStyle(line.kind().incoming() ? ChatFormatting.GREEN : ChatFormatting.RED),
                    "",
                    false));
        }
        return rows;
    }

    /** {@return how long ago, in the coarsest unit that still says something} */
    private static Component ago(long millis) {
        long minutes = millis / 60_000L;
        if (minutes < 1) {
            return Component.translatable("screen.ravencoin.atm.ago.now").withStyle(ChatFormatting.DARK_GRAY);
        }
        if (minutes < 60) {
            return Component.translatable("screen.ravencoin.atm.ago.minutes", minutes)
                    .withStyle(ChatFormatting.DARK_GRAY);
        }
        if (minutes < 60 * 24) {
            return Component.translatable("screen.ravencoin.atm.ago.hours", minutes / 60)
                    .withStyle(ChatFormatting.DARK_GRAY);
        }
        return Component.translatable("screen.ravencoin.atm.ago.days", minutes / (60 * 24))
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    /** A page with nothing on it still says why, on one unpressable row. */
    private static AtmListPayload.Row notice(String key) {
        return new AtmListPayload.Row(
                Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY), Component.empty(), "", false);
    }

    private AtmPages() {}
}
