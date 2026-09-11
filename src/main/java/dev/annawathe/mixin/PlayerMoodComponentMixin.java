package dev.annawathe.mixin;

import dev.annawathe.mood.MoodTaskState;
import dev.annawathe.bridge.MoodTaskBridge;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用 AnnaWathe 的注册式任务循环接管原版枚举任务循环。
 * 原版 mood 字段仍是唯一真值，避免两个 CCA 各自保存一份心情造成同步分叉。
 */
@Mixin(PlayerMoodComponent.class)
public abstract class PlayerMoodComponentMixin implements MoodTaskBridge {
    @Shadow @Final private PlayerEntity player;
    @Shadow private float mood;
    @Shadow private int nextTaskTimer;
    @Shadow public Map<PlayerMoodComponent.Task, PlayerMoodComponent.TrainTask> tasks;
    @Shadow public Map<PlayerMoodComponent.Task, Integer> timesGotten;
    @Shadow @Final private HashMap<UUID, ItemStack> psychosisItems;
    @Shadow public abstract void sync();

    @Unique private MoodTaskState annawathe$state;
    @Unique private MoodTaskState annawathe$getState() {
        if (annawathe$state == null) annawathe$state = new MoodTaskState((PlayerMoodComponent)(Object)this);
        return annawathe$state;
    }

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void annawathe$serverTick(CallbackInfo ci) {
        annawathe$getState().serverTick();
        ci.cancel();
    }

    @Inject(method = "clientTick", at = @At("HEAD"))
    private void annawathe$clientPredictionAndCleanup(CallbackInfo ci) {
        annawathe$getState().clientPredictMood();
        // 原版在死亡/停局后直接 return，会留下上一局的低心情幻觉缓存；必须先清理。
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (!game.isRunning() || !GameFunctions.isPlayerAliveAndSurvival(player)) psychosisItems.clear();
    }


    @Redirect(method = "clientTick", at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent;setMood(F)V"))
    private void annawathe$preventVanillaClientDrain(PlayerMoodComponent instance, float ignoredMood) {
        // 本地预测已按“任意数量任务只扣一份”执行；阻止原版再次按 enum Map 大小重复扣除。
    }

    @Inject(method = "reset", at = @At("TAIL"))
    private void annawathe$resetTasks(CallbackInfo ci) {
        annawathe$getState().reset();
        sync();
    }

    @Inject(method = "writeToNbt", at = @At("TAIL"))
    private void annawathe$writeState(NbtCompound tag, RegistryWrapper.WrapperLookup lookup, CallbackInfo ci) {
        tag.put("annawathe", annawathe$getState().writeNbt());
    }

    @Inject(method = "readFromNbt", at = @At("TAIL"))
    private void annawathe$readState(NbtCompound tag, RegistryWrapper.WrapperLookup lookup, CallbackInfo ci) {
        annawathe$getState().readNbt(tag.contains("annawathe") ? tag.getCompound("annawathe") : new NbtCompound());
    }

    @Override public PlayerEntity annawathe$player() { return player; }
    @Override public float annawathe$rawMood() { return mood; }
    @Override public void annawathe$setRawMood(float value) { mood = MathHelper.clamp(value, 0F, 1F); }
    @Override public int annawathe$nextTaskTimer() { return nextTaskTimer; }
    @Override public void annawathe$setNextTaskTimer(int ticks) { nextTaskTimer = ticks; }
    @Override public Map<PlayerMoodComponent.Task, PlayerMoodComponent.TrainTask> annawathe$legacyTasks() { return tasks; }
    @Override public Map<PlayerMoodComponent.Task, Integer> annawathe$legacyTimesGotten() { return timesGotten; }
    @Override public MoodTaskState annawathe$taskState() { return annawathe$getState(); }
    @Override public void annawathe$syncMood() { if (!player.getWorld().isClient()) sync(); }
}
