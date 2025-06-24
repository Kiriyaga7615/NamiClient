package me.kiriyaga.essentials.feature.gui;

import me.kiriyaga.essentials.feature.module.Category;
import me.kiriyaga.essentials.feature.module.Module;
import me.kiriyaga.essentials.feature.module.impl.client.ColorModule;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.List;
import java.util.Set;

import static me.kiriyaga.essentials.Essentials.MINECRAFT;
import static me.kiriyaga.essentials.Essentials.MODULE_MANAGER;
import static me.kiriyaga.essentials.feature.gui.ClickGuiScreen.GUI_ALPHA;

public class CategoryPanel {
    public static final int WIDTH = 130;
    public static final int HEADER_HEIGHT = 20;
    public static final int GAP = 5;
    private static final int PADDING = 5;
    public static final int BORDER_WIDTH = 2;
    public static final int BOTTOM_MARGIN = 4;

    private final Category category;
    private final Set<Category> expandedCategories;
    private final Set<Module> expandedModules;

    private ColorModule getColorModule() {
        return MODULE_MANAGER.getModule(ColorModule.class);
    }

    public CategoryPanel(Category category, Set<Category> expandedCategories, Set<Module> expandedModules) {
        this.category = category;
        this.expandedCategories = expandedCategories;
        this.expandedModules = expandedModules;
    }

    public void render(DrawContext context, TextRenderer textRenderer, int x, int y, int mouseX, int mouseY, int screenHeight) {
        boolean hovered = isHeaderHovered(mouseX, mouseY, x, y);
        boolean expanded = expandedCategories.contains(category);

        ColorModule colorModule = getColorModule();
        Color primary = colorModule.getStyledPrimaryColor();
        Color secondary = colorModule.getStyledSecondaryColor();
        Color textCol = colorModule.getStyledTextColor();

        Color headerBgColor = expanded ? primary : (hovered ? brighten(secondary, 0.3f) : secondary);
        context.fill(x, y, x + WIDTH, y + HEADER_HEIGHT, headerBgColor.getRGB());
        context.drawText(textRenderer, category.name(), x + PADDING, y + 6, toRGBA(textCol), false);

        if (expanded) {
            int totalHeight = HEADER_HEIGHT;
            List<Module> modules = MODULE_MANAGER.getModulesByCategory(category);

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
            context.fill(x, y + HEADER_HEIGHT, x + 1, y + totalHeight, borderColor);
            context.fill(x + WIDTH - 1, y + HEADER_HEIGHT, x + WIDTH, y + totalHeight, borderColor);

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