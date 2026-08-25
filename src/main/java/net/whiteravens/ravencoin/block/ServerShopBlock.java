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
package net.whiteravens.ravencoin.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * The operator's shop: the same counter with the back wall knocked out.
 *
 * <p>It never runs out, it has no owner and it keeps no chest — the payment it
 * takes leaves the economy entirely, which is what makes this the other end of
 * the money supply from the minting recipe. Only an operator can set one up.
 *
 * <p>Not breakable in survival, on purpose. A server shop is map furniture: it
 * is usually standing in a spawn town that players can reach, and one that could
 * be mined is one that will be.
 */
public class ServerShopBlock extends ShopBlock {
    public static final MapCodec<ServerShopBlock> CODEC = simpleCodec(ServerShopBlock::new);

    public ServerShopBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
