package dev.annawathe.api.client.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 通用 HUD 的轻量布局工具，不保存任何职业或对局状态。
 *
 * <p>扩展仍负责决定具体文案和显示条件；这些方法只统一最常见的屏幕坐标、换行高度和矩阵缩放。</p>
 */
@Environment(EnvType.CLIENT)
public final class HudOverlayLayout {
    public static final int DEFAULT_RIGHT_MARGIN = 0;
    public static final int DEFAULT_BOTTOM_MARGIN = 0;

    private HudOverlayLayout() {
    }

    public static void drawBottomRightLine(@NotNull HudOverlayContext context,
                                           @NotNull Text line,
                                           int color) {
        drawBottomRightLines(context, List.of(line), color);
    }

    public static void drawBottomRightLines(@NotNull HudOverlayContext context,
                                            @NotNull List<Text> lines,
                                            int color) {
        drawBottomRightLines(context, lines, color, DEFAULT_RIGHT_MARGIN, DEFAULT_BOTTOM_MARGIN);
    }

    public static void drawBottomRightLines(@NotNull HudOverlayContext context,
                                            @NotNull List<Text> lines,
                                            int color,
                                            int rightMargin,
                                            int bottomMargin) {
        TextRenderer renderer = context.textRenderer();
        DrawContext drawContext = context.drawContext();
        int drawY = context.height() - bottomMargin;
        // 从最后一行向上排版，使调用方传入的文本顺序与最终从上到下的阅读顺序一致。
        for (int index = lines.size() - 1; index >= 0; index--) {
            Text line = lines.get(index);
            drawY -= renderer.getWrappedLinesHeight(line, 999999);
            drawContext.drawTextWithShadow(
                    renderer,
                    line,
                    context.width() - rightMargin - renderer.getWidth(line),
                    drawY,
                    color
            );
        }
    }

    public static void drawCenteredNearCrosshair(@NotNull HudOverlayContext context,
                                                 @NotNull Text text,
                                                 int y,
                                                 int color) {
        drawCenteredNearCrosshair(context.textRenderer(), context.drawContext(), text, y, color);
    }

    public static void drawCenteredNearCrosshair(@NotNull TextRenderer renderer,
                                                 @NotNull DrawContext context,
                                                 @NotNull Text text,
                                                 int y,
                                                 int color) {
        context.getMatrices().push();
        // 先移动到屏幕中心，再缩放局部坐标；传入的 y 因此属于 0.6 倍的准心局部坐标系。
        context.getMatrices().translate(
                context.getScaledWindowWidth() / 2.0F,
                context.getScaledWindowHeight() / 2.0F + 6.0F,
                0.0F
        );
        context.getMatrices().scale(0.6F, 0.6F, 1.0F);
        context.drawTextWithShadow(renderer, text, -renderer.getWidth(text) / 2, y, color);
        context.getMatrices().pop();
    }
}
