package dev.annawathe.compat.wathe;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * Wathe 任务完成提示的跨版本发送器。
 *
 * <p>Wathe 1.3.2 将 payload 放在 {@code util} 包，1.4.1 将同一个无字段
 * payload 移到了 {@code network} 包。这里故意不在字段、方法签名或字节码
 * 中静态引用任意一边的类名，避免另一版本在任务完成时触发
 * {@link NoClassDefFoundError}。客户端仍然收到 Wathe 原生 payload，因而保留
 * 两个版本原本的完成提示和声音行为。</p>
 */
public final class WatheTaskCompleteNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger("AnnaWathe/WatheCompat");
    private static final String[] PAYLOAD_CLASSES = {
            // Wathe 1.4.1 的新包路径优先，旧路径用于 1.3.2。
            "dev.doctor4t.wathe.network.TaskCompletePayload",
            "dev.doctor4t.wathe.util.TaskCompletePayload"
    };

    private WatheTaskCompleteNotifier() {}

    public static void send(ServerPlayerEntity player) {
        for (String className : PAYLOAD_CLASSES) {
            try {
                Class<?> rawClass = Class.forName(className, true, WatheTaskCompleteNotifier.class.getClassLoader());
                if (!CustomPayload.class.isAssignableFrom(rawClass)) {
                    LOGGER.warn("Wathe task-complete class {} is not a CustomPayload; skipping", className);
                    continue;
                }
                Constructor<?> constructor = rawClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                CustomPayload payload = (CustomPayload) constructor.newInstance();
                ServerPlayNetworking.send(player, payload);
                return;
            } catch (ClassNotFoundException ignored) {
                // 当前 Wathe 版本没有这条路径，继续尝试另一个版本的路径。
            } catch (ReflectiveOperationException | LinkageError exception) {
                LOGGER.warn("Unable to send Wathe task-complete payload from {}", className, exception);
                return;
            }
        }
        // 提示属于客户端显示层；找不到时不能回滚服务端任务完成或让服务器崩溃。
        LOGGER.warn("No compatible Wathe TaskCompletePayload class was found; task completion continues without the client notification");
    }
}
