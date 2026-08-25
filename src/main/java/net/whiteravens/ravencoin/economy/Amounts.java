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
package net.whiteravens.ravencoin.economy;

import java.util.Locale;

/** Turns a balance into something readable at nine digits. */
public final class Amounts {
    /**
     * Formats an amount with thousands separators.
     *
     * <p>Grouped with {@link Locale#ROOT} rather than the reader's locale on
     * purpose: the number is formatted on the server, which has no idea what
     * language any given player is running, and a balance that renders one way
     * in chat and another in the ATM would be worse than a comma in the wrong
     * place. The season's target is a billion, so the separators earn their
     * keep either way.
     */
    public static String format(long amount) {
        return String.format(Locale.ROOT, "%,d", amount);
    }

    private Amounts() {}
}
