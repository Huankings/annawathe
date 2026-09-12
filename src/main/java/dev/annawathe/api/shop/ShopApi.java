package dev.annawathe.api.shop;
import dev.annawathe.api.economy.CurrencyAmount;
import dev.annawathe.api.economy.EconomyApi;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Supplier;
/** Anna 商店注册、动态解析和原版商品适配门面。 */
public final class ShopApi {
    public static final int DEFAULT_PRIORITY = 0;
    private static final Map<Role, RoleShopProvider> ROLE_SHOPS = new HashMap<>();
    private static final List<PrioritizedModifier> MODIFIERS = new ArrayList<>();
    private static final RoleShopProvider DEFAULT_PROVIDER = player -> AnnaDefaultShop.entries();
    private static long order;
    private ShopApi() {}
    public static synchronized void registerRoleShop(Role role, RoleShopProvider provider) { ROLE_SHOPS.put(Objects.requireNonNull(role), Objects.requireNonNull(provider)); }
    public static synchronized void registerStaticRoleShop(Role role, Supplier<List<dev.annawathe.api.shop.ShopEntry>> supplier) { registerRoleShop(role, p -> supplier.get()); }
    public static synchronized void registerStaticRoleShop(Supplier<List<dev.annawathe.api.shop.ShopEntry>> supplier, Role... roles) { for (Role role : roles) registerStaticRoleShop(role, supplier); }
    public static synchronized void registerStaticRoleShops(Collection<Role> roles, Supplier<List<dev.annawathe.api.shop.ShopEntry>> supplier) { for (Role role : roles) registerStaticRoleShop(role, supplier); }
    public static synchronized void registerShopModifier(Identifier id, int priority, ShopModifier modifier) {
        MODIFIERS.removeIf(e -> e.id.equals(id)); MODIFIERS.add(new PrioritizedModifier(id, priority, order++, modifier));
        MODIFIERS.sort(Comparator.<PrioritizedModifier>comparingInt(PrioritizedModifier::priority).reversed()
                .thenComparing(Comparator.comparingLong(PrioritizedModifier::order).reversed()));
    }
    public static List<dev.annawathe.api.shop.ShopEntry> getEntriesForPlayer(PlayerEntity player) { return resolveShop(player).entries(); }
    public static boolean hasShop(PlayerEntity player) { return !getEntriesForPlayer(player).isEmpty(); }
    public static synchronized boolean hasRoleShop(Role role) { return ROLE_SHOPS.containsKey(role); }
    public static ResolvedShop resolveShop(PlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld()); Role role = game.getRole(player); RoleShopProvider provider; synchronized (ShopApi.class) { provider = ROLE_SHOPS.get(role); } boolean specific = provider != null;
        List<dev.annawathe.api.shop.ShopEntry> entries = new ArrayList<>();
        if (provider != null) entries.addAll(provider.getShopEntries(player));
        else if (game.canUseKillerFeatures(player)) entries.addAll(AnnaDefaultShop.entries());
        ShopContext context = new ShopContext(player, game, role, specific); synchronized (ShopApi.class) { for (PrioritizedModifier e : MODIFIERS) e.modifier.modify(context, entries); }
        return new ResolvedShop(provider == null ? DEFAULT_PROVIDER : provider, List.copyOf(entries), game, role, specific);
    }
    public static ShopPurchaseResult defaultPurchase(ShopPurchaseContext context) {
        if (!context.canAffordEntry() || context.player().getItemCooldownManager().isCoolingDown(context.entry().stack().getItem())) {
            return ShopPurchaseResult.FAIL_SHOW_MESSAGE;
        }
        if (context.entry().onBuy(context.player())) return ShopPurchaseResult.SUCCESS;
        return context.entry().shouldShowPurchaseFailedMessage(context.player())
                ? ShopPurchaseResult.FAIL_SHOW_MESSAGE : ShopPurchaseResult.FAIL_SILENT;
    }
    public static @Nullable ShopPrice getDefaultShopPrice(Item item) { for (dev.annawathe.api.shop.ShopEntry e : AnnaDefaultShop.entries()) if (e.stack().isOf(item)) return e.shopPrice(); return null; }
    public static int getDefaultPrice(Item item, int fallback) { ShopPrice price = getDefaultShopPrice(item); return price == null ? fallback : price.legacyPrice(); }
    public static int getDefaultCurrencyPrice(Item item, int option, Identifier currency, int fallback) { ShopPrice p = getDefaultShopPrice(item); if (p == null || option < 0 || option >= p.options().size()) return fallback; for (CurrencyAmount c : p.options().get(option).costs()) if (c.currency().equals(currency)) return c.amount(); return fallback; }
    public static int getDefaultMoneyPrice(Item item, int option, int fallback) { return getDefaultCurrencyPrice(item, option, EconomyApi.MONEY, fallback); }
    public static int getDefaultTaskMoneyPrice(Item item, int option, int fallback) { return getDefaultCurrencyPrice(item, option, EconomyApi.TASK_MONEY, fallback); }
    public static void sendPurchaseFailedMessage(PlayerEntity player) { player.sendMessage(Text.translatable("shop.purchase_failed").withColor(0xAA0000), true); }
    public static void playBuySound(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.playSoundToPlayer(WatheSounds.UI_SHOP_BUY, SoundCategory.PLAYERS, 1.0F,
                    0.9F + player.getRandom().nextFloat() * 0.2F);
        }
    }
    public static void playFailSound(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.playSoundToPlayer(WatheSounds.UI_SHOP_BUY_FAIL, SoundCategory.PLAYERS, 1.0F,
                    0.9F + player.getRandom().nextFloat() * 0.2F);
        }
    }
    public record ResolvedShop(RoleShopProvider provider, List<dev.annawathe.api.shop.ShopEntry> entries, GameWorldComponent gameWorld, @Nullable Role role, boolean roleSpecificShop) {}
    private record PrioritizedModifier(Identifier id, int priority, long order, ShopModifier modifier) {}
}
