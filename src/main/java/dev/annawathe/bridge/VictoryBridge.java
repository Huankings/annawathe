package dev.annawathe.bridge;

import dev.annawathe.cca.AnnaRoundEndState;
import net.minecraft.server.world.ServerWorld;

/**
 * 独立胜利的普通代码桥接层。
 *
 * <p>这个类不能放在 {@code dev.annawathe.mixin} 包中：该包由 Mixin 配置声明为
 * Mixin 所有权包，Fabric 会禁止普通 API 在运行时直接加载其中的类。桥接层本身
 * 不包含注入逻辑，只负责调用 Anna 的结算状态，因此必须位于普通 bridge 包。</p>
 */
public final class VictoryBridge {
    private VictoryBridge() {
    }

    /**
     * 独立胜利写入 CustomVictory 后的扩展钩子。当前结算玩家列表由原版组件和
     * AnnaRoundEndState 的快照逻辑负责保存，这里保留稳定的非 Mixin 调用边界。
     */
    public static void capture(ServerWorld world) {
        // 预留扩展点；不得在这里直接加载 Mixin 类或保存 Player 实例。
    }

    /** 在开局时清理上一局独立胜利旁路状态。 */
    public static void reset(ServerWorld world) {
        AnnaRoundEndState.KEY.get(world).reset();
    }
}
