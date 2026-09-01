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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.whiteravens.ravencoin.client.ClientPayloads;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.economy.Amounts;
import net.whiteravens.ravencoin.economy.EconomyService;
import net.whiteravens.ravencoin.economy.LedgerEntry;
import net.whiteravens.ravencoin.economy.TransactionResult;
import net.whiteravens.ravencoin.block.entity.ShopBlockEntity;
import net.whiteravens.ravencoin.command.RankCommands;
import net.whiteravens.ravencoin.menu.AtmMenu;
import net.whiteravens.ravencoin.menu.ShopConfigMenu;
import net.whiteravens.ravencoin.menu.ShopMenu;
import net.whiteravens.ravencoin.menu.ShopMenuBase;
import net.whiteravens.ravencoin.rank.RankPurchase;
import net.whiteravens.ravencoin.rank.RankService;
import net.whiteravens.ravencoin.shop.ShopResult;
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
        registrar.playToServer(
                AtmTransferPayload.TYPE, AtmTransferPayload.STREAM_CODEC, ModNetwork::onAtmTransfer);
        registrar.playToServer(
                AtmRequestPayload.TYPE, AtmRequestPayload.STREAM_CODEC, ModNetwork::onAtmRequest);
        registrar.playToServer(
                AtmRankBuyPayload.TYPE, AtmRankBuyPayload.STREAM_CODEC, ModNetwork::onAtmRankBuy);
        registrar.playToServer(ShopStallPayload.TYPE, ShopStallPayload.STREAM_CODEC, ModNetwork::onShopStall);
        // Both sides register every channel, including the two that only ever
        // travel one way. A channel the client knows and the server does not is
        // not a channel the server simply never uses — it is a handshake the
        // client refuses, and the server could not have sent on it anyway.
        registrar.playToClient(AtmListPayload.TYPE, AtmListPayload.STREAM_CODEC, ClientPayloads::list);
        registrar.playToClient(AtmNoticePayload.TYPE, AtmNoticePayload.STREAM_CODEC, ClientPayloads::notice);
    }

    /**
     * Rents a stall, restocks one, or puts one on the market.
     *
     * <p>Three different permissions on one packet, checked here rather than
     * trusted from the screen: renting is open to anyone standing at the
     * counter, restocking is the renter's alone — it opens a container they are
     * deliberately unable to open themselves — and putting a shop on the market
     * is the operator's.
     */
    private static void onShopStall(ShopStallPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ShopBlockEntity shop = openShop(player);
        if (shop == null) {
            return;
        }
        switch (payload.action()) {
            case RENT -> {
                ShopResult result = shop.rent(player);
                player.sendSystemMessage(result == ShopResult.OK
                        ? Component.translatable("screen.ravencoin.shop.rent.taken")
                        : Component.translatable(ShopText.errorKey(result)).withStyle(ChatFormatting.RED));
                if (result == ShopResult.OK) {
                    // Straight into the settings, because a stall that sells
                    // nothing is what they have just paid for.
                    shop.openSettings(player);
                }
            }
            case RESTOCK -> {
                if (shop.mayConfigure(player)) {
                    ShopResult opened = shop.openStock(player);
                    if (opened != ShopResult.OK) {
                        player.sendSystemMessage(Component.translatable(ShopText.errorKey(opened))
                                .withStyle(ChatFormatting.RED));
                    }
                }
            }
            case TO_LET -> {
                if (player.hasPermissions(2) && shop.admin()) {
                    shop.setRentable(!shop.rentable());
                }
            }
        }
    }

    /** {@return the shop whose buying or settings screen the sender has open, or null} */
    @Nullable
    private static ShopBlockEntity openShop(ServerPlayer player) {
        if (player.containerMenu instanceof ShopMenuBase menu && menu.stillValid(player)) {
            return menu.shop();
        }
        return null;
    }

    /** Pays another player from the ATM's transfer page. */
    private static void onAtmTransfer(AtmTransferPayload payload, IPayloadContext context) {
        AtmMenu menu = atm(context);
        if (menu == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        AtmMenu.Outcome outcome = menu.transfer(payload.player(), payload.amount());
        if (!outcome.result().ok()) {
            // Chiefly UNKNOWN_PLAYER: the name is the one field on this screen
            // that can be wrong in a way the client cannot see, and the answer
            // belongs under it rather than in chat behind the screen.
            refuse(player, Component.translatable(errorKey(outcome.result())));
            return;
        }

        String payee = menu.resolve(payload.player());
        EconomyService.note(
                player.server, player.getUUID(), LedgerEntry.Kind.PAY_OUT, outcome.amount(), payee);
        EconomyService.byName(player.server, payee)
                .ifPresent(account -> EconomyService.note(
                        player.server,
                        account.id(),
                        LedgerEntry.Kind.PAY_IN,
                        outcome.amount(),
                        player.getGameProfile().getName()));
        tell(player, Component.translatable(
                "commands.ravencoin.pay.sent", Amounts.format(outcome.amount()), payee));
        ServerPlayer online = player.server.getPlayerList().getPlayerByName(payee);
        if (online != null) {
            online.sendSystemMessage(Component.translatable(
                    "commands.ravencoin.pay.received",
                    player.getGameProfile().getName(),
                    Amounts.format(outcome.amount())));
        }
    }

    /** Fills in a page that only the server knows the contents of. */
    private static void onAtmRequest(AtmRequestPayload payload, IPayloadContext context) {
        if (atm(context) != null && context.player() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, AtmPages.build(player, payload.page()));
        }
    }

    /**
     * Sells a rank from the ATM's rank page.
     *
     * <p>Sends the page back afterwards whatever happened. A purchase changes
     * the row that was pressed — it stops naming a price and starts saying the
     * rank is yours — and a refusal has to leave the row exactly as it was
     * rather than as the client optimistically drew it.
     */
    private static void onAtmRankBuy(AtmRankBuyPayload payload, IPayloadContext context) {
        if (atm(context) == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        RankPurchase result = RankService.buy(player, payload.rank());
        if (result.ok()) {
            RankService.find(payload.rank()).ifPresent(rank -> {
                EconomyService.note(
                        player.server, player.getUUID(), LedgerEntry.Kind.RANK, rank.price(), rank.name());
                tell(player, Component.translatable(
                        "commands.ravencoin.rank.bought", rank.name(), Amounts.format(rank.price())));
            });
        } else {
            refuse(player, Component.translatable(RankCommands.errorKey(result)));
        }
        PacketDistributor.sendToPlayer(player, AtmPages.build(player, Page.RANKS));
    }

    private static void tell(ServerPlayer player, Component text) {
        PacketDistributor.sendToPlayer(player, new AtmNoticePayload(text, false));
    }

    private static void refuse(ServerPlayer player, Component text) {
        PacketDistributor.sendToPlayer(player, new AtmNoticePayload(text, true));
    }

    /** {@return the ATM menu the sender has open, or null if they have not} */
    @Nullable
    private static AtmMenu atm(IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return null;
        }
        if (!(player.containerMenu instanceof AtmMenu menu) || !menu.stillValid(player)) {
            return null;
        }
        return menu;
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
        player.sendSystemMessage(ShopText.outcome(shop, shop.buy(player, payload.lots(), payload.fromAccount())));
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

        if (outcome.result().ok()) {
            EconomyService.note(
                    player.server,
                    player.getUUID(),
                    payload.action() == AtmActionPayload.Action.DEPOSIT
                            ? LedgerEntry.Kind.DEPOSIT
                            : LedgerEntry.Kind.WITHDRAW,
                    outcome.amount(),
                    "");
        }
        if (outcome.result().ok()) {
            tell(player, message(payload.action(), outcome));
        } else {
            refuse(player, message(payload.action(), outcome));
        }
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
            case UNKNOWN_PLAYER -> "commands.ravencoin.error.unknown_player";
            case OK -> throw new IllegalArgumentException("OK is not an error");
        };
    }

    private ModNetwork() {}
}
