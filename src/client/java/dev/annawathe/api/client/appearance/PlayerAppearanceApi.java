package dev.annawathe.api.client.appearance;

import dev.annawathe.cca.PlayerAppearanceOverrideComponent;
import dev.annawathe.bridge.PlayerBodyAppearanceBridge;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import java.util.*;

@Environment(EnvType.CLIENT)
/**
 * 客户端玩家/尸体皮肤解析链。
 * 原始皮肤按 UUID 从玩家列表和缓存读取，避免把已经覆盖过的临时皮肤再次套用。
 */
public final class PlayerAppearanceApi {
    private static final UUID FALLBACK_PLAYER_UUID = UUID.fromString("25adae11-cd98-48f4-990b-9fe1b2ee0886");
    public static final int DEFAULT_PRIORITY = 0;
    private static final List<Entry<PlayerSkinHandler>> PLAYER = new ArrayList<>();
    private static final List<Entry<BodySkinHandler>> BODY = new ArrayList<>();
    private static long order;
    private PlayerAppearanceApi() {}
    public static void registerPlayerSkin(Identifier id, int priority, PlayerSkinHandler handler) { synchronized (PLAYER) { PLAYER.removeIf(e -> e.id.equals(id)); PLAYER.add(new Entry<>(id, priority, order++, handler)); sort(PLAYER); } }
    public static void registerBodySkin(Identifier id, int priority, BodySkinHandler handler) { synchronized (BODY) { BODY.removeIf(e -> e.id.equals(id)); BODY.add(new Entry<>(id, priority, order++, handler)); sort(BODY); } }
    public static @Nullable SkinTextures resolvePlayerSkin(AbstractClientPlayerEntity player) {
        List<Entry<PlayerSkinHandler>> snapshot; synchronized (PLAYER) { snapshot = List.copyOf(PLAYER); }
        for (var e : snapshot) { SkinTextures result = e.handler.getSkin(player); if (result != null) return result; }
        // 调试变形只作为低优先级兜底，不压过职业伪装、灵术师或时间狭缝视角。
        var override = PlayerAppearanceOverrideComponent.KEY.get(player);
        if (override.isActive() && override.getTargetUuid() != null) return resolveOriginalSkinTextures(override.getTargetUuid(), true);
        return null;
    }
    public static SkinTextures resolveBodySkin(PlayerBodyEntity body) {
        List<Entry<BodySkinHandler>> snapshot; synchronized (BODY) { snapshot = List.copyOf(BODY); }
        for (var e : snapshot) { SkinTextures result = e.handler.getSkin(body); if (result != null) return result; }
        UUID appearance = body instanceof PlayerBodyAppearanceBridge bridge && bridge.annawathe$getAppearanceUuid() != null
                ? bridge.annawathe$getAppearanceUuid() : body.getPlayerUuid();
        return resolveOriginalSkinTextures(appearance, true);
    }
    public static SkinTextures resolveOriginalSkinTextures(@Nullable UUID uuid, boolean fallback) {
        UUID id = uuid == null ? FALLBACK_PLAYER_UUID : uuid;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) { PlayerListEntry entry = client.player.networkHandler.getPlayerListEntry(id); if (entry != null) return entry.getSkinTextures(); }
        if (fallback) { PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(id); if (entry != null) return entry.getSkinTextures(); }
        return DefaultSkinHelper.getSkinTextures(id);
    }
    public static @Nullable String resolveOriginalPlayerName(@Nullable UUID uuid) {
        if (uuid == null) return null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) { PlayerListEntry entry = client.player.networkHandler.getPlayerListEntry(uuid); if (entry != null && entry.getProfile() != null) return entry.getProfile().getName(); }
        PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(uuid); return entry == null || entry.getProfile() == null ? null : entry.getProfile().getName();
    }
    private static <T> void sort(List<Entry<T>> list) { list.sort(Comparator.comparingInt((Entry<T> e) -> e.priority).reversed().thenComparing(Comparator.comparingLong((Entry<T> e) -> e.order).reversed())); }
    @FunctionalInterface public interface PlayerSkinHandler { @Nullable SkinTextures getSkin(AbstractClientPlayerEntity player); }
    @FunctionalInterface public interface BodySkinHandler { @Nullable SkinTextures getSkin(PlayerBodyEntity body); }
    private record Entry<T>(Identifier id, int priority, long order, T handler) {}
}
