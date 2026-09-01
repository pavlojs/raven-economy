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

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.whiteravens.ravencoin.block.entity.ShopBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * What the two shop screens have in common: the player's own inventory, and a
 * shop somewhere in the world that both sides can read.
 *
 * <p>Neither screen carries the shop's numbers in {@link net.minecraft.world.inventory.ContainerData}.
 * The block entity already syncs itself to everyone who can see the block —
 * that is how the floating label is drawn — so the screen reads the same copy
 * the label does and gets live updates for free.
 */
public abstract class ShopMenuBase extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Player player;
    private final ContainerData balance;

    protected ShopMenuBase(MenuType<?> type, int containerId, Inventory inventory, BlockPos pos, int inventoryTop) {
        super(type, containerId);
        this.pos = pos;
        this.player = inventory.player;
        // A shop can be paid out of the account as well as out of the pockets, so
        // this screen has to be able to show the same number the bank does.
        this.balance = inventory.player.level().isClientSide
                ? BalanceData.empty()
                : BalanceData.of(inventory.player);
        this.addDataSlots(this.balance);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, inventoryTop + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, 8 + column * 18, inventoryTop + 58));
        }
    }

    /** {@return the shopper's account balance, as the server last sent it} */
    public long balance() {
        return BalanceData.read(this.balance);
    }

    public BlockPos pos() {
        return this.pos;
    }

    /** {@return the shop this screen is looking at, or null if it has been broken} */
    @Nullable
    public ShopBlockEntity shop() {
        return this.player.level().getBlockEntity(this.pos) instanceof ShopBlockEntity shop ? shop : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        // Only the player's own inventory is on these screens, so a shift-click
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

    /**
     * Checked against the block entity rather than the block.
     *
     * <p>Two different blocks share this screen, and what actually has to still
     * be there is the shop — the thing holding the trade the player is about to
     * agree to.
     */
    @Override
    public boolean stillValid(Player player) {
        return this.shop() != null && player.canInteractWithBlock(this.pos, 4.0);
    }
}
