package dev.annawathe.mixin;

import dev.annawathe.api.collision.PlayerCollisionApi;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.EntityView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 在真正生成移动 VoxelShape 的入口接管玩家碰撞，避免只改 collidesWith 后
 * 客户端预测仍然穿过 SOLID 玩家。
 */
@Mixin(EntityView.class)
public interface EntityViewCollisionMixin {
    @Shadow List<Entity> getOtherEntities(Entity except, Box box, Predicate<? super Entity> predicate);
    @Inject(method="getEntityCollisions", at=@At("HEAD"), cancellable=true)
    private void annawathe$collisions(Entity entity, Box box, CallbackInfoReturnable<List<VoxelShape>> cir) {
        if (!(entity instanceof PlayerEntity self) || box.getAverageSideLength() < 1.0E-7D) return;
        List<Entity> candidates = getOtherEntities(entity, box.expand(1.0E-7D), EntityPredicates.EXCEPT_SPECTATOR);
        List<VoxelShape> shapes = new ArrayList<>();
        for (Entity candidate : candidates) {
            // 尸体是展示/交互实体，不是玩家移动碰撞体；必须允许玩家正常穿过尸体。
            if (candidate instanceof PlayerBodyEntity) continue;
            if (candidate instanceof PlayerEntity other && PlayerCollisionApi.blocksMovement(self, other)) {
                shapes.add(VoxelShapes.cuboid(other.getBoundingBox()));
            } else if (!(candidate instanceof PlayerEntity) && candidate.isCollidable()) {
                // 恢复原版边界：只有实体自身声明为可碰撞时才加入移动碰撞体。
                // Firecracker、Note 等 Wathe 展示/功能实体默认不可碰撞，不能因为这里重建列表而挡住玩家。
                shapes.add(VoxelShapes.cuboid(candidate.getBoundingBox()));
            }
        }
        cir.setReturnValue(List.copyOf(shapes));
    }
}
