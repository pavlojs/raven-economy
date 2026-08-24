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
package net.whiteravens.ravencoin.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.whiteravens.ravencoin.RavenCoin;

/**
 * The currency itself.
 *
 * <p>RavenCoin is an ordinary item, deliberately. Making it a physical thing you
 * carry is what lets a shop price goods in it, a thief take it, and a player
 * hand one to another across a chest without any of it going through a command.
 * The account exists alongside it — see the ATM — not instead of it.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RavenCoin.MOD_ID);

    /** One RavenCoin. The unit every price in the economy is quoted in. */
    public static final DeferredItem<Item> COIN = ITEMS.registerSimpleItem("coin");

    /**
     * The item form of the storage block. Registered here rather than beside the
     * block because NeoForge keeps blocks and items in separate registries, and
     * a block with no item cannot be picked up, traded or priced.
     */
    public static final DeferredItem<BlockItem> COIN_BLOCK =
            ITEMS.registerSimpleBlockItem("coin_block", ModBlocks.COIN_BLOCK);

    private ModItems() {}
}
