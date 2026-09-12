package dev.annawathe.mixin;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(PlayerShopComponent.class)
public interface PlayerShopComponentAccess { @Accessor("player") PlayerEntity annawathe$player(); }
