package dev.annawathe.api.client.invisibility;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import java.util.*;

@Environment(EnvType.CLIENT)
/**
 * 只改变客户端模型的手持物隐藏规则。
 * 持有者本人、死亡/普通旁观视角和服务端真实物品均不受影响。
 */
public final class HeldItemInvisibilityApi {
    private static final Map<Role, Set<Item>> ROLE_ITEMS = new HashMap<>();
    private static final List<Entry> RULES = new ArrayList<>();
    private static long order;
    private HeldItemInvisibilityApi() {}
    public static synchronized void registerHiddenItem(Role role, Item item) { ROLE_ITEMS.computeIfAbsent(role, ignored -> new HashSet<>()).add(item); }
    public static synchronized void registerHiddenItems(Role role, Collection<Item> items) { for (Item item : items) registerHiddenItem(role, item); }
    public static synchronized void registerRule(Identifier id, int priority, VisibilityRule rule) { RULES.removeIf(e -> e.id.equals(id)); RULES.add(new Entry(id, priority, order++, rule)); RULES.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(Comparator.comparingLong(Entry::order).reversed())); }
    /** 渲染层统一调用；命中隐藏时返回空栈，后续幻觉层仍可重新覆盖。 */
    public static ItemStack applyInvisibility(@Nullable PlayerEntity viewer, LivingEntity holder, Hand hand, ItemStack stack) { return shouldHideFromOtherLivingPlayers(viewer, holder, hand, stack) ? ItemStack.EMPTY : stack; }
    public static ItemStack applyInvisibility(@Nullable PlayerEntity viewer, LivingEntity holder, Hand hand) { return applyInvisibility(viewer, holder, hand, holder.getStackInHand(hand)); }
    public static boolean shouldHideFromOtherLivingPlayers(@Nullable PlayerEntity viewer, LivingEntity holder, Hand hand, ItemStack stack) {
        if (stack.isEmpty() || !(holder instanceof PlayerEntity owner) || !GameFunctions.isPlayerAliveAndSurvival(owner) || viewer == null || viewer.getUuid().equals(owner.getUuid()) || !GameFunctions.isPlayerAliveAndSurvival(viewer)) return false;
        return matches(owner, hand, stack);
    }
    public static boolean isHiddenByAnyRule(PlayerEntity holder, Hand hand) { return isHiddenByAnyRule(holder, hand, holder.getStackInHand(hand)); }
    public static boolean isHiddenByAnyRule(PlayerEntity holder, Hand hand, ItemStack stack) { return !stack.isEmpty() && GameFunctions.isPlayerAliveAndSurvival(holder) && matches(holder, hand, stack); }
    public static boolean hasHiddenHeldItem(PlayerEntity holder) { return isHiddenByAnyRule(holder, Hand.MAIN_HAND) || isHiddenByAnyRule(holder, Hand.OFF_HAND); }
    private static boolean matches(PlayerEntity holder, Hand hand, ItemStack stack) {
        GameWorldComponent game = GameWorldComponent.KEY.get(holder.getWorld()); if (!game.isRunning()) return false;
        Role role = game.getRole(holder); List<Entry> snapshot; synchronized (RULES) { snapshot = List.copyOf(RULES); }
        for (Entry entry : snapshot) if (entry.rule.hide(new VisibilityContext(game, holder, hand, stack, role))) return true;
        synchronized (HeldItemInvisibilityApi.class) { return role != null && ROLE_ITEMS.getOrDefault(role, Set.of()).contains(stack.getItem()); }
    }
    @FunctionalInterface public interface VisibilityRule { boolean hide(VisibilityContext context); }
    public record VisibilityContext(GameWorldComponent gameWorld, PlayerEntity holder, Hand hand, ItemStack stack, @Nullable Role role) {}
    private record Entry(Identifier id, int priority, long order, VisibilityRule rule) {}
}
