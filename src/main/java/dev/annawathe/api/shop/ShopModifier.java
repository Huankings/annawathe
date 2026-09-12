package dev.annawathe.api.shop;
import org.jetbrains.annotations.NotNull;
import java.util.List;
@FunctionalInterface public interface ShopModifier { void modify(@NotNull ShopContext context, @NotNull List<ShopEntry> entries); }
