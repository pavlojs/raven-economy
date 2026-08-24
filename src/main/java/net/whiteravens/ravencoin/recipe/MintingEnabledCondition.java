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
package net.whiteravens.ravencoin.recipe;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.whiteravens.ravencoin.config.RavenCoinConfig;

/**
 * Recipe condition backing {@code mintingEnabled}.
 *
 * <p>A condition rather than a runtime check on purpose: with minting off the
 * recipe does not exist, so it is absent from JEI too. A player never sees a
 * recipe the server will refuse — which is the difference between a rule and
 * a trap.
 */
public record MintingEnabledCondition() implements ICondition {
    public static final MintingEnabledCondition INSTANCE = new MintingEnabledCondition();
    public static final MapCodec<MintingEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(IContext context) {
        return RavenCoinConfig.COMMON.mintingEnabled.getAsBoolean();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
