package dev.annawathe.client.mixin;

import dev.annawathe.client.bridge.DrawContextBridge;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.GuiAtlasManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 原版 Wathe 通过 access widener 读取该字段；AnnaWathe 用局部 accessor 达到同一效果。 */
@Mixin(DrawContext.class)
public interface DrawContextAccessor extends DrawContextBridge {
    @Override @Accessor("guiAtlasManager") GuiAtlasManager annawathe$getGuiAtlasManager();
}
