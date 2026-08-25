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

/**
 * Storage form of the currency: nine coins in, nine coins back out.
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

    private ModBlocks() {}
}
