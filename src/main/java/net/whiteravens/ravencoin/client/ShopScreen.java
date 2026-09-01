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
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.block.entity.ShopBlockEntity;
import net.whiteravens.ravencoin.menu.ShopMenu;
import net.whiteravens.ravencoin.network.ShopBuyPayload;
import net.whiteravens.ravencoin.shop.ShopStock;
import net.whiteravens.ravencoin.shop.ShopText;
import org.lwjgl.glfw.GLFW;

/**
 * The buying screen.
 *
 * <p>Reads the offer straight off the block entity, which every client that can
 * see the block already has a copy of. That is why the price on this screen and
 * the price on the floating label cannot drift apart: they are the same field,
 * read twice.
 *
 * <p>The button says what the whole order costs, because the number a buyer
 * actually needs is the total and working it out in their head is exactly the
 * moment a shop loses a sale.
 */
public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "textures/gui/shop.png");

    /** One order, in lots. Four digits is more than any shop's chest can supply anyway. */
    private static final int MAX_DIGITS = 4;

    /** Left edge of the four lines of the offer, clear of the product's icon. */
    private static final int TEXT_LEFT = 34;

    /** How much room those lines have before they run off the panel. */
    private static final int TEXT_WIDTH = 176 - TEXT_LEFT - 8;

    private static final FormattedText ELLIPSIS = FormattedText.of("…");

    private EditBox lots;
    private Button buy;

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
                40,
                16,
                Component.translatable("screen.ravencoin.shop.lots"));
        this.lots.setMaxLength(MAX_DIGITS);
        this.lots.setFilter(text -> text.chars().allMatch(Character::isDigit));
        this.lots.setValue("1");
        this.addRenderableWidget(this.lots);

        this.buy = Button.builder(Component.translatable("screen.ravencoin.shop.buy"), button -> this.send())
                .bounds(this.leftPos + 52, this.topPos + 77, 116, 18)
                .build();
        this.addRenderableWidget(this.buy);
    }

    private void send() {
        int wanted = this.wanted();
        if (wanted > 0) {
            PacketDistributor.sendToServer(new ShopBuyPayload(wanted));
        }
    }

    private int wanted() {
        try {
            return Integer.parseInt(this.lots.getValue().trim());
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }

    /** Keeps the button's total honest while the buyer types, and greys it out when the shop cannot serve. */
    @Override
    protected void containerTick() {
        super.containerTick();
        ShopBlockEntity shop = this.menu.shop();
        int wanted = this.wanted();
        if (shop == null || !shop.configured() || wanted <= 0) {
            this.buy.setMessage(Component.translatable("screen.ravencoin.shop.buy"));
            this.buy.active = false;
            return;
        }
        this.buy.setMessage(Component.translatable(
                "screen.ravencoin.shop.buy_for", ShopText.amount(shop.price(), wanted * shop.priceUnits())));
        this.buy.active = true;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        ShopBlockEntity shop = this.menu.shop();
        if (shop != null && shop.configured()) {
            graphics.renderItem(shop.product(), this.leftPos + 12, this.topPos + 22);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        ShopBlockEntity shop = this.menu.shop();
        if (shop == null || !shop.configured()) {
            this.line(graphics, Component.translatable("screen.ravencoin.shop.error.not_set_up"), 24, 0x404040, false);
            return;
        }

        this.line(graphics, ShopText.amount(shop.product(), shop.productUnits()), 24, 0xFFFFFF, true);
        this.line(
                graphics,
                Component.translatable("screen.ravencoin.shop.for", ShopText.amount(shop.price(), shop.priceUnits())),
                36,
                0xFFD700,
                true);
        this.line(graphics, ShopText.stock(shop), 48, 0xC0C0C0, true);

        long held = ShopStock.count(ShopStock.pockets(this.minecraft.player), shop.price());
        this.line(
                graphics,
                Component.translatable("screen.ravencoin.shop.you_have", ShopText.amount(shop.price(), (int) Math.min(held, Integer.MAX_VALUE))),
                60,
                0xC0C0C0,
                false);
    }

    /**
     * Draws one line of the offer, cut short rather than off the edge.
     *
     * <p>A price is an arbitrary item, and the server shop's buy-back prices
     * goods that are named things like "Supermassive QIO Drive" — measured at
     * 150 pixels against the 134 this panel has. {@code drawString} neither
     * wraps nor clips, so without this the name runs out over the player's own
     * inventory and off the screen.
     */
    private void line(GuiGraphics graphics, Component text, int y, int color, boolean shadow) {
        if (this.font.width(text) <= TEXT_WIDTH) {
            graphics.drawString(this.font, text, TEXT_LEFT, y, color, shadow);
            return;
        }
        FormattedText clipped = FormattedText.composite(
                this.font.substrByWidth(text, TEXT_WIDTH - this.font.width(ELLIPSIS)), ELLIPSIS);
        graphics.drawString(
                this.font, Language.getInstance().getVisualOrder(clipped), TEXT_LEFT, y, color, shadow);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Without this, typing a quantity that contains the inventory key closes
        // the screen mid-number.
        if (this.lots.isFocused() && this.lots.canConsumeInput() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            return this.lots.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
