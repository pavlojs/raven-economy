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

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.config.RavenCoinConfig;
import net.whiteravens.ravencoin.economy.Amounts;
import net.whiteravens.ravencoin.menu.AtmMenu;
import net.whiteravens.ravencoin.network.AtmActionPayload;
import net.whiteravens.ravencoin.network.AtmListPayload;
import net.whiteravens.ravencoin.network.AtmNoticePayload;
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

    /**
     * The statement is set smaller than the rest of the screen.
     *
     * <p>Its lines are the longest anything here produces — a time, a verb and
     * whatever the other side was called — and three quarters buys back a third
     * of the characters. Below this it stops being reading and starts being
     * texture, which is what the tooltip is for.
     */
    private static final float STATEMENT_SCALE = 0.75F;

    private static final int STATEMENT_PITCH = 10;

    private static final int STATEMENT_ROWS = 11;

    /** Where an outcome from the server is written, above the footer. */
    private static final int NOTICE_Y = 162;

    /** How many lines a notice may wrap onto. Two is what fits above the footer. */
    private static final int NOTICE_LINES = 2;

    /** How many a lone row on an otherwise empty page may take. It has the page. */
    private static final int NOTICE_ROW_LINES = 3;

    /** The gap between wrapped notice lines: the font's own height plus a pixel. */
    private static final int NOTICE_PITCH = 10;

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
    private Component notice;

    private boolean noticeIsError;

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

    /**
     * Takes an outcome and shows it where the button was.
     *
     * <p>Nothing is cleared on a timer: a refusal stays until the player does
     * something else, because a message that vanishes while you are reading it
     * is the same as no message.
     */
    public static void notice(AtmNoticePayload payload, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof AtmScreen screen) {
            screen.notice = payload.text();
            screen.noticeIsError = payload.error();
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
        List<View> entries = new ArrayList<>(List.of(View.BANK, View.TRANSFER, View.HISTORY, View.RANKS));
        if (RavenCoinConfig.CLIENT.showLeaderboard.get()) {
            entries.add(View.TOP);
        }
        // Spread over the whole content area rather than stacked at the top, so
        // the menu fills the panel whether or not the leaderboard is on it.
        int pitch = (FOOTER_Y - CONTENT_TOP - 18) / Math.max(1, entries.size() - 1);
        for (int i = 0; i < entries.size(); i++) {
            View entry = entries.get(i);
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
        this.notice = null;
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
        if (amount <= 0) {
            this.refuse("screen.ravencoin.atm.error.no_amount");
            return;
        }
        PacketDistributor.sendToServer(new AtmActionPayload(action, amount));
        this.clear(this.amountField);
    }

    /**
     * Sends a transfer, once the two halves the client can check are filled in.
     *
     * <p>Whether the name belongs to anybody is not one of them — only the
     * server holds the accounts — so that half comes back as a refusal under the
     * field. What is checked here is only what a button doing nothing would
     * otherwise leave the player guessing about.
     */
    private void transfer() {
        String payee = this.playerField == null ? "" : this.playerField.getValue().trim();
        if (payee.isEmpty()) {
            this.refuse("screen.ravencoin.atm.error.no_player");
            return;
        }
        long amount = this.amount();
        if (amount <= 0) {
            this.refuse("screen.ravencoin.atm.error.no_amount");
            return;
        }
        PacketDistributor.sendToServer(new AtmTransferPayload(payee, amount));
        this.clear(this.amountField);
    }

    private void refuse(String key) {
        this.notice = Component.translatable(key);
        this.noticeIsError = true;
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
            case RANKS -> this.drawRows(graphics, RANK_PITCH, 5, this.rows.size(), 0, 1.0F);
            case TOP -> this.drawRows(graphics, TEXT_PITCH, 2, this.rows.size(), 0, 1.0F);
            case HISTORY -> this.drawRows(
                    graphics,
                    STATEMENT_PITCH,
                    1,
                    Math.min(STATEMENT_ROWS, this.rows.size() - this.scroll),
                    this.scroll,
                    STATEMENT_SCALE);
            default -> { }
        }

        if (this.notice != null) {
            // Wrapped, not clipped. These are sentences borrowed from the chat
            // lines the commands print, and measured against this panel six of
            // the seven the transfer page can produce are wider than 160px:
            // "Nikt o tej nazwie nie ma tu kont…" is not an answer. The last
            // line sits on NOTICE_Y so a one-line notice has not moved.
            List<FormattedCharSequence> lines = this.font.split(this.notice, FULL_WIDTH);
            int shown = Math.min(lines.size(), NOTICE_LINES);
            int ink = this.noticeIsError ? 0xAA0000 : 0x006622;
            for (int i = 0; i < shown; i++) {
                graphics.drawString(
                        this.font, lines.get(i), MARGIN, NOTICE_Y - (shown - 1 - i) * NOTICE_PITCH, ink, false);
            }
        }

        Branding.draw(graphics, this.font, DETAIL_RIGHT + 2, this.imageHeight - 20);
    }

    /**
     * Draws {@code count} rows starting at {@code from}.
     *
     * <p>Everything is divided by the scale rather than the scale being applied
     * afterwards: inside a scaled pose every coordinate is in the scaled space,
     * so a row drawn at the panel's own numbers would land somewhere else
     * entirely. At scale 1 the division is identity and this is the plain path.
     *
     * @param textOffset how far below the row's top the text sits, so a line
     *                   beside an 18-pixel button is centred against it
     */
    private void drawRows(GuiGraphics graphics, int pitch, int textOffset, int count, int from, float scale) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int labelX = Math.round(LABEL_X / scale);

        for (int i = 0; i < count; i++) {
            AtmListPayload.Row row = this.rows.get(from + i);
            int y = Math.round((CONTENT_TOP + i * pitch + textOffset) / scale);
            // A page whose only row carries no number is not a table, it is a
            // sentence — "Brak wtyczki uprawnień, więc rangi nie mają jak zostać
            // nadane" is 300px, and clipped to "Brak wtyczki uprawnień, więc…"
            // it stops exactly before the part that says what to do. Given the
            // whole panel and two lines, it fits.
            if (this.rows.size() == 1 && row.detail().getString().isEmpty()) {
                int room = Math.round(DETAIL_RIGHT / scale) - labelX;
                List<FormattedCharSequence> wrapped = this.font.split(row.label(), room);
                for (int line = 0; line < Math.min(wrapped.size(), NOTICE_ROW_LINES); line++) {
                    graphics.drawString(
                            this.font, wrapped.get(line), labelX, y + line * NOTICE_PITCH, 0x404040, false);
                }
                continue;
            }
            int right = Math.round((row.actionable() ? DETAIL_RIGHT_WITH_BUTTON : DETAIL_RIGHT) / scale);
            // The right-hand half is measured first and takes what it needs, up
            // to leaving the label a readable stub. It is the half that carries
            // the number, and a clipped price says less than a clipped name.
            int detailRoom = right - labelX - Math.round(MIN_LABEL / scale);
            int detailWidth = Math.min(this.font.width(row.detail()), detailRoom);
            Labels.drawRight(graphics, this.font, row.detail(), right, y, detailRoom, 0x555555);
            Labels.draw(
                    graphics,
                    this.font,
                    row.label(),
                    labelX,
                    y,
                    right - detailWidth - labelX - Math.round(4 / scale),
                    0x404040,
                    false);
        }
        graphics.pose().popPose();
    }

    /**
     * The whole of a row, on hover.
     *
     * <p>The statement is the page that needs this: its lines are set small and
     * clipped, and "Kupno: Yellow Laser…" is exactly the half of a line somebody
     * wants the rest of. Drawn from the unclipped components, so the tooltip is
     * the row rather than a longer guess at it.
     */
    private void rowTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int pitch = this.view == View.RANKS ? RANK_PITCH : this.view == View.HISTORY ? STATEMENT_PITCH : TEXT_PITCH;
        int shown = this.view == View.HISTORY
                ? Math.min(STATEMENT_ROWS, this.rows.size() - this.scroll)
                : this.rows.size();
        int x = mouseX - this.leftPos;
        int y = mouseY - this.topPos - CONTENT_TOP;
        if (x < MARGIN || x > MARGIN + FULL_WIDTH || y < 0) {
            return;
        }
        int index = y / pitch;
        if (index < 0 || index >= shown) {
            return;
        }
        AtmListPayload.Row row = this.rows.get(this.scroll + index);
        graphics.renderTooltip(
                this.font,
                List.of(row.label().getVisualOrderText(), row.detail().getVisualOrderText()),
                mouseX,
                mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.view.page != null) {
            this.rowTooltip(graphics, mouseX, mouseY);
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
