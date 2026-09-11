package dev.annawathe.mixin;

import dev.annawathe.bridge.PlayerBodyAppearanceBridge;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import java.util.Optional;
import net.minecraft.nbt.NbtCompound;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

/** 给原版尸体补充可同步、可存档的 appearance UUID，同时保留真实 owner 字段不变。 */
@Mixin(PlayerBodyEntity.class)
public abstract class PlayerBodyEntityMixin implements PlayerBodyAppearanceBridge {
    @Unique private static final TrackedData<Optional<UUID>> ANNA_APPEARANCE = DataTracker.registerData(PlayerBodyEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    @Inject(method="initDataTracker", at=@At("TAIL")) private void annawathe$initTracker(DataTracker.Builder builder, CallbackInfo ci) { builder.add(ANNA_APPEARANCE, Optional.empty()); }
    @Override public @Nullable UUID annawathe$getAppearanceUuid() { return ((PlayerBodyEntity)(Object)this).getDataTracker().get(ANNA_APPEARANCE).orElse(null); }
    @Override public void annawathe$setAppearanceUuid(@Nullable UUID uuid) { ((PlayerBodyEntity)(Object)this).getDataTracker().set(ANNA_APPEARANCE, Optional.ofNullable(uuid)); }
    @Inject(method="writeCustomDataToNbt", at=@At("TAIL")) private void annawathe$write(NbtCompound nbt, CallbackInfo ci) { UUID uuid = annawathe$getAppearanceUuid(); if (uuid != null) nbt.putUuid("AnnaAppearancePlayer", uuid); }
    @Inject(method="readCustomDataFromNbt", at=@At("TAIL")) private void annawathe$read(NbtCompound nbt, CallbackInfo ci) { annawathe$setAppearanceUuid(nbt.containsUuid("AnnaAppearancePlayer") ? nbt.getUuid("AnnaAppearancePlayer") : null); }
}
