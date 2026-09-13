package dev.annawathe.mood;

import dev.annawathe.api.mood.MoodApi;
import dev.annawathe.api.task.MoodTaskApi;
import dev.annawathe.api.task.MoodTaskDefinition;
import dev.annawathe.api.task.MoodTaskInstance;
import dev.annawathe.api.task.TaskCompletionApi;
import dev.annawathe.cca.AnnaMoodSettings;
import dev.annawathe.bridge.MoodTaskBridge;
import dev.annawathe.compat.wathe.WatheTaskCompleteNotifier;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.doctor4t.wathe.game.GameConstants.getInTicks;

/**
 * 附着在原版 PlayerMoodComponent 上的 AnnaWathe 任务状态。
 * 服务端是任务进度和心情的唯一权威；客户端副本只用于平滑 HUD 和读取任务 ID。
 */
public final class MoodTaskState {
    public static final int MAX_TASKS = 3;
    public static final int TASK_DURATION = getInTicks(0, 8);
    /**
     * 完成一个真实心情任务回复的心情值。
     * 固定采用自改 Wathe 的 0.4，不读取原版 Wathe 1.3.2 的 0.5，避免单次任务回复过多。
     */
    public static final float MOOD_GAIN = 0.4F;
    /**
     * 有任意任务时每 tick 的基础心情下降值。
     * 1 / 4000 表示持续约 3 分 20 秒会从满心情降到零；多任务仍只扣一份。
     */
    //public static final float MOOD_DRAIN = 1F / (3 * 60 * 20 + 20 * 20);
    public static final float MOOD_DRAIN = 1F / getInTicks(3, 20);
    public static final float SECOND_TASK_THRESHOLD = 0.51F;
    public static final float THIRD_TASK_THRESHOLD = 0.17F;
    public static final float BREAKDOWN_WARNING_THRESHOLD = 0.15F;
    public static final double AWAY_RANGE = 12D;

    private final PlayerMoodComponent component;
    private final MoodTaskBridge bridge;
    private final LinkedHashMap<Identifier, MoodTaskInstance> activeTasks = new LinkedHashMap<>();
    private final HashMap<Identifier, Integer> timesGotten = new HashMap<>();
    private final HashMap<Identifier, Integer> stuckCounts = new HashMap<>();
    private float drainMultiplier = 1F;
    private int drainProtectionTicks;
    private boolean dirty;

    public MoodTaskState(PlayerMoodComponent component) {
        this.component = component;
        this.bridge = (MoodTaskBridge) (Object) component;
    }

    public List<Identifier> activeTaskIds() { return List.copyOf(activeTasks.keySet()); }
    public boolean hasTask(Identifier id) { return activeTasks.containsKey(id); }
    public int activeCount() { return activeTasks.size(); }
    public int remainingSlots() { return Math.max(0, MAX_TASKS - activeTasks.size()); }
    public float getDrainMultiplier() { return drainMultiplier; }
    public int getDrainProtectionTicks() { return drainProtectionTicks; }

    public void setDrainMultiplier(float value) {
        drainMultiplier = Math.max(0F, value);
        dirty = true;
        syncNow();
    }
    public void protectFromDrain(int ticks) {
        drainProtectionTicks = Math.max(drainProtectionTicks, Math.max(0, ticks));
        dirty = true;
        syncNow();
    }
    public void clearExternalDrainState() {
        drainMultiplier = 1F;
        drainProtectionTicks = 0;
        dirty = true;
        syncNow();
    }

    public void reset() {
        activeTasks.clear(); timesGotten.clear(); stuckCounts.clear();
        drainMultiplier = 1F; drainProtectionTicks = 0; dirty = false;
        bridge.annawathe$legacyTasks().clear();
        bridge.annawathe$legacyTimesGotten().clear();
    }

    public void clientPredictMood() {
        PlayerEntity player = bridge.annawathe$player();
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game.getRole(player);
        if (drainProtectionTicks > 0) drainProtectionTicks--;
        if (game.isRunning() && GameFunctions.isPlayerAliveAndSurvival(player) && isReal(role)
                && !activeTasks.isEmpty() && drainProtectionTicks <= 0) {
            bridge.annawathe$setRawMood(bridge.annawathe$rawMood() - MOOD_DRAIN * drainMultiplier);
        }
    }

    public void serverTick() {
        PlayerEntity player = bridge.annawathe$player();
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (!game.isRunning() || !GameFunctions.isPlayerAliveAndSurvival(player)) return;
        Role role = game.getRole(player);
        if (!isReal(role) && !isFake(role)) {
            // 转成 MoodType.NONE 后不能把上一职业的任务留到后续转职或下一局。
            if (!activeTasks.isEmpty()) { reset(); dirty = true; syncNow(); }
            return;
        }

        if (drainProtectionTicks > 0) {
            drainProtectionTicks--;
            // 保护倒计时不需要每 tick 刷整份 CCA；每秒和归零时同步一次即可。
            if (drainProtectionTicks == 0 || drainProtectionTicks % 20 == 0) dirty = true;
        }
        if (isReal(role) && !activeTasks.isEmpty() && drainProtectionTicks <= 0) {
            bridge.annawathe$setRawMood(bridge.annawathe$rawMood() - MOOD_DRAIN * drainMultiplier);
            dirty = true;
        }

        if (isReal(role)) fillToExpected(expectedCount());
        tickPrimaryCooldown(isReal(role));

        ArrayList<Identifier> completed = new ArrayList<>();
        for (var entry : new ArrayList<>(activeTasks.entrySet())) {
            entry.getValue().tick(player);
            if (entry.getValue().isFulfilled(player)) completed.add(entry.getKey());
        }
        for (Identifier id : completed) complete(id, isReal(role));

        // 完成任务可能在同一 tick 把心情拉回零以上，所以死亡仲裁必须放在完成结算之后。
        if (isReal(role) && bridge.annawathe$rawMood() <= 0F
                && AnnaMoodSettings.KEY.get(player.getWorld()).isMoodDeathEnabled()
                && GameFunctions.isPlayerAliveAndSurvival(player)) {
            GameFunctions.killPlayer(player, true, null, MoodApi.MENTAL_BREAKDOWN);
            return;
        }
        if (dirty) syncNow();
    }

    private int expectedCount() {
        float mood = bridge.annawathe$rawMood();
        return mood <= THIRD_TASK_THRESHOLD ? 3 : mood <= SECOND_TASK_THRESHOLD ? 2 : 1;
    }
    private void tickPrimaryCooldown(boolean refillExtra) {
        if (!activeTasks.isEmpty()) return;
        int timer = bridge.annawathe$nextTaskTimer() - 1;
        bridge.annawathe$setNextTaskTimer(timer);
        if (timer > 0) return;
        if (assignRandom(MoodTaskApi.AssignmentSource.INTERNAL_PRIMARY_COOLDOWN) != null && refillExtra) fillToExpected(expectedCount());
        bridge.annawathe$setNextTaskTimer(randomCooldown());
        dirty = true;
    }
    private void fillToExpected(int expected) {
        if (activeTasks.isEmpty()) return;
        while (activeTasks.size() < Math.min(MAX_TASKS, expected)
                && assignRandom(MoodTaskApi.AssignmentSource.INTERNAL_SLOT_REFILL) != null) {}
    }
    private int randomCooldown() {
        int value = (int)(bridge.annawathe$player().getRandom().nextFloat()
                * (GameConstants.MAX_TASK_COOLDOWN - GameConstants.MIN_TASK_COOLDOWN) + GameConstants.MIN_TASK_COOLDOWN);
        return Math.max(2, value);
    }

    private @Nullable Identifier assignRandom(MoodTaskApi.AssignmentSource source) {
        HashMap<Identifier, Float> weights = new HashMap<>();
        float total = 0F;
        for (MoodTaskDefinition definition : MoodTaskApi.getRandomAssignableDefinitions()) {
            if (activeTasks.containsKey(definition.id()) || !canAssign(definition, source)) continue;
            float weight = definition.randomWeight() / Math.max(1, timesGotten.getOrDefault(definition.id(), 0) + 1);
            weights.put(definition.id(), weight); total += weight;
        }
        if (total <= 0F) return null;
        float random = bridge.annawathe$player().getRandom().nextFloat() * total;
        for (var entry : weights.entrySet()) {
            random -= entry.getValue();
            if (random <= 0F) {
                MoodTaskDefinition definition = MoodTaskApi.getDefinition(entry.getKey());
                if (definition == null) return null;
                put(definition.id(), definition.create(bridge.annawathe$player()));
                timesGotten.merge(definition.id(), 1, Integer::sum);
                syncLegacyTimes(definition);
                return definition.id();
            }
        }
        return null;
    }

    private boolean canAssign(MoodTaskDefinition definition, MoodTaskApi.AssignmentSource source) {
        if (!(bridge.annawathe$player() instanceof ServerPlayerEntity serverPlayer)) return true;
        GameWorldComponent game = GameWorldComponent.KEY.get(serverPlayer.getWorld());
        return MoodTaskApi.canAssign(new MoodTaskApi.AssignmentContext(serverPlayer, game, game.getRole(serverPlayer),
                definition.id(), definition, source, activeTasks.size(), MAX_TASKS));
    }

    public MoodTaskApi.TaskAssignmentResult assignSpecific(ServerPlayerEntity player, Identifier id) {
        var invalid = validate(player, 1);
        if (invalid != null) return result(invalid, 1, List.of());
        MoodTaskDefinition definition = MoodTaskApi.getDefinition(id);
        if (definition == null) return result(MoodTaskApi.AssignmentStatus.TASK_NOT_REGISTERED, 1, List.of());
        if (activeTasks.containsKey(id)) return result(MoodTaskApi.AssignmentStatus.TASK_ALREADY_ACTIVE, 1, List.of());
        if (!canAssign(definition, MoodTaskApi.AssignmentSource.EXTERNAL_SPECIFIC)) return result(MoodTaskApi.AssignmentStatus.ASSIGNMENT_DENIED, 1, List.of());
        put(id, definition.create(player)); syncNow();
        return result(MoodTaskApi.AssignmentStatus.SUCCESS, 1, List.of(id));
    }

    public MoodTaskApi.TaskAssignmentResult assignRandomExternal(int requested) {
        if (!(bridge.annawathe$player() instanceof ServerPlayerEntity player)) return result(MoodTaskApi.AssignmentStatus.PLAYER_NOT_ALIVE, requested, List.of());
        var invalid = validate(player, requested);
        if (invalid != null) return result(invalid, requested, List.of());
        int wanted = Math.min(requested, remainingSlots());
        ArrayList<Identifier> assigned = new ArrayList<>();
        while (assigned.size() < wanted) { Identifier id = assignRandom(MoodTaskApi.AssignmentSource.EXTERNAL_RANDOM); if (id == null) break; assigned.add(id); }
        if (!assigned.isEmpty()) syncNow();
        if (assigned.isEmpty()) return result(MoodTaskApi.AssignmentStatus.NO_AVAILABLE_TASK, requested, assigned);
        return result(assigned.size() == requested ? MoodTaskApi.AssignmentStatus.SUCCESS : MoodTaskApi.AssignmentStatus.PARTIAL_SUCCESS, requested, assigned);
    }

    private @Nullable MoodTaskApi.AssignmentStatus validate(ServerPlayerEntity player, int count) {
        if (count <= 0) return MoodTaskApi.AssignmentStatus.INVALID_COUNT;
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (!game.isRunning()) return MoodTaskApi.AssignmentStatus.GAME_NOT_RUNNING;
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) return MoodTaskApi.AssignmentStatus.PLAYER_NOT_ALIVE;
        Role role = game.getRole(player);
        if (!isReal(role) && !isFake(role)) return MoodTaskApi.AssignmentStatus.MOOD_NOT_SUPPORTED;
        if (remainingSlots() <= 0) return MoodTaskApi.AssignmentStatus.TASK_LIMIT_REACHED;
        return null;
    }
    private MoodTaskApi.TaskAssignmentResult result(MoodTaskApi.AssignmentStatus status,int requested,List<Identifier> ids) {
        return new MoodTaskApi.TaskAssignmentResult(status, requested, List.copyOf(ids), activeTasks.size(), MAX_TASKS);
    }

    public MoodTaskApi.TaskOperationResult removeExternal(Identifier id) {
        if (remove(id) == null) return new MoodTaskApi.TaskOperationResult(MoodTaskApi.OperationStatus.TASK_NOT_ACTIVE, id);
        syncNow(); return new MoodTaskApi.TaskOperationResult(MoodTaskApi.OperationStatus.SUCCESS, id);
    }
    public MoodTaskApi.TaskOperationResult completeExternal(Identifier id, boolean reward) {
        if (!activeTasks.containsKey(id)) return new MoodTaskApi.TaskOperationResult(MoodTaskApi.OperationStatus.TASK_NOT_ACTIVE, id);
        if (!complete(id, reward)) return new MoodTaskApi.TaskOperationResult(MoodTaskApi.OperationStatus.COMPLETION_DENIED, id);
        syncNow(); return new MoodTaskApi.TaskOperationResult(MoodTaskApi.OperationStatus.SUCCESS, id);
    }

    private boolean complete(Identifier id, boolean rewardMood) {
        MoodTaskDefinition definition = MoodTaskApi.getDefinition(id);
        if (definition == null || !activeTasks.containsKey(id)) return false;
        if (bridge.annawathe$player() instanceof ServerPlayerEntity serverPlayer) {
            GameWorldComponent game = GameWorldComponent.KEY.get(serverPlayer.getWorld());
            if (!MoodTaskApi.canComplete(new MoodTaskApi.CompletionContext(serverPlayer, game, game.getRole(serverPlayer), id, definition, rewardMood))) return false;
        }
        remove(id);
        // 必须经过原版公共 setMood，让旧扩展对该方法的合法监听仍然生效。
        if (rewardMood) component.setMood(bridge.annawathe$rawMood() + MOOD_GAIN);
        if (bridge.annawathe$player() instanceof ServerPlayerEntity serverPlayer) {
            // Wathe 1.3.2/1.4.1 的原版完成提示 payload 包路径不同，由兼容层运行时选择。
            WatheTaskCompleteNotifier.send(serverPlayer);
            GameWorldComponent game = GameWorldComponent.KEY.get(serverPlayer.getWorld());
            TaskCompletionApi.dispatch(new TaskCompletionApi.TaskCompletionContext(serverPlayer, game, game.getRole(serverPlayer), id, definition, rewardMood));
        }
        for (Identifier remaining : activeTasks.keySet()) stuckCounts.merge(remaining, 1, Integer::sum);
        clearStuck();
        if (activeTasks.isEmpty()) bridge.annawathe$setNextTaskTimer(randomCooldown()); else fillToExpected(expectedCount());
        dirty = true;
        return true;
    }

    private void clearStuck() {
        List<Identifier> six = activeTasks.keySet().stream().filter(id -> stuckCounts.getOrDefault(id,0) >= 6).toList();
        if (six.size() >= 2) { for (Identifier id : six) remove(id); resetStuck(); return; }
        List<Identifier> four = activeTasks.keySet().stream().filter(id -> stuckCounts.getOrDefault(id,0) >= 4).toList();
        if (four.size() == 1) { remove(four.getFirst()); resetStuck(); }
    }
    private void resetStuck() { stuckCounts.clear(); for(Identifier id:activeTasks.keySet())stuckCounts.put(id,0); }

    private void put(Identifier id, MoodTaskInstance instance) {
        activeTasks.put(id, instance); stuckCounts.put(id, 0); dirty = true;
        MoodTaskDefinition definition = MoodTaskApi.getDefinition(id);
        if (definition != null && definition.legacyTask() != null && instance instanceof BuiltInMoodTasks.Legacy legacy)
            bridge.annawathe$legacyTasks().put(definition.legacyTask(), legacy.delegate());
    }
    private @Nullable MoodTaskInstance remove(Identifier id) {
        MoodTaskInstance removed = activeTasks.remove(id); stuckCounts.remove(id);
        MoodTaskDefinition definition = MoodTaskApi.getDefinition(id);
        if (removed != null && definition != null && definition.legacyTask() != null) bridge.annawathe$legacyTasks().remove(definition.legacyTask());
        if (removed != null) dirty = true;
        return removed;
    }
    private void syncLegacyTimes(MoodTaskDefinition definition) {
        if (definition.legacyTask() != null) bridge.annawathe$legacyTimesGotten().put(definition.legacyTask(), timesGotten.getOrDefault(definition.id(),0));
    }
    private void syncNow() { if (dirty) { bridge.annawathe$syncMood(); dirty = false; } }

    public NbtCompound writeNbt() {
        NbtCompound root = new NbtCompound();
        root.putInt("NextTaskTimer", bridge.annawathe$nextTaskTimer());
        root.putFloat("DrainMultiplier", drainMultiplier);
        root.putInt("DrainProtectionTicks", drainProtectionTicks);
        NbtList tasks = new NbtList();
        for (var entry : activeTasks.entrySet()) { NbtCompound n = entry.getValue().writeNbt(); n.putString("id", entry.getKey().toString()); tasks.add(n); }
        root.put("Tasks", tasks);
        NbtCompound times = new NbtCompound(); for(var e:timesGotten.entrySet())times.putInt(e.getKey().toString(),e.getValue()); root.put("TimesGotten",times);
        NbtCompound stuck = new NbtCompound(); for(var e:stuckCounts.entrySet())stuck.putInt(e.getKey().toString(),e.getValue()); root.put("StuckCounts",stuck);
        return root;
    }

    public void readNbt(@NotNull NbtCompound root) {
        activeTasks.clear(); timesGotten.clear(); stuckCounts.clear();
        bridge.annawathe$setNextTaskTimer(root.contains("NextTaskTimer", NbtElement.INT_TYPE) ? root.getInt("NextTaskTimer") : GameConstants.TIME_TO_FIRST_TASK);
        drainMultiplier = root.contains("DrainMultiplier",NbtElement.FLOAT_TYPE) ? Math.max(0F,root.getFloat("DrainMultiplier")) : 1F;
        drainProtectionTicks = root.contains("DrainProtectionTicks",NbtElement.INT_TYPE) ? Math.max(0,root.getInt("DrainProtectionTicks")) : 0;
        if (root.contains("Tasks",NbtElement.LIST_TYPE)) for(NbtElement e:root.getList("Tasks",NbtElement.COMPOUND_TYPE)) {
            NbtCompound n=(NbtCompound)e; Identifier id=Identifier.tryParse(n.getString("id")); MoodTaskDefinition d=id==null?null:MoodTaskApi.getDefinition(id);
            if(d!=null)put(id,d.read(bridge.annawathe$player(),n));
        } else {
            // 首次安装 AnnaWathe 时，把原版已经读取出的四个任务迁入稳定 ID 表。
            for(var e:new ArrayList<>(bridge.annawathe$legacyTasks().entrySet())) put(MoodTaskApi.getTaskId(e.getKey()),new BuiltInMoodTasks.Legacy(e.getValue()));
        }
        readIntMap(root,"TimesGotten",timesGotten); readIntMap(root,"StuckCounts",stuckCounts);
        for(Identifier id:activeTasks.keySet())stuckCounts.putIfAbsent(id,0);
        for(MoodTaskDefinition d:MoodTaskApi.getDefinitions())syncLegacyTimes(d);
        dirty=false;
    }
    private static void readIntMap(NbtCompound root,String key,Map<Identifier,Integer> target) {
        if(!root.contains(key,NbtElement.COMPOUND_TYPE))return; NbtCompound n=root.getCompound(key);
        for(String raw:n.getKeys()){Identifier id=Identifier.tryParse(raw);if(id!=null&&n.contains(raw,NbtElement.INT_TYPE))target.put(id,n.getInt(raw));}
    }
    private static boolean isReal(@Nullable Role role){return role!=null&&role.getMoodType()==Role.MoodType.REAL;}
    private static boolean isFake(@Nullable Role role){return role!=null&&role.getMoodType()==Role.MoodType.FAKE;}
}
