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
package net.whiteravens.ravencoin.block.entity;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.whiteravens.ravencoin.block.ServerShopBlock;
import net.whiteravens.ravencoin.config.RavenCoinConfig;
import net.whiteravens.ravencoin.menu.ShopConfigMenu;
import net.whiteravens.ravencoin.menu.ShopMenu;
import net.whiteravens.ravencoin.rank.Rank;
import net.whiteravens.ravencoin.rank.RankService;
import net.whiteravens.ravencoin.registry.ModBlockEntities;
import net.whiteravens.ravencoin.shop.ShopResult;
import net.whiteravens.ravencoin.shop.ShopStock;
import org.jetbrains.annotations.Nullable;

/**
 * One shop: what it sells, what it charges, and who is allowed to change that.
 *
 * <p>Both shop blocks share this class. A server shop is the same machine with
 * its stock question answered differently — it never runs out and the payment
 * leaves the economy — and the handful of places that care ask {@link #admin()}
 * rather than being written twice.
 *
 * <p><b>Counts are fields, not stack sizes.</b> A price of 240 RavenCoin does
 * not fit in an {@link ItemStack}, and a shop that could only ever charge 64 of
 * something would be useless on a server whose season target is a billion. The
 * stacks here say <em>what</em>; the ints say <em>how many</em>.
 *
 * <p>A player shop keeps nothing itself: goods come out of the container beside
 * it and payment goes back into the same one. That container is found by looking
 * around rather than fixed at placement, so a chest added later is picked up on
 * the next tick without the shop having to be rebuilt.
 */
public class ShopBlockEntity extends BlockEntity {
    /** The largest per-trade amount an owner can type. Well past any real price, well short of overflow. */
    public static final int MAX_UNITS = 1_000_000;

    /** How many complete trades the label will admit to. Beyond this the number stops being information. */
    private static final int MAX_TRADES_SHOWN = 99_999;

    /** A server shop's stock, which is not a number. */
    public static final int UNLIMITED = -1;

    /** Stock is recounted this often. Once a second is faster than a chest realistically empties. */
    private static final int RECOUNT_TICKS = 20;

    /** Looked at in this order, so a shop standing on a chest prefers the chest under it. */
    private static final Direction[] STOCK_SIDES = {
        Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP
    };

    private ItemStack product = ItemStack.EMPTY;
    private int productUnits = 1;
    private ItemStack price = ItemStack.EMPTY;
    private int priceUnits = 1;
    private String requiredRank = "";
    private boolean showLabel = true;

    private UUID owner;
    private String ownerName = "";

    @Nullable
    private Direction stockSide;

    private int trades;

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHOP.get(), pos, state);
    }

    /** {@return true when this is the operator's shop: infinite stock, no owner, no chest} */
    public boolean admin() {
        return this.getBlockState().getBlock() instanceof ServerShopBlock;
    }

    public ItemStack product() {
        return this.product;
    }

    public int productUnits() {
        return this.productUnits;
    }

    public ItemStack price() {
        return this.price;
    }

    public int priceUnits() {
        return this.priceUnits;
    }

    public String requiredRank() {
        return this.requiredRank;
    }

    public boolean showLabel() {
        return this.showLabel;
    }

    public String ownerName() {
        return this.ownerName;
    }

    /** {@return how many complete trades are in stock, or {@link #UNLIMITED}} */
    public int trades() {
        return this.trades;
    }

    /** {@return whether a player shop has found a container to work out of} */
    public boolean hasContainer() {
        return this.admin() || this.stockSide != null;
    }

    /** {@return whether this shop has been told what it trades} */
    public boolean configured() {
        return !this.product.isEmpty() && !this.price.isEmpty();
    }

    /** Records who placed this. Called once, from the block. */
    public void claim(Player player) {
        this.owner = player.getUUID();
        this.ownerName = player.getGameProfile().getName();
        this.setChanged();
    }

    /**
     * {@return whether this player may change what the shop trades}
     *
     * <p>Operators can edit anything, which is what makes a shop left behind by
     * a player who never came back fixable at all.
     */
    public boolean mayConfigure(Player player) {
        if (player.hasPermissions(2)) {
            return true;
        }
        return !this.admin() && player.getUUID().equals(this.owner);
    }

    /** Opens the buying screen. */
    public void openTrade(ServerPlayer player) {
        player.openMenu(
                new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return ShopBlockEntity.this.title();
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player opener) {
                        return new ShopMenu(id, inventory, ShopBlockEntity.this.worldPosition);
                    }
                },
                this.worldPosition);
    }

    /** Opens the owner's settings screen. */
    public void openSettings(ServerPlayer player) {
        player.openMenu(
                new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("container.ravencoin.shop.settings");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player opener) {
                        return new ShopConfigMenu(id, inventory, ShopBlockEntity.this.worldPosition);
                    }
                },
                this.worldPosition);
    }

    private Component title() {
        if (this.admin()) {
            return Component.translatable("block.ravencoin.server_shop");
        }
        return this.ownerName.isEmpty()
                ? Component.translatable("block.ravencoin.shop")
                : Component.translatable("container.ravencoin.shop.of", this.ownerName);
    }

    /**
     * Copies whatever the player is holding on their cursor into one of the two
     * settings stacks.
     *
     * <p>The stack is read from the server's own copy of the carried item rather
     * than sent by the client, which is the difference between a player choosing
     * from what they are physically holding and a client naming any item in the
     * game.
     */
    public void pick(Player player, boolean forPrice) {
        ItemStack carried = player.containerMenu.getCarried();
        ItemStack chosen = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
        if (forPrice) {
            this.price = chosen;
        } else {
            this.product = chosen;
        }
        this.setChangedAndSync();
    }

    /** Applies everything the settings screen can change in one go. */
    public void configure(int productUnits, int priceUnits, String rank, boolean label) {
        this.productUnits = Math.clamp(productUnits, 1, MAX_UNITS);
        this.priceUnits = Math.clamp(priceUnits, 1, MAX_UNITS);
        this.requiredRank = RankService.find(rank.trim()).map(Rank::id).orElse("");
        this.showLabel = label;
        this.setChangedAndSync();
    }

    /**
     * Sells up to {@code wanted} lots to this player.
     *
     * <p>Everything is checked before anything moves — stock, wallet, the buyer's
     * free slots and the shop's free slots — and the trade is then cut down to
     * however many of those all agree on. A half-finished trade is the one
     * outcome this must never produce, because the halves are somebody's money.
     */
    public Outcome buy(ServerPlayer buyer, int wanted) {
        if (!RavenCoinConfig.COMMON.shopsEnabled.get()) {
            return new Outcome(ShopResult.DISABLED, 0);
        }
        if (!this.configured()) {
            return new Outcome(ShopResult.NOT_SET_UP, 0);
        }
        if (!this.rankSatisfied(buyer)) {
            return new Outcome(ShopResult.RANK_REQUIRED, 0);
        }

        IItemHandler till = this.container();
        if (!this.admin() && till == null) {
            return new Outcome(ShopResult.NO_CONTAINER, 0);
        }

        IItemHandler pockets = ShopStock.pockets(buyer);
        long batch = Math.clamp(wanted, 1, MAX_TRADES_SHOWN);
        long inStock = this.admin() ? batch : ShopStock.count(till, this.product) / this.productUnits;
        long affordable = ShopStock.count(pockets, this.price) / this.priceUnits;
        long carryable = ShopStock.room(pockets, this.product) / this.productUnits;
        long tillRoom = this.admin() ? batch : ShopStock.room(till, this.price) / this.priceUnits;

        long lots = Math.min(Math.min(batch, inStock), Math.min(affordable, Math.min(carryable, tillRoom)));
        if (lots <= 0) {
            return new Outcome(this.refusal(inStock, affordable, carryable), 0);
        }

        long paid = lots * this.priceUnits;
        long got = lots * this.productUnits;

        // Goods out of the shop first, then payment out of the buyer. Each step
        // checks what it actually moved and puts it back if it came up short,
        // because the counts above are a photograph and this is the exchange —
        // and a trade that takes one half without giving the other is somebody's
        // money gone. None of these branches should be reachable; they are here
        // because "should not be reachable" is what the last money bug said too.
        if (!this.admin()) {
            long pulled = ShopStock.take(till, this.product, got);
            if (pulled < got) {
                // Put back what came out, not what was asked for. Restoring the
                // request would hand the shop goods it never had.
                this.restock(till, this.product, pulled);
                return new Outcome(ShopResult.OUT_OF_STOCK, 0);
            }
        }

        long taken = ShopStock.take(pockets, this.price, paid);
        if (taken < paid) {
            // Reported as no room, not as no money: the wallet was counted a few
            // lines ago and had enough. What is missing is a free slot for the
            // change from a broken coin block, and telling a player with 540 RC
            // that they cannot afford 100 sends them to find money they already
            // have.
            this.give(buyer, pockets, this.price, taken);
            if (!this.admin()) {
                this.restock(till, this.product, got);
            }
            return new Outcome(ShopResult.NO_ROOM, 0);
        }

        if (!this.admin()) {
            long unbanked = ShopStock.put(till, this.price, paid);
            if (unbanked > 0) {
                // The room check said this would fit. If it somehow did not, the
                // owner finds their money on the floor rather than nowhere.
                ShopStock.spill(this.level, this.worldPosition, this.price, unbanked);
            }
        }

        this.give(buyer, pockets, this.product, got);
        this.refresh();
        return new Outcome(ShopResult.OK, (int) lots);
    }

    /** Hands units to the buyer, dropping at their feet whatever will not fit. */
    private void give(ServerPlayer buyer, IItemHandler pockets, ItemStack good, long units) {
        long leftover = ShopStock.put(pockets, good, units);
        if (leftover > 0) {
            ShopStock.spill(buyer.level(), buyer.blockPosition(), good, leftover);
        }
    }

    /** Puts goods back in the till after a called-off trade, spilling what no longer fits. */
    private void restock(IItemHandler till, ItemStack good, long units) {
        long leftover = ShopStock.put(till, good, units);
        if (leftover > 0) {
            ShopStock.spill(this.level, this.worldPosition, good, leftover);
        }
    }

    /** What one press of the buy button actually did, in lots. */
    public record Outcome(ShopResult result, int lots) {}

    private ShopResult refusal(long inStock, long affordable, long carryable) {
        if (inStock <= 0) {
            return ShopResult.OUT_OF_STOCK;
        }
        if (affordable <= 0) {
            return ShopResult.CANNOT_PAY;
        }
        return carryable <= 0 ? ShopResult.NO_ROOM : ShopResult.TILL_FULL;
    }

    private boolean rankSatisfied(ServerPlayer buyer) {
        if (this.requiredRank.isEmpty()) {
            return true;
        }
        return RankService.find(this.requiredRank)
                .map(rank -> RankService.owns(buyer, rank))
                .orElse(true);
    }

    /** {@return the container this shop works out of, or null for a server shop or a lonely one} */
    @Nullable
    private IItemHandler container() {
        if (this.admin() || this.level == null) {
            return null;
        }
        if (this.stockSide != null) {
            IItemHandler found = this.handlerAt(this.stockSide);
            if (found != null) {
                return found;
            }
        }
        for (Direction side : STOCK_SIDES) {
            IItemHandler found = this.handlerAt(side);
            if (found != null) {
                this.stockSide = side;
                return found;
            }
        }
        this.stockSide = null;
        return null;
    }

    @Nullable
    private IItemHandler handlerAt(Direction side) {
        return this.level.getCapability(
                Capabilities.ItemHandler.BLOCK, this.worldPosition.relative(side), side.getOpposite());
    }

    /** Recounts what the label shows. Cheap, and only as often as a label needs to be right. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ShopBlockEntity shop) {
        if (level.getGameTime() % RECOUNT_TICKS == 0) {
            shop.refresh();
        }
    }

    private void refresh() {
        int before = this.trades;
        Direction sideBefore = this.stockSide;

        if (this.admin()) {
            this.trades = UNLIMITED;
        } else if (!this.configured()) {
            this.trades = 0;
            this.container();
        } else {
            IItemHandler till = this.container();
            long available = till == null ? 0 : ShopStock.count(till, this.product) / this.productUnits;
            this.trades = (int) Math.min(available, MAX_TRADES_SHOWN);
        }

        if (before != this.trades || sideBefore != this.stockSide) {
            this.setChangedAndSync();
        }
    }

    private void setChangedAndSync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(
                    this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.write(tag, registries);
        if (this.owner != null) {
            tag.putUUID("Owner", this.owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.product = tag.contains("Product") ? ItemStack.parseOptional(registries, tag.getCompound("Product")) : ItemStack.EMPTY;
        this.price = tag.contains("Price") ? ItemStack.parseOptional(registries, tag.getCompound("Price")) : ItemStack.EMPTY;
        this.productUnits = Math.clamp(tag.getInt("ProductUnits"), 1, MAX_UNITS);
        this.priceUnits = Math.clamp(tag.getInt("PriceUnits"), 1, MAX_UNITS);
        this.requiredRank = tag.getString("Rank");
        this.showLabel = !tag.contains("Label") || tag.getBoolean("Label");
        this.ownerName = tag.getString("OwnerName");
        this.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.trades = tag.getInt("Trades");
        // from3DDataValue rather than values()[…]: this byte comes off disk, and a
        // corrupt or hand-edited one indexed straight into the array throws while
        // the block entity is loading, which fails the whole chunk. The 3D data
        // value is also the stable id — it happens to equal the ordinal today, so
        // this reads saves written by earlier versions unchanged.
        this.stockSide = tag.contains("StockSide") ? Direction.from3DDataValue(tag.getByte("StockSide")) : null;
    }

    private void write(CompoundTag tag, HolderLookup.Provider registries) {
        if (!this.product.isEmpty()) {
            tag.put("Product", this.product.save(registries, new CompoundTag()));
        }
        if (!this.price.isEmpty()) {
            tag.put("Price", this.price.save(registries, new CompoundTag()));
        }
        tag.putInt("ProductUnits", this.productUnits);
        tag.putInt("PriceUnits", this.priceUnits);
        tag.putString("Rank", this.requiredRank);
        tag.putBoolean("Label", this.showLabel);
        tag.putString("OwnerName", this.ownerName);
        tag.putInt("Trades", this.trades);
        if (this.stockSide != null) {
            tag.putByte("StockSide", (byte) this.stockSide.get3DDataValue());
        }
    }

    /**
     * The whole shop, sent to every client that can see it.
     *
     * <p>The label is drawn from this, so what is here is exactly what a passer-by
     * can read off the block — deliberately including the stock count and the
     * owner's name, and deliberately not including the owner's UUID.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.write(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
