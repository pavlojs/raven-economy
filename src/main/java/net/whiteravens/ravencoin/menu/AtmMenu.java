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
package net.whiteravens.ravencoin.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.whiteravens.ravencoin.economy.EconomyService;
import net.whiteravens.ravencoin.economy.PhysicalCoins;
import net.whiteravens.ravencoin.economy.TransactionResult;
import net.whiteravens.ravencoin.registry.ModBlocks;
import net.whiteravens.ravencoin.registry.ModMenus;

/**
 * The ATM's server-side half.
 *
 * <p>Holds no inventory of its own — the only slots are the player's, so they
 * can see the coins they are about to bank. Deposits and withdrawals are methods
 * here, called by the payload handler, and they go through
 * {@link EconomyService} exactly like the commands do. Nothing on this screen
 * ever composes a command string.
 *
 * <p><b>The balance is synced as four shorts, not one long.</b> {@link ContainerData}
 * looks like it carries ints, but {@code ClientboundContainerSetDataPacket}
 * writes each value with {@code writeShort} — so a balance over 32767 would
 * arrive mangled, and the whole point of this server is a race to a billion.
 * Four 16-bit slices reassemble losslessly, and vanilla's per-tick change
 * detection then keeps the screen live even when the money moves for some other
 * reason, such as another player paying you while you stand at the machine.
 */
public class AtmMenu extends AbstractContainerMenu {
    /** Number of {@link ContainerData} slots used to carry a 64-bit balance. */
    private static final int BALANCE_SLICES = 4;

    private final ContainerLevelAccess access;
    private final ContainerData balance;
    private final Player player;

    /** Client-side constructor: no world access, and a balance that only ever arrives from the server. */
    public AtmMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL, new SimpleContainerData(BALANCE_SLICES));
    }

    /** Server-side constructor: reads the live balance out of the ledger every tick. */
    public AtmMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        this(containerId, inventory, access, liveBalance(inventory.player));
    }

    private AtmMenu(int containerId, Inventory inventory, ContainerLevelAccess access, ContainerData balance) {
        super(ModMenus.ATM.get(), containerId);
        this.access = access;
        this.balance = balance;
        this.player = inventory.player;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 104 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, 8 + column * 18, 162));
        }

        this.addDataSlots(balance);
    }

    /** {@return the account balance, reassembled from the four synced slices} */
    public long balance() {
        long value = 0;
        for (int slice = 0; slice < BALANCE_SLICES; slice++) {
            value = (value << 16) | (this.balance.get(slice) & 0xFFFFL);
        }
        return value;
    }

    /** {@return the value of the coins and coin blocks the player is carrying} */
    public long carried() {
        return PhysicalCoins.carried(this.player.getInventory());
    }

    /**
     * Banks physical coins.
     *
     * <p>Takes the metal first and credits exactly what was taken, so a full
     * inventory or a broken block can never mint value that was not there.
     */
    public Outcome deposit(long amount) {
        if (amount <= 0) {
            return new Outcome(TransactionResult.INVALID_AMOUNT, 0);
        }
        long taken = PhysicalCoins.take(this.player, amount);
        if (taken <= 0) {
            return new Outcome(TransactionResult.INSUFFICIENT_FUNDS, 0);
        }
        TransactionResult result = EconomyService.deposit(
                this.player.getServer(), this.player.getUUID(), this.player.getGameProfile().getName(), taken);
        if (!result.ok()) {
            // The ledger refused it, so hand the metal straight back rather than eat it.
            PhysicalCoins.give(this.player, taken);
            return new Outcome(result, 0);
        }
        return new Outcome(result, taken);
    }

    /**
     * Draws physical coins out.
     *
     * <p>Hands over the metal first and debits only what actually fit, which is
     * what stops a withdrawal from vanishing into a full inventory.
     */
    public Outcome withdraw(long amount) {
        if (amount <= 0) {
            return new Outcome(TransactionResult.INVALID_AMOUNT, 0);
        }
        if (this.rawBalance() < amount) {
            return new Outcome(TransactionResult.INSUFFICIENT_FUNDS, 0);
        }
        long given = PhysicalCoins.give(this.player, amount);
        if (given <= 0) {
            return new Outcome(TransactionResult.NO_ROOM, 0);
        }
        TransactionResult result = EconomyService.withdraw(
                this.player.getServer(), this.player.getUUID(), this.player.getGameProfile().getName(), given);
        return new Outcome(result, result.ok() ? given : 0);
    }

    /**
     * What one press of a button actually did.
     *
     * <p>The amount is what moved, not what was asked for: a deposit stops at
     * the coins the player really had, and a withdrawal stops at the space in
     * their inventory. Reporting the request instead of the result is how a
     * screen ends up lying about a half-finished transaction.
     */
    public record Outcome(TransactionResult result, long amount) {}

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        // Only the player's own inventory is on this screen, so a shift-click
        // can do nothing more interesting than swap between pack and hotbar.
        boolean fromMainInventory = index < 27;
        if (fromMainInventory) {
            if (!this.moveItemStackTo(stack, 27, 36, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, 27, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.ATM.get());
    }

    private long rawBalance() {
        return EconomyService.balance(this.player.getServer(), this.player.getUUID());
    }

    private static ContainerData liveBalance(Player player) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                long value = EconomyService.balance(player.getServer(), player.getUUID());
                return (int) ((value >>> (16 * (BALANCE_SLICES - 1 - index))) & 0xFFFF);
            }

            @Override
            public void set(int index, int value) {
                // The ledger is the source of truth; a client cannot write to it.
            }

            @Override
            public int getCount() {
                return BALANCE_SLICES;
            }
        };
    }
}
