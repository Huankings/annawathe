package dev.annawathe.api.shop;

/** 商品交付结果；扣款、同步、音效由统一购买流程完成。 */
public enum ShopPurchaseResult {
    SUCCESS(true, false), FAIL_SHOW_MESSAGE(false, true), FAIL_SILENT(false, false);
    private final boolean successful;
    private final boolean notifyFailure;
    ShopPurchaseResult(boolean successful, boolean notifyFailure) { this.successful = successful; this.notifyFailure = notifyFailure; }
    public boolean successful() { return successful; }
    public boolean shouldNotifyFailure() { return notifyFailure; }
}
