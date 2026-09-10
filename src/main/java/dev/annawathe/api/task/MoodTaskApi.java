package dev.annawathe.api.task;

import dev.annawathe.AnnaWathe;
import dev.annawathe.bridge.MoodTaskBridge;
import dev.annawathe.mood.BuiltInMoodTasks;
import dev.annawathe.mood.MoodTaskState;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** AnnaWathe 心情任务注册、发放、删除和完成的统一公开入口。 */
public final class MoodTaskApi {
    public static final int DEFAULT_PRIORITY = 0;
    public static final Identifier SLEEP = Identifier.of("wathe", "sleep");
    public static final Identifier OUTSIDE = Identifier.of("wathe", "outside");
    public static final Identifier EAT = Identifier.of("wathe", "eat");
    public static final Identifier DRINK = Identifier.of("wathe", "drink");
    public static final Identifier SHIFT = AnnaWathe.id("shift");
    public static final Identifier RUN = AnnaWathe.id("run");
    public static final Identifier SIT = AnnaWathe.id("sit");
    public static final Identifier STAY = AnnaWathe.id("stay");
    public static final Identifier AWAY = AnnaWathe.id("away");

    private static final Map<Identifier, MoodTaskDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<PlayerMoodComponent.Task, Identifier> LEGACY_IDS = new LinkedHashMap<>();
    private static final ArrayList<AssignmentEntry> ASSIGNMENT_RULES = new ArrayList<>();
    private static final ArrayList<CompletionEntry> COMPLETION_RULES = new ArrayList<>();
    private static long order;

    static {
        legacy(SLEEP, "task.sleep", PlayerMoodComponent.Task.SLEEP,
                p -> new PlayerMoodComponent.SleepTask(GameConstants.SLEEP_TASK_DURATION),
                (p, n) -> new PlayerMoodComponent.SleepTask(n.getInt("timer")), MoodTaskPointApi.BED);
        legacy(OUTSIDE, "task.outside", PlayerMoodComponent.Task.OUTSIDE,
                p -> new PlayerMoodComponent.OutsideTask(GameConstants.OUTSIDE_TASK_DURATION),
                (p, n) -> new PlayerMoodComponent.OutsideTask(n.getInt("timer")));
        legacy(EAT, "task.eat", PlayerMoodComponent.Task.EAT,
                p -> new PlayerMoodComponent.EatTask(), (p, n) -> new PlayerMoodComponent.EatTask(), MoodTaskPointApi.FOOD_TRAY);
        legacy(DRINK, "task.drink", PlayerMoodComponent.Task.DRINK,
                p -> new PlayerMoodComponent.DrinkTask(), (p, n) -> new PlayerMoodComponent.DrinkTask(), MoodTaskPointApi.COCKTAIL_TRAY);

        timed(SHIFT, "annawathe.task.shift", MoodTaskState.TASK_DURATION, BuiltInMoodTasks.Shift::new);
        timed(RUN, "annawathe.task.run", MoodTaskState.TASK_DURATION, BuiltInMoodTasks.Run::new);
        registerTask(MoodTaskDefinition.builder(SIT, "annawathe.task.sit",
                p -> new BuiltInMoodTasks.Sit(MoodTaskState.TASK_DURATION),
                (p, n) -> new BuiltInMoodTasks.Sit(n.getInt("timer")))
                .randomlyAssignable().taskPoints(MoodTaskPointApi.SEAT).build());
        timed(STAY, "annawathe.task.stay", MoodTaskState.TASK_DURATION, BuiltInMoodTasks.Stay::new);
        timed(AWAY, "annawathe.task.away", MoodTaskState.TASK_DURATION, BuiltInMoodTasks.Away::new);
    }

    private MoodTaskApi() {}

    private static void timed(Identifier id, String key, int duration, java.util.function.IntFunction<MoodTaskInstance> factory) {
        registerTask(MoodTaskDefinition.builder(id, key, p -> factory.apply(duration), (p, n) -> factory.apply(n.getInt("timer")))
                .randomlyAssignable().build());
    }
    private static void legacy(Identifier id, String key, PlayerMoodComponent.Task task,
                               java.util.function.Function<net.minecraft.entity.player.PlayerEntity, PlayerMoodComponent.TrainTask> factory,
                               java.util.function.BiFunction<net.minecraft.entity.player.PlayerEntity, net.minecraft.nbt.NbtCompound, PlayerMoodComponent.TrainTask> reader,
                               Identifier... points) {
        registerTask(MoodTaskDefinition.builder(id, key, p -> new BuiltInMoodTasks.Legacy(factory.apply(p)),
                        (p, n) -> new BuiltInMoodTasks.Legacy(reader.apply(p, n)))
                .legacyTask(task).randomlyAssignable().taskPoints(points).build());
    }

    public static synchronized void registerTask(MoodTaskDefinition definition) {
        Objects.requireNonNull(definition);
        DEFINITIONS.put(definition.id(), definition);
        if (definition.legacyTask() != null) LEGACY_IDS.put(definition.legacyTask(), definition.id());
    }
    public static synchronized @Nullable MoodTaskDefinition getDefinition(Identifier id) { return DEFINITIONS.get(id); }
    public static synchronized List<MoodTaskDefinition> getDefinitions() { return List.copyOf(DEFINITIONS.values()); }
    public static synchronized List<MoodTaskDefinition> getRandomAssignableDefinitions() { return DEFINITIONS.values().stream().filter(MoodTaskDefinition::randomlyAssignable).toList(); }
    public static synchronized List<Identifier> getRegisteredTaskIds() { return List.copyOf(DEFINITIONS.keySet()); }
    public static synchronized Identifier getTaskId(PlayerMoodComponent.Task task) { return LEGACY_IDS.getOrDefault(task, Identifier.of("wathe", task.name().toLowerCase(java.util.Locale.ROOT))); }
    public static String getTranslationKey(Identifier id) { var d = getDefinition(id); return d == null ? "annawathe.task.unknown" : d.translationKey(); }
    public static List<Identifier> getTaskPointIds(Identifier id) { var d = getDefinition(id); return d == null ? List.of() : List.copyOf(d.taskPointIds()); }

    public static synchronized void registerAssignmentRule(Identifier id, int priority, AssignmentRule rule) {
        ASSIGNMENT_RULES.removeIf(e -> e.id().equals(id));
        ASSIGNMENT_RULES.add(new AssignmentEntry(id, priority, order++, rule));
        ASSIGNMENT_RULES.sort(Comparator.<AssignmentEntry>comparingInt(AssignmentEntry::priority).reversed().thenComparing(Comparator.comparingLong(AssignmentEntry::order).reversed()));
    }
    public static synchronized void registerCompletionRule(Identifier id, int priority, CompletionRule rule) {
        COMPLETION_RULES.removeIf(e -> e.id().equals(id));
        COMPLETION_RULES.add(new CompletionEntry(id, priority, order++, rule));
        COMPLETION_RULES.sort(Comparator.<CompletionEntry>comparingInt(CompletionEntry::priority).reversed().thenComparing(Comparator.comparingLong(CompletionEntry::order).reversed()));
    }
    public static boolean canAssign(AssignmentContext context) { List<AssignmentEntry> s; synchronized (MoodTaskApi.class) { s=List.copyOf(ASSIGNMENT_RULES); } for(var e:s) if(e.rule().decide(context)==Decision.DENY)return false; return true; }
    public static boolean canComplete(CompletionContext context) { List<CompletionEntry> s; synchronized (MoodTaskApi.class) { s=List.copyOf(COMPLETION_RULES); } for(var e:s) if(e.rule().decide(context)==Decision.DENY)return false; return true; }

    public static TaskAssignmentResult assignTask(ServerPlayerEntity player, Identifier id) { return state(player).assignSpecific(player, id); }
    public static TaskAssignmentResult assignRandomTask(ServerPlayerEntity player) { return state(player).assignRandomExternal(1); }
    public static TaskAssignmentResult assignRandomTasks(ServerPlayerEntity player, int count) { return state(player).assignRandomExternal(count); }
    public static TaskAssignmentResult fillRandomTaskSlots(ServerPlayerEntity player) { return state(player).assignRandomExternal(state(player).remainingSlots()); }
    public static TaskOperationResult removeTask(ServerPlayerEntity player, Identifier id) { return state(player).removeExternal(id); }
    public static TaskOperationResult completeTask(ServerPlayerEntity player, Identifier id, boolean rewardMood) { return state(player).completeExternal(id, rewardMood); }
    public static boolean hasTask(net.minecraft.entity.player.PlayerEntity player, Identifier id) { return state(player).hasTask(id); }
    public static List<Identifier> getActiveTaskIds(net.minecraft.entity.player.PlayerEntity player) { return state(player).activeTaskIds(); }

    private static MoodTaskState state(net.minecraft.entity.player.PlayerEntity player) {
        return ((MoodTaskBridge)(Object)PlayerMoodComponent.KEY.get(player)).annawathe$taskState();
    }

    public enum Decision { PASS, DENY }
    public enum AssignmentSource { INTERNAL_PRIMARY_COOLDOWN, INTERNAL_SLOT_REFILL, EXTERNAL_RANDOM, EXTERNAL_SPECIFIC }
    public enum AssignmentStatus { SUCCESS, PARTIAL_SUCCESS, INVALID_COUNT, GAME_NOT_RUNNING, PLAYER_NOT_ALIVE, MOOD_NOT_SUPPORTED, TASK_LIMIT_REACHED, TASK_NOT_REGISTERED, TASK_ALREADY_ACTIVE, ASSIGNMENT_DENIED, NO_AVAILABLE_TASK }
    public enum OperationStatus { SUCCESS, TASK_NOT_ACTIVE, COMPLETION_DENIED }
    public record TaskAssignmentResult(AssignmentStatus status, int requested, List<Identifier> assigned, int active, int maximum) { public boolean success(){return status==AssignmentStatus.SUCCESS||status==AssignmentStatus.PARTIAL_SUCCESS;} }
    public record TaskOperationResult(OperationStatus status, Identifier taskId) { public boolean success(){return status==OperationStatus.SUCCESS;} }
    public record AssignmentContext(ServerPlayerEntity player, dev.doctor4t.wathe.cca.GameWorldComponent gameWorld,
                                    @Nullable dev.doctor4t.wathe.api.Role role, Identifier taskId,
                                    MoodTaskDefinition definition, AssignmentSource source, int activeCount, int maximum) {}
    public record CompletionContext(ServerPlayerEntity player, dev.doctor4t.wathe.cca.GameWorldComponent gameWorld,
                                    @Nullable dev.doctor4t.wathe.api.Role role, Identifier taskId,
                                    MoodTaskDefinition definition, boolean rewardMood) {}
    @FunctionalInterface public interface AssignmentRule { Decision decide(AssignmentContext context); }
    @FunctionalInterface public interface CompletionRule { Decision decide(CompletionContext context); }
    private record AssignmentEntry(Identifier id,int priority,long order,AssignmentRule rule){}
    private record CompletionEntry(Identifier id,int priority,long order,CompletionRule rule){}
}
