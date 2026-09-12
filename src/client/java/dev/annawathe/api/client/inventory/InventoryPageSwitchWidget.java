package dev.annawathe.api.client.inventory;

import dev.annawathe.api.shop.ShopEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

/** 玩家头像列表可复用的上一页/下一页按钮。 */
@Environment(EnvType.CLIENT)
public class InventoryPageSwitchWidget extends ButtonWidget {
    private final ItemStack icon;
    private final Text tooltip;

    public InventoryPageSwitchWidget(int x, int y, @NotNull ItemStack icon, @NotNull Text tooltip,
                                     @NotNull PressAction onPress) {
        super(x, y, 16, 16, tooltip, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.icon = icon;
        this.tooltip = tooltip;
    }

    @Override
    protected void renderWidget(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawGuiTexture(ShopEntry.Type.TOOL.texture(), this.getX() - 7, this.getY() - 7, 30, 30);
        context.drawItem(this.icon, this.getX(), this.getY());
        if (this.isHovered()) {
            int color = 0x90FFBF49;
            context.fillGradient(RenderLayer.getGuiOverlay(), getX(), getY(), getX() + 16, getY() + 16, color, color, 0);
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, this.tooltip, mouseX, mouseY);
        }
    }
}
