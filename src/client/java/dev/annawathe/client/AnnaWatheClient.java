package dev.annawathe.client;
import dev.annawathe.AnnaWathe; import dev.annawathe.api.instinct.InstinctApi; import dev.annawathe.client.gui.AnnaRoundTextRenderer; import dev.annawathe.client.tooltip.ItemTooltipApi; import dev.annawathe.cca.PlayerInstinctComponent; import dev.doctor4t.wathe.api.Role; import dev.doctor4t.wathe.cca.*; import dev.doctor4t.wathe.entity.*; import dev.doctor4t.wathe.game.*; import dev.doctor4t.wathe.index.WatheItems; import net.fabricmc.api.ClientModInitializer; import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents; import net.minecraft.client.MinecraftClient; import net.minecraft.entity.ItemEntity; import net.minecraft.entity.player.PlayerEntity; import net.minecraft.util.math.MathHelper;
/** 客户端入口：注册默认本能规则、标准 tooltip，并驱动新结算渲染器。 */
public final class AnnaWatheClient implements ClientModInitializer { public static boolean instinctToggleActive;
 @Override public void onInitializeClient(){registerDefaultInstinctRules();ItemTooltipApi.initialize();ItemTooltipApi.registerItems(WatheItems.KNIFE,WatheItems.REVOLVER,WatheItems.DERRINGER,WatheItems.GRENADE,WatheItems.PSYCHO_MODE,WatheItems.POISON_VIAL,WatheItems.SCORPION,WatheItems.FIRECRACKER,WatheItems.LOCKPICK,WatheItems.CROWBAR,WatheItems.BODY_BAG,WatheItems.BLACKOUT,WatheItems.NOTE);ClientTickEvents.END_CLIENT_TICK.register(c->{AnnaRoundTextRenderer.tick();if(c.player!=null&&dev.doctor4t.wathe.client.WatheClient.instinctKeybind!=null)while(dev.doctor4t.wathe.client.WatheClient.instinctKeybind.wasPressed()&&PlayerInstinctComponent.KEY.get(c.player).isToggleModeEnabled())instinctToggleActive=!instinctToggleActive;else instinctToggleActive=false;});}
 public static boolean inputActive(){MinecraftClient c=MinecraftClient.getInstance();if(c.player==null||dev.doctor4t.wathe.client.WatheClient.instinctKeybind==null)return false;return PlayerInstinctComponent.KEY.get(c.player).isToggleModeEnabled()?instinctToggleActive:dev.doctor4t.wathe.client.WatheClient.instinctKeybind.isPressed();}
 private static void registerDefaultInstinctRules(){InstinctApi.registerAvailability(AnnaWathe.id("default_instinct"),0,p->{if(!inputActive())return InstinctApi.AvailabilityResult.PASS;GameWorldComponent g=GameWorldComponent.KEY.get(p.getWorld());return g.canUseKillerFeatures(p)&&GameFunctions.isPlayerAliveAndSurvival(p)||GameFunctions.isPlayerSpectatingOrCreative(p)?InstinctApi.AvailabilityResult.ENABLE:InstinctApi.AvailabilityResult.PASS;});InstinctApi.registerHighlight(AnnaWathe.id("default_highlight"),0,(v,t)->{if(InstinctApi.resolveAvailability(v)!=InstinctApi.AvailabilityResult.ENABLE)return InstinctApi.HighlightResult.pass();GameWorldComponent g=GameWorldComponent.KEY.get(v.getWorld());
  /*
   * 尸体不是 PlayerEntity，而是 Wathe 的 PlayerBodyEntity。
   * 原版 Wathe 没有尸体本能描边；这里仅允许 Wathe 定义的非存活观察者查看，
   * 并使用尸体绑定的真实玩家 UUID 查角色颜色。getAppearanceUuid() 只能表示外观伪装，
   * 不能用于判断死者职业，因此必须使用 getPlayerUuid()。
   */
  if(t instanceof PlayerBodyEntity body){
   if(!GameFunctions.isPlayerSpectatingOrCreative(v))return InstinctApi.HighlightResult.pass();
   Role bodyRole=g.getRole(body.getPlayerUuid());
   return bodyRole==null?InstinctApi.HighlightResult.pass():InstinctApi.HighlightResult.color(bodyRole.color());
  }
  if(t instanceof ItemEntity||t instanceof NoteEntity||t instanceof FirecrackerEntity)return InstinctApi.HighlightResult.color(0xDB9D00);if(t instanceof PlayerEntity p){if(GameFunctions.isPlayerSpectatingOrCreative(p))return InstinctApi.HighlightResult.pass();
   /*
    * 非存活玩家（旁观/创造视角）查看活体玩家时，应显示目标真实职业色。
    * 这一分支必须放在“杀手红色”和“平民心情色”之前：那些颜色是存活杀手
    * 的玩法语义，而旁观者需要直接获知完整职业信息。找不到角色时才回退白色，
    * 避免中途加入或尚未同步的玩家完全没有描边。
    */
   if(GameFunctions.isPlayerSpectatingOrCreative(v)){Role targetRole=g.getRole(p.getUuid());return targetRole==null?InstinctApi.HighlightResult.color(0xFFFFFF):InstinctApi.HighlightResult.color(targetRole.color());}
   if(g.canUseKillerFeatures(v)&&g.canUseKillerFeatures(p))return InstinctApi.HighlightResult.color(MathHelper.hsvToRgb(0,1,.6f));if(g.isInnocent(p)){float m=PlayerMoodComponent.KEY.get(p).getMood();return InstinctApi.HighlightResult.color(m<GameConstants.DEPRESSIVE_MOOD_THRESHOLD?0x171DC6:m<GameConstants.MID_MOOD_THRESHOLD?0x1FAFAF:0x4EDD35);}if(GameFunctions.isPlayerSpectatingOrCreative(v))return InstinctApi.HighlightResult.color(0xFFFFFF);}return InstinctApi.HighlightResult.pass();});}
}
