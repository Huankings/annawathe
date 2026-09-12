package dev.annawathe.client.gui;

import dev.annawathe.api.client.gui.CrosshairHudApi;
import dev.annawathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.item.DerringerItem;
import dev.doctor4t.wathe.item.KnifeItem;
import dev.doctor4t.wathe.item.RevolverItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * AnnaWathe 的唯一准心图标 renderer。
 *
 * <p>此类重现原版 Wathe 1.3.2 的默认武器准心，再在默认逻辑外提供公开 provider/overlay
 * 调度。它只产生客户端操作提示，不能决定服务端是否真的命中或允许发动技能。</p>
 */
public final class AnnaCrosshairRenderer {
    private AnnaCrosshairRenderer() {
    }

    public static void renderCrosshair(@NotNull MinecraftClient client,
                                       @NotNull ClientPlayerEntity player,
                                       @NotNull DrawContext drawContext,
                                       @NotNull RenderTickCounter tickCounter) {
        // 与原版 Wathe 保持一致：第三人称不显示 Wathe 准心，也不调度扩展准心 provider。
        if (!client.options.getPerspective().isFirstPerson()) {
            return;
        }

        ItemStack mainHandStack = player.getMainHandStack();
        float tickDelta = tickCounter.getTickDelta(true);
        CrosshairHudApi.Context apiContext = new CrosshairHudApi.Context(
                client,
                player,
                drawContext,
                tickCounter,
                mainHandStack,
                tickDelta
        );

        /*
         * Provider 是完整接管入口。只有全部返回 PASS 时才回退到原版 Wathe 准心；HANDLED
         * 既可以代表已经画完，也可以代表扩展希望这一帧隐藏默认准心。
         */
        if (CrosshairHudApi.renderProvider(apiContext) == CrosshairHudApi.Result.PASS) {
            renderDefaultCrosshair(apiContext);
        }

        // Overlay 永不短路，用于在 provider 或默认准心之后追加小型提示。
        CrosshairHudApi.renderOverlays(apiContext);
    }

    private static void renderDefaultCrosshair(@NotNull CrosshairHudApi.Context context) {
        boolean target = false;
        ClientPlayerEntity player = context.player();
        ItemStack mainHandStack = context.mainHandStack();
        ItemCooldownManager cooldowns = player.getItemCooldownManager();

        if (mainHandStack.isOf(WatheItems.REVOLVER)
                && !cooldowns.isCoolingDown(mainHandStack.getItem())
                && RevolverItem.getGunTarget(player) instanceof EntityHitResult result
                && TargetVisibilityApi.canTargetEntity(player, result.getEntity())) {
            target = true;
        } else if (mainHandStack.isOf(WatheItems.DERRINGER)
                && !cooldowns.isCoolingDown(mainHandStack.getItem())
                && DerringerItem.getGunTarget(player) instanceof EntityHitResult result
                && TargetVisibilityApi.canTargetEntity(player, result.getEntity())) {
            target = true;
        } else if (mainHandStack.isOf(WatheItems.KNIFE)) {
            if (!cooldowns.isCoolingDown(WatheItems.KNIFE)
                    && KnifeItem.getKnifeTarget(player) instanceof EntityHitResult result
                    && TargetVisibilityApi.canTargetEntity(player, result.getEntity())) {
                CrosshairHudApi.renderKnifeProgressCrosshair(context, true, true, 1.0F);
            } else {
                float progress = 1.0F - cooldowns.getCooldownProgress(WatheItems.KNIFE, context.tickDelta());
                CrosshairHudApi.renderKnifeProgressCrosshair(context, false, false, progress);
            }
            return;
        } else if (mainHandStack.isOf(WatheItems.BAT)) {
            /*
             * 自改 Wathe 此处依赖其独有 PsychoModeApi；AnnaWathe 以原版 1.3.2 的 BAT 为边界。
             * 扩展近战武器应注册 CrosshairHudApi provider，不把自改线的 Psycho profile 偷渡进来。
             */
            if (player.getAttackCooldownProgress(context.tickDelta()) >= 1.0F
                    && context.client().crosshairTarget instanceof EntityHitResult result
                    && result.getEntity() instanceof PlayerEntity targetPlayer
                    && TargetVisibilityApi.canTargetPlayer(player, targetPlayer)) {
                CrosshairHudApi.renderBatProgressCrosshair(context, true, true, 1.0F);
            } else {
                float progress = player.getAttackCooldownProgress(context.tickDelta());
                CrosshairHudApi.renderBatProgressCrosshair(context, false, false, progress);
            }
            return;
        }

        CrosshairHudApi.renderStandardCrosshair(context, target);
    }
}
