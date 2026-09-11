package dev.annawathe.mixin;
import dev.annawathe.api.PlayerLifeStateApi;
import dev.annawathe.cca.AnnaRoundEndState;
import dev.annawathe.cca.AnnaCollisionSettings;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** 统一补入特殊存活分类，并维护玩家碰撞开局/停局时间边界。 */
@Mixin(GameFunctions.class)
public abstract class GameFunctionsMixin {
 @Inject(method="initializeGame",at=@At("HEAD")) private static void annawathe$reset(ServerWorld world,CallbackInfo ci){AnnaRoundEndState.KEY.get(world).reset(); AnnaCollisionSettings.KEY.get(world).markRoundStart(-1L);}
 @Inject(method="initializeGame",at=@At("TAIL"))
 private static void annawathe$markCollisionStart(ServerWorld world, CallbackInfo ci) { AnnaCollisionSettings.KEY.get(world).markRoundStart(world.getTime()); }
 @Inject(method="finalizeGame",at=@At("HEAD")) private static void annawathe$clearCollisionStart(ServerWorld world,CallbackInfo ci){AnnaCollisionSettings.KEY.get(world).clearRoundStart();}
 @Inject(method="isPlayerAliveAndSurvival",at=@At("HEAD"),cancellable=true)
 private static void annawathe$alive(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) { if (player != null && (player.isSpectator() || player.isCreative()) && PlayerLifeStateApi.hasAliveOverride(player)) cir.setReturnValue(true); }
 @Inject(method="isPlayerSpectatingOrCreative",at=@At("HEAD"),cancellable=true)
 private static void annawathe$nonAlive(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) { if (player != null && (player.isSpectator() || player.isCreative()) && PlayerLifeStateApi.hasAliveOverride(player)) cir.setReturnValue(false); }
 @Inject(method="isPlayerEliminated",at=@At("HEAD"),cancellable=true)
 private static void annawathe$eliminated(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) { if (player != null && PlayerLifeStateApi.hasAliveOverride(player) && player.isAlive()) cir.setReturnValue(false); }
}
