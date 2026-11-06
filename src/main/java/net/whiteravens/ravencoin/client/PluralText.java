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

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.whiteravens.ravencoin.economy.Amounts;

/**
 * A text that depends on a number, worded for that number in the player's language.
 *
 * <p>Minecraft's language files have no plurals, and two keys picked by
 * {@code count == 1} fit English and Polish only: in Russian 21 takes the form of
 * 1, and Czech has a form of its own for 2 to 4. So such a text is an ICU plural
 * with a case for each plural category of its language, as in
 * {@code {0, plural, one {# dzień} few {# dni} many {# dni} other {# dnia}}}, and
 * ICU picks the case by the rules of the language the player has chosen. ICU comes
 * with Minecraft's client and not with its server, which is why this lives here.
 */
public final class PluralText {
    /**
     * {@return the text under {@code key}, worded for {@code count}}
     *
     * <p>A text that does not parse shows the bare number: a translation with a typo
     * in it should cost a word, not the screen. {@code checkTranslations} keeps such
     * a text out of the jar.
     */
    public static Component of(String key, int count) {
        String pattern = Language.getInstance().getOrDefault(key);
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        try {
            ULocale locale = ULocale.forLanguageTag(language.replace('_', '-'));
            return Component.literal(new MessageFormat(pattern, locale).format(new Object[] {count}));
        } catch (IllegalArgumentException e) {
            return Component.literal(Amounts.format(count));
        }
    }

    private PluralText() {}
}
