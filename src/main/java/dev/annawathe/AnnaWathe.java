package dev.annawathe;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.annawathe.cca.PlayerInstinctComponent;
import dev.annawathe.command.MoodCommands;
import dev.annawathe.network.TaskPointSyncPayload;
import dev.annawathe.task.TaskPointSyncManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** AnnaWathe common/服务端入口。 */
public final class AnnaWathe implements ModInitializer {
    public static final String MOD_ID = "annawathe";
    public static Identifier id(String path) { return Identifier.of(MOD_ID, path); }

    @Override public void onInitialize() {
        // 任务点整表使用专用 S2C payload；注册必须在任何玩家连接之前完成。
        PayloadTypeRegistry.playS2C().register(TaskPointSyncPayload.ID, TaskPointSyncPayload.CODEC);
        TaskPointSyncManager.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MoodCommands.register(dispatcher);
            dispatcher.register(CommandManager.literal("instinct")
                    .then(CommandManager.literal("key").then(CommandManager.argument("toggleMode", BoolArgumentType.bool())
                            .executes(ctx -> setMode(ctx.getSource(), BoolArgumentType.getBool(ctx, "toggleMode"))))
                            .executes(ctx -> queryMode(ctx.getSource())))
                    .executes(ctx -> queryMode(ctx.getSource())));
        });
    }
    private static int setMode(ServerCommandSource source, boolean enabled) throws CommandSyntaxException {
        PlayerInstinctComponent c = PlayerInstinctComponent.KEY.get(source.getPlayerOrThrow()); c.setToggleModeEnabled(enabled);
        source.sendFeedback(() -> Text.translatable(enabled ? "annawathe.instinct.toggle" : "annawathe.instinct.hold"), false); return 1;
    }
    private static int queryMode(ServerCommandSource source) throws CommandSyntaxException {
        boolean enabled = PlayerInstinctComponent.KEY.get(source.getPlayerOrThrow()).isToggleModeEnabled();
        source.sendFeedback(() -> Text.translatable(enabled ? "annawathe.instinct.toggle" : "annawathe.instinct.hold"), false); return 1;
    }
}
