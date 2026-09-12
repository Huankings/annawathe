package dev.annawathe.api.client.hud;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 通用 HUD renderer 每帧收到的只读客户端上下文。
 *
 * <p>这里集中提供玩家、对局、玩法存活、界面状态和热键栏复画入口，避免扩展在自己的 HUD
 * 中重复读取 Wathe 内部字段。上下文中的状态只供客户端显示使用，不能替代服务端对职业、
 * 存活、距离、冷却和技能合法性的重新校验。</p>
 */
@Environment(EnvType.CLIENT)
public record HudOverlayContext(
        @NotNull MinecraftClient client,
        @NotNull ClientPlayerEntity player,
        @NotNull TextRenderer textRenderer,
        @NotNull DrawContext drawContext,
        @NotNull RenderTickCounter tickCounter,
        @NotNull GameWorldComponent gameWorld,
        boolean aliveAndSurvival,
        boolean spectatingOrCreative,
        boolean debugHudVisible,
        boolean hudHidden,
        @Nullable Screen currentScreen,
        @Nullable HotbarRenderer hotbarRenderer
) {
    public int width() {
        return this.drawContext.getScaledWindowWidth();
    }

    public int height() {
        return this.drawContext.getScaledWindowHeight();
    }

    public float tickDelta() {
        return this.tickCounter.getTickDelta(true);
    }

    public boolean isRunning() {
        return this.gameWorld.isRunning();
    }

    public boolean isRole(@NotNull Role role) {
        return this.gameWorld.isRole(this.player, role);
    }

    public boolean isAliveRole(@NotNull Role role) {
        return this.aliveAndSurvival && this.isRole(role);
    }

    /**
     * 在当前 HUD 帧中受控地复画一次热键栏。
     *
     * <p>典型用途是 AFTER_HUD 狙击镜：遮罩先盖住整个界面，再把玩家仍需查看的热键栏画回来。
     * 该调用仍经过原版 Wathe 对 {@code InGameHud#renderHotbar} 的包装，所以会保留 Wathe 的
     * 热键栏纹理和既有行为。</p>
     */
    public void renderHotbar() {
        if (this.hotbarRenderer != null) {
            this.hotbarRenderer.render(this.drawContext, this.tickCounter);
        }
    }

    @FunctionalInterface
    public interface HotbarRenderer {
        void render(@NotNull DrawContext context, @NotNull RenderTickCounter tickCounter);
    }
}
