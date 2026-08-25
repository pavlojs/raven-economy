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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.whiteravens.ravencoin.block.entity.ShopBlockEntity;
import net.whiteravens.ravencoin.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * A player's shop: a counter that sells whatever is in the container behind it.
 *
 * <p>Which screen a right-click opens depends only on who is clicking. The owner
 * gets the settings; everyone else gets the shop. An owner who wants to see
 * their own shop the way a customer does sneaks — otherwise there would be no
 * way to check a price without an operator standing next to you.
 */
public class ShopBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<ShopBlock> CODEC = simpleCodec(ShopBlock::new);

    public ShopBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShopBlockEntity(pos, state);
    }

    // Registered BlockEntityTypes are registry singletons and do not override
    // equals, so identity is the comparison — `.equals` here would be the same
    // check wearing a hat. Vanilla's own createTickerHelper does exactly this;
    // it is just not reachable, because this block extends
    // HorizontalDirectionalBlock for its FACING property rather than BaseEntityBlock.
    @SuppressWarnings("ReferenceEquality")
    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.SHOP.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) ->
                ShopBlockEntity.serverTick(tickLevel, pos, tickState, (ShopBlockEntity) entity);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ShopBlockEntity shop) || !(player instanceof ServerPlayer opener)) {
            return InteractionResult.PASS;
        }
        if (shop.mayConfigure(player) && !player.isShiftKeyDown()) {
            shop.openSettings(opener);
        } else {
            shop.openTrade(opener);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Records the owner and puts the settings screen in front of them.
     *
     * <p>Opening it here rather than waiting to be asked is the whole of this
     * block's tutorial: a shop that has not been told what it sells does nothing
     * at all, and a player who placed one and walked away would have no reason
     * to suspect there was a screen behind it. Skipped for the operator's shop,
     * which has no owner and is usually being placed in the middle of a build.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof ShopBlockEntity shop)) {
            return;
        }
        if (!shop.admin() && placer instanceof ServerPlayer player) {
            shop.claim(player);
            shop.openSettings(player);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
