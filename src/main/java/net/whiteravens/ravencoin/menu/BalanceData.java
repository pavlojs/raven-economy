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

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.whiteravens.ravencoin.economy.EconomyService;

/**
 * A player's balance, on a screen, kept current.
 *
 * <p><b>Four shorts, not one long.</b> {@link ContainerData} is a row of ints on
 * paper, and vanilla's synchroniser writes each of them with {@code writeShort}
 * — so a balance over 32 767 sent as one value arrives truncated, and this is a
 * mod whose season target is measured in millions. Four 16-bit slices reassemble
 * losslessly, and vanilla's per-tick change detection then keeps the number live
 * even when the money moves for some other reason, such as another player paying
 * you while you stand at the counter.
 *
 * <p>Shared by the bank and the shops, which both have to show what you can
 * afford and neither of which is allowed to guess.
 */
public final class BalanceData {
    private static final int SLICES = 4;

    /** {@return an empty holder for a client-side menu, filled in by the server's sync} */
    public static ContainerData empty() {
        return new SimpleContainerData(SLICES);
    }

    /** {@return a view that reads this player's balance out of the ledger whenever it is asked} */
    public static ContainerData of(Player player) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                long value = EconomyService.balance(player.getServer(), player.getUUID());
                return (int) ((value >>> (16 * (SLICES - 1 - index))) & 0xFFFFL);
            }

            @Override
            public void set(int index, int value) {
                // The ledger is the source of truth; a client cannot write to it.
            }

            @Override
            public int getCount() {
                return SLICES;
            }
        };
    }

    /** {@return the balance, reassembled from the slices} */
    public static long read(ContainerData data) {
        long value = 0;
        for (int slice = 0; slice < SLICES; slice++) {
            value = (value << 16) | (data.get(slice) & 0xFFFFL);
        }
        return value;
    }

    private BalanceData() {}
}
