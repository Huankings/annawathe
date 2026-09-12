package dev.annawathe.client.mixin;

import dev.annawathe.api.client.inventory.InventoryButtonApi;
import dev.annawathe.api.client.inventory.InventoryScreenType;
import dev.annawathe.api.shop.ShopApi;
import dev.annawathe.api.shop.ShopEntry;
import dev.annawathe.network.AnnaStoreBuyPayload;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** 完整接管限制背包的商店控件；客户端只显示，购买合法性由服务端重新计算。 */
@Mixin(LimitedInventoryScreen.class)
public abstract class LimitedInventoryButtonsMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    private static final int ITEM_SPACING = 38;
    private static final int ITEM_X_OFFSET = 9;
    private static final int ITEM_Y_OFFSET = 46;
    @Shadow @Final public ClientPlayerEntity player;

    protected LimitedInventoryButtonsMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void annawathe$initializeDynamicShop(CallbackInfo ci) {
        super.init();
        LimitedInventoryScreen screen = (LimitedInventoryScreen) (Object) this;
        List<ShopEntry> entries = ShopApi.getEntriesForPlayer(this.player);
        int startX = this.width / 2 - entries.size() * ITEM_SPACING / 2 + ITEM_X_OFFSET;
        int itemY = this.y - ITEM_Y_OFFSET;
        for (int i = 0; i < entries.size(); i++) {
            this.addDrawableChild(new AnnaStoreItemWidget(
                    screen, this.textRenderer, startX + ITEM_SPACING * i, itemY, entries.get(i), i));
        }
        InventoryButtonApi.initializeScreen(screen, InventoryScreenType.LIMITED, this.player, this.textRenderer,
                widget -> this.addDrawableChild(widget), this.width, this.height, this.x, this.y,
                this.backgroundWidth, this.backgroundHeight);
        ci.cancel();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void annawathe$renderInventoryExtensions(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        InventoryButtonApi.renderScreen((LimitedInventoryScreen) (Object) this, context, mouseX, mouseY, delta);
    }

    private static final class AnnaStoreItemWidget extends ButtonWidget {
        private final LimitedInventoryScreen screen;
        private final TextRenderer renderer;
        private final ShopEntry entry;

        private AnnaStoreItemWidget(LimitedInventoryScreen screen, TextRenderer renderer, int x, int y,
                                    ShopEntry entry, int index) {
            super(x, y, 16, 16, entry.stack().getName(),
                    button -> ClientPlayNetworking.send(new AnnaStoreBuyPayload(index)), DEFAULT_NARRATION_SUPPLIER);
            this.screen = screen;
            this.renderer = renderer;
            this.entry = entry;
        }

        @Override
        protected void renderWidget(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
            context.drawGuiTexture(this.entry.type().texture(), this.getX() - 7, this.getY() - 7, 30, 30);
            context.drawItem(this.entry.stack(), this.getX(), this.getY());
            if (this.isHovered()) {
                this.screen.renderLimitedInventoryTooltip(context, this.entry.stack());
                this.drawHighlight(context);
            }
            this.renderPrice(context);
        }

        private void renderPrice(DrawContext context) {
            List<Text> lines = this.entry.shopPrice().displayLines();
            if (lines.isEmpty()) return;
            int maxWidth = lines.stream().mapToInt(this.renderer::getWidth).max().orElse(0);
            int spaceWidth = Math.max(1, this.renderer.getWidth(" "));
            List<Text> centered = new ArrayList<>(lines.size());
            for (Text line : lines) {
                int pads = Math.round((float) Math.max(0, maxWidth - this.renderer.getWidth(line)) / spaceWidth);
                centered.add(Text.literal(" ".repeat(pads / 2)).append(line).append(" ".repeat(pads - pads / 2)));
            }
            int tooltipX = this.getX() - 4 - maxWidth / 2;
            int tooltipY = this.getY() - 9 - Math.max(0, lines.size() - 1) * 10;
            context.drawTooltip(this.renderer, centered, tooltipX, tooltipY);
        }

        private void drawHighlight(DrawContext context) {
            int color = 0x90FFBF49;
            int x = this.getX();
            int y = this.getY();
            context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, 0);
            context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, 0);
            context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, 0);
        }

        @Override public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {}
    }
}
