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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.whiteravens.ravencoin.RavenCoin;
import net.whiteravens.ravencoin.block.entity.ShopBlockEntity;
import net.whiteravens.ravencoin.menu.ShopConfigMenu;
import net.whiteravens.ravencoin.network.ShopPickPayload;
import net.whiteravens.ravencoin.network.ShopSettingsPayload;
import net.whiteravens.ravencoin.shop.ShopText;
import org.lwjgl.glfw.GLFW;

/**
 * The owner's settings screen.
 *
 * <p>What is sold and what it costs are picked by clicking the two slots while
 * holding the item, the way you would show someone what you mean. The server
 * takes the item from its own copy of your cursor, so the two frames here are
 * pictures of a decision, not slots anything can be put into or taken out of.
 *
 * <p>Everything else applies together on one button. A shop that updated field
 * by field would spend the seconds between them selling the new goods at the
 * old price.
 */
public class ShopConfigScreen extends AbstractContainerScreen<ShopConfigMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RavenCoin.MOD_ID, "textures/gui/shop_config.png");

    private static final int PRODUCT_SLOT_X = 7;
    private static final int PRICE_SLOT_X = 91;
    private static final int SLOT_Y = 28;
    private static final int SLOT_SIZE = 18;

    /** Seven digits covers the million-unit ceiling the block entity enforces. */
    private static final int MAX_DIGITS = 7;

    private EditBox productUnits;
    private EditBox priceUnits;
    private EditBox rank;
    private Button label;
    private boolean showLabel = true;

    public ShopConfigScreen(ShopConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 216;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        ShopBlockEntity shop = this.menu.shop();

        this.productUnits = this.numberField(30, 30, shop == null ? 1 : shop.productUnits(), "screen.ravencoin.shop.goods");
        this.priceUnits = this.numberField(114, 30, shop == null ? 1 : shop.priceUnits(), "screen.ravencoin.shop.price");

        this.rank = new EditBox(
                this.font,
                this.leftPos + 8,
                this.topPos + 62,
                100,
                16,
                Component.translatable("screen.ravencoin.shop.rank"));
        this.rank.setMaxLength(64);
        this.rank.setValue(shop == null ? "" : shop.requiredRank());
        this.rank.setHint(Component.translatable("screen.ravencoin.shop.rank_hint"));
        this.addRenderableWidget(this.rank);

        this.showLabel = shop == null || shop.showLabel();
        this.label = Button.builder(this.labelText(), button -> {
                    this.showLabel = !this.showLabel;
                    button.setMessage(this.labelText());
                })
                .bounds(this.leftPos + 112, this.topPos + 61, 56, 18)
                .build();
        this.addRenderableWidget(this.label);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.ravencoin.shop.apply"), button -> this.apply())
                .bounds(this.leftPos + 8, this.topPos + 96, 160, 18)
                .build());
    }

    private EditBox numberField(int x, int y, int value, String key) {
        EditBox field = new EditBox(this.font, this.leftPos + x, this.topPos + y, 44, 16, Component.translatable(key));
        field.setMaxLength(MAX_DIGITS);
        field.setFilter(text -> text.chars().allMatch(Character::isDigit));
        field.setValue(Integer.toString(value));
        this.addRenderableWidget(field);
        return field;
    }

    private Component labelText() {
        return Component.translatable(this.showLabel ? "screen.ravencoin.shop.label_on" : "screen.ravencoin.shop.label_off");
    }

    private void apply() {
        PacketDistributor.sendToServer(new ShopSettingsPayload(
                parse(this.productUnits.getValue()),
                parse(this.priceUnits.getValue()),
                this.rank.getValue().trim(),
                this.showLabel));
    }

    private static int parse(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException notANumber) {
            return 1;
        }
    }

    /**
     * Handled before the container screen sees the click.
     *
     * <p>The two picture frames are not slots, and letting vanilla work out what
     * a click inside the panel but outside every slot means is how a held stack
     * ends up on the floor.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.over(mouseX, mouseY, PRODUCT_SLOT_X)) {
            PacketDistributor.sendToServer(new ShopPickPayload(false));
            return true;
        }
        if (this.over(mouseX, mouseY, PRICE_SLOT_X)) {
            PacketDistributor.sendToServer(new ShopPickPayload(true));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean over(double mouseX, double mouseY, int slotX) {
        double x = mouseX - this.leftPos - slotX;
        double y = mouseY - this.topPos - SLOT_Y;
        return x >= 0 && x < SLOT_SIZE && y >= 0 && y < SLOT_SIZE;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        ShopBlockEntity shop = this.menu.shop();
        if (shop == null) {
            return;
        }
        graphics.renderItem(shop.product(), this.leftPos + PRODUCT_SLOT_X + 1, this.topPos + SLOT_Y + 1);
        graphics.renderItem(shop.price(), this.leftPos + PRICE_SLOT_X + 1, this.topPos + SLOT_Y + 1);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(this.font, Component.translatable("screen.ravencoin.shop.goods"), 8, 20, 0x404040, false);
        graphics.drawString(this.font, Component.translatable("screen.ravencoin.shop.price"), 92, 20, 0x404040, false);
        graphics.drawString(this.font, Component.translatable("screen.ravencoin.shop.rank"), 8, 52, 0x404040, false);

        ShopBlockEntity shop = this.menu.shop();
        if (shop != null) {
            graphics.drawString(this.font, ShopText.stock(shop), 8, 84, 0xC0C0C0, true);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        ShopBlockEntity shop = this.menu.shop();
        if (shop == null) {
            return;
        }
        this.pickTooltip(graphics, mouseX, mouseY, PRODUCT_SLOT_X, shop.product());
        this.pickTooltip(graphics, mouseX, mouseY, PRICE_SLOT_X, shop.price());
    }

    private void pickTooltip(GuiGraphics graphics, int mouseX, int mouseY, int slotX, ItemStack chosen) {
        if (!this.over(mouseX, mouseY, slotX)) {
            return;
        }
        Component text = chosen.isEmpty()
                ? Component.translatable("screen.ravencoin.shop.pick_hint")
                : chosen.getHoverName();
        graphics.renderTooltip(this.font, text, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox field : new EditBox[] {this.productUnits, this.priceUnits, this.rank}) {
            if (field.isFocused() && field.canConsumeInput() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
                return field.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
