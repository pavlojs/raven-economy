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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.whiteravens.ravencoin.registry.ModMenus;

/** The customer's side of a shop. Shows the offer, and has one button. */
public class ShopMenu extends ShopMenuBase {
    /** Where the player's own slots start on the buying screen. */
    private static final int INVENTORY_TOP = 114;

    /** Client-side: the shop's position is all the extra data this screen needs. */
    public ShopMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, data.readBlockPos());
    }

    public ShopMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.SHOP.get(), containerId, inventory, pos, INVENTORY_TOP);
    }
}
