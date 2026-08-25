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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.whiteravens.ravencoin.RavenCoin;

/** One tab for everything this mod adds, so an admin can find it all at once. */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RavenCoin.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ravencoin.main"))
                    .icon(() -> new ItemStack(ModItems.COIN.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.COIN.get());
                        output.accept(ModItems.COIN_BLOCK.get());
                        output.accept(ModItems.ATM.get());
                        output.accept(ModItems.SHOP.get());
                        output.accept(ModItems.SERVER_SHOP.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
