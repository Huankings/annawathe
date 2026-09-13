package dev.annawathe.api.psycho;

import dev.annawathe.bridge.PsychoComponentBridge;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AnnaWathe 的公开疯魔门面。服务端状态仍附着于原版 wathe:psycho CCA，
 * 但 profile、护盾和武器规则全部由 Anna 统一仲裁。
 */
public final class PsychoModeApi {
    public static final Identifier DEFAULT_PROFILE_ID = Wathe.id("psycho_mode");
    public static final String DEFAULT_MODE_NAME_TRANSLATION_KEY = "psycho_mode.wathe.default";
    public static final String DEFAULT_SHIELD_NAME_TRANSLATION_KEY = "psycho_shield.wathe.default";
    public static final int DEFAULT_PRIORITY = 0;
    public static final String REPLAY_MODE_ID_KEY = "psycho_mode";
    public static final String REPLAY_MODE_NAME_KEY = "psycho_mode_name_key";
    public static final String REPLAY_SHIELD_NAME_KEY = "psycho_shield_name_key";

    private static final Map<Identifier, PsychoModeProfile> PROFILES = new LinkedHashMap<>();
    private static final List<StartEntry> START_RULES = new ArrayList<>();
    private static final List<ShieldEntry> SHIELD_RULES = new ArrayList<>();
    private static long order;
    private static boolean initialized;
    private PsychoModeApi() {}

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        registerProfile(createDefaultProfile());
    }

    public static PsychoModeProfile createDefaultProfile() {
        return PsychoModeProfile.builder(DEFAULT_PROFILE_ID)
                .nameTranslationKey(DEFAULT_MODE_NAME_TRANSLATION_KEY)
                .shieldNameTranslationKey(DEFAULT_SHIELD_NAME_TRANSLATION_KEY)
                .durationTicks(GameConstants.PSYCHO_TIMER)
                .armour(GameConstants.PSYCHO_MODE_ARMOUR)
                .grantItem(WatheItems.BAT.getDefaultStack())
                .lockHotbar(true)
                .lockGrantedItems(true)
                .removeGrantedItemsOnEnd(true)
                .selectFirstGrantedItem(true)
                .preventDroppingLockedItems(true)
                .meleeKill(true, GameConstants.DeathReasons.BAT)
                .meleeWeaponPredicate((player, stack) -> stack.isOf(WatheItems.BAT))
                .shieldSourceId(Wathe.id("psycho_mode"))
                .endEventId(Wathe.id("psycho_mode_end"))
                .hitSound(WatheSounds.ITEM_BAT_HIT)
                .shieldSound(WatheSounds.ITEM_PSYCHO_ARMOUR)
                .visualSettings(PsychoVisualSettings.skin(
                        Wathe.id("textures/entity/psycho.png"),
                        Wathe.id("textures/entity/psycho_thin.png"), true))
                .build();
    }

    public static synchronized void registerProfile(@NotNull PsychoModeProfile profile) {
        Objects.requireNonNull(profile);
        PROFILES.put(profile.id(), profile);
    }
    public static synchronized @Nullable PsychoModeProfile getProfile(@NotNull Identifier id) { ensureInitialized(); return PROFILES.get(id); }
    public static synchronized @NotNull PsychoModeProfile getProfileOrDefault(@Nullable Identifier id) {
        ensureInitialized();
        PsychoModeProfile profile = id == null ? null : PROFILES.get(id);
        return profile == null ? PROFILES.get(DEFAULT_PROFILE_ID) : profile;
    }

    public static boolean start(@NotNull PlayerEntity player) { return start(player, DEFAULT_PROFILE_ID); }
    public static boolean start(@NotNull PlayerEntity player, @NotNull Identifier profileId) {
        return start(player, getProfileOrDefault(resolveProfileId(player, profileId)));
    }
    public static boolean start(@NotNull PlayerEntity player, @NotNull PsychoModeProfile profile) {
        PsychoModeProfile resolved = resolveStartProfile(player, profile);
        return ((PsychoComponentBridge)(Object) PlayerPsychoComponent.KEY.get(player)).annawathe$start(resolved);
    }
    public static void stop(@NotNull PlayerEntity player) { stop(player, true); }
    public static void stop(@NotNull PlayerEntity player, boolean recordReplay) {
        ((PsychoComponentBridge)(Object) PlayerPsychoComponent.KEY.get(player)).annawathe$stop(recordReplay);
    }
    public static boolean isActive(@Nullable PlayerEntity player) {
        return player != null && ((PsychoComponentBridge)(Object) PlayerPsychoComponent.KEY.get(player)).annawathe$isActive();
    }
    public static boolean isActive(@Nullable PlayerEntity player, @NotNull Identifier profileId) {
        if (player == null || !isActive(player)) return false;
        return profileId.equals(((PsychoComponentBridge)(Object) PlayerPsychoComponent.KEY.get(player)).annawathe$profile().id());
    }
    public static @Nullable PsychoModeProfile getActiveProfile(@Nullable PlayerEntity player) {
        return player != null && isActive(player) ? ((PsychoComponentBridge)(Object) PlayerPsychoComponent.KEY.get(player)).annawathe$profile() : null;
    }
    public static int getRemainingTicks(@Nullable PlayerEntity player) { return player == null ? 0 : Math.max(0, PlayerPsychoComponent.KEY.get(player).getPsychoTicks()); }
    public static int getArmour(@Nullable PlayerEntity player) { return player == null ? 0 : Math.max(0, PlayerPsychoComponent.KEY.get(player).getArmour()); }
    public static int getMaxTicks(@Nullable PlayerEntity player) { return player == null ? 1 : ((PsychoComponentBridge)(Object) PlayerPsychoComponent.KEY.get(player)).annawathe$maxTicks(); }
    public static int getInitialArmour(@Nullable PlayerEntity player) { return player == null ? 0 : ((PsychoComponentBridge)(Object) PlayerPsychoComponent.KEY.get(player)).annawathe$initialArmour(); }

    public static boolean isLockedItem(@NotNull PlayerEntity player, @NotNull ItemStack stack) {
        PsychoModeProfile profile = getActiveProfile(player);
        return profile != null && profile.lockHotbar() && profile.isLockedItem(player, stack);
    }
    public static boolean shouldPreventDrop(@NotNull PlayerEntity player, @NotNull ItemStack stack) {
        PsychoModeProfile profile = getActiveProfile(player);
        return profile != null && profile.preventDroppingLockedItems() && profile.isLockedItem(player, stack);
    }
    public static int findLockedHotbarSlot(@NotNull PlayerEntity player) {
        PsychoModeProfile profile = getActiveProfile(player);
        if (profile == null || !profile.lockHotbar()) return -1;
        for (int slot = 0; slot < 9; slot++) if (profile.isLockedItem(player, player.getInventory().getStack(slot))) return slot;
        return -1;
    }
    public static boolean isMeleeKillWeapon(@NotNull PlayerEntity player, @NotNull ItemStack stack) {
        if (stack.isOf(WatheItems.BAT)) return true;
        PsychoModeProfile profile = getActiveProfile(player);
        return profile != null && profile.isMeleeWeapon(player, stack);
    }
    public static @Nullable SoundEvent getMeleeHitSound(@NotNull PlayerEntity player, @NotNull ItemStack stack) {
        PsychoModeProfile profile = getActiveProfile(player);
        return profile != null && profile.isMeleeWeapon(player, stack) ? profile.hitSound() : null;
    }

    /** 至少一个当前激活 profile 声明该声音时，客户端背景音才会播放。 */
    public static boolean shouldPlayBackgroundSound(@Nullable World world, @NotNull SoundEvent sound) {
        if (world == null) return false;
        for (PlayerEntity player : world.getPlayers()) {
            PsychoModeProfile profile = getActiveProfile(player);
            if (profile == null || !profile.playBackgroundSound() || profile.backgroundSound() == null) continue;
            if (profile.backgroundSound() == sound
                    || Registries.SOUND_EVENT.getId(profile.backgroundSound()).equals(Registries.SOUND_EVENT.getId(sound))) return true;
        }
        return false;
    }

    public static void markGrantedItem(@NotNull PsychoModeProfile profile, @NotNull ItemStack stack) {
        stack.set(PsychoDataComponentTypes.PSYCHO_GRANTED_PROFILE, profile.id().toString());
    }
    public static boolean isGrantedForProfile(@NotNull ItemStack stack, @NotNull Identifier profileId) {
        return profileId.toString().equals(stack.get(PsychoDataComponentTypes.PSYCHO_GRANTED_PROFILE));
    }

    public static PsychoShieldResult resolveShield(@NotNull PsychoShieldContext context) {
        for (ShieldEntry entry : snapshot(SHIELD_RULES)) {
            PsychoShieldResult result = entry.handler.resolve(context);
            if (result != null && result != PsychoShieldResult.PASS) return result;
        }
        return context.component().getArmour() > 0 ? PsychoShieldResult.BLOCK : PsychoShieldResult.PASS;
    }
    public static synchronized void registerShieldRule(Identifier id, int priority, PsychoShieldRule handler) {
        SHIELD_RULES.removeIf(e -> e.id.equals(id)); SHIELD_RULES.add(new ShieldEntry(id, priority, order++, handler)); sort(SHIELD_RULES);
    }
    public static synchronized void registerStartProfileProvider(Identifier id, int priority, StartProfileProvider provider) {
        START_RULES.removeIf(e -> e.id.equals(id)); START_RULES.add(new StartEntry(id, priority, order++, provider)); sort(START_RULES);
    }

    public static void putModeReplayData(NbtCompound data, PsychoModeProfile profile) {
        data.putString(REPLAY_MODE_ID_KEY, profile.id().toString());
        data.putString(REPLAY_MODE_NAME_KEY, profile.nameTranslationKey());
        data.putString(REPLAY_SHIELD_NAME_KEY, profile.shieldNameTranslationKey());
    }
    public static NbtCompound createModeReplayData(PsychoModeProfile profile) { NbtCompound data = new NbtCompound(); putModeReplayData(data, profile); return data; }
    public static String resolveModeNameTranslationKey(NbtCompound data) { String s = data.getString(REPLAY_MODE_NAME_KEY); return s.isEmpty() ? DEFAULT_MODE_NAME_TRANSLATION_KEY : s; }
    public static String resolveShieldNameTranslationKey(NbtCompound data) { String s = data.getString(REPLAY_SHIELD_NAME_KEY); return s.isEmpty() ? DEFAULT_SHIELD_NAME_TRANSLATION_KEY : s; }

    private static Identifier resolveProfileId(PlayerEntity player, Identifier requested) { return requested; }
    private static PsychoModeProfile resolveStartProfile(PlayerEntity player, PsychoModeProfile requested) {
        for (StartEntry entry : snapshot(START_RULES)) { PsychoModeProfile result = entry.provider.resolve(player, requested); if (result != null) return result; }
        return requested;
    }
    private static synchronized void ensureInitialized() { if (!initialized) init(); }
    private static <T extends Prioritized> List<T> snapshot(List<T> list) { synchronized (list) { return List.copyOf(list); } }
    private static <T extends Prioritized> void sort(List<T> list) { list.sort(Comparator.comparingInt(T::priority).reversed().thenComparing(Comparator.comparingLong(T::order).reversed())); }
    private interface Prioritized { int priority(); long order(); }
    @FunctionalInterface public interface StartProfileProvider { @Nullable PsychoModeProfile resolve(PlayerEntity player, PsychoModeProfile requestedProfile); }
    @FunctionalInterface public interface PsychoShieldRule { @Nullable PsychoShieldResult resolve(PsychoShieldContext context); }
    private record StartEntry(Identifier id, int priority, long order, StartProfileProvider provider) implements Prioritized {}
    private record ShieldEntry(Identifier id, int priority, long order, PsychoShieldRule handler) implements Prioritized {}
}
