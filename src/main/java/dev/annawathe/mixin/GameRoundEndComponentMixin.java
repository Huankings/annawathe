package dev.annawathe.mixin;
import dev.annawathe.cca.AnnaRoundEndState;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import java.util.List;
import java.util.UUID;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** 让原版 didWin 识别 annawathe 的独立赢家和额外共胜玩家。 */
@Mixin(GameRoundEndComponent.class)
public abstract class GameRoundEndComponentMixin {
 @Shadow @Final private World world;
 @Inject(method="didWin",at=@At("HEAD"),cancellable=true)
 private void annawathe$didWin(UUID uuid,CallbackInfoReturnable<Boolean> cir){if(world==null)return;var s=AnnaRoundEndState.KEY.get(world);if(s.getCustomVictory()!=null){cir.setReturnValue(s.getCustomVictory().isWinner(uuid));}else if(s.getExtraWinners().contains(uuid)){cir.setReturnValue(true);}}

 /**
  * 原版 setRoundEndData 只记录三种原版阵营。这里在它完成写入后立即复制真实职业，
  * 这样 HarpyModLoader/扩展在随后清理 GameWorldComponent 角色表时，客户端仍有稳定快照可显示。
  */
  @Inject(method="setRoundEndData", at=@At("TAIL"))
  private void annawathe$captureRoles(List<ServerPlayerEntity> players, GameFunctions.WinStatus status, CallbackInfo ci) {
      if (world == null) return;
      GameWorldComponent game = GameWorldComponent.KEY.get(world);
      GameRoundEndComponent self = (GameRoundEndComponent)(Object)this;
      List<GameRoundEndComponent.RoundEndData> endData = self.getPlayers();
      AnnaRoundEndState.KEY.get(world).captureRoleSnapshots(players, game, new AnnaRoundEndState.GameRoundEndComponentFallback() {
          @Override public AnnaRoundEndState.EndRoleGroup groupFor(UUID uuid) {
              for (var entry : endData) {
                  if (!entry.player().getId().equals(uuid)) continue;
                  if (entry.role() == RoleAnnouncementTexts.KILLER) return AnnaRoundEndState.EndRoleGroup.KILLER;
                  if (entry.role() == RoleAnnouncementTexts.VIGILANTE) return AnnaRoundEndState.EndRoleGroup.VIGILANTE;
                  if (entry.role() == RoleAnnouncementTexts.CIVILIAN) return AnnaRoundEndState.EndRoleGroup.CIVILIAN;
                  if (entry.role() == RoleAnnouncementTexts.LOOSE_END) return AnnaRoundEndState.EndRoleGroup.LOOSE_END;
                  return AnnaRoundEndState.EndRoleGroup.NEUTRAL;
              }
              return AnnaRoundEndState.EndRoleGroup.UNKNOWN;
          }
          @Override public int colorFor(UUID uuid) {
              AnnaRoundEndState.EndRoleGroup group = groupFor(uuid);
              return switch (group) {
                  case KILLER -> WatheRoles.KILLER.color();
                  case VIGILANTE -> WatheRoles.VIGILANTE.color();
                  case LOOSE_END -> 0x9F0000;
                  default -> WatheRoles.CIVILIAN.color();
              };
          }
      });
  }
}
