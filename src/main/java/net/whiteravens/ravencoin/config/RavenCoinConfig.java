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
package net.whiteravens.ravencoin.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Server-operator settings. Every feature this mod adds can be switched off,
 * because the first live season is what tells you which of them were a good
 * idea — not the design document.
 *
 * <p>Deliberately COMMON rather than SERVER: the minting switch decides whether
 * a recipe exists at all, and recipe conditions are read when the datapack
 * loads. A per-world config would be read too late to be trusted here. It also
 * means a change needs a restart, which for a money-supply lever is the right
 * amount of friction.
 */
public final class RavenCoinConfig {
    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static final class Common {
        /**
         * Whether players can mint RavenCoin themselves.
         *
         * <p>This is the money supply valve, and the single most consequential
         * line in the file. On, and the recipe's cost sets the exchange rate:
         * a coin is worth what its gold, emerald and diamond cost to gather.
         * Off, and every coin in the world came from the server shop, a quest
         * reward or a rank — the operator holds the whole supply.
         *
         * <p>Expect to turn it off eventually. This is a tech pack; given long
         * enough, players automate the gathering of every ingredient, and a
         * recipe priced against hand-mining stops meaning anything.
         */
        public final ModConfigSpec.BooleanValue mintingEnabled;

        /**
         * What a brand new account opens with.
         *
         * <p>Charged to nobody — this is minted out of nothing the first time a
         * player logs in, so it is part of the money supply just as much as the
         * recipe is. Zero means players start with nothing and the first coin
         * has to be earned.
         */
        public final ModConfigSpec.LongValue startingBalance;

        /** Whether players may transfer money to each other at all. */
        public final ModConfigSpec.BooleanValue payEnabled;

        /**
         * Whether to also register the bare command names.
         *
         * <p>Off by default, and deliberately so: {@code /balance}, {@code /pay}
         * and {@code /baltop} are the names every economy mod and every server
         * plugin wants, and two mods claiming one name is how a server ends up
         * with a command that silently does the wrong thing. The prefixed forms
         * under {@code /rc} are always registered and can never collide, so
         * nothing is lost by leaving this off — turn it on when you know this is
         * the only economy on the server.
         */
        public final ModConfigSpec.BooleanValue shortCommandAliases;

        /**
         * Whether shops trade at all.
         *
         * <p>Off leaves every shop standing and configured but refusing to sell,
         * which is what you want in the hour after finding a pricing mistake:
         * nobody loses their shop, and nobody empties it at the wrong price
         * while the operator works out what the right one was.
         */
        public final ModConfigSpec.BooleanValue shopsEnabled;

        /**
         * Whether ranks can be bought at all.
         *
         * <p>Off is not the same as an empty ladder: with this off the rank tab
         * and commands say so plainly, rather than showing a shop with nothing
         * in it.
         */
        public final ModConfigSpec.BooleanValue ranksEnabled;

        /**
         * Whether a rank can only be bought once the one below it is held.
         *
         * <p>On, the ladder is a ladder. Off, a player with enough saved can jump
         * straight to the top, which is a different kind of server — and cheaper
         * for them, since they skip paying for every rung on the way.
         */
        public final ModConfigSpec.BooleanValue requireLadderOrder;

        private Common(ModConfigSpec.Builder builder) {
            builder.comment("RavenCoin — the White Ravens Forge economy").push("currency");
            mintingEnabled = builder
                    .comment(
                            "Whether the minting recipe exists at all.",
                            "true  — players mint their own; the recipe's cost is the exchange rate.",
                            "false — the server shop, quests and ranks are the only sources of coin.",
                            "Changing this needs a restart: recipes are built when the datapack loads.")
                    .define("mintingEnabled", true);
            startingBalance = builder
                    .comment("How much RavenCoin a player's account is opened with on first login.")
                    .defineInRange("startingBalance", 0L, 0L, Long.MAX_VALUE);
            builder.pop();

            builder.comment("Commands").push("commands");
            payEnabled = builder
                    .comment("Whether players can transfer money to each other.")
                    .define("payEnabled", true);
            shortCommandAliases = builder
                    .comment(
                            "Whether to register /balance, /bal, /pay and /baltop as well.",
                            "The prefixed forms /rc and /ravencoin are always registered.",
                            "Leave this off if another economy mod or plugin owns those names.")
                    .define("shortCommandAliases", false);
            builder.pop();

            builder.comment("Shops — each block is configured in game, by whoever owns it").push("shops");
            shopsEnabled = builder
                    .comment(
                            "Whether shop blocks will trade.",
                            "false leaves them standing and configured, but closed.")
                    .define("shopsEnabled", true);
            builder.pop();

            builder.comment("Ranks — the ladder itself lives in ravencoin-ranks.json").push("ranks");
            ranksEnabled = builder
                    .comment(
                            "Whether ranks can be bought with RavenCoin.",
                            "Needs a permissions plugin; without LuckPerms this does nothing.")
                    .define("ranksEnabled", true);
            requireLadderOrder = builder
                    .comment("Whether each rank requires the one below it to be owned first.")
                    .define("requireLadderOrder", true);
            builder.pop();
        }
    }

    private RavenCoinConfig() {}
}
