package dev.annawathe.network;

import dev.annawathe.api.economy.PlayerEconomyApi;
import dev.annawathe.api.shop.ShopApi;
import dev.annawathe.api.shop.ShopEntry;
import dev.annawathe.api.shop.ShopPayment;
import dev.annawathe.api.shop.ShopPurchaseContext;
import dev.annawathe.api.shop.ShopPurchaseResult;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

/** Anna 商店购买包。客户端只提交索引，服务端重新解析列表、价格和支付条件。 */
public record AnnaStoreBuyPayload(int index) implements CustomPayload {
    public static final Id<AnnaStoreBuyPayload> ID = new Id<>(Identifier.of("annawathe", "store_buy"));
    public static final PacketCodec<PacketByteBuf, AnnaStoreBuyPayload> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER, AnnaStoreBuyPayload::index, AnnaStoreBuyPayload::new);
    @Override public Id<? extends CustomPayload> getId() { return ID; }
    public static final class Receiver implements ServerPlayNetworking.PlayPayloadHandler<AnnaStoreBuyPayload> {
        @Override public void receive(@NotNull AnnaStoreBuyPayload payload, ServerPlayNetworking.@NotNull Context context) {
            ServerPlayerEntity player = context.player();
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
            ShopApi.ResolvedShop resolved = ShopApi.resolveShop(player);
            // C2S 包只表达点击意图；服务端必须重新确认对局和玩法存活状态。
            if (!resolved.gameWorld().isRunning() || !GameFunctions.isPlayerAliveAndSurvival(player)) return;
            if (payload.index() < 0 || payload.index() >= resolved.entries().size()) return;
            ShopEntry entry = resolved.entries().get(payload.index());
            ShopPayment payment = entry.shopPrice().selectPayment(player);
            if (payment == null && FabricLoader.getInstance().isDevelopmentEnvironment()) {
                ShopPayment developmentPayment = entry.shopPrice().cheapestPaymentForDevelopment();
                if (developmentPayment != null) {
                    // 只在开发环境补齐当前测试方案；正式服务器绝不会从客户端点击产生货币。
                    for (var cost : developmentPayment.costs()) {
                        int current = PlayerEconomyApi.get(player, cost.currency());
                        PlayerEconomyApi.set(player, cost.currency(), Math.max(current, cost.amount() * 10));
                    }
                    payment = entry.shopPrice().selectPayment(player);
                }
            }
            if (payment == null || player.getItemCooldownManager().isCoolingDown(entry.stack().getItem())) {
                if (entry.shouldShowPurchaseFailedMessage(player)) ShopApi.sendPurchaseFailedMessage(player);
                ShopApi.playFailSound(player);
                return;
            }
            ShopPurchaseContext purchase = new ShopPurchaseContext(player, shop, entry, payload.index(), resolved.gameWorld(), resolved.role(), resolved.roleSpecificShop());
            ShopPurchaseResult result = resolved.provider().purchase(purchase);
            if (result != null && result.successful() && PlayerEconomyApi.spend(player, payment.costs())) {
                ShopApi.playBuySound(player);
                return;
            }
            if ((result == null || result.shouldNotifyFailure()) && entry.shouldShowPurchaseFailedMessage(player)) {
                ShopApi.sendPurchaseFailedMessage(player);
            }
            ShopApi.playFailSound(player);
        }
    }
}
