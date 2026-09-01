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

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

/**
 * Text that stays inside the panel it was drawn on.
 *
 * <p>{@code GuiGraphics.drawString} neither wraps nor clips: a line too long for
 * the space it was given runs out across whatever is next to it and off the edge
 * of the screen. On these screens the long lines are not hypothetical — an item
 * name is whatever mod made it, a player's name is whatever they chose, and both
 * end up on a 176-pixel panel.
 */
public final class Labels {
    private static final FormattedText ELLIPSIS = FormattedText.of("…");

    /**
     * Draws one line, cut short with an ellipsis rather than off the panel.
     *
     * @return where the text ended, whether or not it had to be cut
     */
    public static int draw(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            int maxWidth,
            int colour,
            boolean shadow) {
        if (font.width(text) <= maxWidth) {
            return graphics.drawString(font, text, x, y, colour, shadow);
        }
        FormattedText clipped = FormattedText.composite(
                font.substrByWidth(text, maxWidth - font.width(ELLIPSIS)), ELLIPSIS);
        return graphics.drawString(font, Language.getInstance().getVisualOrder(clipped), x, y, colour, shadow);
    }

    /** Draws one line ending at {@code right}, cut short from the left edge in if it has to be. */
    public static void drawRight(
            GuiGraphics graphics, Font font, Component text, int right, int y, int maxWidth, int colour) {
        int width = Math.min(font.width(text), maxWidth);
        draw(graphics, font, text, right - width, y, maxWidth, colour, false);
    }

    private Labels() {}
}
