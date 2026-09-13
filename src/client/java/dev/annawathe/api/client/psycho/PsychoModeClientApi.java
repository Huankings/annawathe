package dev.annawathe.api.client.psycho;

import dev.annawathe.api.client.appearance.PlayerAppearanceApi;
import dev.annawathe.api.psycho.PsychoModeApi;
import dev.annawathe.api.psycho.PsychoModeProfile;
import dev.annawathe.api.psycho.PsychoVisualSettings;
import dev.doctor4t.ratatouille.client.util.ambience.AmbienceUtil;
import dev.doctor4t.ratatouille.client.util.ambience.BackgroundAmbience;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Psycho 的客户端视觉和背景音乐公开入口；只影响显示，不改变服务端身份或权限。 */
public final class PsychoModeClientApi {
    public static final int DEFAULT_VISUAL_PRIORITY = 0;
    public static final int PLAYER_APPEARANCE_PRIORITY = 10_000;
    private static final List<Entry> VISUALS = new ArrayList<>();
    private static final Set<Identifier> AMBIENCE = new HashSet<>();
    private static long order;
    private static boolean initialized;
    private PsychoModeClientApi() {}

    public static synchronized void registerDefaultClientHandlers() {
        if (initialized) return;
        initialized = true;
        PlayerAppearanceApi.registerPlayerSkin(Wathe.id("psycho/default_player_skin"), PLAYER_APPEARANCE_PRIORITY,
                PsychoModeClientApi::resolveSkinTextures);
        registerBackgroundAmbience(WatheSounds.AMBIENT_PSYCHO_DRONE, 20);
    }

    public static synchronized void registerVisualProvider(Identifier id, int priority, VisualProvider provider) {
        Objects.requireNonNull(id); Objects.requireNonNull(provider);
        VISUALS.removeIf(e -> e.id.equals(id));
        VISUALS.add(new Entry(id, priority, order++, provider));
        sort(VISUALS);
    }

    public static void registerBackgroundAmbience(@NotNull SoundEvent sound, int intervalTicks) {
        Identifier id = Registries.SOUND_EVENT.getId(sound);
        synchronized (AMBIENCE) {
            if (!AMBIENCE.add(id)) return;
        }
        AmbienceUtil.registerBackgroundAmbience(new BackgroundAmbience(sound,
                player -> PsychoModeApi.shouldPlayBackgroundSound(player.getWorld(), sound), intervalTicks));
    }

    public static @Nullable PsychoVisualSettings resolveVisualSettings(@NotNull AbstractClientPlayerEntity player) {
        PlayerPsychoComponent component = PlayerPsychoComponent.KEY.get(player);
        if (!PsychoModeApi.isActive(player)) return null;
        PsychoModeProfile profile = PsychoModeApi.getActiveProfile(player);
        if (profile == null) return null;
        List<Entry> entries;
        synchronized (VISUALS) { entries = List.copyOf(VISUALS); }
        for (Entry entry : entries) {
            PsychoVisualSettings result = entry.provider.resolve(player, component, profile);
            if (result != null) return result;
        }
        return profile.visualSettings();
    }

    public static @Nullable SkinTextures resolveSkinTextures(@NotNull AbstractClientPlayerEntity player) {
        PsychoVisualSettings visual = resolveVisualSettings(player);
        if (visual == null) return null;
        SkinTextures original = PlayerAppearanceApi.resolveOriginalSkinTextures(player.getUuid(), true);
        Identifier texture = visual.texture(original.model() == SkinTextures.Model.SLIM);
        if (texture == null) return null;
        return new SkinTextures(texture, original.textureUrl(), original.capeTexture(), original.elytraTexture(), original.model(), original.secure());
    }

    public static boolean shouldHideFeatures(@NotNull AbstractClientPlayerEntity player) {
        PsychoVisualSettings visual = resolveVisualSettings(player);
        return visual != null && visual.hideFeatures();
    }

    private static void sort(List<Entry> entries) {
        entries.sort(Comparator.comparingInt((Entry e) -> e.priority).reversed()
                .thenComparing(Comparator.comparingLong((Entry e) -> e.order).reversed()));
    }

    @FunctionalInterface
    public interface VisualProvider {
        @Nullable PsychoVisualSettings resolve(@NotNull AbstractClientPlayerEntity player,
                                               @NotNull PlayerPsychoComponent component,
                                               @NotNull PsychoModeProfile profile);
    }
    private record Entry(Identifier id, int priority, long order, VisualProvider provider) {}
}
