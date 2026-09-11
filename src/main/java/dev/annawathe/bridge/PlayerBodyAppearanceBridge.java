package dev.annawathe.bridge;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
/** PlayerBodyEntity 的窄桥接接口，只暴露视觉 UUID，不触碰真实尸体 owner。 */
public interface PlayerBodyAppearanceBridge {
    @Nullable UUID annawathe$getAppearanceUuid();
    void annawathe$setAppearanceUuid(@Nullable UUID uuid);
}
