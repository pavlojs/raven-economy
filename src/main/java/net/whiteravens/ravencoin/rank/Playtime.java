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
package net.whiteravens.ravencoin.rank;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

/** How long someone has played, and how to say it. */
public final class Playtime {
    private static final int TICKS_PER_MINUTE = 20 * 60;
    private static final long MINUTES_PER_HOUR = 60;

    /**
     * {@return minutes this player has been in the world}
     *
     * <p>Read from the vanilla {@code minecraft:play_time} statistic rather than
     * counted here. That statistic is already persisted per player, already
     * survives restarts, and is already what {@code /stats} and every playtime
     * mod on the market agree on — a second counter of our own would be one more
     * thing to keep in step and one more thing to lose.
     *
     * <p>It counts time <em>connected</em>, not time active. Someone standing
     * still at a furnace is playing as far as this is concerned, and so is
     * someone who walked away from the keyboard. That is the same bargain every
     * playtime ladder makes; a rung measured in tens of hours can afford it.
     */
    public static long minutes(ServerPlayer player) {
        return (long) player.getStats().getValue(Stats.CUSTOM, Stats.PLAY_TIME) / TICKS_PER_MINUTE;
    }

    /**
     * {@return a duration a player can read, like {@code 40 h} or {@code 1 h 30 min}}
     *
     * <p>Built from bare symbols rather than translated words, because {@code h}
     * and {@code min} are written the same way in both languages this mod ships,
     * and a duration assembled out of translatable fragments reads badly in at
     * least one of them. If a language ever arrives where that stops being true,
     * this is the one place to fix it.
     */
    public static String format(long minutes) {
        long hours = minutes / MINUTES_PER_HOUR;
        long rest = minutes % MINUTES_PER_HOUR;
        if (hours == 0) {
            return rest + " min";
        }
        return rest == 0 ? hours + " h" : hours + " h " + rest + " min";
    }

    private Playtime() {}
}
