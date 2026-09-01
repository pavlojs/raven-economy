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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.config.RavenCoinConfig;
import net.whiteravens.ravencoin.economy.Amounts;
import net.whiteravens.ravencoin.economy.EconomyService;
import net.whiteravens.ravencoin.economy.TransactionResult;
import net.whiteravens.ravencoin.network.AtmPages;
import net.whiteravens.ravencoin.network.Page;
import org.jetbrains.annotations.Nullable;

/**
 * Selling rungs of the rank ladder.
 *
 * <p>Sits between the ladder file, the ledger and the permissions plugin, and is
 * the only thing that knows all three. Like {@link EconomyService} it is called
 * identically by a command and by the ATM screen.
 */
public final class RankService {
    private static final String LUCKPERMS = "luckperms";

    @Nullable
    private static RankLadder ladder;

    @Nullable
    private static Boolean permissionsAvailable;

    /**
     * Players with a playtime grant in flight.
     *
     * <p>Concurrent because it is cleared from whichever thread LuckPerms saved
     * on, and read from the server thread on the next pass.
     */
    private static final Set<UUID> promoting = ConcurrentHashMap.newKeySet();

    /** Reads the ladder from disk. Called when the server starts and by {@code /rc rank reload}. */
    public static boolean reload() {
        ladder = RankLadder.load();
        return ladder != null;
    }

    /** {@return the ladder, or an empty one if the file failed to parse} */
    public static List<Rank> ranks() {
        return ladder == null ? List.of() : ladder.ranks();
    }

    public static Optional<Rank> find(String id) {
        return ladder == null ? Optional.empty() : ladder.find(id);
    }

    /**
     * {@return whether a permissions plugin is here to grant anything}
     *
     * <p>Cached after the first look: the answer cannot change while the server
     * is up, and {@link ModList} is not free.
     */
    public static boolean permissionsAvailable() {
        if (permissionsAvailable == null) {
            permissionsAvailable = ModList.get().isLoaded(LUCKPERMS);
        }
        return permissionsAvailable;
    }

    /** {@return whether rank sales are switched on and able to work} */
    public static boolean enabled() {
        return RavenCoinConfig.COMMON.ranksEnabled.get() && permissionsAvailable() && ladder != null;
    }

    /** {@return true if this player already holds the rank} */
    public static boolean owns(ServerPlayer player, Rank rank) {
        return permissionsAvailable() && LuckPermsBridge.hasGroup(player.getUUID(), rank.group());
    }

    /**
     * Sells one rank.
     *
     * <p>Charges first, then grants, then refunds if the grant did not survive
     * being saved. Doing it the other way round — grant, then charge — hands out
     * the rank for free whenever the debit fails, and this runs on a server where
     * the whole point is that money is scarce.
     */
    public static RankPurchase buy(ServerPlayer player, String rankId) {
        if (!RavenCoinConfig.COMMON.ranksEnabled.get()) {
            return RankPurchase.DISABLED;
        }
        if (!permissionsAvailable()) {
            return RankPurchase.NO_PERMISSIONS;
        }
        Optional<Rank> found = find(rankId);
        if (found.isEmpty()) {
            return RankPurchase.UNKNOWN_RANK;
        }
        Rank rank = found.get();

        if (owns(player, rank)) {
            return RankPurchase.ALREADY_OWNED;
        }
        // A rank with a time requirement is earned, and a price on it is dead
        // text. Selling it anyway would let a rung priced at zero — which is how
        // a pure time rank is written — be claimed at nought hours by typing the
        // buy command.
        if (rank.earned()) {
            return RankPurchase.EARNED_ONLY;
        }
        Optional<Rank> needed = prerequisite(rank);
        if (needed.isPresent() && !owns(player, needed.get())) {
            return RankPurchase.OUT_OF_ORDER;
        }

        // Captured while the player is certainly still here. LuckPerms answers on
        // its own thread and can take longer than someone stays logged in;
        // reaching back through `player` at that point gives a null server, and
        // the NPE would disappear into a future nobody waits on — leaving a
        // player charged for a rank they never got and no refund.
        MinecraftServer server = player.getServer();
        UUID playerId = player.getUUID();
        String playerName = player.getGameProfile().getName();

        TransactionResult charged = EconomyService.withdraw(server, playerId, playerName, rank.price());
        if (!charged.ok()) {
            return RankPurchase.INSUFFICIENT_FUNDS;
        }

        // Deliberately not awaited: this callback is the whole of the handling and
        // no caller has anything to do until it runs. Named `unused` because that
        // is what says so to both a reader and Error Prone.
        CompletableFuture<Void> unused = LuckPermsBridge.grant(playerId, rank.group())
                .whenComplete((ignored, failure) -> {
                    if (failure == null) {
                        // The list the buyer is looking at was drawn before this
                        // ran, so it still offers the rank they have just bought.
                        // Sending it again on the way in is what the caller
                        // already does, and it is a packet too early — the grant
                        // is what makes owns() true, and this is when it is.
                        server.execute(() -> {
                            ServerPlayer viewer = server.getPlayerList().getPlayer(playerId);
                            if (viewer != null) {
                                PacketDistributor.sendToPlayer(viewer, AtmPages.build(viewer, Page.RANKS));
                            }
                        });
                        return;
                    }
                    RavenCoin.LOG.error("Granting {} to {} failed — refunding", rank.group(), playerName, failure);
                    // Back on the server thread: the ledger is not thread safe, and
                    // this callback arrives on whichever thread LuckPerms saved on.
                    server.execute(() -> {
                        EconomyService.deposit(server, playerId, playerName, rank.price());
                        // The money goes back whether or not they are still online —
                        // the ledger holds absent players. Only the message needs
                        // somebody to read it.
                        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
                        if (online != null) {
                            online.sendSystemMessage(Component.translatable(
                                    "commands.ravencoin.rank.refunded", rank.name(), Amounts.format(rank.price())));
                        }
                    });
                });

        return RankPurchase.OK;
    }

    /**
     * Grants whatever this player has now played long enough for.
     *
     * <p>Called on login and on a slow timer. Deliberately grants <b>one</b> rung
     * per call, lowest first: a grant is asynchronous, so the second rung of a
     * catch-up would be decided against a permissions state that has not caught
     * up with the first. Someone who arrives already past several thresholds
     * climbs them a timer-tick apart, which nobody will notice and nothing can
     * get wrong.
     *
     * <p>Nothing here ever takes a group away. A rank bought with money and a
     * rank earned with time land in the same place, and the automation must not
     * be able to undo the one it did not hand out.
     */
    public static void promoteForPlaytime(ServerPlayer player) {
        if (!enabled() || !RavenCoinConfig.COMMON.playtimePromotion.get()) {
            return;
        }
        if (promoting.contains(player.getUUID())) {
            return;
        }
        Optional<Rank> earned = nextEarned(player);
        if (earned.isEmpty()) {
            return;
        }
        Rank rank = earned.get();

        // Held from here until LuckPerms has answered. Without it a save that
        // takes longer than the timer would be granted — and announced — twice.
        promoting.add(player.getUUID());
        // Captured now for the same reason as in buy(): the answer arrives on
        // LuckPerms' thread, by which time the player may be gone.
        MinecraftServer server = player.getServer();
        UUID playerId = player.getUUID();
        String playerName = player.getGameProfile().getName();
        String played = Playtime.format(Playtime.minutes(player));

        // Not awaited, as in buy() — see the note there.
        CompletableFuture<Void> unused = LuckPermsBridge.grant(playerId, rank.group())
                .whenComplete((ignored, failure) -> server.execute(() -> {
                    promoting.remove(playerId);
                    if (failure != null) {
                        // No money changed hands, so there is nothing to put back
                        // and no reason to tell the player. The next pass retries.
                        RavenCoin.LOG.error(
                                "Could not grant {} to {} for playtime — will retry",
                                rank.group(),
                                playerName,
                                failure);
                        return;
                    }
                    RavenCoin.LOG.info("{} reached {} after {}", playerName, rank.id(), played);
                    server.getPlayerList()
                            .broadcastSystemMessage(
                                    Component.translatable(
                                            "commands.ravencoin.rank.promoted", playerName, rank.name()),
                                    false);
                }));
    }

    /**
     * {@return the lowest rung this player has earned and does not yet hold}
     *
     * <p>A prerequisite still counts. An operator who hangs a timed rank off a
     * bought one means it, and time alone should not walk past the rung that has
     * to be paid for.
     */
    public static Optional<Rank> nextEarned(ServerPlayer player) {
        long played = Playtime.minutes(player);
        for (Rank rank : ranks()) {
            if (!rank.earned() || played < rank.playtimeMinutes() || owns(player, rank)) {
                continue;
            }
            Optional<Rank> needed = prerequisite(rank);
            if (needed.isPresent() && !owns(player, needed.get())) {
                continue;
            }
            return Optional.of(rank);
        }
        return Optional.empty();
    }

    /** Sets or clears the time a rank takes to earn. {@return false if there is no such rank} */
    public static boolean setPlaytime(String id, long minutes) {
        if (ladder == null) {
            return false;
        }
        Optional<Rank> rank = ladder.find(id);
        if (rank.isEmpty()) {
            return false;
        }
        ladder.put(rank.get().withPlaytime(minutes));
        return true;
    }

    /**
     * {@return the rank that must be owned before this one, if any}
     *
     * <p>An explicit {@code requires} always wins. Only when a rank does not name
     * one does the file's own order stand in, and only while
     * {@code requireLadderOrder} is on — so an operator can leave the ladder
     * linear by default and still branch it wherever they want to.
     */
    public static Optional<Rank> prerequisite(Rank rank) {
        if (ladder == null) {
            return Optional.empty();
        }
        Optional<String> explicit = rank.prerequisite();
        if (explicit.isPresent()) {
            return ladder.find(explicit.get());
        }
        if (RavenCoinConfig.COMMON.requireLadderOrder.get()) {
            return ladder.below(rank.id());
        }
        return Optional.empty();
    }

    /**
     * Points one rank at another as its prerequisite, or clears it with
     * {@code null}.
     *
     * @return false if either rank is unknown, or the link would close a loop
     */
    public static boolean setPrerequisite(String id, @Nullable String requiredId) {
        if (ladder == null) {
            return false;
        }
        Optional<Rank> rank = ladder.find(id);
        if (rank.isEmpty()) {
            return false;
        }
        if (requiredId != null && (ladder.find(requiredId).isEmpty() || ladder.wouldCycle(id, requiredId))) {
            return false;
        }
        ladder.put(rank.get().withRequires(requiredId));
        return true;
    }

    /** Adds or reprices a rung. {@return false if the group does not exist in LuckPerms} */
    public static boolean define(Rank rank) {
        if (ladder == null) {
            return false;
        }
        if (permissionsAvailable() && !LuckPermsBridge.groupExists(rank.group())) {
            return false;
        }
        // Repricing a rank must not quietly drop the prerequisite or the time
        // requirement someone set up with a separate command.
        Rank existing = ladder.find(rank.id()).orElse(null);
        ladder.put(
                existing == null
                        ? rank
                        : rank.withRequires(existing.requires()).withPlaytime(existing.playtimeMinutes()));
        return true;
    }

    public static boolean undefine(String id) {
        return ladder != null && ladder.remove(id);
    }

    private RankService() {}
}
