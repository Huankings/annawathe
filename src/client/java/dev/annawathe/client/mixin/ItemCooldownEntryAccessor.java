package dev.annawathe.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取单个物品冷却条目的真实结束 tick。
 *
 * <p>不能通过反射按开发环境字段名查找 endTick：生产环境经过重映射后字段名可能变化，
 * 反射失败又会让倒计时静默退化为 0。Mixin Accessor 会随构建一起正确重映射，
 * 因而客户端可以稳定使用“结束 tick - 当前 tick”计算实际剩余冷却。</p>
 */
/* Entry 是 ItemCooldownManager 的私有嵌套类，Java 源码不能直接引用其 class；
 * 使用字符串目标可让 Mixin 在不突破 Java 访问控制的情况下生成并重映射 accessor。
 */
@Mixin(targets = "net.minecraft.entity.player.ItemCooldownManager$Entry")
public interface ItemCooldownEntryAccessor {
    @Accessor("endTick")
    int annawathe$endTick();
}
