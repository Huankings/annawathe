package dev.annawathe.client.compat;

import dev.annawathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * HarpyModLoader 的准心职业/词条 HUD 软兼容层。
 *
 * <p>AnnaWathe 不在编译期依赖 Harpy 的类；只有检测到 harpymodloader 已加载时，
 * 才通过反射读取 Harpy 的 WorldModifierComponent 和 Modifier 对象。
 * Harpy 未安装、版本不兼容或反射失败时，本 provider 静默 PASS，不影响原版局和其它扩展。</p>
 */
public final class HarpyRoleHudCompat {
    private static final String MOD_ID = "harpymodloader";
    private static boolean registered;

    private HarpyRoleHudCompat() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        RoleNameHudApi.registerExtraHud(
                dev.annawathe.AnnaWathe.id("compat/harpy_role_modifiers"),
                0,
                HarpyRoleHudCompat::render
        );
    }

    private static void render(RoleNameHudApi.Context context) {
        // Harpy 原逻辑只在普通旁观/创造观察者显示职业和词条；特殊存活视角仍按活人 HUD。
        ClientPlayerEntity viewer = context.player();
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)
                || !dev.doctor4t.wathe.game.GameFunctions.isPlayerSpectatingOrCreative(viewer)
                || context.targetPlayer() == null
                || context.nametagAlpha() <= 0.05F) {
            return;
        }

        ResolvedRole resolved = resolveRole(context.targetPlayer());
        if (resolved == null || resolved.name() == null) return;

        MutableText roleLine = resolved.name().copy().withColor(resolved.color());
        for (ModifierInfo modifier : resolved.modifiers()) {
            roleLine.append(Text.literal(" [")
                    .append(modifier.name())
                    .append("]")
                    .withColor(modifier.color()));
        }

        context.drawContext().getMatrices().push();
        context.drawContext().getMatrices().translate(
                context.drawContext().getScaledWindowWidth() / 2F,
                context.drawContext().getScaledWindowHeight() / 2F + 6,
                0
        );
        context.drawContext().getMatrices().scale(0.6F, 0.6F, 1F);
        int alpha = (int) (context.nametagAlpha() * 255.0F) << 24;
        context.drawContext().drawTextWithShadow(
                context.renderer(),
                roleLine,
                -context.renderer().getWidth(roleLine) / 2,
                0,
                resolved.color() | alpha
        );
        context.drawContext().getMatrices().pop();
    }

    private static @Nullable ResolvedRole resolveRole(PlayerEntity target) {
        try {
            Object role = GameWorldComponent.KEY.get(target.getWorld()).getRole(target);
            if (role == null) return null;

            Class<?> harpyClass = Class.forName("org.agmas.harpymodloader.Harpymodloader");
            // getRoleName 的参数通常声明为 Wathe 的 Role；使用可赋值扫描兼容不同映射/版本。
            Method getRoleName = findSingleArgumentMethod(harpyClass, "getRoleName", role.getClass());
            Object roleNameObject = getRoleName.invoke(null, role);
            if (!(roleNameObject instanceof MutableText roleName)) return null;

            int roleColor = invokeInt(role, "color", 0xFFFFFF);
            List<ModifierInfo> modifiers = resolveModifiers(target);
            return new ResolvedRole(roleName, roleColor, modifiers);
        } catch (Throwable ignored) {
            // 软兼容必须容忍 Harpy 版本变化，不能让 HUD 反射异常拖垮整个客户端渲染线程。
            return null;
        }
    }

    /**
     * 尸体身份 HUD 使用的 Harpy 职业名称/颜色解析。
     * 这里按死亡快照中的 Role Identifier 查找 Wathe Role，再通过 Harpy 的公开静态方法取本地化名称；
     * Harpy 未安装或版本不兼容时返回 null，由 BodyInfoHudApi 回退到原版翻译键。
     */
    public static @Nullable RoleDisplay resolveRoleDisplay(@Nullable net.minecraft.util.Identifier roleId) {
        if (roleId == null || !FabricLoader.getInstance().isModLoaded(MOD_ID)) return null;
        try {
            Role role = null;
            for (Role candidate : WatheRoles.ROLES) {
                if (roleId.equals(candidate.identifier())) {
                    role = candidate;
                    break;
                }
            }
            if (role == null) return null;
            Class<?> harpyClass = Class.forName("org.agmas.harpymodloader.Harpymodloader");
            Method getRoleName = findSingleArgumentMethod(harpyClass, "getRoleName", role.getClass());
            Object value = getRoleName.invoke(null, role);
            if (!(value instanceof MutableText name)) return null;
            return new RoleDisplay(name.copy(), invokeInt(role, "color", 0xFFFFFF));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<ModifierInfo> resolveModifiers(PlayerEntity target) {
        try {
            Class<?> componentClass = Class.forName("org.agmas.harpymodloader.component.WorldModifierComponent");
            Field keyField = componentClass.getField("KEY");
            Object key = keyField.get(null);
            Method get = findSingleArgumentMethod(key.getClass(), "get", target.getWorld().getClass());
            Object component = get.invoke(key, target.getWorld());
            Method getModifiers = componentClass.getMethod("getModifiers", net.minecraft.entity.player.PlayerEntity.class);
            Object result = getModifiers.invoke(component, target);
            if (!(result instanceof Iterable<?> iterable)) return List.of();

            java.util.ArrayList<ModifierInfo> modifiers = new java.util.ArrayList<>();
            for (Object modifier : iterable) {
                if (modifier == null) continue;
                Method nameMethod = modifier.getClass().getMethod("getName");
                Object name = nameMethod.invoke(modifier);
                if (!(name instanceof Text text)) continue;
                modifiers.add(new ModifierInfo(text, invokeInt(modifier, "color", 0xFFFFFF)));
            }
            return List.copyOf(modifiers);
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static int invokeInt(Object object, String methodName, int fallback) {
        try {
            Object value = object.getClass().getMethod(methodName).invoke(object);
            return value instanceof Number number ? number.intValue() & 0xFFFFFF : fallback;
        } catch (Throwable ignored) {
            try {
                Field field = object.getClass().getField(methodName);
                Object value = field.get(object);
                return value instanceof Number number ? number.intValue() & 0xFFFFFF : fallback;
            } catch (Throwable ignoredAgain) {
                return fallback;
            }
        }
    }

    private static Method findSingleArgumentMethod(Class<?> owner, String name, Class<?> valueType) throws NoSuchMethodException {
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(name)
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(valueType)) {
                return method;
            }
        }
        throw new NoSuchMethodException(name);
    }

    private record ResolvedRole(MutableText name, int color, List<ModifierInfo> modifiers) {
    }

    public record RoleDisplay(MutableText name, int color) {
    }

    private record ModifierInfo(Text name, int color) {
    }
}
