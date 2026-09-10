package dev.annawathe.mixin;
import dev.annawathe.cca.AnnaRoundEndState; import dev.doctor4t.wathe.cca.GameWorldComponent; import dev.doctor4t.wathe.game.GameFunctions; import net.minecraft.server.world.ServerWorld;
/** 将原版结算调用桥接到 annawathe 状态；后续可在此集中扩展真实职业显示资料。 */
public final class VictoryBridge { private VictoryBridge(){} public static void capture(ServerWorld world){ /* 原版玩家列表由渲染器直接读取，此处保留扩展点。 */ } public static void reset(ServerWorld world){AnnaRoundEndState.KEY.get(world).reset();} }
