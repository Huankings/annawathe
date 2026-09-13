package dev.annawathe.api.psycho;

import com.mojang.serialization.Codec;
import dev.annawathe.AnnaWathe;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** 临时疯魔物品的 profile 标记；只删除 Anna 自己授予的物品。 */
public final class PsychoDataComponentTypes {
    public static final ComponentType<String> PSYCHO_GRANTED_PROFILE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            AnnaWathe.id("psycho_granted_profile"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );
    private PsychoDataComponentTypes() {}

    /**
     * 在 AnnaWathe common 初始化阶段主动触发本类加载。
     * 数据组件必须在 Minecraft registry 冻结前注册，不能等到玩家第一次购买疯魔时再懒加载。
     */
    public static void init() {
        // 仅用于触发静态字段初始化，方法体本身不需要额外逻辑。
    }
}
