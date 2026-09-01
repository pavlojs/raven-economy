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

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.economy.Amounts;
import net.whiteravens.ravencoin.menu.AtmMenu;
import net.whiteravens.ravencoin.network.AtmActionPayload;
import net.whiteravens.ravencoin.network.AtmListPayload;
import net.whiteravens.ravencoin.network.AtmRankBuyPayload;
import net.whiteravens.ravencoin.network.AtmRequestPayload;
import net.whiteravens.ravencoin.network.AtmTransferPayload;
import net.whiteravens.ravencoin.network.Page;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The bank terminal.
 *
 * <p>A menu and five pages rather than one screen of controls, because the bank
 * does five different things and a screen that showed all of them at once would
 * be a wall of fields. It has no inventory grid: what the player is carrying is
 * counted for them on the display, and a slot they could drop a stack into would
 * be a second way to bank money with none of the accounting that
 * {@link AtmMenu#deposit} does.
 *
 * <p>It computes nothing about any outcome. Pressing a button says what was
 * asked for; the balance on the display is whatever the server last sent through
 * the menu's data slots, and the two list pages are filled in by the server
 * because their contents — which rungs are yours, what your statement says — are
 * not things a client is told otherwise.
 */
public class AtmScreen extends AbstractContainerScreen<AtmMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "textures/gui/atm.png");

    /** Refuse to even send anything longer than a balance could ever be. */
    private static final int MAX_DIGITS = 19;

    private static final int MARGIN = 8;
    private static final int FULL_WIDTH = 160;
    private static final int CONTENT_TOP = 56;
    private static final int FOOTER_Y = 178;

    /** Rank rows carry a button, so they are as tall as one. */
    private static final int RANK_PITCH = 20;

    /** Read-only rows are just text, and pack in twice as tight. */
    private static final int TEXT_PITCH = 12;

    private static final int STATEMENT_ROWS = 10;

    /** Where a row's right-hand half ends, and where it ends when a button follows it. */
    private static final int DETAIL_RIGHT = 166;

    private static final int DETAIL_RIGHT_WITH_BUTTON = 118;

    private static final int LABEL_X = 12;

    /** However long the right-hand half is, the name keeps at least this much. */
    private static final int MIN_LABEL = 34;

    private View view = View.MENU;
    private List<AtmListPayload.Row> rows = List.of();
    private int scroll;

    @Nullable
    private EditBox amountField;

    @Nullable
    private EditBox playerField;

    public AtmScreen(AtmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    /**
     * Takes a page the server has filled in.
     *
     * <p>Static because a packet handler has no screen to be called on. If the
     * player has since closed the bank or walked to a different page, the answer
     * to a question they are no longer asking is dropped.
     */
    public static void accept(AtmListPayload payload, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof AtmScreen screen && screen.view.page == payload.page()) {
            screen.rows = payload.rows();
            screen.scroll = Math.min(screen.scroll, Math.max(0, payload.rows().size() - 1));
            screen.rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        super.init();
        this.amountField = null;
        this.playerField = null;

        switch (this.view) {
            case MENU -> this.menuPage();
            case BANK -> this.bankPage();
            case TRANSFER -> this.transferPage();
            case HISTORY -> this.statementPage();
            case RANKS -> this.rankPage();
            case TOP -> this.listFooter();
        }
    }

    // ------------------------------------------------------------------ pages

    private void menuPage() {
        View[] entries = {View.BANK, View.TRANSFER, View.HISTORY, View.RANKS, View.TOP};
        // Spread over the whole content area rather than stacked at the top:
        // five buttons and a footer's worth of room, so they use it.
        int pitch = (FOOTER_Y - CONTENT_TOP - 18) / (entries.length - 1);
        for (int i = 0; i < entries.length; i++) {
            View entry = entries[i];
            this.addRenderableWidget(Button.builder(
                            Component.translatable(entry.key), button -> this.open(entry))
                    .bounds(this.leftPos + MARGIN, this.topPos + CONTENT_TOP + i * pitch, FULL_WIDTH, 18)
                    .build());
        }
    }

    private void bankPage() {
        this.amountField = this.field(MARGIN, CONTENT_TOP, FULL_WIDTH, "screen.ravencoin.atm.amount");
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.ravencoin.atm.deposit"),
                        button -> this.send(AtmActionPayload.Action.DEPOSIT))
                .bounds(this.leftPos + MARGIN, this.topPos + CONTENT_TOP + 22, 78, 18)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.ravencoin.atm.withdraw"),
                        button -> this.send(AtmActionPayload.Action.WITHDRAW))
                .bounds(this.leftPos + 90, this.topPos + CONTENT_TOP + 22, 78, 18)
                .build());
        this.backButton(MARGIN, FULL_WIDTH);
    }

    private void transferPage() {
        this.playerField = this.field(MARGIN, CONTENT_TOP, FULL_WIDTH, "screen.ravencoin.atm.player");
        this.playerField.setFilter(text -> text.length() <= 32);
        this.playerField.setMaxLength(32);
        this.amountField = this.field(MARGIN, CONTENT_TOP + 22, FULL_WIDTH, "screen.ravencoin.atm.amount");
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.ravencoin.atm.send"), button -> this.transfer())
                .bounds(this.leftPos + MARGIN, this.topPos + CONTENT_TOP + 44, FULL_WIDTH, 18)
                .build());
        this.backButton(MARGIN, FULL_WIDTH);
    }

    private void rankPage() {
        int shown = Math.min(this.rows.size(), (FOOTER_Y - CONTENT_TOP - 4) / RANK_PITCH);
        for (int i = 0; i < shown; i++) {
            AtmListPayload.Row row = this.rows.get(i);
            if (!row.actionable()) {
                continue;
            }
            this.addRenderableWidget(Button.builder(
                            Component.translatable("screen.ravencoin.atm.rank.buy"),
                            button -> this.buy(row.action()))
                    .bounds(this.leftPos + 122, this.topPos + CONTENT_TOP + i * RANK_PITCH, 46, 18)
                    .build());
        }
        this.listFooter();
    }

    /**
     * The statement, ten lines at a time.
     *
     * <p>Twenty are kept and ten fit, so the two arrows are the whole of the
     * paging: there is never a third page, and a scrollbar for one press would
     * be more machinery than the thing it scrolls.
     */
    private void statementPage() {
        boolean paged = this.rows.size() > STATEMENT_ROWS;
        if (!paged) {
            this.listFooter();
            return;
        }
        this.addRenderableWidget(Button.builder(Component.literal("◀"), button -> this.page(-1))
                .bounds(this.leftPos + MARGIN, this.topPos + FOOTER_Y, 20, 18)
                .build());
        this.backButton(32, 112);
        this.addRenderableWidget(Button.builder(Component.literal("▶"), button -> this.page(1))
                .bounds(this.leftPos + 148, this.topPos + FOOTER_Y, 20, 18)
                .build());
    }

    private void listFooter() {
        this.backButton(MARGIN, FULL_WIDTH);
    }

    private void backButton(int x, int width) {
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.ravencoin.atm.back"), button -> this.open(View.MENU))
                .bounds(this.leftPos + x, this.topPos + FOOTER_Y, width, 18)
                .build());
    }

    private EditBox field(int x, int y, int width, String key) {
        EditBox box = new EditBox(
                this.font, this.leftPos + x, this.topPos + y, width, 16, Component.translatable(key));
        box.setMaxLength(MAX_DIGITS);
        box.setFilter(AtmScreen::isDigits);
        box.setHint(Component.translatable(key));
        this.addRenderableWidget(box);
        return box;
    }

    // ----------------------------------------------------------------- intent

    private void open(View next) {
        this.view = next;
        this.rows = List.of();
        this.scroll = 0;
        if (next.page != null) {
            PacketDistributor.sendToServer(new AtmRequestPayload(next.page));
        }
        this.rebuildWidgets();
    }

    private void page(int delta) {
        int limit = Math.max(0, this.rows.size() - STATEMENT_ROWS);
        this.scroll = Math.clamp(this.scroll + delta * STATEMENT_ROWS, 0, limit);
    }

    private void send(AtmActionPayload.Action action) {
        long amount = this.amount();
        if (amount > 0) {
            PacketDistributor.sendToServer(new AtmActionPayload(action, amount));
            this.clear(this.amountField);
        }
    }

    private void transfer() {
        long amount = this.amount();
        String payee = this.playerField == null ? "" : this.playerField.getValue().trim();
        if (amount > 0 && !payee.isEmpty()) {
            PacketDistributor.sendToServer(new AtmTransferPayload(payee, amount));
            this.clear(this.amountField);
        }
    }

    private void buy(String rank) {
        PacketDistributor.sendToServer(new AtmRankBuyPayload(rank));
    }

    private long amount() {
        if (this.amountField == null) {
            return 0;
        }
        try {
            // Nineteen digits fit in a long only sometimes, so the overflow is
            // caught here rather than sent for the server to reject.
            return Long.parseLong(this.amountField.getValue().trim());
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }

    private void clear(@Nullable EditBox box) {
        if (box != null) {
            box.setValue("");
        }
    }

    private static boolean isDigits(String text) {
        return text.chars().allMatch(Character::isDigit);
    }

    // --------------------------------------------------------------- drawing

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    /**
     * Draws the display and whichever page is open.
     *
     * <p>Deliberately does not call {@code super}: that draws the inventory's
     * name, and this screen has no inventory to name.
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, Component.translatable(this.view.title), 8, 6, 0x404040, false);
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

        switch (this.view) {
            case RANKS -> this.drawRows(graphics, RANK_PITCH, 5, this.rows.size(), 0);
            case TOP -> this.drawRows(graphics, TEXT_PITCH, 2, this.rows.size(), 0);
            case HISTORY -> this.drawRows(
                    graphics, TEXT_PITCH, 2, Math.min(STATEMENT_ROWS, this.rows.size() - this.scroll), this.scroll);
            default -> { }
        }

    }

    /**
     * Draws {@code count} rows starting at {@code from}.
     *
     * @param textOffset how far below the row's top the 8-pixel text sits, so a
     *                   line beside an 18-pixel button is centred against it
     */
    private void drawRows(GuiGraphics graphics, int pitch, int textOffset, int count, int from) {
        for (int i = 0; i < count; i++) {
            AtmListPayload.Row row = this.rows.get(from + i);
            int y = CONTENT_TOP + i * pitch + textOffset;
            int right = row.actionable() ? DETAIL_RIGHT_WITH_BUTTON : DETAIL_RIGHT;
            // The right-hand half is measured first and takes what it needs, up
            // to leaving the label a readable stub. It is the half that carries
            // the number, and a clipped price says less than a clipped name.
            int detailRoom = right - LABEL_X - MIN_LABEL;
            int detailWidth = Math.min(this.font.width(row.detail()), detailRoom);
            Labels.drawRight(graphics, this.font, row.detail(), right, y, detailRoom, 0x555555);
            Labels.draw(
                    graphics,
                    this.font,
                    row.label(),
                    LABEL_X,
                    y,
                    right - detailWidth - LABEL_X - 4,
                    0x404040,
                    false);
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Without this, typing an amount that contains the inventory key closes
        // the screen mid-number.
        for (EditBox box : new EditBox[] {this.amountField, this.playerField}) {
            if (box != null && box.isFocused() && box.canConsumeInput() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
                return box.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        // Escape steps back to the menu before it closes the bank, which is what
        // every other machine with pages in this pack does.
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.view != View.MENU) {
            this.open(View.MENU);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * One page of the terminal.
     *
     * <p>Each carries what its menu button says, what the screen is called while
     * it is open, and the list the server has to send — null for the three pages
     * the client can draw entirely on its own.
     */
    private enum View {
        MENU("screen.ravencoin.atm.menu", "container.ravencoin.atm", null),
        BANK("screen.ravencoin.atm.bank", "screen.ravencoin.atm.bank", null),
        TRANSFER("screen.ravencoin.atm.transfer", "screen.ravencoin.atm.transfer", null),
        HISTORY("screen.ravencoin.atm.history", "screen.ravencoin.atm.history", Page.HISTORY),
        RANKS("screen.ravencoin.atm.ranks", "screen.ravencoin.atm.ranks", Page.RANKS),
        TOP("screen.ravencoin.atm.top", "screen.ravencoin.atm.top", Page.TOP);

        private final String key;
        private final String title;

        @Nullable
        private final Page page;

        View(String key, String title, @Nullable Page page) {
            this.key = key;
            this.title = title;
            this.page = page;
        }
    }
}
