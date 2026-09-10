package dev.annawathe.api.mood;

import dev.annawathe.cca.AnnaMoodSettings;
import dev.annawathe.bridge.MoodTaskBridge;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

/** 心情值、下降控制和精神崩溃开关的稳定公开门面。 */
public final class MoodApi {
    public static final Identifier MENTAL_BREAKDOWN = Identifier.of("wathe", "mental_breakdown");
    private MoodApi() {}
    public static float getMood(@NotNull PlayerEntity player) { return PlayerMoodComponent.KEY.get(player).getMood(); }
    public static void setMood(@NotNull PlayerEntity player, float mood) { PlayerMoodComponent.KEY.get(player).setMood(mood); }
    public static void setDrainMultiplier(@NotNull PlayerEntity player, float multiplier) { state(player).setDrainMultiplier(multiplier); }
    public static float getDrainMultiplier(@NotNull PlayerEntity player) { return state(player).getDrainMultiplier(); }
    public static void protectFromDrain(@NotNull PlayerEntity player, int ticks) { state(player).protectFromDrain(ticks); }
    public static int getDrainProtectionTicks(@NotNull PlayerEntity player) { return state(player).getDrainProtectionTicks(); }
    public static void clearExternalDrainState(@NotNull PlayerEntity player) { state(player).clearExternalDrainState(); }
    public static boolean isMoodDeathEnabled(@NotNull PlayerEntity player) { return AnnaMoodSettings.KEY.get(player.getWorld()).isMoodDeathEnabled(); }
    public static void setMoodDeathEnabled(@NotNull PlayerEntity player, boolean enabled) { AnnaMoodSettings.KEY.get(player.getWorld()).setMoodDeathEnabled(enabled); }
    private static dev.annawathe.mood.MoodTaskState state(PlayerEntity player) {
        return ((MoodTaskBridge) (Object) PlayerMoodComponent.KEY.get(player)).annawathe$taskState();
    }
}
