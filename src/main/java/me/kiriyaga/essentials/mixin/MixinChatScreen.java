package me.kiriyaga.essentials.mixin;

import me.kiriyaga.essentials.feature.module.impl.client.HUDModule;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;

import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.ArrayList;
import java.awt.Color;
import java.util.Comparator;
import java.util.stream.Collectors;

import me.kiriyaga.essentials.util.ChatAnimationHelper;
import me.kiriyaga.essentials.feature.module.impl.client.ColorModule;

import static me.kiriyaga.essentials.Essentials.*;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen {

    @Unique private float animationOffset = 20f;
    @Unique private long lastUpdateTime = System.currentTimeMillis();
    @Unique private List<String> suggestions;
    @Unique private String selectedSuggestion = "";
    @Unique private int selectedSuggestionIndex = 0;
    @Shadow @Final private TextFieldWidget chatField;
    @Unique private boolean drawSuggestions = false;

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        animationOffset = 20f;
        lastUpdateTime = System.currentTimeMillis();
        ChatAnimationHelper.setAnimationOffset(animationOffset);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        animationOffset = 20f;
        ChatAnimationHelper.setAnimationOffset(animationOffset);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        float elapsed = (now - lastUpdateTime) / 1000f;
        lastUpdateTime = now;

        animationOffset = Math.max(animationOffset - elapsed * 60f, 0f);

        ChatAnimationHelper.setAnimationOffset(animationOffset);
    }

    @Inject(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            shift = At.Shift.BEFORE
    ))
    private void beforeInputRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MODULE_MANAGER.getModule(HUDModule.class).chatAnimation.get()) {
            context.getMatrices().push();
            context.getMatrices().translate(0, animationOffset, 0);
        }
    }

    @Inject(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            shift = At.Shift.AFTER
    ))
    private void afterInputRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MODULE_MANAGER.getModule(HUDModule.class).chatAnimation.get()) {
            context.getMatrices().pop();
        }
    }

    @Redirect(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
    ))
    private void redirectFill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        int width = MINECRAFT.getWindow().getScaledWidth();
        int height = MINECRAFT.getWindow().getScaledHeight();
        boolean useAnimation = MODULE_MANAGER.getModule(HUDModule.class).chatAnimation.get();

        if (useAnimation && x1 == 2 && x2 == width - 2 && y1 >= height - 14 && y2 <= height - 2) {
            context.fill(x1, (int)(y1 + animationOffset), x2, (int)(y2 + animationOffset), color);
        } else {
            context.fill(x1, y1, x2, y2, color);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderSuggestions(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ColorModule colorModule = MODULE_MANAGER.getModule(ColorModule.class);
        Color primary = colorModule.getStyledPrimaryColor();
        Color secondary = colorModule.getStyledSecondaryColor();
        Color textCol = colorModule.getStyledTextColor();
        Color textColInverted = new Color(
                255 - textCol.getRed(),
                255 - textCol.getGreen(),
                255 - textCol.getBlue()
        );

        String text = chatField.getText();
        String[] parts = text.split("\\s+");
        String cmdName = parts.length > 0 ? parts[0] : "";

        String prefix = COMMAND_MANAGER.getPrefix();
        suggestions = new ArrayList<>(COMMAND_MANAGER.getSuggestions(cmdName));

        if (!suggestions.isEmpty() && selectedSuggestionIndex < suggestions.size()) {
            selectedSuggestion = suggestions.get(selectedSuggestionIndex);
        } else {
            selectedSuggestion = "";
            selectedSuggestionIndex = 0;
        }

        if (cmdName.startsWith(prefix)) {
            chatField.setEditableColor(Color.WHITE.getRGB());
        } else {
            chatField.setEditableColor(14737632);
        }

        if (selectedSuggestion.isEmpty()) return;

        chatField.setEditableColor(14737632);

        if (cmdName.startsWith(prefix) && suggestions.size() > 1 ){
            drawSuggestions = true;
        } else if (cmdName.equals(selectedSuggestion)) {
            drawSuggestions = false;
            return;
        }

        if (!text.startsWith(prefix) || suggestions.size() < 1) {
            drawSuggestions = false;
            return;
        }

        if (drawSuggestions) {
            int width = chatField.getWidth();
            int height = chatField.getHeight();

            List<String> suggestionsRest = suggestions.stream()
                    .filter(s -> !s.equals(selectedSuggestion))
                    .collect(Collectors.toList());

            int x = 10 + (width / 16);
            int y = chatField.getY();

            int _x = chatField.getX();
            int _y = chatField.getY();
            int spacing = 6;

            int selectedColor = new Color(255, 255, 255, 128).getRGB();
            context.drawText(MINECRAFT.textRenderer, selectedSuggestion, _x, _y, selectedColor, true);

            for (int i = 0; i < suggestionsRest.size(); i++) {
                String suggestion = suggestionsRest.get(i);
                int color = Color.LIGHT_GRAY.getRGB();
                boolean isSelected = i == selectedSuggestionIndex;

                if (isSelected) {
                    color = Color.LIGHT_GRAY.darker().getRGB();
                    float scale = 1.01f;

                    context.getMatrices().push();
                    context.getMatrices().translate(x, y, 0);
                    context.getMatrices().scale(scale, scale, 1.0f);
                    context.drawText(MINECRAFT.textRenderer, suggestion, 0, 0, color, true);
                    context.getMatrices().pop();

                    int textWidth = (int) (MINECRAFT.textRenderer.getWidth(suggestion) * scale);
                    x += textWidth + spacing;
                } else {
                    context.drawText(MINECRAFT.textRenderer, suggestion, x, y, color, true);
                    int textWidth = MINECRAFT.textRenderer.getWidth(suggestion);
                    x += textWidth + spacing;
                }
            }
        } else {
            suggestions = new ArrayList<>();
            drawSuggestions = false;
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!drawSuggestions || suggestions.isEmpty()) return;

        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            selectedSuggestionIndex = (selectedSuggestionIndex + 1) % suggestions.size();
            cir.setReturnValue(true);
        } else if (keyCode == GLFW.GLFW_KEY_UP) {
            selectedSuggestionIndex = (selectedSuggestionIndex - 1 + suggestions.size()) % suggestions.size();
            cir.setReturnValue(true);
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            suggestions.clear();
            selectedSuggestionIndex = 0;
            drawSuggestions = false;
            cir.setReturnValue(true);
        } else if (keyCode == GLFW.GLFW_KEY_TAB) {
            if (suggestions.size() == 1) {
                String selected = suggestions.get(0);
                chatField.setText(selected);
                chatField.setCursor(selected.length(), false);
                selectedSuggestionIndex = 0;
                drawSuggestions = false;
            } else {
                selectedSuggestionIndex = (selectedSuggestionIndex + 1) % suggestions.size();
            }
            cir.setReturnValue(true);
        } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            String text = chatField.getText();
            String[] parts = text.split("\\s+");
            String cmdName = parts.length > 0 ? parts[0] : "";
            String selected = suggestions.get(selectedSuggestionIndex);

            if (!cmdName.equals(selected)) {
                chatField.setText(selected);
                chatField.setCursor(selected.length(), false);
            }

            suggestions.clear();
            drawSuggestions = false;
            selectedSuggestionIndex = 0;
            cir.setReturnValue(true);
        }
    }
}