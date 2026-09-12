package dev.annawathe.api.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.Wathe;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * AnnaWathe 准心图标和准心下方小进度图标的公开客户端入口。
 *
 * <p>准心名字、尸体文字、同伙提示应使用 {@link RoleNameHudApi}；自由位置状态和全屏遮罩应使用
 * {@code HudOverlayApi}。本 API 只控制显示，服务端仍必须重新校验职业、存活、目标、距离和冷却。</p>
 */
@Environment(EnvType.CLIENT)
public final class CrosshairHudApi {
    public static final int DEFAULT_PRIORITY = 0;

    // 直接复用原版 Wathe 已提供的资源，AnnaWathe 无需复制或覆盖这些纹理。
    public static final Identifier CROSSHAIR = Wathe.id("hud/crosshair");
    public static final Identifier CROSSHAIR_TARGET = Wathe.id("hud/crosshair_target");
    public static final Identifier KNIFE_ATTACK = Wathe.id("hud/knife_attack");
    public static final Identifier KNIFE_PROGRESS = Wathe.id("hud/knife_progress");
    public static final Identifier KNIFE_BACKGROUND = Wathe.id("hud/knife_background");
    public static final Identifier BAT_ATTACK = Wathe.id("hud/bat_attack");
    public static final Identifier BAT_PROGRESS = Wathe.id("hud/bat_progress");
    public static final Identifier BAT_BACKGROUND = Wathe.id("hud/bat_background");

    /** Provider 是短路链：高 priority 先执行，同 priority 后注册者先执行。 */
    private static final Comparator<ProviderEntry> PROVIDER_COMPARATOR =
            Comparator.<ProviderEntry>comparingInt(ProviderEntry::priority)
                    .reversed()
                    .thenComparing(Comparator.comparingLong(ProviderEntry::order).reversed());

    /** Overlay 全部执行并遵循后画覆盖先画：高 priority 和后注册者更晚绘制。 */
    private static final Comparator<OverlayEntry> OVERLAY_COMPARATOR =
            Comparator.comparingInt(OverlayEntry::priority).thenComparingLong(OverlayEntry::order);

    private static final List<ProviderEntry> PROVIDERS = new ArrayList<>();
    private static final List<OverlayEntry> OVERLAYS = new ArrayList<>();
    private static long nextOrder;

    private CrosshairHudApi() {
    }

    /**
     * 注册可以完整接管默认准心的 provider。返回 HANDLED 也可以表示本帧故意不画任何准心。
     */
    public static void registerProvider(@NotNull Identifier id,
                                        int priority,
                                        @NotNull CrosshairProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        synchronized (PROVIDERS) {
            PROVIDERS.removeIf(entry -> entry.id().equals(id));
            PROVIDERS.add(new ProviderEntry(id, priority, nextOrder(), provider));
            PROVIDERS.sort(PROVIDER_COMPARATOR);
        }
    }

    /**
     * 注册默认准心之后的附加绘制器。Overlay 不会阻止默认准心，也不会阻止其他 overlay。
     */
    public static void registerOverlay(@NotNull Identifier id,
                                       int priority,
                                       @NotNull CrosshairOverlay overlay) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(overlay, "overlay");
        synchronized (OVERLAYS) {
            OVERLAYS.removeIf(entry -> entry.id().equals(id));
            OVERLAYS.add(new OverlayEntry(id, priority, nextOrder(), overlay));
            OVERLAYS.sort(OVERLAY_COMPARATOR);
        }
    }

    /** 由 Anna 的准心 renderer 调度，扩展通常只需要调用注册方法。 */
    public static @NotNull Result renderProvider(@NotNull Context context) {
        for (ProviderEntry entry : providerSnapshot()) {
            Result result = entry.provider().render(context);
            if (result != null && result != Result.PASS) {
                return result;
            }
        }
        return Result.PASS;
    }

    /** Provider 或默认准心完成后，按绘制顺序执行全部 overlay。 */
    public static void renderOverlays(@NotNull Context context) {
        for (OverlayEntry entry : overlaySnapshot()) {
            entry.overlay().render(context);
        }
    }

    /** 渲染 Wathe 标准 3x3 准心，矩阵会自动移动到屏幕中心。 */
    public static void renderStandardCrosshair(@NotNull Context context, boolean target) {
        renderCentered(context, centered -> drawCrosshairIcon(centered, target));
    }

    /** 渲染原版匕首样式的准心和 10x7 ready/progress 图标。 */
    public static void renderKnifeProgressCrosshair(@NotNull Context context,
                                                    boolean highlightCrosshair,
                                                    boolean showAttackIcon,
                                                    float progress) {
        renderCentered(context, centered -> {
            drawKnifeProgressIcon(centered, showAttackIcon, progress);
            drawCrosshairIcon(centered, highlightCrosshair);
        });
    }

    /** 渲染原版棍棒样式的准心和 10x7 ready/progress 图标。 */
    public static void renderBatProgressCrosshair(@NotNull Context context,
                                                  boolean highlightCrosshair,
                                                  boolean showAttackIcon,
                                                  float progress) {
        renderCentered(context, centered -> {
            drawBatProgressIcon(centered, showAttackIcon, progress);
            drawCrosshairIcon(centered, highlightCrosshair);
        });
    }

    /**
     * 使用扩展自己的 10x7 ready/background/fill 纹理，同时复用 Wathe 的标准准心。
     */
    public static void renderIconProgressCrosshair(@NotNull Context context,
                                                   boolean highlightCrosshair,
                                                   boolean showReadyIcon,
                                                   float progress,
                                                   @NotNull Identifier readyTexture,
                                                   @NotNull Identifier backgroundTexture,
                                                   @NotNull Identifier fillTexture) {
        Objects.requireNonNull(readyTexture, "readyTexture");
        Objects.requireNonNull(backgroundTexture, "backgroundTexture");
        Objects.requireNonNull(fillTexture, "fillTexture");
        renderCentered(context, centered -> {
            drawIconProgress(centered, showReadyIcon, progress, readyTexture, backgroundTexture, fillTexture);
            drawCrosshairIcon(centered, highlightCrosshair);
        });
    }

    /**
     * 把矩阵原点移动到屏幕中心后执行绘制，并在结束时恢复矩阵及 Wathe 准心使用的混合状态。
     */
    public static void renderCentered(@NotNull Context context, @NotNull CenteredRenderer renderer) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(renderer, "renderer");
        DrawContext drawContext = context.drawContext();
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(context.centerX(), context.centerY(), 0.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        renderer.render(context);
        drawContext.getMatrices().pop();
        // 防止自定义准心把特殊 blend 状态泄漏给同帧后续 HUD。
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** 在已经居中的矩阵坐标系内绘制 Wathe 标准 3x3 准心。 */
    public static void drawCrosshairIcon(@NotNull Context context, boolean target) {
        DrawContext drawContext = context.drawContext();
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(-1.5F, -1.5F, 0.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        drawContext.drawGuiTexture(target ? CROSSHAIR_TARGET : CROSSHAIR, 0, 0, 3, 3);
        drawContext.getMatrices().pop();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    public static void drawKnifeProgressIcon(@NotNull Context context,
                                             boolean showAttackIcon,
                                             float progress) {
        drawIconProgress(context, showAttackIcon, progress, KNIFE_ATTACK, KNIFE_BACKGROUND, KNIFE_PROGRESS);
    }

    public static void drawBatProgressIcon(@NotNull Context context,
                                           boolean showAttackIcon,
                                           float progress) {
        drawIconProgress(context, showAttackIcon, progress, BAT_ATTACK, BAT_BACKGROUND, BAT_PROGRESS);
    }

    /** 在已经居中的坐标系内绘制 10x7 ready 图标或横向填充的进度图标。 */
    public static void drawIconProgress(@NotNull Context context,
                                        boolean showReadyIcon,
                                        float progress,
                                        @NotNull Identifier readyTexture,
                                        @NotNull Identifier backgroundTexture,
                                        @NotNull Identifier fillTexture) {
        Objects.requireNonNull(readyTexture, "readyTexture");
        Objects.requireNonNull(backgroundTexture, "backgroundTexture");
        Objects.requireNonNull(fillTexture, "fillTexture");
        DrawContext drawContext = context.drawContext();
        if (showReadyIcon) {
            drawContext.drawGuiTexture(readyTexture, -5, 5, 10, 7);
            return;
        }

        int fillWidth = Math.max(0, Math.min(10,
                (int) (MathHelper.clamp(progress, 0.0F, 1.0F) * 10.0F)));
        drawContext.drawGuiTexture(backgroundTexture, -5, 5, 10, 7);
        drawContext.drawGuiTexture(fillTexture, 10, 7, 0, 0, -5, 5, fillWidth, 7);
    }

    private static List<ProviderEntry> providerSnapshot() {
        synchronized (PROVIDERS) {
            return List.copyOf(PROVIDERS);
        }
    }

    private static List<OverlayEntry> overlaySnapshot() {
        synchronized (OVERLAYS) {
            return List.copyOf(OVERLAYS);
        }
    }

    private static synchronized long nextOrder() {
        return nextOrder++;
    }

    public record Context(@NotNull MinecraftClient client,
                          @NotNull ClientPlayerEntity player,
                          @NotNull DrawContext drawContext,
                          @NotNull RenderTickCounter tickCounter,
                          @NotNull ItemStack mainHandStack,
                          float tickDelta) {
        public int centerX() {
            return this.drawContext.getScaledWindowWidth() / 2;
        }

        public int centerY() {
            return this.drawContext.getScaledWindowHeight() / 2;
        }
    }

    @FunctionalInterface
    public interface CrosshairProvider {
        @NotNull Result render(@NotNull Context context);
    }

    @FunctionalInterface
    public interface CrosshairOverlay {
        void render(@NotNull Context context);
    }

    @FunctionalInterface
    public interface CenteredRenderer {
        void render(@NotNull Context context);
    }

    public enum Result {
        PASS,
        HANDLED
    }

    private record ProviderEntry(@NotNull Identifier id,
                                 int priority,
                                 long order,
                                 @NotNull CrosshairProvider provider) {
    }

    private record OverlayEntry(@NotNull Identifier id,
                                int priority,
                                long order,
                                @NotNull CrosshairOverlay overlay) {
    }
}
