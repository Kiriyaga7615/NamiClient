package me.kiriyaga.nami.feature.module.impl.world;

import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.mixin.KeyBindingAccessor;
import me.kiriyaga.nami.setting.impl.BoolSetting;
import me.kiriyaga.nami.setting.impl.DoubleSetting;
import me.kiriyaga.nami.setting.impl.IntSetting;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import static me.kiriyaga.nami.Nami.INVENTORY_MANAGER;
import static me.kiriyaga.nami.Nami.MC;

@RegisterModule
public class AutoEatModule extends Module {

    private final IntSetting swapDelayTicksSetting = addSetting(new IntSetting("delay", 5, 1, 20));
    private final DoubleSetting minHunger = addSetting(new DoubleSetting("hunger", 19.0, 0.0, 19.0));
    private final DoubleSetting minHealth = addSetting(new DoubleSetting("health", 0.0, 0.0, 19.0));
    private final BoolSetting allowGapples = addSetting(new BoolSetting("gapples", true));
    private final BoolSetting allowPoisoned = addSetting(new BoolSetting("poisoned", false));

    private boolean eating = false;
    private int swapCooldown = 0;

    public AutoEatModule() {
        super("auto eat", "Automatically eats best food.", ModuleCategory.of("world"), "фгещуфв", "autoeat");
    }

    @Override
    public void onDisable() {
        setUseHeld(false);
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null) return;

        if (swapCooldown > 0) {
            swapCooldown--;
            return;
        }

        if (eating && !MC.player.isUsingItem()) {
            setUseHeld(false);
            eating = false;
            swapCooldown = swapDelayTicksSetting.get();
            return;
        }

        double hunger = MC.player.getHungerManager().getFoodLevel();
        double health = MC.player.getHealth();

        if ((hunger >= 20.0 || hunger >= minHunger.get()) && health >= minHealth.get()){
            if (eating) {
                setUseHeld(false);
                eating = false;
                swapCooldown = (int) swapDelayTicksSetting.get();
            }
            return;
        }

        int bestSlot = getBestFoodSlot();
        if (bestSlot == -1) {
            if (eating) {
                setUseHeld(false);
                eating = false;
                swapCooldown = (int) swapDelayTicksSetting.get();
            }
            return;
        }

        int currentSlot = MC.player.getInventory().getSelectedSlot();

        if (!eating) {
            if (currentSlot != bestSlot) {
                INVENTORY_MANAGER.getSlotHandler().attemptSwitch(bestSlot);
                swapCooldown = (int) swapDelayTicksSetting.get();
            }
            setUseHeld(true);
            eating = true;
        }
    }

    private int getBestFoodSlot() {
        int bestSlot = -1;
        float bestScore = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
            float score = getFoodScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private float getFoodScore(ItemStack stack) {
        if (stack.isEmpty() || !stack.getItem().getComponents().contains(DataComponentTypes.FOOD)) {
            return -1;
        }

        Item item = stack.getItem();

        if (!allowPoisoned.get() && isPoisonedFood(item)) {
            return -1;
        }

        if (!allowGapples.get() && isGapple(item)) {
            return -1;
        }

        FoodComponent food = item.getComponents().get(DataComponentTypes.FOOD);
        float nutrition = food.comp_2491();
        float saturation = food.comp_2492();
        float totalValue = nutrition + saturation;

        if (isGapple(item)) {
            return allowGapples.get() ? totalValue - 0.5f : -1;
        }

        if (isPoisonedFood(item)) {
            return allowPoisoned.get() ? totalValue - 1.5f : -1;
        }

        return totalValue;
    }

    private boolean isGapple(Item item) {
        return item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE;
    }

    private boolean isPoisonedFood(Item item) {
        return item == Items.ROTTEN_FLESH
                || item == Items.PUFFERFISH
                || item == Items.SPIDER_EYE
                || item == Items.CHORUS_FRUIT;
    }

    private void setUseHeld(boolean held) {
        KeyBinding useKey = MC.options.useKey;
        InputUtil.Key boundKey = ((KeyBindingAccessor) useKey).getBoundKey();
        int keyCode = boundKey.getCode();
        boolean physicallyPressed = InputUtil.isKeyPressed(MC.getWindow().getHandle(), keyCode);
        useKey.setPressed(physicallyPressed || held);
    }
}
