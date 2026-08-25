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

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.whiteravens.ravencoin.economy.Amounts;
import net.whiteravens.ravencoin.rank.Playtime;
import net.whiteravens.ravencoin.rank.Rank;
import net.whiteravens.ravencoin.rank.RankPurchase;
import net.whiteravens.ravencoin.rank.RankService;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /rc rank} — buying a rung, and the operator side of shaping the ladder.
 *
 * <p>Which ranks exist, how many there are and what each costs is entirely the
 * operator's: this mod ships no ladder and invents no group names. Everything
 * here writes to {@code config/ravencoin-ranks.json}, so the file and the
 * commands are two views of one thing rather than two sources of truth.
 */
public final class RankCommands {
    private static final SuggestionProvider<CommandSourceStack> RANK_IDS = (context, builder) ->
            SharedSuggestionProvider.suggest(RankService.ranks().stream().map(Rank::id), builder);

    static LiteralArgumentBuilder<CommandSourceStack> rank() {
        return Commands.literal("rank")
                .executes(RankCommands::list)
                .then(Commands.literal("buy")
                        .then(Commands.argument("rank", StringArgumentType.word())
                                .suggests(RANK_IDS)
                                .executes(RankCommands::buy)))
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("rank", StringArgumentType.word())
                                .suggests(RANK_IDS)
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .then(Commands.argument("price", LongArgumentType.longArg(0))
                                                .executes(context -> define(context, null))
                                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(context -> define(
                                                                context,
                                                                StringArgumentType.getString(context, "name"))))))))
                .then(Commands.literal("requires")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("rank", StringArgumentType.word())
                                .suggests(RANK_IDS)
                                .then(Commands.literal("none").executes(context -> requires(context, null)))
                                .then(Commands.argument("required", StringArgumentType.word())
                                        .suggests(RANK_IDS)
                                        .executes(context -> requires(
                                                context, StringArgumentType.getString(context, "required"))))))
                .then(Commands.literal("playtime")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("rank", StringArgumentType.word())
                                .suggests(RANK_IDS)
                                .then(Commands.literal("none").executes(context -> playtime(context, 0L)))
                                .then(Commands.argument("minutes", LongArgumentType.longArg(1))
                                        .executes(context -> playtime(
                                                context, LongArgumentType.getLong(context, "minutes"))))))
                .then(Commands.literal("remove")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("rank", StringArgumentType.word())
                                .suggests(RANK_IDS)
                                .executes(RankCommands::remove)))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(RankCommands::reload));
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        if (!RankService.permissionsAvailable()) {
            context.getSource().sendFailure(Component.translatable("commands.ravencoin.rank.error.no_permissions"));
            return 0;
        }
        List<Rank> ranks = RankService.ranks();
        if (ranks.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.ravencoin.rank.list.empty"), false);
            return 0;
        }

        ServerPlayer viewer = context.getSource().getPlayer();
        // "?" rather than a zero when the console asks: nobody has played no time,
        // there is simply no player to measure.
        String played = viewer == null ? "?" : Playtime.format(Playtime.minutes(viewer));
        context.getSource().sendSuccess(() -> Component.translatable("commands.ravencoin.rank.list.header"), false);
        for (Rank rank : ranks) {
            boolean owned = viewer != null && RankService.owns(viewer, rank);
            // Only call it locked if the viewer actually lacks the rung below —
            // a prerequisite they already hold is no longer a reason not to buy.
            Rank required = RankService.prerequisite(rank).orElse(null);
            Rank needed = required != null && (viewer == null || !RankService.owns(viewer, required)) ? required : null;
            context.getSource()
                    .sendSuccess(
                            () -> {
                                if (owned) {
                                    return Component.translatable("commands.ravencoin.rank.list.owned", rank.name());
                                }
                                // An earned rank gets its own lines rather than the
                                // priced ones — it has no price worth printing, and
                                // saying "0 RC" would read as an invitation to buy it.
                                if (rank.earned()) {
                                    String at = Playtime.format(rank.playtimeMinutes());
                                    return needed == null
                                            ? Component.translatable(
                                                    "commands.ravencoin.rank.list.earned", rank.name(), at, played)
                                            : Component.translatable(
                                                    "commands.ravencoin.rank.list.earned_locked",
                                                    rank.name(),
                                                    at,
                                                    needed.name());
                                }
                                if (needed != null) {
                                    return Component.translatable(
                                            "commands.ravencoin.rank.list.locked",
                                            rank.name(),
                                            Amounts.format(rank.price()),
                                            needed.name());
                                }
                                return Component.translatable(
                                        "commands.ravencoin.rank.list.line",
                                        rank.name(),
                                        Amounts.format(rank.price()),
                                        rank.id());
                            },
                            false);
        }
        return ranks.size();
    }

    private static int buy(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(context, "rank");

        RankPurchase result = RankService.buy(player, id);
        if (!result.ok()) {
            context.getSource().sendFailure(Component.translatable(errorKey(result)));
            return 0;
        }

        Rank rank = RankService.find(id).orElseThrow();
        context.getSource()
                .sendSuccess(
                        () -> Component.translatable(
                                "commands.ravencoin.rank.bought", rank.name(), Amounts.format(rank.price())),
                        false);
        return 1;
    }

    private static int define(CommandContext<CommandSourceStack> context, @Nullable String name) {
        String id = StringArgumentType.getString(context, "rank");
        String group = StringArgumentType.getString(context, "group");
        long price = LongArgumentType.getLong(context, "price");
        Rank rank = new Rank(id, group, price, name == null ? id : name, null, 0L);

        if (!RankService.define(rank)) {
            context.getSource()
                    .sendFailure(Component.translatable("commands.ravencoin.rank.error.no_group", group));
            return 0;
        }
        context.getSource()
                .sendSuccess(
                        () -> Component.translatable(
                                "commands.ravencoin.rank.defined", rank.name(), group, Amounts.format(price)),
                        true);
        return 1;
    }

    private static int requires(CommandContext<CommandSourceStack> context, @Nullable String requiredId) {
        String id = StringArgumentType.getString(context, "rank");
        if (!RankService.setPrerequisite(id, requiredId)) {
            context.getSource().sendFailure(Component.translatable("commands.ravencoin.rank.error.bad_requirement"));
            return 0;
        }
        context.getSource()
                .sendSuccess(
                        () -> requiredId == null
                                ? Component.translatable("commands.ravencoin.rank.requires.cleared", id)
                                : Component.translatable("commands.ravencoin.rank.requires.set", id, requiredId),
                        true);
        return 1;
    }

    private static int playtime(CommandContext<CommandSourceStack> context, long minutes) {
        String id = StringArgumentType.getString(context, "rank");
        if (!RankService.setPlaytime(id, minutes)) {
            context.getSource().sendFailure(Component.translatable("commands.ravencoin.rank.error.unknown"));
            return 0;
        }
        context.getSource()
                .sendSuccess(
                        () -> minutes == 0
                                ? Component.translatable("commands.ravencoin.rank.playtime.cleared", id)
                                : Component.translatable(
                                        "commands.ravencoin.rank.playtime.set", id, Playtime.format(minutes)),
                        true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "rank");
        if (!RankService.undefine(id)) {
            context.getSource().sendFailure(Component.translatable("commands.ravencoin.rank.error.unknown"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.ravencoin.rank.removed", id), true);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        if (!RankService.reload()) {
            context.getSource().sendFailure(Component.translatable("commands.ravencoin.rank.error.file"));
            return 0;
        }
        int count = RankService.ranks().size();
        context.getSource()
                .sendSuccess(() -> Component.translatable("commands.ravencoin.rank.reloaded", count), true);
        return count;
    }

    private static String errorKey(RankPurchase result) {
        return switch (result) {
            case DISABLED -> "commands.ravencoin.rank.error.disabled";
            case NO_PERMISSIONS -> "commands.ravencoin.rank.error.no_permissions";
            case UNKNOWN_RANK -> "commands.ravencoin.rank.error.unknown";
            case ALREADY_OWNED -> "commands.ravencoin.rank.error.already_owned";
            case OUT_OF_ORDER -> "commands.ravencoin.rank.error.out_of_order";
            case EARNED_ONLY -> "commands.ravencoin.rank.error.earned_only";
            case INSUFFICIENT_FUNDS -> "commands.ravencoin.error.insufficient_funds";
            case FAILED -> "commands.ravencoin.rank.error.failed";
            case OK -> throw new IllegalArgumentException("OK is not an error");
        };
    }

    private RankCommands() {}
}
