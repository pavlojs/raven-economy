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
import net.minecraft.network.chat.Component;
import net.whiteravens.ravencoin.config.RavenCoinConfig;

/**
 * The mark in the corner of every screen this mod draws.
 *
 * <p>Set at half scale, which is the whole idea: at four pixels tall it reads as
 * the printing on the front of a cash machine rather than as a label competing
 * with the numbers. Full-size it would be 150 pixels wide on a 176-pixel panel
 * and there is nowhere on any of the three screens to put that.
 *
 * <p>Not translated. It is a name, and a bank's name does not change language.
 *
 * <p>Client only, and it has to stay that way — {@link RavenCoinConfig#CLIENT}
 * is not loaded on a dedicated server and throws if it is asked for a value
 * there.
 */
public final class Branding {
    private static final Component MARK = Component.literal("White Ravens Financial Systems");

    /** Half size. Any smaller and the strokes fall between pixels and turn to fog. */
    private static final float SCALE = 0.5F;

    /** Grey enough to recede, dark enough to survive being drawn at half height. */
    private static final int COLOUR = 0x6E6E6E;

    /**
     * Draws the mark with its right edge at {@code right} and its top at
     * {@code top}, in the panel's own coordinates.
     *
     * <p>Both are divided by the scale rather than the string being measured
     * against the unscaled panel: inside a scaled pose every coordinate is in
     * the scaled space, and mixing the two is how a corner mark ends up in the
     * middle of a screen.
     */
    public static void draw(GuiGraphics graphics, Font font, int right, int top) {
        if (!RavenCoinConfig.CLIENT.showBranding.get()) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().scale(SCALE, SCALE, 1.0F);
        graphics.drawString(
                font,
                MARK,
                Math.round(right / SCALE) - font.width(MARK),
                Math.round(top / SCALE),
                COLOUR,
                false);
        graphics.pose().popPose();
    }

    private Branding() {}
}
