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
package net.whiteravens.ravencoin.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.registry.ModBlockEntities;
import net.whiteravens.ravencoin.registry.ModMenus;

/** Client-only wiring. Nothing here decides anything about money. */
@EventBusSubscriber(modid = RavenCoin.MOD_ID, value = Dist.CLIENT)
public final class RavenCoinClient {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ATM.get(), AtmScreen::new);
        event.register(ModMenus.SHOP.get(), ShopScreen::new);
        event.register(ModMenus.SHOP_CONFIG.get(), ShopConfigScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SHOP.get(), ShopLabelRenderer::new);
    }

    private RavenCoinClient() {}
}
