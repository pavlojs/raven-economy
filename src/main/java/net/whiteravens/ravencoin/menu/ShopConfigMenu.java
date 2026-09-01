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

/**
 * The owner's side of a shop.
 *
 * <p>Being able to open this <em>is</em> the permission: the block decides who
 * gets this screen instead of the buying one, and the packet handler then trusts
 * nothing except the fact that this menu is the one the player has open.
 */
public class ShopConfigMenu extends ShopMenuBase {
    /** Taller than the buying screen — the settings need the room above the inventory. */
    private static final int INVENTORY_TOP = 154;

    public ShopConfigMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, data.readBlockPos());
    }

    public ShopConfigMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.SHOP_CONFIG.get(), containerId, inventory, pos, INVENTORY_TOP);
    }
}
