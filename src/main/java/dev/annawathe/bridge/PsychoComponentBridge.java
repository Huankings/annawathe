package dev.annawathe.bridge;

import dev.annawathe.api.psycho.PsychoModeProfile;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 原版 Wathe 的 PlayerPsychoComponent 通过 Mixin 暴露给 AnnaWathe 的最小桥接面。
 * 扩展只依赖 PsychoModeApi，不直接读取这个内部桥接接口。
 */
public interface PsychoComponentBridge {
    PlayerEntity annawathe$psychoPlayer();
    boolean annawathe$isActive();
    boolean annawathe$start(PsychoModeProfile profile);
    void annawathe$stop(boolean recordReplay);
    PsychoModeProfile annawathe$profile();
    @Nullable String annawathe$profileId();
    int annawathe$maxTicks();
    int annawathe$initialArmour();
}
