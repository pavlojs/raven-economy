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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.block.entity.ShopBlockEntity;
import net.whiteravens.ravencoin.menu.ShopMenu;
import net.whiteravens.ravencoin.economy.Amounts;
import net.whiteravens.ravencoin.network.ShopBuyPayload;
import net.whiteravens.ravencoin.network.ShopStallPayload;
import net.whiteravens.ravencoin.shop.ShopStock;
import net.whiteravens.ravencoin.shop.ShopText;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The buying screen.
 *
 * <p>Both sides of the trade are shown as the items they are, in the same two
 * frames the owner picked them in. Names are on the tooltip and on the sign
 * above the block, not on the panel: a price is an arbitrary item, and a shop
 * that sells for a "Supermassive QIO Drive" needs 26 more pixels than this
 * panel has to say so.
 *
 * <p>Reads the offer straight off the block entity, which every client that can
 * see the block already has a copy of. That is why the price here and the price
 * on the floating sign cannot drift apart: they are the same field, read twice.
 */
public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "textures/gui/shop.png");

    /** One order, in lots. Four digits is more than any shop's chest can supply anyway. */
    private static final int MAX_DIGITS = 4;

    private static final int PRODUCT_SLOT_X = 7;
    private static final int PRICE_SLOT_X = 91;
    private static final int SLOT_Y = 28;
    private static final int SLOT_SIZE = 18;

    /** Where the count beside each frame starts, and how much room it has. */
    private static final int PRODUCT_TEXT_X = 30;

    private static final int PRICE_TEXT_X = 114;

    private static final int COUNT_WIDTH = 54;

    /** What the "you have" line gets, with the purse switch beside it. */
    private static final int HELD_WIDTH = 94;

    /** Gold dark enough to stay gold against a #C6C6C6 panel. */
    private static final int COIN_INK = 0x8A6A00;

    /** The panel's own grey, for painting out the two slot frames baked into it. */
    private static final int PANEL = 0xFFC6C6C6;

    /**
     * Which purse the buyer is spending from.
     *
     * <p>Remembered for as long as the client runs rather than per shop: a
     * player who banks their money wants to buy from their account at every
     * shop, and being asked again at each counter is the annoying half of a
     * choice.
     */
    private static boolean fromAccount;

    @Nullable
    private EditBox lots;

    @Nullable
    private Button buy;

    @Nullable
    private Button purse;

    @Nullable
    private Button rent;

    public ShopScreen(ShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 196;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        this.lots = new EditBox(
                this.font,
                this.leftPos + 8,
                this.topPos + 78,
                34,
                16,
                Component.translatable("screen.ravencoin.shop.lots"));
        this.lots.setMaxLength(MAX_DIGITS);
        this.lots.setFilter(text -> text.chars().allMatch(Character::isDigit));
        this.lots.setValue("1");
        this.addRenderableWidget(this.lots);

        this.purse = Button.builder(this.purseText(), button -> {
                    fromAccount = !fromAccount;
                    button.setMessage(this.purseText());
                })
                .bounds(this.leftPos + 106, this.topPos + 61, 62, 16)
                .build();
        this.addRenderableWidget(this.purse);

        this.buy = Button.builder(Component.translatable("screen.ravencoin.shop.buy"), button -> this.send())
                .bounds(this.leftPos + 46, this.topPos + 77, 122, 18)
                .build();
        this.addRenderableWidget(this.buy);

        this.rent = Button.builder(
                        Component.translatable("screen.ravencoin.shop.rent.take"),
                        button -> PacketDistributor.sendToServer(
                                new ShopStallPayload(ShopStallPayload.Action.RENT)))
                .bounds(this.leftPos + 8, this.topPos + 77, 160, 18)
                .build();
        this.addRenderableWidget(this.rent);
    }

    private Component purseText() {
        return Component.translatable(
                fromAccount ? "screen.ravencoin.shop.purse.account" : "screen.ravencoin.shop.purse.pocket");
    }

    private void send() {
        int wanted = this.wanted();
        if (wanted > 0) {
            PacketDistributor.sendToServer(new ShopBuyPayload(wanted, fromAccount));
        }
    }

    private int wanted() {
        if (this.lots == null) {
            return 0;
        }
        try {
            return Integer.parseInt(this.lots.getValue().trim());
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }

    /**
     * Keeps the button's total honest while the buyer types, greys it out when
     * the shop cannot serve, and hides the purse switch where it would be a lie.
     *
     * <p>An account holds RavenCoin and nothing else, so a shop priced in iron
     * is paid out of pockets whatever the switch says. Showing a switch that
     * changed nothing would be worse than showing none.
     */
    @Override
    protected void containerTick() {
        super.containerTick();
        ShopBlockEntity shop = this.menu.shop();
        int wanted = this.wanted();

        // An empty stall is an advertisement, not a counter: there is nothing to
        // buy until somebody has rented it and put something in it.
        boolean toLet = shop != null && shop.toLet();
        if (this.rent != null) {
            this.rent.visible = toLet;
            // A stall still holding the last renter's goods is advertised but
            // not available, and a button that refuses every press is worse
            // than one that is plainly not ready.
            this.rent.active = shop != null && shop.stallReady();
        }
        if (this.lots != null) {
            this.lots.visible = !toLet;
        }
        if (this.buy != null) {
            this.buy.visible = !toLet;
        }
        if (toLet) {
            if (this.purse != null) {
                this.purse.visible = false;
            }
            return;
        }

        if (this.purse != null) {
            this.purse.visible = shop != null && shop.configured() && shop.pricedInCoin();
        }
        if (this.buy == null) {
            return;
        }
        if (shop == null || !shop.configured() || wanted <= 0) {
            this.buy.setMessage(Component.translatable("screen.ravencoin.shop.buy"));
            this.buy.active = false;
            return;
        }
        this.buy.setMessage(Component.translatable(
                "screen.ravencoin.shop.buy_for", ShopText.count(shop.price(), wanted * shop.priceUnits())));
        this.buy.active = true;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        ShopBlockEntity shop = this.menu.shop();
        if (shop != null && shop.toLet()) {
            // The two sockets are painted into the panel texture, and this page
            // is an advertisement rather than a counter: without this the rent
            // is written across two empty slots that mean nothing here.
            this.blank(graphics, PRODUCT_SLOT_X);
            this.blank(graphics, PRICE_SLOT_X);
            return;
        }
        if (shop != null && shop.configured()) {
            graphics.renderItem(shop.product(), this.leftPos + PRODUCT_SLOT_X + 1, this.topPos + SLOT_Y + 1);
            graphics.renderItem(shop.price(), this.leftPos + PRICE_SLOT_X + 1, this.topPos + SLOT_Y + 1);
        }
    }

    private void blank(GuiGraphics graphics, int slotX) {
        graphics.fill(
                this.leftPos + slotX,
                this.topPos + SLOT_Y,
                this.leftPos + slotX + SLOT_SIZE,
                this.topPos + SLOT_Y + SLOT_SIZE,
                PANEL);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Branding.draw(graphics, this.font, 168, this.inventoryLabelY);

        ShopBlockEntity shop = this.menu.shop();
        if (shop != null && shop.toLet()) {
            Labels.draw(
                    graphics,
                    this.font,
                    Component.translatable("screen.ravencoin.shop.rent.offer"),
                    8,
                    22,
                    160,
                    0x404040,
                    false);
            Labels.draw(
                    graphics,
                    this.font,
                    Component.translatable(
                            "screen.ravencoin.shop.rent.terms",
                            Amounts.format(shop.quotedRent()),
                            shop.quotedDays()),
                    8,
                    36,
                    160,
                    COIN_INK,
                    false);
            // Two lines, because one that says all of this is 205px wide on a
            // 160px panel and arrives with its last three words cut off.
            boolean ready = shop.stallReady();
            String prefix = ready ? "screen.ravencoin.shop.rent.note" : "screen.ravencoin.shop.rent.not_ready";
            int ink = ready ? 0x555555 : 0xAA0000;
            Labels.draw(graphics, this.font, Component.translatable(prefix), 8, 52, 160, ink, false);
            Labels.draw(graphics, this.font, Component.translatable(prefix + "2"), 8, 63, 160, ink, false);
            return;
        }
        if (shop == null || !shop.configured()) {
            Labels.draw(
                    graphics,
                    this.font,
                    Component.translatable("screen.ravencoin.shop.error.not_set_up"),
                    8,
                    32,
                    160,
                    0x404040,
                    false);
            return;
        }

        graphics.drawString(this.font, Component.translatable("screen.ravencoin.shop.goods"), 8, 18, 0x404040, false);
        graphics.drawString(this.font, Component.translatable("screen.ravencoin.shop.price"), 92, 18, 0x404040, false);
        Labels.draw(
                graphics,
                this.font,
                ShopText.count(shop.product(), shop.productUnits()),
                PRODUCT_TEXT_X,
                SLOT_Y + 5,
                COUNT_WIDTH,
                0x404040,
                false);
        Labels.draw(
                graphics,
                this.font,
                ShopText.count(shop.price(), shop.priceUnits()),
                PRICE_TEXT_X,
                SLOT_Y + 5,
                COUNT_WIDTH,
                COIN_INK,
                false);

        Labels.draw(
                graphics,
                this.font,
                shop.closed()
                        ? Component.translatable("screen.ravencoin.shop.error.closed")
                                .withStyle(net.minecraft.ChatFormatting.DARK_RED)
                        : ShopText.stockOnPanel(shop),
                8,
                52,
                160,
                0x555555,
                false);

        long held = shop.pricedInCoin() && fromAccount
                ? this.menu.balance()
                : ShopStock.count(ShopStock.pockets(this.minecraft.player), shop.price());
        Labels.draw(
                graphics,
                this.font,
                Component.translatable(
                        "screen.ravencoin.shop.you_have",
                        ShopText.count(shop.price(), (int) Math.min(held, Integer.MAX_VALUE))),
                8,
                64,
                HELD_WIDTH,
                0x555555,
                false);
    }

    /**
     * The names live here, on the tooltip.
     *
     * <p>Drawn after everything else so a name that is wider than the panel is
     * over the panel rather than clipped inside it, which is the whole reason
     * the two counts on the panel no longer carry one.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        ShopBlockEntity shop = this.menu.shop();
        if (shop == null || !shop.configured()) {
            return;
        }
        this.nameTooltip(graphics, mouseX, mouseY, PRODUCT_SLOT_X, shop.product());
        this.nameTooltip(graphics, mouseX, mouseY, PRICE_SLOT_X, shop.price());
    }

    private void nameTooltip(GuiGraphics graphics, int mouseX, int mouseY, int slotX, ItemStack good) {
        if (this.over(mouseX, mouseY, slotX)) {
            graphics.renderTooltip(this.font, good.getHoverName(), mouseX, mouseY);
        }
    }

    private boolean over(double mouseX, double mouseY, int slotX) {
        double x = mouseX - this.leftPos - slotX;
        double y = mouseY - this.topPos - SLOT_Y;
        return x >= 0 && x < SLOT_SIZE && y >= 0 && y < SLOT_SIZE;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Without this, typing a quantity that contains the inventory key closes
        // the screen mid-number.
        if (this.lots != null
                && this.lots.isFocused()
                && this.lots.canConsumeInput()
                && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            return this.lots.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
