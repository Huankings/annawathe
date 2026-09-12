package dev.annawathe.api.client.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * 通用屏幕 HUD 的三个绘制阶段。
 *
 * <p>阶段描述的是相对于 Minecraft 和 Wathe 主 HUD 的绘制位置。扩展只需要选择语义最合适的
 * 阶段，不应再为了争抢渲染顺序而直接 Mixin {@code InGameHud}。</p>
 */
@Environment(EnvType.CLIENT)
public enum HudOverlayLayer {
    /** 在 Minecraft 主 HUD 之前绘制，适合需要较早覆盖画面的状态。 */
    BEFORE_HUD,
    /** 在 Wathe 的 Mood、准心名字、商店和时间等主 HUD 之后绘制。 */
    MAIN_HUD,
    /** 在整套 HUD 最后绘制，适合狙击镜等必须位于最上层的遮罩。 */
    AFTER_HUD
}
