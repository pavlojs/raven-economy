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
package net.whiteravens.ravencoin.shop;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.whiteravens.ravencoin.block.entity.ShopBlockEntity;
import net.whiteravens.ravencoin.economy.Amounts;
import net.whiteravens.ravencoin.registry.ModItems;

/**
 * Wording a trade, in the one place that does it.
 *
 * <p>The floating label, the two screens and the line the server sends to chat
 * all describe the same offer, and a shop whose sign and screen disagreed about
 * the price would be worse than one with no sign at all.
 */
public final class ShopText {
    /**
     * {@return an amount of one good, worded the way that good is normally counted}
     *
     * <p>RavenCoin gets its own phrasing. "240 RC" is what every other part of
     * this mod calls that number, and "240x RavenCoin" on a price tag would be
     * the only place it did not.
     */
    public static Component amount(ItemStack good, int units) {
        if (good.is(ModItems.COIN.get())) {
            return Component.translatable("screen.ravencoin.shop.coins", Amounts.format(units));
        }
        return Component.translatable("screen.ravencoin.shop.stack", Amounts.format(units), good.getHoverName());
    }

    /**
     * {@return an amount of one good, worded for a screen that is already
     * showing you what the good is}
     *
     * <p>The same number as {@link #amount}, without the name. Both shop screens
     * draw the item itself now, so repeating "Supermassive QIO Drive" beside its
     * own icon bought nothing and cost 26 pixels more than the panel has.
     * RavenCoin keeps its unit, because "120" and "120 RC" are not equally clear
     * and the two extra letters always fit.
     */
    public static Component count(ItemStack good, int units) {
        if (good.is(ModItems.COIN.get())) {
            return Component.translatable("screen.ravencoin.shop.coins", Amounts.format(units));
        }
        return Component.translatable("screen.ravencoin.shop.units", Amounts.format(units));
    }

    /** {@return what the sign over a shop says, top line first} */
    public static List<Component> label(ShopBlockEntity shop) {
        List<Component> lines = new ArrayList<>(3);
        lines.add(amount(shop.product(), shop.productUnits()).copy().withStyle(ChatFormatting.WHITE));
        lines.add(Component.translatable("screen.ravencoin.shop.for", amount(shop.price(), shop.priceUnits()))
                .withStyle(ChatFormatting.GOLD));
        lines.add(stock(shop));
        return lines;
    }

    /**
     * {@return the stock line for the sign above the block}
     *
     * <p>The one thing a passer-by most wants to know, in colours meant for a
     * sign floating in the world — bright, because whatever is behind it is
     * darker than they are.
     */
    public static Component stock(ShopBlockEntity shop) {
        return stock(shop, ChatFormatting.AQUA, ChatFormatting.RED, ChatFormatting.GRAY);
    }

    /**
     * {@return the same line, in colours that survive a screen}
     *
     * <p>A Minecraft panel is #C6C6C6, and aqua, red and grey were all chosen
     * against a dark sky. On this background the first two are pale and the
     * third is all but invisible — measured, by drawing the panel and looking at
     * it. Same words, darker end of the same hues.
     */
    public static Component stockOnPanel(ShopBlockEntity shop) {
        return stock(shop, ChatFormatting.DARK_AQUA, ChatFormatting.DARK_RED, ChatFormatting.DARK_GRAY);
    }

    private static Component stock(
            ShopBlockEntity shop, ChatFormatting unlimited, ChatFormatting problem, ChatFormatting count) {
        if (shop.admin()) {
            return Component.translatable("screen.ravencoin.shop.unlimited").withStyle(unlimited);
        }
        if (!shop.hasContainer()) {
            return Component.translatable("screen.ravencoin.shop.no_container").withStyle(problem);
        }
        if (shop.trades() <= 0) {
            return Component.translatable("screen.ravencoin.shop.out_of_stock").withStyle(problem);
        }
        return Component.translatable("screen.ravencoin.shop.in_stock", Amounts.format(shop.trades()))
                .withStyle(count);
    }

    /** {@return the chat line for a trade that happened, or the reason one did not} */
    public static Component outcome(ShopBlockEntity shop, ShopBlockEntity.Outcome outcome) {
        if (outcome.result() != ShopResult.OK) {
            return Component.translatable(errorKey(outcome.result())).withStyle(ChatFormatting.RED);
        }
        return Component.translatable(
                "screen.ravencoin.shop.bought",
                amount(shop.product(), outcome.lots() * shop.productUnits()),
                amount(shop.price(), outcome.lots() * shop.priceUnits()));
    }

    public static String errorKey(ShopResult result) {
        return switch (result) {
            case NOT_SET_UP -> "screen.ravencoin.shop.error.not_set_up";
            case NO_CONTAINER -> "screen.ravencoin.shop.error.no_container";
            case OUT_OF_STOCK -> "screen.ravencoin.shop.error.out_of_stock";
            case TILL_FULL -> "screen.ravencoin.shop.error.till_full";
            case CANNOT_PAY -> "screen.ravencoin.shop.error.cannot_pay";
            case NO_ROOM -> "screen.ravencoin.shop.error.no_room";
            case RANK_REQUIRED -> "screen.ravencoin.shop.error.rank_required";
            case DISABLED -> "screen.ravencoin.shop.error.disabled";
            case OK -> throw new IllegalArgumentException("OK is not an error");
        };
    }

    private ShopText() {}
}
