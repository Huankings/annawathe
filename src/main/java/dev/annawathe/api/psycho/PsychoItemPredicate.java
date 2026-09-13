package dev.annawathe.api.psycho;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** 疯魔 profile 的物品判断器；只用于服务端规则和客户端显示解析。 */
@FunctionalInterface
public interface PsychoItemPredicate {
    boolean test(PlayerEntity player, ItemStack stack);
}
