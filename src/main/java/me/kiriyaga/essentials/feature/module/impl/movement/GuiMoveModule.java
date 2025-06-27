package me.kiriyaga.essentials.feature.module.impl.movement;

import me.kiriyaga.essentials.event.SubscribeEvent;
import me.kiriyaga.essentials.event.impl.KeyInputEvent;
import me.kiriyaga.essentials.event.impl.Render3DEvent;
import me.kiriyaga.essentials.feature.module.Category;
import me.kiriyaga.essentials.feature.module.Module;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class GuiMoveModule extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public GuiMoveModule() {
        super("gui move", "Allows movement and camera control in most GUIs.", Category.MOVEMENT);
    }

    @Override
    public void onDisable() {
        setKeysPressed(false);
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (!canMove()) return;
        handleInput(event.key, event.action, false);
    }

    private boolean canMove() {
        if (mc.currentScreen == null) return false;
        return !(mc.currentScreen instanceof ChatScreen
                || mc.currentScreen instanceof SignEditScreen
                || mc.currentScreen instanceof AnvilScreen
                || mc.currentScreen instanceof AbstractCommandBlockScreen
                || mc.currentScreen instanceof StructureBlockScreen
                || mc.currentScreen instanceof CreativeInventoryScreen
        );
    }

    private void handleInput(int key, int action, boolean mouse) {
        updateKey(mc.options.forwardKey);
        updateKey(mc.options.backKey);
        updateKey(mc.options.leftKey);
        updateKey(mc.options.rightKey);
        updateKey(mc.options.jumpKey);
        updateKey(mc.options.sneakKey);
        updateKey(mc.options.sprintKey);
    }

    private void updateKey(KeyBinding bind) {
        boolean physicallyPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), bind.getDefaultKey().getCode());
        bind.setPressed(physicallyPressed);
    }

    private void passKey(KeyBinding bind, int key, int action, boolean mouse) {
        if (!mouse && !bind.matchesKey(key, 0)) return;
        if (mouse && !bind.matchesMouse(key)) return;
        if (action == 1) bind.setPressed(true);  // GLFW_PRESS
        if (action == 0) bind.setPressed(false); // GLFW_RELEASE
    }

    private void setKeysPressed(boolean pressed) {
        mc.options.forwardKey.setPressed(pressed);
        mc.options.backKey.setPressed(pressed);
        mc.options.leftKey.setPressed(pressed);
        mc.options.rightKey.setPressed(pressed);
        mc.options.jumpKey.setPressed(pressed);
        mc.options.sneakKey.setPressed(pressed);
        mc.options.sprintKey.setPressed(pressed);
    }
}
