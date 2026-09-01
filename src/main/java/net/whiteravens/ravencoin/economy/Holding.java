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
package net.whiteravens.ravencoin.economy;

import java.util.UUID;

/**
 * What one player is worth, as far as anything can tell.
 *
 * <p>Two halves, because they are found in two different places and only one of
 * them is exact. The banked half comes out of the ledger and is the whole truth.
 * The carried half is counted out of that player's own pockets and ender chest,
 * and stops there — see {@link MoneyCensus} for what it therefore misses.
 *
 * @param id      the player's UUID
 * @param name    the player's name as of their last login
 * @param banked  what the ledger holds for them
 * @param carried the value of the coins found on them
 */
public record Holding(UUID id, String name, long banked, long carried) {
    public long total() {
        return this.banked + this.carried;
    }
}
