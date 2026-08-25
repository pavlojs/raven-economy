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

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.whiteravens.ravencoin.block.entity.ShopBlockEntity;
import net.whiteravens.ravencoin.shop.ShopText;
import org.joml.Matrix4f;

/**
 * The sign that floats over a shop.
 *
 * <p>Drawn by the block, not by an entity standing on it. Armour stands and
 * display entities are how a server-side-only mod does this, and they cost a
 * real entity per shop, survive the block being broken, and can be pushed,
 * killed or duplicated. A renderer costs nothing and cannot be out of date: it
 * reads the same block entity the buying screen does.
 */
public class ShopLabelRenderer implements BlockEntityRenderer<ShopBlockEntity> {
    /** How far a label is legible. Past this it is a smear, and a market street is a lot of smears. */
    private static final int VIEW_DISTANCE = 24;

    /** Height of one line in the text-space this renderer works in. */
    private static final int LINE = 10;

    /** Vanilla's own colour for the through-the-wall pass: white at a quarter alpha. */
    private static final int GHOST = 553648127;

    private final Font font;
    private final ItemRenderer items;
    private final EntityRenderDispatcher camera;

    public ShopLabelRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
        this.items = context.getItemRenderer();
        this.camera = context.getEntityRenderer();
    }

    @Override
    public void render(
            ShopBlockEntity shop,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        if (!shop.showLabel() || !shop.configured()) {
            return;
        }

        this.renderGoods(shop, pose, buffers);
        this.renderLines(ShopText.label(shop), pose, buffers);
    }

    /** The item itself, hanging over the counter, so the shop is readable before it is read. */
    private void renderGoods(ShopBlockEntity shop, PoseStack pose, MultiBufferSource buffers) {
        pose.pushPose();
        pose.translate(0.5, 2.15, 0.5);
        pose.mulPose(this.camera.cameraOrientation());
        pose.scale(0.5F, 0.5F, 0.5F);
        this.items.renderStatic(
                shop.product(),
                ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                pose,
                buffers,
                shop.getLevel(),
                0);
        pose.popPose();
    }

    /**
     * The text, billboarded and centred.
     *
     * <p>The negative Y scale is not a mistake — vanilla's own name tags do the
     * same thing. It flips the font's downward axis, which is why the first line
     * is placed at the <em>most negative</em> offset to end up on top.
     *
     * <p>Lit at full brightness rather than by the block's own light, because a
     * price nobody can read after dusk is a shop that closes at dusk.
     */
    private void renderLines(List<Component> lines, PoseStack pose, MultiBufferSource buffers) {
        pose.pushPose();
        pose.translate(0.5, 1.35, 0.5);
        pose.mulPose(this.camera.cameraOrientation());
        pose.scale(0.025F, -0.025F, 0.025F);

        Matrix4f matrix = pose.last().pose();
        int background = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;

        for (int line = 0; line < lines.size(); line++) {
            Component text = lines.get(line);
            float x = -this.font.width(text) / 2.0F;
            float y = (line - (lines.size() - 1)) * LINE;

            // Twice, exactly as vanilla draws a name tag: a dim pass that ignores
            // depth, then the solid one that does not. A shop with a cow standing
            // in front of it is still a shop, and a price you have to walk around
            // an animal to read is a price nobody reads.
            this.font.drawInBatch(
                    text, x, y, GHOST, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, background, LightTexture.FULL_BRIGHT);
            this.font.drawInBatch(
                    text, x, y, -1, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }
}
