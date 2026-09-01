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

/**
 * A page of the ATM whose contents the server has to supply.
 *
 * <p>Banking and transferring are not here: those are typed into fields the
 * client already owns and need nothing from the server until the button is
 * pressed. A ladder, a leaderboard and a statement are the server's own state
 * and have to be fetched.
 */
public enum Page {
    /** The rank ladder, as it stands for the player looking at it. */
    RANKS,
    /** The richest accounts. Banked balances only, the same as {@code /rc top}. */
    TOP,
    /** This account's own statement, newest first. */
    HISTORY
}
