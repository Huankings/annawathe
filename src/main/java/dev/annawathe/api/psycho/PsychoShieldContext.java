package dev.annawathe.api.psycho;

import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 服务端一次“可能击中疯魔玩家”的只读上下文。 */
public record PsychoShieldContext(
        @NotNull PlayerEntity victim,
        @Nullable PlayerEntity killer,
        @NotNull Identifier deathReason,
        @NotNull PlayerPsychoComponent component,
        @NotNull PsychoModeProfile profile,
        @NotNull NbtCompound replayData
) {}
