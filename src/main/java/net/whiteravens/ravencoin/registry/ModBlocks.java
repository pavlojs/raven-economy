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

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.block.AtmBlock;
import net.whiteravens.ravencoin.block.ServerShopBlock;
import net.whiteravens.ravencoin.block.ShopBlock;

/**
 * The blocks this mod adds: the currency in storage form, and the three
 * machines that move it.
 *
 * <p>The same bargain vanilla strikes with ingots and blocks, and it is here for
 * the same reason — a player banking a serious sum should not be carrying
 * fifty-four stacks of coins to do it. Nothing is lost or gained in the
 * conversion; this is compression, not a denomination with its own value.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RavenCoin.MOD_ID);

    /** Nine RavenCoins, stacked. */
    public static final DeferredBlock<Block> COIN_BLOCK = BLOCKS.registerSimpleBlock(
            "coin_block",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    /** The teller machine. Immovable so a piston cannot walk the bank away from its owner. */
    public static final DeferredBlock<Block> ATM = BLOCKS.register(
            "atm",
            () -> new AtmBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.5F, 8.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .pushReaction(PushReaction.BLOCK)));

    /** A player's shop counter. Timber and iron, and it keeps nothing itself. */
    public static final DeferredBlock<Block> SHOP = BLOCKS.register(
            "shop",
            () -> new ShopBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F, 3.0F)
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.BLOCK)));

    /**
     * The operator's shop.
     *
     * <p>Unbreakable in survival, the way bedrock is. A server shop stands where
     * players can reach it, usually in a spawn town, and one that could be mined
     * is one that will be — along with every price an operator set on it.
     */
    public static final DeferredBlock<Block> SERVER_SHOP = BLOCKS.register(
            "server_shop",
            () -> new ServerShopBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.DEEPSLATE)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)));

    private ModBlocks() {}
}
