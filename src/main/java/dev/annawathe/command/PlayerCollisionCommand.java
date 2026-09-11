package dev.annawathe.command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import dev.annawathe.cca.AnnaCollisionSettings;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** 管理员调试玩家碰撞总开关；配置保存在世界组件，不要求当前正在对局。 */
public final class PlayerCollisionCommand {
    private PlayerCollisionCommand() {}
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("annawathe:playerCollision").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("enabled", BoolArgumentType.bool()).executes(c -> { AnnaCollisionSettings settings = AnnaCollisionSettings.KEY.get(c.getSource().getWorld()); settings.setEnabled(BoolArgumentType.getBool(c, "enabled")); c.getSource().sendFeedback(() -> Text.literal("玩家碰撞已更新"), true); return 1; }))
                .executes(c -> { AnnaCollisionSettings settings = AnnaCollisionSettings.KEY.get(c.getSource().getWorld()); c.getSource().sendFeedback(() -> Text.literal("玩家碰撞：" + settings.isEnabled()), false); return 1; }));
    }
}
