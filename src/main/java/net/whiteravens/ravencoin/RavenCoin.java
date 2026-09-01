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
package net.whiteravens.ravencoin;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.whiteravens.ravencoin.config.RavenCoinConfig;
import net.whiteravens.ravencoin.registry.ModBlockEntities;
import net.whiteravens.ravencoin.registry.ModBlocks;
import net.whiteravens.ravencoin.registry.ModConditions;
import net.whiteravens.ravencoin.registry.ModCreativeTabs;
import net.whiteravens.ravencoin.registry.ModItems;
import net.whiteravens.ravencoin.registry.ModMenus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The White Ravens Forge economy.
 *
 * <p>Everything here is server-authoritative: balances live in the server's own
 * saved data and a client is never trusted with a number it could edit. The
 * client half exists only to draw the ATM screen and the shop screen.
 *
 * <p>Scope for the first version is set out in the README. The short form: one
 * currency that is also a physical item, an ATM that moves coins between hand
 * and account, player and server shops, ranks bought through the LuckPerms API,
 * and a leaderboard by balance. Every feature is switchable off, because the
 * first live season is what will tell us which of them were a good idea.
 */
@Mod(RavenCoin.MOD_ID)
public class RavenCoin {
    public static final String MOD_ID = "ravencoin";
    public static final Logger LOG = LoggerFactory.getLogger("RavenCoin");

    public RavenCoin(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModConditions.CONDITIONS.register(modBus);
        ModMenus.MENUS.register(modBus);

        container.registerConfig(ModConfig.Type.COMMON, RavenCoinConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, RavenCoinConfig.CLIENT_SPEC);
    }
}
