package dev.annawathe.client.bridge;

import net.minecraft.client.texture.GuiAtlasManager;

/** 由客户端 Mixin 注入 DrawContext 的安全桥，业务 renderer 不直接加载 Mixin 包类型。 */
public interface DrawContextBridge {
    GuiAtlasManager annawathe$getGuiAtlasManager();
}
