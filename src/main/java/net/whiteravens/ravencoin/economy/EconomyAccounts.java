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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.whiteravens.ravencoin.config.RavenCoinConfig;
import org.jetbrains.annotations.Nullable;

/**
 * Every account on the server, saved next to the world.
 *
 * <p>Chosen over per-player data attachments, which is what the plan originally
 * said, for three reasons that all point the same way. The leaderboard has to
 * rank players who are offline, and the whole season is a race to a billion, so
 * that is the headline feature rather than a nicety. {@code /eco add} on someone
 * who is not logged in has to work, or an operator cannot fix a mistake until
 * the player happens to come back. And a balance held on the player entity has
 * to be explicitly told to survive death and dimension changes, whereas money
 * kept in one map beside the world simply cannot notice either.
 *
 * <p>Lives in the overworld's storage. Not thread safe, and does not need to be:
 * everything that touches it runs on the server thread.
 */
public final class EconomyAccounts extends SavedData {
    /** File name under the world's {@code data/} directory. */
    public static final String FILE_NAME = "ravencoin_accounts";

    public static final SavedData.Factory<EconomyAccounts> FACTORY =
            new SavedData.Factory<>(EconomyAccounts::new, EconomyAccounts::load);

    /**
     * How much of an account's statement is kept.
     *
     * <p>Twenty is what the ATM shows, over two pages. Keeping more would mean
     * keeping it for every player who ever joined, in the one file the whole
     * economy is saved to, to answer a question nobody asks past the last
     * screenful.
     */
    public static final int HISTORY_LENGTH = 20;

    private final Map<UUID, Account> accounts = new HashMap<>();

    /** Newest first, capped at {@link #HISTORY_LENGTH}, and never held for an account that does not exist. */
    private final Map<UUID, List<LedgerEntry>> history = new HashMap<>();

    /** {@return the accounts for this server, loading or creating them on first use} */
    public static EconomyAccounts of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_NAME);
    }

    /** {@return the account for this id, or empty if that player has never had one} */
    public Optional<Account> find(UUID id) {
        return Optional.ofNullable(this.accounts.get(id));
    }

    /**
     * {@return the account for this id, opening one with the configured starting
     * balance if it does not exist yet}
     *
     * <p>The name is recorded so the leaderboard can show it later, and is
     * refreshed every time this is called with a fresh one.
     */
    public Account open(UUID id, String name) {
        Account existing = this.accounts.get(id);
        if (existing == null) {
            Account created = new Account(id, name, RavenCoinConfig.COMMON.startingBalance.get());
            this.accounts.put(id, created);
            this.setDirty();
            return created;
        }
        if (!existing.name().equals(name)) {
            Account renamed = existing.withName(name);
            this.accounts.put(id, renamed);
            this.setDirty();
            return renamed;
        }
        return existing;
    }

    /** Writes one line onto an account's statement, dropping the oldest if it is full. */
    void note(UUID id, LedgerEntry entry) {
        List<LedgerEntry> lines = this.history.computeIfAbsent(id, key -> new ArrayList<>(HISTORY_LENGTH));
        lines.add(0, entry);
        while (lines.size() > HISTORY_LENGTH) {
            lines.remove(lines.size() - 1);
        }
        this.setDirty();
    }

    /** {@return this account's statement, newest first} */
    public List<LedgerEntry> history(UUID id) {
        return List.copyOf(this.history.getOrDefault(id, List.of()));
    }

    /** Overwrites a balance. Callers are responsible for validating the number. */
    void store(Account account) {
        this.accounts.put(account.id(), account);
        this.setDirty();
    }

    /** {@return the highest balances first, at most {@code limit} of them} */
    public List<Account> leaderboard(int limit) {
        return this.accounts.values().stream()
                .sorted(Comparator.comparingLong(Account::balance)
                        .reversed()
                        .thenComparing(Account::name))
                .limit(limit)
                .toList();
    }

    /**
     * {@return the account belonging to this name, ignoring case}
     *
     * <p>Resolves a typed name without touching Mojang's session server, which
     * {@code GameProfileCache} would do for a name it has not seen — on the
     * server thread, for as long as that takes. Anyone worth paying has logged
     * in here at least once, so the ledger already knows them; anyone it does
     * not know has no account to pay into.
     */
    public Optional<Account> findByName(String name) {
        return this.accounts.values().stream()
                .filter(account -> account.name().equalsIgnoreCase(name))
                .findFirst();
    }

    /** {@return every account, in no particular order} */
    public Collection<Account> all() {
        return this.accounts.values();
    }

    /** {@return how many accounts exist} */
    public int size() {
        return this.accounts.size();
    }

    private static EconomyAccounts load(CompoundTag tag, HolderLookup.Provider registries) {
        EconomyAccounts data = new EconomyAccounts();
        ListTag list = tag.getList("Accounts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Id")) {
                continue;
            }
            UUID id = entry.getUUID("Id");
            data.accounts.put(id, new Account(id, entry.getString("Name"), entry.getLong("Balance")));
            data.history.put(id, readHistory(entry.getList("History", Tag.TAG_COMPOUND)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Account account : this.accounts.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", account.id());
            entry.putString("Name", account.name());
            entry.putLong("Balance", account.balance());
            entry.put("History", writeHistory(this.history.getOrDefault(account.id(), List.of())));
            list.add(entry);
        }
        tag.put("Accounts", list);
        return tag;
    }

    /**
     * Reads a statement back, dropping any line it cannot make sense of.
     *
     * <p>A kind is stored by name, so a line written by a later version of this
     * mod arrives here as a word this one has never heard of. Skipping it loses
     * one line of history; refusing the file loses everybody's money.
     */
    private static List<LedgerEntry> readHistory(ListTag lines) {
        List<LedgerEntry> history = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            CompoundTag line = lines.getCompound(i);
            LedgerEntry.Kind kind = kind(line.getString("Kind"));
            if (kind == null) {
                continue;
            }
            history.add(new LedgerEntry(
                    line.getLong("When"), kind, line.getLong("Amount"), line.getString("Other")));
        }
        return history;
    }

    private static ListTag writeHistory(List<LedgerEntry> history) {
        ListTag lines = new ListTag();
        for (LedgerEntry entry : history) {
            CompoundTag line = new CompoundTag();
            line.putLong("When", entry.when());
            line.putString("Kind", entry.kind().name());
            line.putLong("Amount", entry.amount());
            line.putString("Other", entry.other());
            lines.add(line);
        }
        return lines;
    }

    @Nullable
    private static LedgerEntry.Kind kind(String name) {
        for (LedgerEntry.Kind kind : LedgerEntry.Kind.values()) {
            if (kind.name().equals(name)) {
                return kind;
            }
        }
        return null;
    }

    private EconomyAccounts() {}
}
