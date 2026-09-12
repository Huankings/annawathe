package dev.annawathe.api.time;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** 顶部时间 HUD 的优先级 provider 链；扩展只描述显示决策，不接触 renderer 内部动画。 */
public final class TimeHudApi {
    public static final int DEFAULT_PRIORITY = 0;
    public static final int NO_LOW_TIME_WARNING = -1;
    public static final int DEFAULT_CHANGE_FLASH_THRESHOLD = 10;
    private static final List<Entry> PROVIDERS = new ArrayList<>();
    private static long order;
    private TimeHudApi() {}
    public static synchronized void registerProvider(Identifier id, int priority, TimeDisplayProvider provider) {
        PROVIDERS.removeIf(e -> e.id.equals(id)); PROVIDERS.add(new Entry(id, priority, order++, provider));
        PROVIDERS.sort(Comparator.<Entry>comparingInt(Entry::priority).reversed()
                .thenComparing(Comparator.comparingLong(Entry::order).reversed()));
    }
    public static synchronized void registerDefaultProvider(Identifier id, int priority, TimeDisplayProvider provider) {
        PROVIDERS.removeIf(e -> e.id.equals(id)); PROVIDERS.add(new Entry(id, priority, Long.MIN_VALUE, provider));
        PROVIDERS.sort(Comparator.<Entry>comparingInt(Entry::priority).reversed()
                .thenComparing(Comparator.comparingLong(Entry::order).reversed()));
    }
    public static @NotNull TimeDisplay resolveDisplay(@NotNull PlayerEntity viewer) {
        synchronized (TimeHudApi.class) { for (Entry e : PROVIDERS) { TimeDisplay display = e.provider.getTimeDisplay(viewer); if (display != null && display.action != TimeDisplay.Action.PASS) return display.withSourceId(e.id); } }
        return TimeDisplay.pass();
    }
    @FunctionalInterface public interface TimeDisplayProvider { @NotNull TimeDisplay getTimeDisplay(@NotNull PlayerEntity viewer); }
    public record TimeDisplay(Action action, int ticks, ColorMode colorMode, int fixedColor, int lowTimeWarningTicks, int changeFlashThreshold, @Nullable Identifier sourceId) {
        private static final TimeDisplay PASS = new TimeDisplay(Action.PASS, 0, ColorMode.DYNAMIC, 0xFFFFFFFF, NO_LOW_TIME_WARNING, DEFAULT_CHANGE_FLASH_THRESHOLD, null);
        public static TimeDisplay pass() { return PASS; }
        public static TimeDisplay hide() { return new TimeDisplay(Action.HIDE, 0, ColorMode.DYNAMIC, 0xFFFFFFFF, NO_LOW_TIME_WARNING, DEFAULT_CHANGE_FLASH_THRESHOLD, null); }
        public static TimeDisplay show(int ticks) { return showDynamic(ticks, NO_LOW_TIME_WARNING, DEFAULT_CHANGE_FLASH_THRESHOLD); }
        public static TimeDisplay showCountdown(int ticks, int warning) { return showDynamic(ticks, warning, DEFAULT_CHANGE_FLASH_THRESHOLD); }
        public static TimeDisplay showDynamic(int ticks, int warning, int threshold) { return new TimeDisplay(Action.SHOW, Math.max(0, ticks), ColorMode.DYNAMIC, 0xFFFFFFFF, warning, Math.max(0, threshold), null); }
        public static TimeDisplay showFixedColor(int ticks, int color) { return new TimeDisplay(Action.SHOW, Math.max(0, ticks), ColorMode.FIXED, color, NO_LOW_TIME_WARNING, DEFAULT_CHANGE_FLASH_THRESHOLD, null); }
        private TimeDisplay withSourceId(Identifier id) { return new TimeDisplay(action, ticks, colorMode, fixedColor, lowTimeWarningTicks, changeFlashThreshold, id); }
        public enum Action { PASS, HIDE, SHOW }
        public enum ColorMode { DYNAMIC, FIXED }
    }
    private record Entry(Identifier id, int priority, long order, TimeDisplayProvider provider) {}
}
