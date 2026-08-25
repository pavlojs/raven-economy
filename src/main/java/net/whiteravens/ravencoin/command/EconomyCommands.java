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
package net.whiteravens.ravencoin.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.config.RavenCoinConfig;
import net.whiteravens.ravencoin.economy.Account;
import net.whiteravens.ravencoin.economy.Amounts;
import net.whiteravens.ravencoin.economy.EconomyService;
import net.whiteravens.ravencoin.economy.TransactionResult;

/**
 * The chat half of the economy.
 *
 * <p>These are a thin skin over {@link EconomyService} and hold no rules of
 * their own — the ATM screen calls the same methods without going anywhere near
 * a command string.
 *
 * <p>Everything lives under {@code /rc} (and the spelt out {@code /ravencoin}),
 * because {@code /balance} and {@code /pay} are names half the mods and plugins
 * in this corner of the world want, and Brigadier gives no warning when two of
 * them claim one. Those bare names are registered too, but only when the
 * operator opts in.
 */
@EventBusSubscriber(modid = RavenCoin.MOD_ID)
public final class EconomyCommands {
    private static final int LEADERBOARD_SIZE = 10;

    private static final SimpleCommandExceptionType ERROR_ONE_PLAYER =
            new SimpleCommandExceptionType(Component.translatable("commands.ravencoin.error.one_player"));

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("rc")
                .then(balance("balance"))
                .then(pay("pay"))
                .then(top("top"))
                .then(RankCommands.rank())
                .then(eco()));
        dispatcher.register(Commands.literal("ravencoin")
                .then(balance("balance"))
                .then(pay("pay"))
                .then(top("top"))
                .then(RankCommands.rank())
                .then(eco()));

        if (RavenCoinConfig.COMMON.shortCommandAliases.get()) {
            dispatcher.register(balance("balance"));
            dispatcher.register(balance("bal"));
            dispatcher.register(pay("pay"));
            dispatcher.register(top("baltop"));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> balance(String name) {
        return Commands.literal(name)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    long balance = EconomyService.balance(server(ctx), player.getUUID());
                    ctx.getSource()
                            .sendSuccess(
                                    () -> Component.translatable(
                                            "commands.ravencoin.balance.self", Amounts.format(balance)),
                                    false);
                    return (int) Math.min(balance, Integer.MAX_VALUE);
                })
                .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(ctx -> {
                    GameProfile target = singleProfile(ctx);
                    long balance = EconomyService.balance(server(ctx), target.getId());
                    ctx.getSource()
                            .sendSuccess(
                                    () -> Component.translatable(
                                            "commands.ravencoin.balance.other",
                                            target.getName(),
                                            Amounts.format(balance)),
                                    false);
                    return (int) Math.min(balance, Integer.MAX_VALUE);
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> pay(String name) {
        return Commands.literal(name)
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayer payer = ctx.getSource().getPlayerOrException();
                                    GameProfile payee = singleProfile(ctx);
                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                    MinecraftServer server = server(ctx);

                                    TransactionResult result = EconomyService.transfer(
                                            server,
                                            payer.getUUID(),
                                            payer.getGameProfile().getName(),
                                            payee.getId(),
                                            payee.getName(),
                                            amount);
                                    if (!result.ok()) {
                                        return fail(ctx, result);
                                    }

                                    ctx.getSource()
                                            .sendSuccess(
                                                    () -> Component.translatable(
                                                            "commands.ravencoin.pay.sent",
                                                            Amounts.format(amount),
                                                            payee.getName()),
                                                    false);
                                    ServerPlayer online = server.getPlayerList().getPlayer(payee.getId());
                                    if (online != null) {
                                        online.sendSystemMessage(Component.translatable(
                                                "commands.ravencoin.pay.received",
                                                payer.getGameProfile().getName(),
                                                Amounts.format(amount)));
                                    }
                                    return 1;
                                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> top(String name) {
        return Commands.literal(name).executes(ctx -> {
            List<Account> accounts = EconomyService.leaderboard(server(ctx), LEADERBOARD_SIZE);
            if (accounts.isEmpty()) {
                ctx.getSource()
                        .sendSuccess(() -> Component.translatable("commands.ravencoin.top.empty"), false);
                return 0;
            }
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.ravencoin.top.header"), false);
            for (int i = 0; i < accounts.size(); i++) {
                Account account = accounts.get(i);
                int place = i + 1;
                ctx.getSource()
                        .sendSuccess(
                                () -> Component.translatable(
                                        "commands.ravencoin.top.line",
                                        place,
                                        account.name(),
                                        Amounts.format(account.balance())),
                                false);
            }
            return accounts.size();
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> eco() {
        return Commands.literal("eco")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(ecoAction("add", 1, (server, target, amount) -> EconomyService.deposit(
                        server, target.getId(), target.getName(), amount)))
                .then(ecoAction("take", 1, (server, target, amount) -> EconomyService.withdraw(
                        server, target.getId(), target.getName(), amount)))
                .then(ecoAction("set", 0, (server, target, amount) -> EconomyService.set(
                        server, target.getId(), target.getName(), amount)))
                .then(Commands.literal("get")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> {
                                    GameProfile target = singleProfile(ctx);
                                    long balance = EconomyService.balance(server(ctx), target.getId());
                                    ctx.getSource()
                                            .sendSuccess(
                                                    () -> Component.translatable(
                                                            "commands.ravencoin.balance.other",
                                                            target.getName(),
                                                            Amounts.format(balance)),
                                                    false);
                                    return (int) Math.min(balance, Integer.MAX_VALUE);
                                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ecoAction(String name, long minimum, EcoAction action) {
        return Commands.literal(name)
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .then(Commands.argument("amount", LongArgumentType.longArg(minimum))
                                .executes(ctx -> {
                                    GameProfile target = singleProfile(ctx);
                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                    MinecraftServer server = server(ctx);

                                    TransactionResult result = action.apply(server, target, amount);
                                    if (!result.ok()) {
                                        return fail(ctx, result);
                                    }

                                    long balance = EconomyService.balance(server, target.getId());
                                    ctx.getSource()
                                            .sendSuccess(
                                                    () -> Component.translatable(
                                                            "commands.ravencoin.eco." + name,
                                                            Amounts.format(amount),
                                                            target.getName(),
                                                            Amounts.format(balance)),
                                                    true);
                                    return 1;
                                })));
    }

    /** Sends the operator-facing reason a transaction was refused, and reports failure to the caller. */
    private static int fail(CommandContext<CommandSourceStack> ctx, TransactionResult result) {
        ctx.getSource().sendFailure(Component.translatable(errorKey(result)));
        return 0;
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

    /**
     * {@return the one profile the {@code player} argument named}
     *
     * <p>The argument accepts selectors, so {@code @a} would otherwise pay
     * everyone from one balance check. Money moves one account at a time.
     */
    private static GameProfile singleProfile(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
        if (profiles.size() != 1) {
            throw ERROR_ONE_PLAYER.create();
        }
        return profiles.iterator().next();
    }

    private static MinecraftServer server(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getServer();
    }

    @FunctionalInterface
    private interface EcoAction {
        TransactionResult apply(MinecraftServer server, GameProfile target, long amount);
    }

    private EconomyCommands() {}
}
