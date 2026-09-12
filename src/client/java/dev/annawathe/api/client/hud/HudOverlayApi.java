package dev.annawathe.api.client.hud;

import dev.doctor4t.wathe.api.Role;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AnnaWathe 通用屏幕 HUD 的客户端注册与调度入口。
 *
 * <p>该 API 面向职业状态、词条提示、全屏遮罩和额外进度条等自由绘制内容。Mood、顶部时间、
 * 准心图标、准心名字和背包按钮已经有更窄的专用 API，扩展应优先使用对应入口。</p>
 */
@Environment(EnvType.CLIENT)
public final class HudOverlayApi {
    public static final int DEFAULT_PRIORITY = 0;

    /**
     * HUD 遵循后画覆盖先画，因此 priority 越大越晚绘制；同 priority 下后注册者也更晚绘制。
     */
    private static final Comparator<Entry> ENTRY_COMPARATOR =
            Comparator.comparingInt(Entry::priority).thenComparingLong(Entry::order);

    private static final Map<HudOverlayLayer, List<Entry>> RENDERERS =
            new EnumMap<>(HudOverlayLayer.class);
    private static long nextOrder;

    static {
        for (HudOverlayLayer layer : HudOverlayLayer.values()) {
            RENDERERS.put(layer, new ArrayList<>());
        }
    }

    private HudOverlayApi() {
    }

    /**
     * 在指定阶段注册 renderer。同一阶段内重复使用相同 ID 会替换旧注册，便于扩展热重载配置。
     */
    public static void register(@NotNull Identifier id,
                                @NotNull HudOverlayLayer layer,
                                int priority,
                                @NotNull HudOverlayRenderer renderer) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(renderer, "renderer");
        synchronized (RENDERERS) {
            List<Entry> entries = RENDERERS.get(layer);
            entries.removeIf(entry -> entry.id().equals(id));
            entries.add(new Entry(id, priority, nextOrder++, renderer));
            entries.sort(ENTRY_COMPARATOR);
        }
    }

    /**
     * 注册仅在本地玩家属于指定职业且仍按 Wathe 玩法存活时显示的 HUD。
     *
     * <p>存活判断通过 {@code GameFunctions.isPlayerAliveAndSurvival} 生成上下文，因此也会遵守
     * AnnaWathe {@code PlayerLifeStateApi} 对 creative/spectator 的特殊玩法存活授权。</p>
     */
    public static void registerAliveRole(@NotNull Identifier id,
                                         @NotNull HudOverlayLayer layer,
                                         int priority,
                                         @NotNull Role role,
                                         @NotNull HudOverlayRenderer renderer) {
        Objects.requireNonNull(role, "role");
        register(id, layer, priority, context -> {
            if (context.isAliveRole(role)) {
                renderer.render(context);
            }
        });
    }

    /** 由 AnnaWathe 的 InGameHud Mixin 调用；扩展通常只需要调用注册方法。 */
    public static void render(@NotNull HudOverlayLayer layer, @NotNull HudOverlayContext context) {
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(context, "context");
        for (Entry entry : snapshot(layer)) {
            entry.renderer().render(context);
        }
    }

    private static List<Entry> snapshot(HudOverlayLayer layer) {
        synchronized (RENDERERS) {
            return List.copyOf(RENDERERS.get(layer));
        }
    }

    @FunctionalInterface
    public interface HudOverlayRenderer {
        void render(@NotNull HudOverlayContext context);
    }

    private record Entry(@NotNull Identifier id,
                         int priority,
                         long order,
                         @NotNull HudOverlayRenderer renderer) {
    }
}
