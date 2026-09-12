package dev.annawathe.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.annawathe.api.economy.EconomyApi;
import dev.annawathe.api.win.VictoryApi; import dev.annawathe.cca.AnnaRoundEndState; import dev.doctor4t.wathe.cca.*; import dev.doctor4t.wathe.game.GameFunctions; import dev.doctor4t.wathe.game.gamemode.MurderGameMode; import net.minecraft.entity.player.PlayerEntity; import net.minecraft.server.network.ServerPlayerEntity; import net.minecraft.server.world.ServerWorld; import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.*; import org.spongepowered.asm.mixin.injection.callback.CallbackInfo; import com.llamalad7.mixinextras.sugar.Local;
/** 在原版 Murder 循环中接入经济资格/数值规则，并在结算写入前仲裁 VictoryApi。 */
@Mixin(MurderGameMode.class)
public abstract class MurderGameModeMixin {
 @WrapOperation(method="tickServerGameLoop",at=@At(value="INVOKE",target="Ldev/doctor4t/wathe/cca/GameWorldComponent;canUseKillerFeatures(Lnet/minecraft/entity/player/PlayerEntity;)Z"))
 private boolean annawathe$passiveIncomeEligibility(GameWorldComponent instance, PlayerEntity player, Operation<Boolean> original,
                                                    ServerWorld world, GameWorldComponent game) {
  // 这里替换的只是被动收入循环中的资格判断；攻击、商店和其它杀手能力仍使用原版判定。
  return player instanceof ServerPlayerEntity serverPlayer
          ? EconomyApi.canReceivePassiveIncome(world, game, serverPlayer)
          : original.call(instance, player);
 }

 @WrapOperation(method="tickServerGameLoop",at=@At(value="INVOKE",target="Ldev/doctor4t/wathe/cca/PlayerShopComponent;addToBalance(I)V"))
 private void annawathe$modifyPassiveIncome(PlayerShopComponent shop, int baseIncome, Operation<Void> original,
                                            ServerWorld world, GameWorldComponent game) {
  PlayerEntity owner = ((PlayerShopComponentAccess) (Object) shop).annawathe$player();
  if (owner instanceof ServerPlayerEntity serverPlayer) {
   int income = EconomyApi.calculatePassiveIncome(world, game, serverPlayer, baseIncome);
   if (income > 0) original.call(shop, income);
  } else {
   original.call(shop, baseIncome);
  }
 }

 @Inject(method="tickServerGameLoop",at=@At(value="INVOKE",target="Ldev/doctor4t/wathe/cca/GameRoundEndComponent;setRoundEndData(Ljava/util/List;Ldev/doctor4t/wathe/game/GameFunctions$WinStatus;)V"),cancellable=true)
 private void annawathe$victory(ServerWorld world,GameWorldComponent game,CallbackInfo ci,@Local GameFunctions.WinStatus status){var r=VictoryApi.evaluate(world,game,status);switch(r.action()){case KEEP_RUNNING->ci.cancel();case CUSTOM_WIN-> {if(r.customVictory()!=null)VictoryApi.endGameWithCustomVictory(world,r.customVictory());ci.cancel();}case VANILLA_WIN-> {if(r.winStatus()!=null)VictoryApi.endGameWithVanillaWin(world,r.winStatus(),r.extraWinnerUuids());ci.cancel();}default->{}}}
}
