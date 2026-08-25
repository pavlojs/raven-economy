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
                    .define("payEnabled", true);            builder.pop();
        }
    }

    private RavenCoinConfig() {}
}
