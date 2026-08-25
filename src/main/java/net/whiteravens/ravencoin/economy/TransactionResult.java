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

/**
 * Why a transaction did or did not happen.
 *
 * <p>Returned rather than thrown, and deliberately free of any text: the same
 * value has to be renderable as a chat message for a command and as a line in
 * the ATM screen, and those want different wording. Each caller translates it
 * itself.
 */
public enum TransactionResult {
    /** The money moved. */
    OK,
    /** The amount was zero or negative. */
    INVALID_AMOUNT,
    /** The payer did not have that much. */
    INSUFFICIENT_FUNDS,
    /** The deposit would have pushed the balance past what a long can hold. */
    TOO_LARGE,
    /** The operator switched this off in the config. */
    DISABLED,
    /** Someone tried to pay themselves. */
    SAME_ACCOUNT;

    public boolean ok() {
        return this == OK;
    }
}
