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

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.block.entity.ShopBlockEntity;

/** The state this mod's blocks keep. Only the shops have any. */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RavenCoin.MOD_ID);

    /**
     * Bound to both shop blocks.
     *
     * <p>One type for two blocks because it is one machine: the operator's shop
     * differs in where its stock comes from and who may edit it, not in what it
     * stores. Splitting them would have meant maintaining the trade logic twice.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShopBlockEntity>> SHOP =
            BLOCK_ENTITIES.register(
                    "shop",
                    () -> BlockEntityType.Builder.of(
                                    ShopBlockEntity::new, ModBlocks.SHOP.get(), ModBlocks.SERVER_SHOP.get())
                            .build(null));

    private ModBlockEntities() {}
}
