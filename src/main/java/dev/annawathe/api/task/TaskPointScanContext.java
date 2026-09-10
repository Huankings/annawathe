package dev.annawathe.api.task;

import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

/** 扫描器逐方块传给扩展的只读上下文。 */
public record TaskPointScanContext(@NotNull ServerWorld world,
                                   @NotNull MapVariablesWorldComponent mapVariables,
                                   @NotNull BlockPos pos,
                                   @NotNull BlockState state,
                                   @Nullable BlockEntity blockEntity,
                                   @NotNull Consumer<Identifier> adder) {
    public void addTaskPoint(Identifier id) {
        // 未注册 ID 不得进入网络缓存，避免客户端出现无名称、无颜色的孤儿点。
        if (MoodTaskPointApi.isRegistered(id)) adder.accept(id);
    }
}
