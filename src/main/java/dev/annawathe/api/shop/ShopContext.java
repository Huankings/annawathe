package dev.annawathe.api.shop;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
public record ShopContext(PlayerEntity player, GameWorldComponent gameWorld, @Nullable Role role, boolean roleSpecificShop) {}
