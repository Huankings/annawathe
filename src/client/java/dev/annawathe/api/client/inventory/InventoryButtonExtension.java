package dev.annawathe.api.client.inventory;
import net.fabricmc.api.EnvType; import net.fabricmc.api.Environment; import net.minecraft.client.gui.DrawContext;
@Environment(EnvType.CLIENT) public interface InventoryButtonExtension {
    default void init(InventoryButtonContext context) {} default void tick(InventoryButtonContext context) {}
    default void render(InventoryButtonContext context, DrawContext draw, int mouseX, int mouseY, float delta) {}
    default boolean allowInventoryKeyClose(InventoryButtonContext context, int keyCode, int scanCode) { return true; }
    default void close(InventoryButtonContext context) {}
}
