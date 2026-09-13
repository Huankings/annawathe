package dev.annawathe.api.psycho;

import dev.annawathe.AnnaWathe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 一个完整的疯魔配置。profile 是扩展作者的主要接入对象，避免扩展继续 Mixin
 * PlayerPsychoComponent 的私有字段。
 */
public final class PsychoModeProfile {
    private final Identifier id;
    private final String nameTranslationKey;
    private final String shieldNameTranslationKey;
    private final int durationTicks;
    private final int armour;
    private final List<ItemStack> grantedItems;
    private final boolean lockHotbar;
    private final boolean lockGrantedItems;
    private final boolean removeGrantedItemsOnEnd;
    private final boolean selectFirstGrantedItem;
    private final boolean preventDroppingLockedItems;
    private final boolean meleeKillEnabled;
    private final Identifier meleeDeathReason;
    private final Identifier shieldSourceId;
    private final Identifier endEventId;
    private final @Nullable SoundEvent hitSound;
    private final @Nullable SoundEvent shieldSound;
    private final @Nullable SoundEvent backgroundSound;
    private final boolean playBackgroundSound;
    private final PsychoVisualSettings visualSettings;
    private final @Nullable PsychoItemPredicate lockedItemPredicate;
    private final @Nullable PsychoItemPredicate meleeWeaponPredicate;

    private PsychoModeProfile(Builder b) {
        id = Objects.requireNonNull(b.id);
        nameTranslationKey = Objects.requireNonNull(b.nameTranslationKey);
        shieldNameTranslationKey = Objects.requireNonNull(b.shieldNameTranslationKey);
        durationTicks = Math.max(1, b.durationTicks);
        armour = Math.max(0, b.armour);
        grantedItems = copyStacks(b.grantedItems);
        lockHotbar = b.lockHotbar;
        lockGrantedItems = b.lockGrantedItems;
        removeGrantedItemsOnEnd = b.removeGrantedItemsOnEnd;
        selectFirstGrantedItem = b.selectFirstGrantedItem;
        preventDroppingLockedItems = b.preventDroppingLockedItems;
        meleeKillEnabled = b.meleeKillEnabled;
        meleeDeathReason = Objects.requireNonNull(b.meleeDeathReason);
        shieldSourceId = Objects.requireNonNull(b.shieldSourceId);
        endEventId = Objects.requireNonNull(b.endEventId);
        hitSound = b.hitSound;
        shieldSound = b.shieldSound;
        backgroundSound = b.backgroundSound;
        playBackgroundSound = b.playBackgroundSound;
        visualSettings = Objects.requireNonNull(b.visualSettings);
        lockedItemPredicate = b.lockedItemPredicate;
        meleeWeaponPredicate = b.meleeWeaponPredicate;
    }

    public static Builder builder(@NotNull Identifier id) { return new Builder(id); }

    public static Builder copyOf(@NotNull PsychoModeProfile profile, @NotNull Identifier newId) {
        return builder(newId)
                .nameTranslationKey(profile.nameTranslationKey)
                .shieldNameTranslationKey(profile.shieldNameTranslationKey)
                .durationTicks(profile.durationTicks)
                .armour(profile.armour)
                .grantedItems(profile.grantedItems())
                .lockHotbar(profile.lockHotbar)
                .lockGrantedItems(profile.lockGrantedItems)
                .removeGrantedItemsOnEnd(profile.removeGrantedItemsOnEnd)
                .selectFirstGrantedItem(profile.selectFirstGrantedItem)
                .preventDroppingLockedItems(profile.preventDroppingLockedItems)
                .meleeKill(profile.meleeKillEnabled, profile.meleeDeathReason)
                .shieldSourceId(profile.shieldSourceId)
                .endEventId(profile.endEventId)
                .hitSound(profile.hitSound)
                .shieldSound(profile.shieldSound)
                .backgroundSound(profile.backgroundSound, profile.playBackgroundSound)
                .visualSettings(profile.visualSettings)
                .lockedItemPredicate(profile.lockedItemPredicate)
                .meleeWeaponPredicate(profile.meleeWeaponPredicate);
    }

    public Identifier id() { return id; }
    public String nameTranslationKey() { return nameTranslationKey; }
    public String shieldNameTranslationKey() { return shieldNameTranslationKey; }
    public int durationTicks() { return durationTicks; }
    public int armour() { return armour; }
    public List<ItemStack> grantedItems() { return copyStacks(grantedItems); }
    public boolean lockHotbar() { return lockHotbar; }
    public boolean lockGrantedItems() { return lockGrantedItems; }
    public boolean removeGrantedItemsOnEnd() { return removeGrantedItemsOnEnd; }
    public boolean selectFirstGrantedItem() { return selectFirstGrantedItem; }
    public boolean preventDroppingLockedItems() { return preventDroppingLockedItems; }
    public boolean meleeKillEnabled() { return meleeKillEnabled; }
    public Identifier meleeDeathReason() { return meleeDeathReason; }
    public Identifier shieldSourceId() { return shieldSourceId; }
    public Identifier endEventId() { return endEventId; }
    public @Nullable SoundEvent hitSound() { return hitSound; }
    public @Nullable SoundEvent shieldSound() { return shieldSound; }
    public @Nullable SoundEvent backgroundSound() { return backgroundSound; }
    public boolean playBackgroundSound() { return playBackgroundSound; }
    public PsychoVisualSettings visualSettings() { return visualSettings; }

    public boolean isLockedItem(PlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (lockedItemPredicate != null && lockedItemPredicate.test(player, stack)) return true;
        return lockGrantedItems && PsychoModeApi.isGrantedForProfile(stack, id);
    }

    public boolean isMeleeWeapon(PlayerEntity player, ItemStack stack) {
        if (!meleeKillEnabled || stack.isEmpty()) return false;
        return meleeWeaponPredicate != null ? meleeWeaponPredicate.test(player, stack) : isLockedItem(player, stack);
    }

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        ArrayList<ItemStack> copy = new ArrayList<>();
        for (ItemStack stack : stacks) if (!stack.isEmpty()) copy.add(stack.copy());
        return List.copyOf(copy);
    }

    public static final class Builder {
        private final Identifier id;
        private String nameTranslationKey;
        private String shieldNameTranslationKey;
        private int durationTicks = 1;
        private int armour;
        private final List<ItemStack> grantedItems = new ArrayList<>();
        private boolean lockHotbar = true;
        private boolean lockGrantedItems = true;
        private boolean removeGrantedItemsOnEnd = true;
        private boolean selectFirstGrantedItem = true;
        private boolean preventDroppingLockedItems = true;
        private boolean meleeKillEnabled;
        private Identifier meleeDeathReason = Identifier.of("wathe", "bat_hit");
        private Identifier shieldSourceId = Identifier.of("wathe", "psycho_mode");
        private Identifier endEventId = Identifier.of("wathe", "psycho_mode_end");
        private @Nullable SoundEvent hitSound;
        private @Nullable SoundEvent shieldSound;
        private @Nullable SoundEvent backgroundSound;
        private boolean playBackgroundSound = true;
        private PsychoVisualSettings visualSettings = PsychoVisualSettings.none();
        private @Nullable PsychoItemPredicate lockedItemPredicate;
        private @Nullable PsychoItemPredicate meleeWeaponPredicate;

        private Builder(Identifier id) {
            this.id = id;
            nameTranslationKey = "psycho_mode." + id.getNamespace() + "." + id.getPath();
            shieldNameTranslationKey = "psycho_shield." + id.getNamespace() + "." + id.getPath();
        }
        public Builder nameTranslationKey(String value) { nameTranslationKey = value; return this; }
        public Builder shieldNameTranslationKey(String value) { shieldNameTranslationKey = value; return this; }
        public Builder durationTicks(int value) { durationTicks = value; return this; }
        public Builder armour(int value) { armour = value; return this; }
        public Builder grantItem(ItemStack value) { if (!value.isEmpty()) grantedItems.add(value.copy()); return this; }
        public Builder grantedItems(List<ItemStack> values) { grantedItems.clear(); values.forEach(this::grantItem); return this; }
        public Builder lockHotbar(boolean value) { lockHotbar = value; return this; }
        public Builder lockGrantedItems(boolean value) { lockGrantedItems = value; return this; }
        public Builder removeGrantedItemsOnEnd(boolean value) { removeGrantedItemsOnEnd = value; return this; }
        public Builder selectFirstGrantedItem(boolean value) { selectFirstGrantedItem = value; return this; }
        public Builder preventDroppingLockedItems(boolean value) { preventDroppingLockedItems = value; return this; }
        public Builder meleeKill(boolean enabled, Identifier reason) { meleeKillEnabled = enabled; meleeDeathReason = reason; return this; }
        public Builder shieldSourceId(Identifier value) { shieldSourceId = value; return this; }
        public Builder endEventId(Identifier value) { endEventId = value; return this; }
        public Builder hitSound(@Nullable SoundEvent value) { hitSound = value; return this; }
        public Builder shieldSound(@Nullable SoundEvent value) { shieldSound = value; return this; }
        public Builder backgroundSound(@Nullable SoundEvent value, boolean play) { backgroundSound = value; playBackgroundSound = play; return this; }
        public Builder visualSettings(PsychoVisualSettings value) { visualSettings = value; return this; }
        public Builder lockedItemPredicate(@Nullable PsychoItemPredicate value) { lockedItemPredicate = value; return this; }
        public Builder meleeWeaponPredicate(@Nullable PsychoItemPredicate value) { meleeWeaponPredicate = value; return this; }
        public PsychoModeProfile build() { return new PsychoModeProfile(this); }
    }
}
