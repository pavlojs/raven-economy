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
package net.whiteravens.ravencoin.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import net.whiteravens.ravencoin.economy.PhysicalCoins;
import net.whiteravens.ravencoin.registry.ModItems;

/**
 * Counting, taking and handing over the goods a shop trades in.
 *
 * <p>Everything here speaks {@link IItemHandler}, so one code path serves both
 * sides of a trade: the shop's chest and the buyer's pockets are the same shape
 * of thing, and a bug in one would have been a bug in the other anyway.
 *
 * <p>The one special case is RavenCoin. A price quoted in coins is also payable
 * in coin blocks and is stored back as blocks plus change, because nine coins
 * and one block are the same money — see {@link PhysicalCoins}. Without that a
 * player carrying their savings compressed could not buy anything, and a busy
 * shop would fill a double chest with loose coins in an afternoon.
 */
public final class ShopStock {
    /** The buyer's own 36 slots. Armour and the offhand are not somewhere to shop from. */
    public static IItemHandler pockets(Player player) {
        return new PlayerMainInvWrapper(player.getInventory());
    }

    /** {@return how many units of this good the handler holds} */
    public static long count(IItemHandler handler, ItemStack good) {
        if (isCoin(good)) {
            return coinCount(handler);
        }
        long total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack held = handler.getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(held, good)) {
                total += held.getCount();
            }
        }
        return total;
    }

    /**
     * {@return how many more units this handler could accept}
     *
     * <p>Walked slot by slot rather than asked for by simulating an insert:
     * a simulated insert answers for one stack at a time and cannot be
     * accumulated, and the whole point of this call is to decide a whole trade
     * before any of it happens.
     */
    public static long room(IItemHandler handler, ItemStack good) {
        if (isCoin(good)) {
            return coinRoom(handler);
        }
        long room = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack held = handler.getStackInSlot(slot);
            int limit = Math.min(handler.getSlotLimit(slot), good.getMaxStackSize());
            if (held.isEmpty()) {
                if (handler.isItemValid(slot, good)) {
                    room += limit;
                }
            } else if (ItemStack.isSameItemSameComponents(held, good)) {
                room += Math.max(0, limit - held.getCount());
            }
        }
        return room;
    }

    /** Removes units. {@return how many were actually removed} */
    public static long take(IItemHandler handler, ItemStack good, long units) {
        return isCoin(good) ? coinTake(handler, units) : plainTake(handler, good, units);
    }

    /** Puts units in. {@return how many would not fit} */
    public static long put(IItemHandler handler, ItemStack good, long units) {
        return isCoin(good) ? coinPut(handler, units) : plainPut(handler, good, units);
    }

    /** Throws units on the floor, in stacks. The last resort, so that value is never simply deleted. */
    public static void spill(Level level, BlockPos pos, ItemStack good, long units) {
        while (units > 0) {
            int batch = (int) Math.min(units, good.getMaxStackSize());
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, good.copyWithCount(batch));
            units -= batch;
        }
    }

    private static boolean isCoin(ItemStack good) {
        return good.is(ModItems.COIN.get());
    }

    private static long coinCount(IItemHandler handler) {
        long total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack held = handler.getStackInSlot(slot);
            if (held.is(ModItems.COIN.get())) {
                total += held.getCount();
            } else if (held.is(ModItems.COIN_BLOCK.get())) {
                total += (long) held.getCount() * PhysicalCoins.BLOCK_VALUE;
            }
        }
        return total;
    }

    /**
     * {@return the coin value this handler could still take}
     *
     * <p>An empty slot is measured as a stack of blocks, because a stack of
     * blocks is what would go in it. That makes this an upper bound rather than
     * an exact figure, which is why every caller still checks what the insert
     * actually returned.
     */
    private static long coinRoom(IItemHandler handler) {
        ItemStack block = new ItemStack(ModItems.COIN_BLOCK.get());
        long room = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack held = handler.getStackInSlot(slot);
            int limit = Math.min(handler.getSlotLimit(slot), 64);
            if (held.isEmpty()) {
                if (handler.isItemValid(slot, block)) {
                    room += (long) limit * PhysicalCoins.BLOCK_VALUE;
                }
            } else if (held.is(ModItems.COIN_BLOCK.get())) {
                room += (long) Math.max(0, limit - held.getCount()) * PhysicalCoins.BLOCK_VALUE;
            } else if (held.is(ModItems.COIN.get())) {
                room += Math.max(0, limit - held.getCount());
            }
        }
        return room;
    }

    /**
     * Takes coin value, spending loose coins before breaking a block.
     *
     * <p>A block is only broken once there is somewhere for the change to go.
     * Nine coins go in and eight can come back out, and change that cannot be
     * returned is money destroyed — so a handler with no room for it is left
     * alone and this simply reports taking less. The caller decides what a short
     * take means; nothing here quietly makes up the difference.
     */
    private static long coinTake(IItemHandler handler, long units) {
        long remaining = units;

        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            if (!handler.getStackInSlot(slot).is(ModItems.COIN.get())) {
                continue;
            }
            int want = (int) Math.min(handler.getStackInSlot(slot).getCount(), remaining);
            remaining -= handler.extractItem(slot, want, false).getCount();
        }

        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack held = handler.getStackInSlot(slot);
            if (!held.is(ModItems.COIN_BLOCK.get())) {
                continue;
            }
            long needed = (remaining + PhysicalCoins.BLOCK_VALUE - 1) / PhysicalCoins.BLOCK_VALUE;
            int want = (int) Math.min(held.getCount(), needed);
            long unpacked = (long) want * PhysicalCoins.BLOCK_VALUE;
            long used = Math.min(unpacked, remaining);
            int change = (int) (unpacked - used);
            if (change > 0 && !changeFits(handler, change, want == held.getCount())) {
                continue;
            }

            handler.extractItem(slot, want, false);
            remaining -= used;
            if (change > 0) {
                ItemHandlerHelper.insertItemStacked(handler, new ItemStack(ModItems.COIN.get(), change), false);
            }
        }
        return units - remaining;
    }

    /**
     * {@return whether change from a broken block could be put back}
     *
     * <p>Taking the whole stack answers this by itself: the slot it came out of
     * is about to be free, and a slot holds far more than eight coins. Otherwise
     * the insert is simulated, because the simulation runs before the extraction
     * and would not know about a slot that does not exist yet.
     */
    private static boolean changeFits(IItemHandler handler, int change, boolean emptiesSlot) {
        return emptiesSlot
                || ItemHandlerHelper.insertItemStacked(handler, new ItemStack(ModItems.COIN.get(), change), true)
                        .isEmpty();
    }

    /** Stores coin value as blocks plus loose change. {@return what would not fit} */
    private static long coinPut(IItemHandler handler, long units) {
        long blocks = units / PhysicalCoins.BLOCK_VALUE;
        int loose = (int) (units % PhysicalCoins.BLOCK_VALUE);
        long leftover = 0;

        while (blocks > 0) {
            int batch = (int) Math.min(blocks, 64);
            int rejected = ItemHandlerHelper.insertItemStacked(
                            handler, new ItemStack(ModItems.COIN_BLOCK.get(), batch), false)
                    .getCount();
            blocks -= batch;
            leftover += (long) rejected * PhysicalCoins.BLOCK_VALUE;
            if (rejected == batch) {
                // Nothing at all fit, so no later batch will either.
                leftover += blocks * PhysicalCoins.BLOCK_VALUE;
                blocks = 0;
            }
        }

        if (loose > 0) {
            leftover += ItemHandlerHelper.insertItemStacked(handler, new ItemStack(ModItems.COIN.get(), loose), false)
                    .getCount();
        }
        return leftover;
    }

    private static long plainTake(IItemHandler handler, ItemStack good, long units) {
        long remaining = units;
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            if (!ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), good)) {
                continue;
            }
            int want = (int) Math.min(handler.getStackInSlot(slot).getCount(), remaining);
            remaining -= handler.extractItem(slot, want, false).getCount();
        }
        return units - remaining;
    }

    private static long plainPut(IItemHandler handler, ItemStack good, long units) {
        long remaining = units;
        long leftover = 0;
        while (remaining > 0) {
            int batch = (int) Math.min(remaining, good.getMaxStackSize());
            int rejected = ItemHandlerHelper.insertItemStacked(handler, good.copyWithCount(batch), false)
                    .getCount();
            remaining -= batch;
            leftover += rejected;
            if (rejected == batch) {
                leftover += remaining;
                remaining = 0;
            }
        }
        return leftover;
    }

    private ShopStock() {}
}
