package dev.annawathe.mixin.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/**
 * HarpyModLoader 是可选软兼容目标，不能写入 AnnaWathe 的硬依赖。
 * 只有原版 HarpyModLoader 已加载时，才启用对应 Mixin；单独运行 AnnaWathe/原版 Wathe 时不会解析 Harpy 类。
 */
public final class AnnaHarpyMixinPlugin implements IMixinConfigPlugin {
    private static final String HARPY_MOD_ID = "harpymodloader";

    /**
     * 这里只检查 Fabric 的 mod 元数据，不主动 Class.forName Harpy 目标类。
     * Mixin 配置准备阶段若提前加载 ModdedMurderGameMode，会连带加载原版 MurderGameMode，
     * 从而阻止 NoellesRoles 后续向 MurderGameMode 注入自己的 Mixin，触发
     * MixinTargetAlreadyLoadedException。目标类是否存在由 Mixin 本身在实际选择阶段处理。
     */
    private static boolean available() {
        return FabricLoader.getInstance().isModLoaded(HARPY_MOD_ID);
    }

    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return available(); }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
