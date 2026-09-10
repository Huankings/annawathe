package dev.annawathe.client;

import dev.annawathe.AnnaWathe;
import dev.annawathe.api.instinct.InstinctApi;
import dev.annawathe.client.gui.AnnaMoodRenderer;
import dev.annawathe.client.gui.AnnaRoundTextRenderer;
import dev.annawathe.client.task.TaskPointClientState;
import dev.annawathe.client.task.TaskPointOverlayRenderer;
import dev.annawathe.client.tooltip.ItemTooltipApi;
import dev.annawathe.cca.PlayerInstinctComponent;
import dev.annawathe.network.TaskPointSyncPayload;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.entity.FirecrackerEntity;
import dev.doctor4t.wathe.entity.NoteEntity;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/** 客户端入口：本能、Tooltip、结算、Mood HUD 与任务点透视。 */
public final class AnnaWatheClient implements ClientModInitializer {
    public static boolean instinctToggleActive;
    private static KeyBinding taskPointKey;

    @Override public void onInitializeClient() {
        registerDefaultInstinctRules();
        ItemTooltipApi.initialize();
        ItemTooltipApi.registerItems(WatheItems.KNIFE, WatheItems.REVOLVER, WatheItems.DERRINGER, WatheItems.GRENADE,
                WatheItems.PSYCHO_MODE, WatheItems.POISON_VIAL, WatheItems.SCORPION, WatheItems.FIRECRACKER,
                WatheItems.LOCKPICK, WatheItems.CROWBAR, WatheItems.BODY_BAG, WatheItems.BLACKOUT, WatheItems.NOTE);

        taskPointKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.annawathe.task_points", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y, "category.wathe.keybinds"));
        ClientPlayNetworking.registerGlobalReceiver(TaskPointSyncPayload.ID,
                (payload, context) -> context.client().execute(() -> TaskPointClientState.replace(payload.points())));
        WorldRenderEvents.AFTER_TRANSLUCENT.register(TaskPointOverlayRenderer::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TaskPointClientState.clear(); AnnaMoodRenderer.reset(); instinctToggleActive = false;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AnnaRoundTextRenderer.tick();
            if (client.player != null && dev.doctor4t.wathe.client.WatheClient.instinctKeybind != null) {
                while (dev.doctor4t.wathe.client.WatheClient.instinctKeybind.wasPressed()
                        && PlayerInstinctComponent.KEY.get(client.player).isToggleModeEnabled()) instinctToggleActive = !instinctToggleActive;
                if (!PlayerInstinctComponent.KEY.get(client.player).isToggleModeEnabled()) instinctToggleActive = false;
                while (taskPointKey.wasPressed()) {
                    boolean enabled = TaskPointClientState.toggle();
                    client.player.sendMessage(Text.translatable(enabled ? "annawathe.hud.task_point.toggle.enabled" : "annawathe.hud.task_point.toggle.disabled")
                            .formatted(enabled ? Formatting.GREEN : Formatting.RED), true);
                }
            } else instinctToggleActive = false;
        });
    }

    public static boolean inputActive() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || dev.doctor4t.wathe.client.WatheClient.instinctKeybind == null) return false;
        return PlayerInstinctComponent.KEY.get(client.player).isToggleModeEnabled()
                ? instinctToggleActive : dev.doctor4t.wathe.client.WatheClient.instinctKeybind.isPressed();
    }

    private static void registerDefaultInstinctRules() {
        InstinctApi.registerAvailability(AnnaWathe.id("default_instinct"), 0, player -> {
            if (!inputActive()) return InstinctApi.AvailabilityResult.PASS;
            GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
            return game.canUseKillerFeatures(player) && GameFunctions.isPlayerAliveAndSurvival(player)
                    || GameFunctions.isPlayerSpectatingOrCreative(player)
                    ? InstinctApi.AvailabilityResult.ENABLE : InstinctApi.AvailabilityResult.PASS;
        });
        InstinctApi.registerHighlight(AnnaWathe.id("default_highlight"), 0, (viewer, target) -> {
            if (InstinctApi.resolveAvailability(viewer) != InstinctApi.AvailabilityResult.ENABLE) return InstinctApi.HighlightResult.pass();
            GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld());
            if (target instanceof PlayerBodyEntity body) {
                if (!GameFunctions.isPlayerSpectatingOrCreative(viewer)) return InstinctApi.HighlightResult.pass();
                Role bodyRole = game.getRole(body.getPlayerUuid());
                return bodyRole == null ? InstinctApi.HighlightResult.pass() : InstinctApi.HighlightResult.color(bodyRole.color());
            }
            if (target instanceof ItemEntity || target instanceof NoteEntity || target instanceof FirecrackerEntity) return InstinctApi.HighlightResult.color(0xDB9D00);
            if (target instanceof PlayerEntity player) {
                if (GameFunctions.isPlayerSpectatingOrCreative(player)) return InstinctApi.HighlightResult.pass();
                if (GameFunctions.isPlayerSpectatingOrCreative(viewer)) {
                    Role targetRole = game.getRole(player.getUuid());
                    return InstinctApi.HighlightResult.color(targetRole == null ? 0xFFFFFF : targetRole.color());
                }
                if (game.canUseKillerFeatures(viewer) && game.canUseKillerFeatures(player)) return InstinctApi.HighlightResult.color(MathHelper.hsvToRgb(0, 1, .6F));
                if (game.isInnocent(player)) {
                    float mood = PlayerMoodComponent.KEY.get(player).getMood();
                    return InstinctApi.HighlightResult.color(mood < GameConstants.DEPRESSIVE_MOOD_THRESHOLD ? 0x171DC6 : mood < GameConstants.MID_MOOD_THRESHOLD ? 0x1FAFAF : 0x4EDD35);
                }
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
