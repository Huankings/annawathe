package dev.annawathe.mixin;
import dev.annawathe.cca.AnnaRoundEndState; import dev.doctor4t.wathe.game.GameFunctions; import net.minecraft.server.world.ServerWorld; import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.*; import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameFunctions.class)
public abstract class GameFunctionsMixin { @Inject(method="initializeGame",at=@At("HEAD")) private static void annawathe$reset(ServerWorld world,CallbackInfo ci){AnnaRoundEndState.KEY.get(world).reset();} }
