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
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.economy.Amounts;
import net.whiteravens.ravencoin.economy.TransactionResult;
import net.whiteravens.ravencoin.menu.AtmMenu;

/** Registers this mod's packets and handles the one the ATM sends. */
@EventBusSubscriber(modid = RavenCoin.MOD_ID)
public final class ModNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(AtmActionPayload.TYPE, AtmActionPayload.STREAM_CODEC, ModNetwork::onAtmAction);
    }

    /**
     * Runs one ATM action for the player who asked for it.
     *
     * <p>Handlers registered this way run on the server thread, so the ledger is
     * touched from the one place that is allowed to touch it.
     *
     * <p>The open menu is the authorisation. A packet is only ever worth what the
     * player could have done by standing at the machine and clicking, so anyone
     * who is not actually looking at an ATM they can reach is ignored — that
     * check, not the screen, is what makes the button safe.
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
