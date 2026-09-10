package dev.annawathe.mixin;
import dev.annawathe.api.win.VictoryApi; import dev.annawathe.cca.AnnaRoundEndState; import dev.doctor4t.wathe.cca.*; import dev.doctor4t.wathe.game.GameFunctions; import dev.doctor4t.wathe.game.gamemode.MurderGameMode; import net.minecraft.server.network.ServerPlayerEntity; import net.minecraft.server.world.ServerWorld; import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.*; import org.spongepowered.asm.mixin.injection.callback.CallbackInfo; import com.llamalad7.mixinextras.sugar.Local;
/** 在原版 Murder 结算写入前仲裁 VictoryApi，保持原版循环和金币逻辑不变。 */
@Mixin(MurderGameMode.class)
public abstract class MurderGameModeMixin {
 @Inject(method="tickServerGameLoop",at=@At(value="INVOKE",target="Ldev/doctor4t/wathe/cca/GameRoundEndComponent;setRoundEndData(Ljava/util/List;Ldev/doctor4t/wathe/game/GameFunctions$WinStatus;)V"),cancellable=true)
 private void annawathe$victory(ServerWorld world,GameWorldComponent game,CallbackInfo ci,@Local GameFunctions.WinStatus status){var r=VictoryApi.evaluate(world,game,status);switch(r.action()){case KEEP_RUNNING->ci.cancel();case CUSTOM_WIN-> {if(r.customVictory()!=null)VictoryApi.endGameWithCustomVictory(world,r.customVictory());ci.cancel();}case VANILLA_WIN-> {if(r.winStatus()!=null)VictoryApi.endGameWithVanillaWin(world,r.winStatus(),r.extraWinnerUuids());ci.cancel();}default->{}}}
}
