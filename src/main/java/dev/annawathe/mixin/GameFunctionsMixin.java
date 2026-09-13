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
import dev.annawathe.api.economy.EconomyApi;
import dev.annawathe.api.economy.PlayerEconomyApi;
import net.minecraft.util.Identifier;
import dev.annawathe.api.psycho.PsychoModeApi;
import dev.annawathe.api.psycho.PsychoShieldContext;
import dev.annawathe.api.psycho.PsychoShieldResult;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import net.minecraft.sound.SoundCategory;
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
 @Inject(method="killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
         at=@At(value="INVOKE",target="Ldev/doctor4t/wathe/cca/PlayerShopComponent;addToBalance(I)V",shift=At.Shift.AFTER))
 private static void annawathe$awardTaskMoneyForKill(PlayerEntity victim, boolean spawnBody, PlayerEntity killer,
                                                     Identifier deathReason, CallbackInfo ci) {
  // 任务币当前数值为 0，因此默认没有额外收益；保留服务端确认击杀后的挂点供未来重新启用。
  if (killer != null && EconomyApi.TASK_MONEY_PER_KILL > 0) {
   PlayerEconomyApi.add(killer, EconomyApi.TASK_MONEY, EconomyApi.TASK_MONEY_PER_KILL);
  }
 }

 /**
  * 在原版已经通过 AllowPlayerDeath 后、固定 armour 分支执行前仲裁自定义护盾。
  * BLOCK 会取消整次死亡；BYPASS/护盾耗尽则先停止 profile，再让原版继续完成死亡流程。
  */
 @Inject(method="killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
         at=@At(value="INVOKE", target="Ldev/doctor4t/wathe/cca/PlayerPsychoComponent;getPsychoTicks()I"), cancellable=true)
 private static void annawathe$psychoShield(PlayerEntity victim, boolean spawnBody, PlayerEntity killer,
                                             Identifier deathReason, CallbackInfo ci) {
  if (!PsychoModeApi.isActive(victim)) return;
  PlayerPsychoComponent component = PlayerPsychoComponent.KEY.get(victim);
  var profile = PsychoModeApi.getActiveProfile(victim);
  if (profile == null) return;
  var context = new PsychoShieldContext(victim, killer, deathReason, component, profile, new net.minecraft.nbt.NbtCompound());
  PsychoShieldResult result = PsychoModeApi.resolveShield(context);
  if (result == PsychoShieldResult.BLOCK) {
   if (component.getArmour() > 0) component.setArmour(component.getArmour() - 1);
   if (profile.shieldSound() != null) victim.playSoundToPlayer(profile.shieldSound(), SoundCategory.MASTER, 5F, 1F);
   ci.cancel();
  } else if (result == PsychoShieldResult.BYPASS || component.getArmour() <= 0) {
   PsychoModeApi.stop(victim, false);
  }
 }
}
