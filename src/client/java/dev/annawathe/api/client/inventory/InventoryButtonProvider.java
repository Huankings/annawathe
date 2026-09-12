package dev.annawathe.api.client.inventory;
import net.fabricmc.api.EnvType; import net.fabricmc.api.Environment; import org.jetbrains.annotations.Nullable;
@Environment(EnvType.CLIENT) @FunctionalInterface public interface InventoryButtonProvider { @Nullable InventoryButtonExtension create(InventoryButtonContext context); }
