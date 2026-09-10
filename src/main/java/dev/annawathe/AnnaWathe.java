package dev.annawathe;

import com.mojang.brigadier.arguments.BoolArgumentType;
import dev.annawathe.cca.PlayerInstinctComponent;
import dev.annawathe.cca.AnnaRoundEndState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** annawathe 服务端入口：只注册外挂框架，不修改原版 Wathe 初始化流程。 */
public final class AnnaWathe implements ModInitializer {
    public static final String MOD_ID = "annawathe";
    public static Identifier id(String path) { return Identifier.of(MOD_ID, path); }
    @Override public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("instinct")
                        .then(CommandManager.literal("key").then(CommandManager.argument("toggleMode", BoolArgumentType.bool())
                                .executes(ctx -> setMode(ctx.getSource(), BoolArgumentType.getBool(ctx, "toggleMode"))))
                                .executes(ctx -> queryMode(ctx.getSource())))
                        .executes(ctx -> queryMode(ctx.getSource()))));
    }
    private static int setMode(ServerCommandSource source, boolean enabled) throws CommandSyntaxException {
        PlayerInstinctComponent c = PlayerInstinctComponent.KEY.get(source.getPlayerOrThrow());
        c.setToggleModeEnabled(enabled);
        source.sendFeedback(() -> Text.translatable(enabled ? "annawathe.instinct.toggle" : "annawathe.instinct.hold"), false);
        return 1;
    }
    private static int queryMode(ServerCommandSource source) throws CommandSyntaxException {
        boolean enabled = PlayerInstinctComponent.KEY.get(source.getPlayerOrThrow()).isToggleModeEnabled();
        source.sendFeedback(() -> Text.translatable(enabled ? "annawathe.instinct.toggle" : "annawathe.instinct.hold"), false);
        return 1;
    }
}
