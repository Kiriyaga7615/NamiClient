package me.kiriyaga.essentials.feature.module.impl.movement;

import me.kiriyaga.essentials.event.SubscribeEvent;
import me.kiriyaga.essentials.event.impl.KeyInputEvent;
import me.kiriyaga.essentials.event.impl.Render3DEvent;
import me.kiriyaga.essentials.feature.module.Category;
import me.kiriyaga.essentials.feature.module.Module;
import me.kiriyaga.essentials.mixin.KeyBindingAccessor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import static me.kiriyaga.essentials.Essentials.MINECRAFT;

public class GuiMoveModule extends Module {

    private boolean forwardHeld = false;
    private boolean backHeld = false;
    private boolean leftHeld = false;
    private boolean rightHeld = false;
    private boolean jumpHeld = false;
    private boolean sneakHeld = false;
    private boolean sprintHeld = false;

    public GuiMoveModule() {
        super("gui move", "Allows movement and camera control in most GUIs.", Category.MOVEMENT);
    }

    @Override
    public void onDisable() {
        forwardHeld = false;
        backHeld = false;
        leftHeld = false;
        rightHeld = false;
        jumpHeld = false;
        sneakHeld = false;
        sprintHeld = false;
        setKeysPressed(false);
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (!isEnabled() || !canMove()) return;

        updateHeld(MINECRAFT.options.forwardKey, event.key, event.action, false, v -> forwardHeld = v);
        updateHeld(MINECRAFT.options.backKey, event.key, event.action, false, v -> backHeld = v);
        updateHeld(MINECRAFT.options.leftKey, event.key, event.action, false, v -> leftHeld = v);
        updateHeld(MINECRAFT.options.rightKey, event.key, event.action, false, v -> rightHeld = v);
        updateHeld(MINECRAFT.options.jumpKey, event.key, event.action, false, v -> jumpHeld = v);
        updateHeld(MINECRAFT.options.sneakKey, event.key, event.action, false, v -> sneakHeld = v);
        updateHeld(MINECRAFT.options.sprintKey, event.key, event.action, false, v -> sprintHeld = v);
    }

    private void updateHeld(KeyBinding bind, int key, int action, boolean mouse, java.util.function.Consumer<Boolean> setter) {
        if (!mouse && !bind.matchesKey(key, 0)) return;
        if (mouse && !bind.matchesMouse(key)) return;

        setter.accept(action == GLFW.GLFW_PRESS);
    }

    @SubscribeEvent
    public void onRender3D(Render3DEvent event) {
        if (!isEnabled() || !canMove()) return;

        updateKeyWithHold(MINECRAFT.options.forwardKey, forwardHeld);
        updateKeyWithHold(MINECRAFT.options.backKey, backHeld);
        updateKeyWithHold(MINECRAFT.options.leftKey, leftHeld);
        updateKeyWithHold(MINECRAFT.options.rightKey, rightHeld);
        updateKeyWithHold(MINECRAFT.options.jumpKey, jumpHeld);
        updateKeyWithHold(MINECRAFT.options.sneakKey, sneakHeld);
        updateKeyWithHold(MINECRAFT.options.sprintKey, sprintHeld);
    }

    private void updateKeyWithHold(KeyBinding bind, boolean held) {
        InputUtil.Key boundKey = ((KeyBindingAccessor) bind).getBoundKey();
        int keyCode = boundKey.getCode();
        boolean physicallyPressed = InputUtil.isKeyPressed(MINECRAFT.getWindow().getHandle(), keyCode);
        bind.setPressed(physicallyPressed || held);
    }

    private boolean canMove() {
        if (MINECRAFT.currentScreen == null) return false;
        return !(MINECRAFT.currentScreen instanceof ChatScreen
                || MINECRAFT.currentScreen instanceof SignEditScreen
                || MINECRAFT.currentScreen instanceof AnvilScreen
                || MINECRAFT.currentScreen instanceof AbstractCommandBlockScreen
                || MINECRAFT.currentScreen instanceof StructureBlockScreen
                || MINECRAFT.currentScreen instanceof CreativeInventoryScreen
        );
    }

    private void setKeysPressed(boolean pressed) {
        MINECRAFT.options.forwardKey.setPressed(pressed);
        MINECRAFT.options.backKey.setPressed(pressed);
        MINECRAFT.options.leftKey.setPressed(pressed);
        MINECRAFT.options.rightKey.setPressed(pressed);
        MINECRAFT.options.jumpKey.setPressed(pressed);
        MINECRAFT.options.sneakKey.setPressed(pressed);
        MINECRAFT.options.sprintKey.setPressed(pressed);
    }
}
