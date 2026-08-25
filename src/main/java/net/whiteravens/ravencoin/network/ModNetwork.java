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
package net.whiteravens.ravencoin.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.economy.Amounts;
import net.whiteravens.ravencoin.economy.TransactionResult;
import net.whiteravens.ravencoin.block.entity.ShopBlockEntity;
import net.whiteravens.ravencoin.menu.AtmMenu;
import net.whiteravens.ravencoin.menu.ShopConfigMenu;
import net.whiteravens.ravencoin.menu.ShopMenu;
import net.whiteravens.ravencoin.shop.ShopText;
import org.jetbrains.annotations.Nullable;

/**
 * Registers this mod's packets and handles them.
 *
 * <p>Every handler here starts the same way, and it is the important line in the
 * file: <b>the open menu is the authorisation</b>. A packet is only ever worth
 * what the player could have done by standing at the block and clicking, so
 * anyone who is not actually looking at the right screen is ignored. That check,
 * not the screen, is what makes the buttons safe — a screen is drawn on a client
 * and a client can be made to send anything.
 */
@EventBusSubscriber(modid = RavenCoin.MOD_ID)
public final class ModNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(AtmActionPayload.TYPE, AtmActionPayload.STREAM_CODEC, ModNetwork::onAtmAction);
        registrar.playToServer(ShopBuyPayload.TYPE, ShopBuyPayload.STREAM_CODEC, ModNetwork::onShopBuy);
        registrar.playToServer(ShopPickPayload.TYPE, ShopPickPayload.STREAM_CODEC, ModNetwork::onShopPick);
        registrar.playToServer(
                ShopSettingsPayload.TYPE, ShopSettingsPayload.STREAM_CODEC, ModNetwork::onShopSettings);
    }

    /** Runs one purchase for the player whose shop screen is open. */
    private static void onShopBuy(ShopBuyPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.containerMenu instanceof ShopMenu menu) || !menu.stillValid(player)) {
            return;
        }
        ShopBlockEntity shop = menu.shop();
        if (shop == null) {
            return;
        }
        player.sendSystemMessage(ShopText.outcome(shop, shop.buy(player, payload.lots())));
    }

    /**
     * Copies the sender's cursor into one of the settings slots.
     *
     * <p>Having the settings menu open is the permission: the block only hands
     * that menu to the owner or an operator, so there is nothing further to
     * check here.
     */
    private static void onShopPick(ShopPickPayload payload, IPayloadContext context) {
        ShopBlockEntity shop = editedShop(context);
        if (shop != null) {
            shop.pick(context.player(), payload.forPrice());
        }
    }

    private static void onShopSettings(ShopSettingsPayload payload, IPayloadContext context) {
        ShopBlockEntity shop = editedShop(context);
        if (shop != null) {
            shop.configure(payload.productUnits(), payload.priceUnits(), payload.rank(), payload.showLabel());
        }
    }

    /** {@return the shop whose settings screen the sender has open, or null} */
    @Nullable
    private static ShopBlockEntity editedShop(IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return null;
        }
        if (!(player.containerMenu instanceof ShopConfigMenu menu) || !menu.stillValid(player)) {
            return null;
        }
        return menu.shop();
    }

    /**
     * Runs one ATM action for the player who asked for it.
     *
     * <p>Handlers registered this way run on the server thread, so the ledger is
     * touched from the one place that is allowed to touch it.
     */
    private static void onAtmAction(AtmActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.containerMenu instanceof AtmMenu menu) || !menu.stillValid(player)) {
            return;
        }

        AtmMenu.Outcome outcome = switch (payload.action()) {
            case DEPOSIT -> menu.deposit(payload.amount());
            case WITHDRAW -> menu.withdraw(payload.amount());
        };

        player.sendSystemMessage(message(payload.action(), outcome));
    }

    private static Component message(AtmActionPayload.Action action, AtmMenu.Outcome outcome) {
        if (!outcome.result().ok()) {
            return Component.translatable(errorKey(outcome.result()));
        }
        String key = action == AtmActionPayload.Action.DEPOSIT
                ? "screen.ravencoin.atm.deposited"
                : "screen.ravencoin.atm.withdrew";
        return Component.translatable(key, Amounts.format(outcome.amount()));
    }

    private static String errorKey(TransactionResult result) {
        return switch (result) {
            case INVALID_AMOUNT -> "commands.ravencoin.error.invalid_amount";
            case INSUFFICIENT_FUNDS -> "commands.ravencoin.error.insufficient_funds";
            case TOO_LARGE -> "commands.ravencoin.error.too_large";
            case DISABLED -> "commands.ravencoin.error.disabled";
            case SAME_ACCOUNT -> "commands.ravencoin.error.same_account";
            case NO_ROOM -> "commands.ravencoin.error.no_room";
            case OK -> throw new IllegalArgumentException("OK is not an error");
        };
    }

    private ModNetwork() {}
}
