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
package net.whiteravens.ravencoin.shop;

/**
 * Why a trade did or did not happen.
 *
 * <p>Text-free for the same reason {@link net.whiteravens.ravencoin.economy.TransactionResult}
 * is: the buyer's screen, the owner's screen and the chat line all want to word
 * a missing chest differently.
 */
public enum ShopResult {
    /** The goods and the payment changed hands. */
    OK,
    /** Nobody has told this shop what it sells yet. */
    NOT_SET_UP,
    /** A player shop with no container next to it has nowhere to keep stock. */
    NO_CONTAINER,
    /** The container has none of the goods left. */
    OUT_OF_STOCK,
    /** The container has no room for the payment. */
    TILL_FULL,
    /** The buyer does not have the price. */
    CANNOT_PAY,
    /** The buyer has nowhere to put what they just bought. */
    NO_ROOM,
    /** The shop is reserved for a rank the buyer does not hold. */
    RANK_REQUIRED,
    /** The operator switched shops off in the config. */
    DISABLED,
    /** Somebody else already rents this stall. */
    TAKEN,
    /** The rent has not been paid, so the stall is shut. */
    CLOSED
}
