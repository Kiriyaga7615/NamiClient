package me.kiriyaga.nami.feature.gui;

import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.List;
import java.util.Set;

import static me.kiriyaga.nami.Nami.MODULE_MANAGER;
import static me.kiriyaga.nami.feature.gui.ClickGuiScreen.GUI_ALPHA;

public class CategoryPanel {
    public static final int WIDTH = 130;
    public static final int HEADER_HEIGHT = 15;
    public static final int GAP = 5;
    private static final int PADDING = 5;
    public static final int BORDER_WIDTH = 2;
    public static final int BOTTOM_MARGIN = 4;

    private final ModuleCategory moduleCategory;
    private final Set<ModuleCategory> expandedCategories;
    private final Set<Module> expandedModules;

    private ColorModule getColorModule() {
        return MODULE_MANAGER.getStorage().getByClass(ColorModule.class);
    }

    public CategoryPanel(ModuleCategory moduleCategory, Set<ModuleCategory> expandedCategories, Set<Module> expandedModules) {
        this.moduleCategory = moduleCategory;
        this.expandedCategories = expandedCategories;
        this.expandedModules = expandedModules;
    }

    public void render(DrawContext context, TextRenderer textRenderer, int x, int y, int mouseX, int mouseY, int screenHeight) {
        boolean hovered = isHeaderHovered(mouseX, mouseY, x, y);
        boolean expanded = expandedCategories.contains(moduleCategory);

        ColorModule colorModule = getColorModule();
        Color primary = colorModule.getStyledGlobalColor();
        Color secondary = colorModule.getStyledSecondColor();
        Color textCol = new Color(255, 255, 255, 255);

        Color headerBgColor = expanded ? primary : (hovered ? brighten(secondary, 0.3f) : secondary);
        context.fill(x, y, x + WIDTH, y + HEADER_HEIGHT, headerBgColor.getRGB());
        int textY = y + (HEADER_HEIGHT - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, moduleCategory.getName(), x + PADDING, textY, toRGBA(textCol), false);

        if (expanded) {
            int totalHeight = HEADER_HEIGHT;
            List<Module> modules = MODULE_MANAGER.getStorage().getByCategory(moduleCategory);

            for (Module module : modules) {
                totalHeight += ModulePanel.HEIGHT + ModulePanel.MODULE_SPACING;
                if (expandedModules.contains(module)) {
                    totalHeight += SettingPanel.getSettingsHeight(module);
                }
            }

            totalHeight += BOTTOM_MARGIN;

            int borderColor = toRGBA(primary);
            int bgColor = toRGBA(new Color(30, 30, 30, GUI_ALPHA));

            context.fill(x, y + HEADER_HEIGHT, x + WIDTH, y + totalHeight, bgColor);
            context.fill(x, y + HEADER_HEIGHT, x + WIDTH, y + HEADER_HEIGHT + 1, borderColor);
            context.fill(x, y + totalHeight - 1, x + WIDTH, y + totalHeight, borderColor);
            context.fill(x, y + HEADER_HEIGHT + 1, x + 1, y + totalHeight - 1, borderColor);
            context.fill(x + WIDTH - 1, y + HEADER_HEIGHT + 1, x + WIDTH, y + totalHeight - 1, borderColor);


            int moduleY = y + HEADER_HEIGHT + ModulePanel.MODULE_SPACING;
            for (Module module : modules) {
                ModulePanel modulePanel = new ModulePanel(module, expandedModules);
                modulePanel.render(context, textRenderer, x + BORDER_WIDTH + SettingPanel.INNER_PADDING, moduleY, mouseX, mouseY);
                moduleY += ModulePanel.HEIGHT + ModulePanel.MODULE_SPACING;

                if (expandedModules.contains(module)) {
                    moduleY += SettingPanel.renderSettings(context, textRenderer, module,
                            x + BORDER_WIDTH + SettingPanel.INNER_PADDING,
                            moduleY, mouseX, mouseY);
                }
            }

        }
    }

    public static boolean isHeaderHovered(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }

    private static int toRGBA(Color color) {
        return (color.getAlpha() << 24) |
                (color.getRed() << 16) |
                (color.getGreen() << 8) |
                color.getBlue();
    }

    private static Color brighten(Color color, float amount) {
        int r = Math.min(255, (int)(color.getRed() + 255 * amount));
        int g = Math.min(255, (int)(color.getGreen() + 255 * amount));
        int b = Math.min(255, (int)(color.getBlue() + 255 * amount));
        return new Color(r, g, b, color.getAlpha());
    }

    private static Color darken(Color color, float amount) {
        int r = Math.max(0, (int)(color.getRed() - 255 * amount));
        int g = Math.max(0, (int)(color.getGreen() - 255 * amount));
        int b = Math.max(0, (int)(color.getBlue() - 255 * amount));
        return new Color(r, g, b, color.getAlpha());
    }
}