package dev.annawathe.bridge;

import dev.annawathe.mood.MoodTaskState;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import java.util.Map;

/** Mixin 注入到原版 PlayerMoodComponent 的内部桥；扩展 Mod 不应直接使用。 */
public interface MoodTaskBridge {
    PlayerEntity annawathe$player();
    float annawathe$rawMood();
    void annawathe$setRawMood(float mood);
    int annawathe$nextTaskTimer();
    void annawathe$setNextTaskTimer(int ticks);
    Map<PlayerMoodComponent.Task, PlayerMoodComponent.TrainTask> annawathe$legacyTasks();
    Map<PlayerMoodComponent.Task, Integer> annawathe$legacyTimesGotten();
    MoodTaskState annawathe$taskState();
    void annawathe$syncMood();
}
