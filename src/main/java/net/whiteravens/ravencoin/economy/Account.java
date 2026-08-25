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
 * One player's account.
 *
 * <p>The name is stored alongside the balance only so the leaderboard can be
 * drawn without every listed player being online. It is refreshed on login, so
 * a rename shows up the next time that player joins.
 *
 * @param id      the player's UUID — the real key; names are display only
 * @param name    the player's name as of their last login
 * @param balance RavenCoin held in the account, never negative
 */
public record Account(UUID id, String name, long balance) {
    public Account withBalance(long newBalance) {
        return new Account(this.id, this.name, newBalance);
    }

    public Account withName(String newName) {
        return new Account(this.id, newName, this.balance);
    }
}
