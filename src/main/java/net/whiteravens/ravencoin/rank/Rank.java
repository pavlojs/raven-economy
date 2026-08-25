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

import java.util.Optional;

/**
 * One rung of the rank ladder.
 *
 * @param id              what a player types and what the file keys on; never shown
 * @param group           the LuckPerms group granted on purchase
 * @param price           cost in RavenCoin, taken from the account
 * @param name            what a player sees, so an operator can rename a rank
 *                        without breaking the command every player has learnt
 * @param requires        id of a rank that must be owned first, or {@code null} for
 *                        none. An explicit prerequisite beats the file's own order,
 *                        which is what makes a branching ladder possible — two
 *                        tracks that both start from the same rung, or a prestige
 *                        rank reachable from either side.
 * @param playtimeMinutes minutes a player must have played before this rank is
 *                        granted to them for nothing, or {@code 0} for a rank that
 *                        is only ever bought. Minutes rather than hours so a rung
 *                        can be set to something short enough to actually test.
 */
public record Rank(String id, String group, long price, String name, String requires, long playtimeMinutes) {
    public Optional<String> prerequisite() {
        return Optional.ofNullable(this.requires);
    }

    /**
     * {@return true if playing long enough is what grants this rank}
     *
     * <p>Such a rank is <b>earned, never sold</b>, whatever its price says — see
     * {@link RankService#buy}. Letting both routes exist at once would mean a rung
     * priced at zero could be claimed at nought hours by typing the buy command,
     * which is the opposite of what a time requirement is for.
     */
    public boolean earned() {
        return this.playtimeMinutes > 0;
    }

    public Rank withRequires(String other) {
        return new Rank(this.id, this.group, this.price, this.name, other, this.playtimeMinutes);
    }

    public Rank withPlaytime(long minutes) {
        return new Rank(this.id, this.group, this.price, this.name, this.requires, minutes);
    }
}
