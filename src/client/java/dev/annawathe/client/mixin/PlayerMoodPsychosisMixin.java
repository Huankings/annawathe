package dev.annawathe.client.mixin;

import dev.annawathe.api.client.mood.PsychosisItemApi;
import dev.annawathe.client.psychosis.AnnaPsychosisVisualState;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashMap;
import java.util.UUID;

/** 在原版 Mood 客户端 tick 尾部加入 Anna provider，并在 reset/死亡时清除视觉缓存。 */
@Mixin(PlayerMoodComponent.class)
public abstract class PlayerMoodPsychosisMixin {
    @Shadow @Final private PlayerEntity player;
    @Shadow @Final private HashMap<UUID, ItemStack> psychosisItems;
    @Inject(method="clientTick", at=@At("TAIL"))
    private void annawathe$providers(CallbackInfo ci) {
        if (!PsychosisItemApi.hasProviders()) return;
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (!game.isRunning() || !GameFunctions.isPlayerAliveAndSurvival(player)) { AnnaPsychosisVisualState.clear(player.getUuid()); return; }
        for (PlayerEntity target : player.getWorld().getPlayers()) {
            if (target == player || !GameFunctions.isPlayerAliveAndSurvival(target)) continue;
            if (player.getWorld().getRandom().nextInt(dev.doctor4t.wathe.game.GameConstants.ITEM_PSYCHOSIS_REROLL_TIME) != 0) continue;
            resolve(target, Hand.MAIN_HAND); resolve(target, Hand.OFF_HAND);
        }
    }
    @Inject(method="reset", at=@At("TAIL"))
    private void annawathe$clearOnReset(CallbackInfo ci) { AnnaPsychosisVisualState.clear(player.getUuid()); }
    @Unique private void resolve(PlayerEntity target, Hand hand) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        ItemStack existing = hand == Hand.MAIN_HAND ? psychosisItems.get(target.getUuid()) : null;
        PsychosisItemApi.Result result = PsychosisItemApi.resolve(new PsychosisItemApi.Context(player, target, hand, target.getStackInHand(hand).copy(), (PlayerMoodComponent)(Object)this, game, game.getRole(target), ((PlayerMoodComponent)(Object)this).isLowerThanMid(), true, player.getRandom()), existing == null ? null : PsychosisItemApi.Result.item(existing));
        if (result != null && result.handled()) AnnaPsychosisVisualState.put(player.getUuid(), target.getUuid(), hand, result.stack(), result.armPose());
        else AnnaPsychosisVisualState.remove(player.getUuid(), target.getUuid(), hand);
    }
}
