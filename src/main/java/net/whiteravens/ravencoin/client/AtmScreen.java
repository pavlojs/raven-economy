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
import net.neoforged.neoforge.network.PacketDistributor;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.economy.Amounts;
import net.whiteravens.ravencoin.menu.AtmMenu;
import net.whiteravens.ravencoin.network.AtmActionPayload;
import org.lwjgl.glfw.GLFW;

/**
 * The ATM's screen.
 *
 * <p>Draws numbers and sends intent. It computes nothing about the outcome: the
 * balance shown is whatever the server last sent through the menu's data slots,
 * and pressing a button only says <em>what was asked for</em>. If the request is
 * refused, or only partly filled, the server says so — the screen never guesses.
 */
public class AtmScreen extends AbstractContainerScreen<AtmMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "textures/gui/atm.png");

    /** Refuse to even send anything longer than a balance could ever be. */
    private static final int MAX_DIGITS = 19;

    private EditBox amountField;

    public AtmScreen(AtmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        this.amountField = new EditBox(
                this.font,
                this.leftPos + 8,
                this.topPos + 50,
                160,
                16,
                Component.translatable("screen.ravencoin.atm.amount"));
        this.amountField.setMaxLength(MAX_DIGITS);
        this.amountField.setFilter(AtmScreen::isDigits);
        this.amountField.setHint(Component.translatable("screen.ravencoin.atm.amount"));
        this.addRenderableWidget(this.amountField);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.ravencoin.atm.deposit"),
                        button -> this.send(AtmActionPayload.Action.DEPOSIT))
                .bounds(this.leftPos + 8, this.topPos + 70, 78, 18)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.ravencoin.atm.withdraw"),
                        button -> this.send(AtmActionPayload.Action.WITHDRAW))
                .bounds(this.leftPos + 90, this.topPos + 70, 78, 18)
                .build());
    }

    private void send(AtmActionPayload.Action action) {
        long amount = parseAmount(this.amountField.getValue());
        if (amount <= 0) {
            return;
        }
        PacketDistributor.sendToServer(new AtmActionPayload(action, amount));
        this.amountField.setValue("");
    }

    /**
     * {@return the typed amount, or zero if it is not a usable number}
     *
     * <p>Nineteen digits fit in a {@code long} only sometimes, so the overflow is
     * caught here rather than sent for the server to reject.
     */
    private static long parseAmount(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }

    private static boolean isDigits(String text) {
        return text.chars().allMatch(Character::isDigit);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(
                this.font,
                Component.translatable("screen.ravencoin.atm.balance", Amounts.format(this.menu.balance())),
                12,
                21,
                0x404040,
                false);
        graphics.drawString(
                this.font,
                Component.translatable("screen.ravencoin.atm.carried", Amounts.format(this.menu.carried())),
                12,
                33,
                0x404040,
                false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Without this, typing an amount that contains the inventory key closes
        // the screen mid-number.
        if (this.amountField.isFocused() && this.amountField.canConsumeInput() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            return this.amountField.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
