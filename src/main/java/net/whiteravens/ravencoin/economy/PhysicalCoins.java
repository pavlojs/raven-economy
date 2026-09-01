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

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.whiteravens.ravencoin.registry.ModItems;

/**
 * Moves value between an account's number and the coins in someone's pockets.
 *
 * <p>Kept apart from {@link EconomyService} because the two answer different
 * questions: that class owns the ledger, this one owns the pile of metal. The
 * ATM needs both, and so will the shops.
 */
public final class PhysicalCoins {
    /** A block of RavenCoin is worth this many coins. Nine, as with every vanilla ingot. */
    public static final int BLOCK_VALUE = 9;

    /**
     * {@return the value of every coin and coin block in this container}
     *
     * <p>Takes a {@link Container} rather than an {@link Inventory} so the
     * same weighing works on an ender chest, which is where a player who has
     * to walk to the bank keeps their money.
     */
    public static long carried(Container container) {
        long total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(ModItems.COIN.get())) {
                total += stack.getCount();
            } else if (stack.is(ModItems.COIN_BLOCK.get())) {
                total += (long) stack.getCount() * BLOCK_VALUE;
            }
        }
        return total;
    }

    /**
     * Removes up to {@code amount} of value from the player's inventory.
     *
     * <p>Loose coins go first, so a block is only ever broken when there is no
     * other way to make the sum. When one has to be broken the change comes back
     * as coins, because a player who asked to bank five should not find nine
     * gone — and if their inventory is full at that exact moment the change is
     * dropped at their feet rather than deleted.
     *
     * @return the value actually taken, which is never more than {@code amount}
     */
    public static long take(Player player, long amount) {
        if (amount <= 0) {
            return 0;
        }
        Inventory inventory = player.getInventory();
        long remaining = amount;

        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(ModItems.COIN.get())) {
                continue;
            }
            int taken = (int) Math.min(stack.getCount(), remaining);
            stack.shrink(taken);
            remaining -= taken;
        }

        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(ModItems.COIN_BLOCK.get())) {
                continue;
            }
            long blocksNeeded = (remaining + BLOCK_VALUE - 1) / BLOCK_VALUE;
            int blocksTaken = (int) Math.min(stack.getCount(), blocksNeeded);
            stack.shrink(blocksTaken);

            long unpacked = (long) blocksTaken * BLOCK_VALUE;
            long used = Math.min(unpacked, remaining);
            remaining -= used;

            long change = unpacked - used;
            if (change > 0) {
                giveCoins(player, (int) change);
            }
        }

        inventory.setChanged();
        return amount - remaining;
    }

    /**
     * Puts up to {@code amount} of value into the player's inventory, as blocks
     * plus loose change.
     *
     * <p>Stops at the first thing that will not fit rather than deleting it, so
     * the caller can debit exactly what left the account and no more.
     *
     * @return the value actually handed over
     */
    public static long give(Player player, long amount) {
        if (amount <= 0) {
            return 0;
        }
        long blocks = amount / BLOCK_VALUE;
        int coins = (int) (amount % BLOCK_VALUE);
        long delivered = 0;

        while (blocks > 0) {
            int batch = (int) Math.min(blocks, 64);
            long placed = insert(player, new ItemStack(ModItems.COIN_BLOCK.get(), batch));
            delivered += placed;
            if (placed < (long) batch * BLOCK_VALUE) {
                player.getInventory().setChanged();
                return delivered;
            }
            blocks -= batch;
        }

        if (coins > 0) {
            delivered += insert(player, new ItemStack(ModItems.COIN.get(), coins));
        }

        player.getInventory().setChanged();
        return delivered;
    }

    /**
     * Puts one stack in and reports what that was actually worth, by weighing the
     * inventory before and after.
     *
     * <p>Deliberately does not read the leftover count out of the stack, which is
     * the obvious way to do this and is wrong. When the inventory is full and the
     * player has infinite materials — creative mode — {@link Inventory#add} sets
     * the stack's count to zero and returns {@code true}, so a caller reading the
     * leftover concludes the whole stack landed. It did not, and the account gets
     * debited for coins that never existed. Weighing cannot be lied to.
     */
    private static long insert(Player player, ItemStack stack) {
        Inventory inventory = player.getInventory();
        long before = carried(inventory);
        inventory.add(stack);
        return carried(inventory) - before;
    }

    private static void giveCoins(Player player, int count) {
        long placed = insert(player, new ItemStack(ModItems.COIN.get(), count));
        long leftover = count - placed;
        if (leftover > 0) {
            player.drop(new ItemStack(ModItems.COIN.get(), (int) leftover), false);
        }
    }

    private PhysicalCoins() {}
}
