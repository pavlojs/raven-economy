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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.registry.ModItems;
import org.jetbrains.annotations.Nullable;

/**
 * How much RavenCoin there is, and who is holding it.
 *
 * <p>Counted rather than tracked. Tracking would mean incrementing a number
 * every time a coin was made, and the minting recipe is an ordinary shaped
 * recipe that any autocrafter in the pack can run without a craft event ever
 * firing — so a tracked figure would drift the moment somebody automated the
 * mint, and drift silently. Counting cannot drift, only fall short, and it
 * falls short in a way that can be stated on the same screen as the number.
 *
 * <p><b>What it misses.</b> The ledger is exact and every player's own pockets
 * and ender chest are exact. Coins in a chest, a drawer, an ME network, a
 * shulker box or on the floor are not counted, and cannot be without walking
 * every region file on disk. That matters more than it looks, because the ATM
 * is meant to sit at spawn and nowhere else: a design that makes players travel
 * to bank is a design that leaves more of the money in chests. Read the total
 * as a floor, not as the supply.
 *
 * <p>Offline players are read straight out of their {@code playerdata} file.
 * That is one small decompression per account, on the server thread, so this is
 * an operator's command and not something to put on a tick.
 */
public record MoneyCensus(List<Holding> holdings, int online, int unreadable) {
    /** {@return the holdings of every account on the server, largest first} */
    public static MoneyCensus take(MinecraftServer server) {
        Path playerData = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        List<Holding> holdings = new ArrayList<>();
        int online = 0;
        int unreadable = 0;

        for (Account account : EconomyAccounts.of(server).all()) {
            ServerPlayer player = server.getPlayerList().getPlayer(account.id());
            long carried;
            if (player != null) {
                online++;
                carried = carried(player);
            } else {
                Long stored = readCarried(playerData.resolve(account.id() + ".dat"));
                if (stored == null) {
                    unreadable++;
                    carried = 0;
                } else {
                    carried = stored;
                }
            }
            holdings.add(new Holding(account.id(), account.name(), account.balance(), carried));
        }

        holdings.sort(Comparator.comparingLong(Holding::total)
                .reversed()
                .thenComparing(Holding::name));
        return new MoneyCensus(List.copyOf(holdings), online, unreadable);
    }

    /** {@return what one player is worth, whether or not they are logged in} */
    public static Holding of(MinecraftServer server, UUID id, String name) {
        long banked = EconomyService.balance(server, id);
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) {
            return new Holding(id, name, banked, carried(player));
        }
        Long stored = readCarried(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id + ".dat"));
        return new Holding(id, name, banked, stored == null ? 0 : stored);
    }

    public int accounts() {
        return this.holdings.size();
    }

    public int offline() {
        return this.accounts() - this.online;
    }

    public long banked() {
        return this.holdings.stream().mapToLong(Holding::banked).sum();
    }

    public long carried() {
        return this.holdings.stream().mapToLong(Holding::carried).sum();
    }

    public long total() {
        return this.banked() + this.carried();
    }

    /** {@return the largest holding, or empty when nobody has an account yet} */
    public Optional<Holding> largest() {
        return this.holdings.isEmpty() ? Optional.empty() : Optional.of(this.holdings.get(0));
    }

    /**
     * {@return the largest holding's share of everything counted, as a percentage}
     *
     * <p>The number the season notes say to look at weekly. One player past half
     * is not skill; it is a buy-back entry somebody has automated.
     */
    public int concentration() {
        long total = this.total();
        if (total <= 0) {
            return 0;
        }
        return (int) (this.largest().map(Holding::total).orElse(0L) * 100 / total);
    }

    private static long carried(ServerPlayer player) {
        return PhysicalCoins.carried(player.getInventory())
                + PhysicalCoins.carried(player.getEnderChestInventory());
    }

    /**
     * {@return the coins in a saved player's inventory and ender chest, or null
     * if their file exists but could not be read}
     *
     * <p>A file that is not there at all counts as nothing rather than as a
     * failure: an account is opened on login and the file is only written on
     * save, so the gap between the two is a player who has never had an item.
     */
    @Nullable
    private static Long readCarried(Path file) {
        if (!Files.isRegularFile(file)) {
            return 0L;
        }
        try {
            CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            return value(tag.getList("Inventory", Tag.TAG_COMPOUND))
                    + value(tag.getList("EnderItems", Tag.TAG_COMPOUND));
        } catch (IOException | RuntimeException unreadable) {
            RavenCoin.LOG.warn("Could not count the coins in {}", file.getFileName(), unreadable);
            return null;
        }
    }

    /**
     * {@return the value of the coins in one saved item list}
     *
     * <p>Reads the item format directly rather than round-tripping through
     * {@code ItemStack.CODEC}, which would need a registry lookup per stack and
     * would throw on any item whose mod has since been removed from the pack.
     * Only two ids matter here and both are ours.
     */
    private static long value(ListTag items) {
        String coin = ModItems.COIN.getId().toString();
        String block = ModItems.COIN_BLOCK.getId().toString();
        long total = 0;
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompound(i);
            String id = entry.getString("id");
            // Absent means one: ItemStack's codec leaves the count out of a single.
            long count = entry.contains("count", Tag.TAG_INT) ? entry.getInt("count") : 1;
            if (coin.equals(id)) {
                total += count;
            } else if (block.equals(id)) {
                total += count * PhysicalCoins.BLOCK_VALUE;
            }
        }
        return total;
    }
}
