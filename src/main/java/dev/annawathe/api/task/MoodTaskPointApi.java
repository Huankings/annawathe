package dev.annawathe.api.task;

import dev.annawathe.AnnaWathe;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 心情任务点类型和地图扫描扩展的公开入口。 */
public final class MoodTaskPointApi {
    public static final int DEFAULT_PRIORITY = 0;
    public static final Identifier BED = AnnaWathe.id("bed");
    public static final Identifier FOOD_TRAY = AnnaWathe.id("food_tray");
    public static final Identifier COCKTAIL_TRAY = AnnaWathe.id("cocktail_tray");
    public static final Identifier SEAT = AnnaWathe.id("seat");
    public static final Identifier KEYED_DOOR = AnnaWathe.id("keyed_door");

    private static final Map<Identifier, TaskPointDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final ArrayList<ScanEntry> SCANNERS = new ArrayList<>();
    private static long order;

    static {
        registerTaskPoint(BED, "annawathe.hud.task_point.bed", 0x57D6FF);
        registerTaskPoint(FOOD_TRAY, "annawathe.hud.task_point.food_tray", 0x61D95C);
        registerTaskPoint(COCKTAIL_TRAY, "annawathe.hud.task_point.cocktail_tray", 0xFF85A8);
        registerTaskPoint(SEAT, "annawathe.hud.task_point.seat", 0x7AF4E1);
        registerTaskPoint(KEYED_DOOR, "annawathe.hud.task_point.keyed_door", 0xFFF79B);
    }

    private MoodTaskPointApi() {}

    public static synchronized void registerTaskPoint(TaskPointDefinition definition) {
        DEFINITIONS.put(Objects.requireNonNull(definition).id(), definition);
    }
    public static void registerTaskPoint(Identifier id, String key, int color) {
        registerTaskPoint(new TaskPointDefinition(id, key, color));
    }
    public static synchronized @Nullable TaskPointDefinition getDefinition(Identifier id) { return DEFINITIONS.get(id); }
    public static synchronized Collection<TaskPointDefinition> getDefinitions() { return List.copyOf(DEFINITIONS.values()); }
    public static synchronized List<Identifier> getRegisteredIds() { return List.copyOf(DEFINITIONS.keySet()); }
    public static synchronized boolean isRegistered(Identifier id) { return DEFINITIONS.containsKey(id); }
    public static String getTranslationKey(Identifier id) { var d = getDefinition(id); return d == null ? "annawathe.hud.task_point.unknown" : d.translationKey(); }
    public static int getColor(Identifier id) { var d = getDefinition(id); return d == null ? 0xFFFFFF : d.color(); }

    public static synchronized void registerScanHandler(Identifier id, int priority, ScanHandler handler) {
        SCANNERS.removeIf(entry -> entry.id().equals(id));
        SCANNERS.add(new ScanEntry(id, priority, order++, Objects.requireNonNull(handler)));
        SCANNERS.sort(Comparator.<ScanEntry>comparingInt(ScanEntry::priority).reversed()
                .thenComparing(Comparator.comparingLong(ScanEntry::order).reversed()));
    }
    public static void scanExtraTaskPoints(TaskPointScanContext context) {
        List<ScanEntry> snapshot;
        synchronized (MoodTaskPointApi.class) { snapshot = List.copyOf(SCANNERS); }
        for (ScanEntry entry : snapshot) entry.handler().scan(context);
    }

    @FunctionalInterface public interface ScanHandler { void scan(@NotNull TaskPointScanContext context); }
    private record ScanEntry(Identifier id, int priority, long order, ScanHandler handler) {}
}
