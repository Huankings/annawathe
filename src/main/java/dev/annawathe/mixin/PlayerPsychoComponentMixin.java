package dev.annawathe.mixin;

import dev.annawathe.api.psycho.PsychoModeApi;
import dev.annawathe.api.psycho.PsychoModeProfile;
import dev.annawathe.bridge.PsychoComponentBridge;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 将自改 Wathe 的 profile 状态机桥接到原版 Wathe 的 wathe:psycho CCA。
 * 服务端负责时间、护盾、临时物品和同步；客户端只根据同步后的字段渲染。
 */
@Mixin(PlayerPsychoComponent.class)
public abstract class PlayerPsychoComponentMixin implements PsychoComponentBridge {
    @Shadow @Final private PlayerEntity player;
    @Shadow public int psychoTicks;
    @Shadow public int armour;
    @Shadow public abstract void sync();

    @Unique private Identifier annawathe$profileId = PsychoModeApi.DEFAULT_PROFILE_ID;
    @Unique private int annawathe$maxTicks = 1;
    @Unique private int annawathe$initialArmour;

    @Inject(method = "startPsycho", at = @At("HEAD"), cancellable = true)
    private void annawathe$replaceVanillaStart(CallbackInfoReturnable<Boolean> cir) {
        // 商店仍调用原版 startPsycho；这里统一转入 Anna profile，避免固定球棒逻辑绕过 profile。
        cir.setReturnValue(annawathe$start(PsychoModeApi.getProfileOrDefault(PsychoModeApi.DEFAULT_PROFILE_ID)));
    }

    @Inject(method = "stopPsycho", at = @At("HEAD"), cancellable = true)
    private void annawathe$replaceVanillaStop(CallbackInfo ci) {
        annawathe$stop(true);
        ci.cancel();
    }

    @Inject(method = "reset", at = @At("HEAD"), cancellable = true)
    private void annawathe$replaceVanillaReset(CallbackInfo ci) {
        // 回合清理不应伪造一条“疯魔结束事件”，因此 reset 使用无回放语义的停止。
        annawathe$stop(false);
        ci.cancel();
    }

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void annawathe$serverTick(CallbackInfo ci) {
        if (psychoTicks <= 0) { ci.cancel(); return; }
        if (psychoTicks <= 1) annawathe$stop(true); else psychoTicks--;
        sync();
        ci.cancel();
    }

    @Inject(method = "clientTick", at = @At("HEAD"), cancellable = true)
    private void annawathe$clientTick(CallbackInfo ci) {
        if (psychoTicks <= 0) { ci.cancel(); return; }
        psychoTicks--;
        if (!PsychoModeApi.isLockedItem(player, player.getMainHandStack())
                && GameFunctions.isPlayerAliveAndSurvival(player)) {
            int slot = PsychoModeApi.findLockedHotbarSlot(player);
            if (slot >= 0) player.getInventory().selectedSlot = slot;
        }
        ci.cancel();
    }

    @Inject(method = "writeToNbt", at = @At("TAIL"))
    private void annawathe$writeProfile(NbtCompound tag, RegistryWrapper.WrapperLookup lookup, CallbackInfo ci) {
        tag.putString("AnnaProfileId", annawathe$profileId.toString());
        tag.putInt("AnnaMaxPsychoTicks", annawathe$maxTicks);
        tag.putInt("AnnaInitialArmour", annawathe$initialArmour);
    }

    @Inject(method = "readFromNbt", at = @At("TAIL"))
    private void annawathe$readProfile(NbtCompound tag, RegistryWrapper.WrapperLookup lookup, CallbackInfo ci) {
        String raw = tag.getString("AnnaProfileId");
        Identifier parsed = raw.isEmpty() ? null : Identifier.tryParse(raw);
        annawathe$profileId = parsed == null ? PsychoModeApi.DEFAULT_PROFILE_ID : parsed;
        annawathe$maxTicks = Math.max(1, tag.contains("AnnaMaxPsychoTicks") ? tag.getInt("AnnaMaxPsychoTicks") : Math.max(1, psychoTicks));
        annawathe$initialArmour = Math.max(0, tag.contains("AnnaInitialArmour") ? tag.getInt("AnnaInitialArmour") : armour);
    }

    @Override public PlayerEntity annawathe$psychoPlayer() { return player; }
    @Override public boolean annawathe$isActive() { return psychoTicks > 0; }

    @Override
    public boolean annawathe$start(PsychoModeProfile profile) {
        if (player.getWorld().isClient || annawathe$isActive()) return false;
        if (profile.grantedItems().size() > countFreeHotbarSlots()) return false;

        int firstSlot = -1;
        for (ItemStack template : profile.grantedItems()) {
            ItemStack granted = template.copy();
            PsychoModeApi.markGrantedItem(profile, granted);
            int slot = insertGrantedItem(granted);
            if (firstSlot < 0) firstSlot = slot;
        }

        annawathe$profileId = profile.id();
        annawathe$maxTicks = profile.durationTicks();
        annawathe$initialArmour = profile.armour();
        psychoTicks = profile.durationTicks();
        armour = profile.armour();
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        game.setPsychosActive(game.getPsychosActive() + 1);
        if (profile.selectFirstGrantedItem()) {
            int locked = PsychoModeApi.findLockedHotbarSlot(player);
            if (locked >= 0) player.getInventory().selectedSlot = locked;
            else if (firstSlot >= 0) player.getInventory().selectedSlot = firstSlot;
        }
        player.playerScreenHandler.sendContentUpdates();
        sync();
        return true;
    }

    @Override
    public void annawathe$stop(boolean recordReplay) {
        boolean wasActive = psychoTicks > 0 || !PsychoModeApi.DEFAULT_PROFILE_ID.equals(annawathe$profileId);
        PsychoModeProfile profile = annawathe$profile();
        if (wasActive && !player.getWorld().isClient) {
            GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
            game.setPsychosActive(Math.max(0, game.getPsychosActive() - 1));
        }
        psychoTicks = 0;
        player.getInventory().remove(stack -> PsychoModeApi.isGrantedForProfile(stack, profile.id()), Integer.MAX_VALUE,
                player.playerScreenHandler.getCraftingInput());
        annawathe$profileId = PsychoModeApi.DEFAULT_PROFILE_ID;
        annawathe$maxTicks = Math.max(1, profile.durationTicks());
        annawathe$initialArmour = profile.armour();
        player.playerScreenHandler.sendContentUpdates();
        sync();
    }

    private int countFreeHotbarSlots() {
        int count = 0;
        for (int i = 0; i < 9; i++) if (player.getInventory().getStack(i).isEmpty()) count++;
        return count;
    }

    private int insertGrantedItem(ItemStack stack) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isEmpty()) {
                player.getInventory().setStack(i, stack);
                return i;
            }
        }
        ShopEntry.insertStackInFreeSlot(player, stack);
        return -1;
    }

    @Override public PsychoModeProfile annawathe$profile() { return PsychoModeApi.getProfileOrDefault(annawathe$profileId); }
    @Override public @NotNull String annawathe$profileId() { return annawathe$profileId.toString(); }
    @Override public int annawathe$maxTicks() { return Math.max(1, annawathe$maxTicks); }
    @Override public int annawathe$initialArmour() { return Math.max(0, annawathe$initialArmour); }
}
