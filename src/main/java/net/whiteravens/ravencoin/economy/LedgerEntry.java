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

/**
 * One line of an account's statement.
 *
 * <p>Kept because a balance on its own answers nothing when it is wrong. A
 * player who logs in poorer than they left wants to know what happened, and on
 * a server where money can leave an account while its owner is offline — a shop
 * of theirs sells something, somebody pays them — that question has no other
 * answer.
 *
 * @param when   epoch milliseconds, so "how long ago" can be worked out later
 * @param kind   which way the money went and why
 * @param amount how much moved, always positive; the direction is the kind
 * @param other  who or what was on the other side — a player's name, a shop's
 *               goods — or empty when there was nobody
 */
public record LedgerEntry(long when, Kind kind, long amount, String other) {
    /**
     * Why an account changed.
     *
     * <p>Stored by name rather than ordinal: this goes in the world's save file
     * and outlives any particular version of this mod, so inserting a constant
     * must not silently relabel everybody's history.
     */
    public enum Kind {
        /** Coins fed into the machine. */
        DEPOSIT(true),
        /** Coins drawn out of it. */
        WITHDRAW(false),
        /** Paid to another player. */
        PAY_OUT(false),
        /** Received from another player. */
        PAY_IN(true),
        /** Spent in a shop. */
        BUY(false),
        /** Earned by one of your own shops selling something. */
        SALE(true),
        /** Spent on a rank. */
        RANK(false),
        /** An operator moved it. */
        ADJUST(true);

        private final boolean incoming;

        Kind(boolean incoming) {
            this.incoming = incoming;
        }

        /** {@return whether this kind adds to the balance} */
        public boolean incoming() {
            return this.incoming;
        }

        /** {@return the translation key describing this kind on the statement} */
        public String key() {
            return "screen.ravencoin.atm.history." + this.name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
