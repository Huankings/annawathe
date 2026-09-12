package dev.annawathe.client.mixin;

import dev.annawathe.api.client.hud.HudOverlayApi;
import dev.annawathe.api.client.hud.HudOverlayContext;
import dev.annawathe.api.client.hud.HudOverlayLayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AnnaWathe 通用 HUD 的唯一底层调度点。
 *
 * <p>优先级低于原版 Wathe 的默认 Mixin，使 MAIN_HUD 的 TAIL 注入在 Wathe 主 HUD 完成后执行。
 * 扩展不得继续直接 Mixin InGameHud，而应根据覆盖需求选择三个公开 layer。</p>
 */
@Mixin(value = InGameHud.class, priority = 900)
public abstract class InGameHudOverlayMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    /**
     * 只向 HudOverlayContext 暴露一次受控调用，不把 InGameHud 实例或受保护方法交给扩展。
     */
    @Invoker("renderHotbar")
    protected abstract void annawathe$invokeRenderHotbar(DrawContext context,
                                                         RenderTickCounter tickCounter);

    @Inject(method = "render", at = @At("HEAD"))
    private void annawathe$renderBeforeHud(DrawContext context,
                                           RenderTickCounter tickCounter,
                                           CallbackInfo ci) {
        renderLayer(HudOverlayLayer.BEFORE_HUD, context, tickCounter);
    }

    @Inject(method = "renderMainHud", at = @At("TAIL"))
    private void annawathe$renderMainHud(DrawContext context,
                                         RenderTickCounter tickCounter,
                                         CallbackInfo ci) {
        renderLayer(HudOverlayLayer.MAIN_HUD, context, tickCounter);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void annawathe$renderAfterHud(DrawContext context,
                                          RenderTickCounter tickCounter,
                                          CallbackInfo ci) {
        renderLayer(HudOverlayLayer.AFTER_HUD, context, tickCounter);
    }

    @Unique
    private void renderLayer(HudOverlayLayer layer,
                             DrawContext context,
                             RenderTickCounter tickCounter) {
        HudOverlayContext overlayContext = createContext(context, tickCounter);
        if (overlayContext != null) {
            HudOverlayApi.render(layer, overlayContext);
        }
    }

    @Unique
    private @Nullable HudOverlayContext createContext(DrawContext context,
                                                      RenderTickCounter tickCounter) {
        ClientPlayerEntity player = this.client.player;
        if (player == null) {
            return null;
        }

        DebugHud debugHud = this.client.inGameHud.getDebugHud();
        /*
         * 两个玩法状态统一走 GameFunctions。AnnaWathe 已在该入口接入 PlayerLifeStateApi，
         * 因此 creative/spectator 的特殊局内存活授权会自动反映到所有扩展 HUD。
         */
        boolean aliveAndSurvival = GameFunctions.isPlayerAliveAndSurvival(player);
        boolean spectatingOrCreative = GameFunctions.isPlayerSpectatingOrCreative(player);
        return new HudOverlayContext(
                this.client,
                player,
                this.client.textRenderer,
                context,
                tickCounter,
                GameWorldComponent.KEY.get(player.getWorld()),
                aliveAndSurvival,
                spectatingOrCreative,
                debugHud != null && debugHud.shouldShowDebugHud(),
                this.client.options.hudHidden,
                this.client.currentScreen,
                this::annawathe$invokeRenderHotbar
        );
    }
}
