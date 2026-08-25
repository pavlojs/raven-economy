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

import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.whiteravens.ravencoin.config.RavenCoinConfig;

/**
 * The only way money is allowed to move.
 *
 * <p>Every front end goes through here — the commands, the ATM screen, the
 * shops, and whatever gets added later. In particular the ATM does <em>not</em>
 * work by running the commands: a screen is driven by a packet the client sent,
 * so a menu that composed {@code /eco add} strings would hand every player the
 * operator's own command, and a command reports failure by writing a sentence
 * into chat, which a screen cannot read, cannot show in place, and cannot roll
 * back. Both front ends call these methods and translate the
 * {@link TransactionResult} themselves.
 *
 * <p>Server thread only. Amounts are whole coins; there is no subdivision, and
 * there will not be one.
 */
public final class EconomyService {
    /** {@return the balance held by this player, or the starting balance if they have no account yet} */
    public static long balance(MinecraftServer server, UUID id) {
        return EconomyAccounts.of(server)
                .find(id)
                .map(Account::balance)
                .orElseGet(() -> RavenCoinConfig.COMMON.startingBalance.get());
    }

    /** {@return the account for this player, opening one if this is their first time} */
    public static Account account(MinecraftServer server, UUID id, String name) {
        return EconomyAccounts.of(server).open(id, name);
    }

    /**
     * Adds money to an account out of nothing.
     *
     * <p>Used by the ATM when a player feeds physical coins in, by the server
     * shop when it buys something, and by {@code /rc eco add}. The coins are
     * destroyed or created by the caller — this side only moves the number.
     */
    public static TransactionResult deposit(MinecraftServer server, UUID id, String name, long amount) {
        if (amount <= 0) {
            return TransactionResult.INVALID_AMOUNT;
        }
        EconomyAccounts accounts = EconomyAccounts.of(server);
        Account account = accounts.open(id, name);
        if (account.balance() > Long.MAX_VALUE - amount) {
            return TransactionResult.TOO_LARGE;
        }
        accounts.store(account.withBalance(account.balance() + amount));
        return TransactionResult.OK;
    }

    /** Takes money out of an account, refusing rather than going negative. */
    public static TransactionResult withdraw(MinecraftServer server, UUID id, String name, long amount) {
        if (amount <= 0) {
            return TransactionResult.INVALID_AMOUNT;
        }
        EconomyAccounts accounts = EconomyAccounts.of(server);
        Account account = accounts.open(id, name);
        if (account.balance() < amount) {
            return TransactionResult.INSUFFICIENT_FUNDS;
        }
        accounts.store(account.withBalance(account.balance() - amount));
        return TransactionResult.OK;
    }

    /**
     * Moves money between two accounts, or moves none at all.
     *
     * <p>The recipient's ceiling is checked before the payer is debited, so a
     * transfer that cannot land does not quietly delete the money on the way.
     */
    public static TransactionResult transfer(
            MinecraftServer server, UUID from, String fromName, UUID to, String toName, long amount) {
        if (!RavenCoinConfig.COMMON.payEnabled.get()) {
            return TransactionResult.DISABLED;
        }
        if (from.equals(to)) {
            return TransactionResult.SAME_ACCOUNT;
        }
        if (amount <= 0) {
            return TransactionResult.INVALID_AMOUNT;
        }
        EconomyAccounts accounts = EconomyAccounts.of(server);
        Account payer = accounts.open(from, fromName);
        Account payee = accounts.open(to, toName);
        if (payer.balance() < amount) {
            return TransactionResult.INSUFFICIENT_FUNDS;
        }
        if (payee.balance() > Long.MAX_VALUE - amount) {
            return TransactionResult.TOO_LARGE;
        }
        accounts.store(payer.withBalance(payer.balance() - amount));
        accounts.store(payee.withBalance(payee.balance() + amount));
        return TransactionResult.OK;
    }

    /** Sets a balance outright. Operator territory: nothing is charged to anyone. */
    public static TransactionResult set(MinecraftServer server, UUID id, String name, long amount) {
        if (amount < 0) {
            return TransactionResult.INVALID_AMOUNT;
        }
        EconomyAccounts accounts = EconomyAccounts.of(server);
        accounts.store(accounts.open(id, name).withBalance(amount));
        return TransactionResult.OK;
    }

    /** {@return the richest accounts, highest first} */
    public static List<Account> leaderboard(MinecraftServer server, int limit) {
        return EconomyAccounts.of(server).leaderboard(limit);
    }

    /** Opens this player's account if they do not have one, and refreshes the stored name. */
    public static void onLogin(ServerPlayer player) {
        EconomyAccounts.of(player.server).open(player.getUUID(), player.getGameProfile().getName());
    }

    private EconomyService() {}
}
