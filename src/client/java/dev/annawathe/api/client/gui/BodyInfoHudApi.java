package dev.annawathe.api.client.gui;

import dev.annawathe.api.body.BodyInfoApi;
import dev.annawathe.api.body.BodyInfoSnapshot;
import dev.annawathe.api.visibility.TargetVisibilityApi;
import dev.annawathe.client.compat.HarpyRoleHudCompat;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 尸体信息 HUD 的公开规则链。deathSummary 永远表示“死亡时间+死因”整行，
 * roleIdentity 单独表示死者身份，扩展可以只开启其中一项。
 */
public final class BodyInfoHudApi {
    public static final int DEFAULT_PRIORITY = 0;
    private static final List<Entry> RULES = new ArrayList<>();
    private static final List<NameEntry> ROLE_NAMES = new ArrayList<>();
    private static long order;
    private BodyInfoHudApi() {}

    public static synchronized void registerRule(Identifier id, int priority, VisibilityRule rule) {
        RULES.removeIf(e -> e.id.equals(id));
        RULES.add(new Entry(id, priority, order++, rule));
        RULES.sort(Comparator.comparingInt((Entry e) -> e.priority).reversed().thenComparing(Comparator.comparingLong((Entry e) -> e.order).reversed()));
    }
    public static synchronized void registerRoleNameProvider(Identifier id, int priority, RoleNameProvider provider) {
        ROLE_NAMES.removeIf(e -> e.id.equals(id));
        ROLE_NAMES.add(new NameEntry(id, priority, order++, provider));
        ROLE_NAMES.sort(Comparator.comparingInt((NameEntry e) -> e.priority).reversed().thenComparing(Comparator.comparingLong((NameEntry e) -> e.order).reversed()));
    }

    public static Visibility resolveVisibility(ClientPlayerEntity viewer, PlayerBodyEntity body) {
        List<Entry> entries;
        synchronized (RULES) { entries = List.copyOf(RULES); }
        BodyContext context = new BodyContext(viewer, body, BodyInfoApi.get(body));
        for (Entry entry : entries) {
            Visibility result = entry.rule.resolve(context);
            if (result != null && result.decision != Decision.PASS) return result;
        }
        return GameFunctions.isPlayerSpectatingOrCreative(viewer) ? Visibility.show(true, true) : Visibility.hide();
    }

    /** 由 AnnaRoleNameRenderer 调用；所有文字只属于客户端显示层。 */
    public static void render(ClientPlayerEntity viewer, TextRenderer renderer, DrawContext draw, float range) {
        PlayerBodyEntity body = RoleNameHudApi.findLookedAtBody(viewer, range);
        if (body == null || !TargetVisibilityApi.canRenderBody(viewer, body)) return;
        Visibility visibility = resolveVisibility(viewer, body);
        if (visibility.decision == Decision.HIDE) return;
        BodyInfoSnapshot info = BodyInfoApi.get(body);
        if (visibility.deathSummary) {
            Text reason = Text.translatable("death_reason." + info.deathReason().getNamespace() + "." + info.deathReason().getPath());
            Text line = Text.translatable("hud.annawathe.body.death_info", info.elapsedSeconds(body)).append(reason);
            drawCentered(renderer, draw, line, 32, Colors.RED);
        }
        if (visibility.roleIdentity && info.roleId() != null) {
            Text role = resolveRoleName(viewer, body, info.roleId());
            // 标签统一使用与死亡摘要相同的红色；职业名称自身保留 Harpy 提供的职业色。
            MutableText label = Text.translatable("hud.annawathe.body.role_info").withColor(Colors.RED);
            drawCentered(renderer, draw, label.append(role), 50, Colors.RED);
        }
    }

    private static Text resolveRoleName(ClientPlayerEntity viewer, PlayerBodyEntity body, Identifier roleId) {
        List<NameEntry> entries;
        synchronized (ROLE_NAMES) { entries = List.copyOf(ROLE_NAMES); }
        for (NameEntry entry : entries) {
            Text result = entry.provider.get(new RoleNameContext(viewer, body, roleId));
            if (result != null) return result;
        }
        HarpyRoleHudCompat.RoleDisplay harpy = HarpyRoleHudCompat.resolveRoleDisplay(roleId);
        if (harpy != null) return harpy.name().withColor(harpy.color());

        // 原版 Wathe 的四个基础角色翻译键没有 namespace 段；直接拼接命名空间会显示裸 key。
        for (Role role : WatheRoles.ROLES) {
            if (roleId.equals(role.identifier())) {
                return Text.translatable("announcement.role." + roleId.getPath()).withColor(role.color());
            }
        }
        // 扩展角色没有 Harpy 名称 provider 时，保留标准 Anna/扩展命名空间键作为最终回退。
        return Text.translatable("announcement.role." + roleId.getNamespace() + "." + roleId.getPath()).withColor(0xFFFFFF);
    }
    private static void drawCentered(TextRenderer renderer, DrawContext draw, Text text, int y, int color) {
        draw.getMatrices().push();
        draw.getMatrices().translate(draw.getScaledWindowWidth() / 2F, draw.getScaledWindowHeight() / 2F + 6F, 0);
        draw.getMatrices().scale(.6F, .6F, 1F);
        draw.drawTextWithShadow(renderer, text, -renderer.getWidth(text) / 2, y, color);
        draw.getMatrices().pop();
    }

    public record BodyContext(ClientPlayerEntity viewer, PlayerBodyEntity body, BodyInfoSnapshot info) {}
    public record RoleNameContext(ClientPlayerEntity viewer, PlayerBodyEntity body, Identifier roleId) {}
    public record Visibility(Decision decision, boolean deathSummary, boolean roleIdentity) {
        public static Visibility pass() { return new Visibility(Decision.PASS, false, false); }
        public static Visibility hide() { return new Visibility(Decision.HIDE, false, false); }
        public static Visibility show(boolean deathSummary, boolean roleIdentity) { return new Visibility(Decision.SHOW, deathSummary, roleIdentity); }
    }
    public enum Decision { PASS, SHOW, HIDE }
    @FunctionalInterface public interface VisibilityRule { @Nullable Visibility resolve(BodyContext context); }
    @FunctionalInterface public interface RoleNameProvider { @Nullable Text get(RoleNameContext context); }
    private record Entry(Identifier id, int priority, long order, VisibilityRule rule) {}
    private record NameEntry(Identifier id, int priority, long order, RoleNameProvider provider) {}
}
