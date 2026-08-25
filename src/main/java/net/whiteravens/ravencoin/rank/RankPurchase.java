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

/** Why a rank was or was not sold. */
public enum RankPurchase {
    /** Bought and granted. */
    OK,
    /** The operator switched rank sales off. */
    DISABLED,
    /** No permissions plugin is installed, so there is nothing to grant. */
    NO_PERMISSIONS,
    /** No rank by that id. */
    UNKNOWN_RANK,
    /** The player is already in that group. */
    ALREADY_OWNED,
    /** The rung below has not been bought yet. */
    OUT_OF_ORDER,
    /** The rank is granted for time played, so there is nothing to buy. */
    EARNED_ONLY,
    /** The account cannot cover the price. */
    INSUFFICIENT_FUNDS,
    /** The permissions plugin refused or failed; the money has been returned. */
    FAILED;

    public boolean ok() {
        return this == OK;
    }
}
