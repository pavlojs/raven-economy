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
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.whiteravens.ravencoin.RavenCoin;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.whiteravens.ravencoin.menu.AtmMenu;
import net.whiteravens.ravencoin.menu.ShopConfigMenu;
import net.whiteravens.ravencoin.menu.ShopMenu;

/** Screens this mod can open. */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, RavenCoin.MOD_ID);

    /**
     * The ATM screen.
     *
     * <p>Registered with the plain two-argument factory because the client needs
     * nothing at open time beyond its own inventory — the balance arrives through
     * the menu's data slots and keeps arriving as it changes.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<AtmMenu>> ATM =
            MENUS.register("atm", () -> new MenuType<>(AtmMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /**
     * The buying screen.
     *
     * <p>Built with the extra-data factory because a client opening this has to
     * be told <em>which</em> shop — everything else about the offer it already
     * has, from the block entity it is standing in front of.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<ShopMenu>> SHOP =
            MENUS.register("shop", () -> IMenuTypeExtension.create(ShopMenu::new));

    /** The owner's settings screen. Same extra data, different permission to have got here. */
    public static final DeferredHolder<MenuType<?>, MenuType<ShopConfigMenu>> SHOP_CONFIG =
            MENUS.register("shop_config", () -> IMenuTypeExtension.create(ShopConfigMenu::new));

    private ModMenus() {}
}
