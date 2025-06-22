package me.kiriyaga.essentials.feature.module.impl.client;

import me.kiriyaga.essentials.event.EventPriority;
import me.kiriyaga.essentials.event.SubscribeEvent;
import me.kiriyaga.essentials.event.impl.Render2DEvent;
import me.kiriyaga.essentials.event.impl.PreTickEvent;
import me.kiriyaga.essentials.feature.module.Category;
import me.kiriyaga.essentials.feature.module.Module;
import me.kiriyaga.essentials.feature.module.impl.render.FreecamModule;
import me.kiriyaga.essentials.setting.impl.BoolSetting;
import me.kiriyaga.essentials.setting.impl.IntSetting;
import me.kiriyaga.essentials.util.ChatAnimationHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.awt.*;
import java.text.Normalizer;

import static me.kiriyaga.essentials.Essentials.*;

public class HUDModule extends Module {

    public final IntSetting updateInterval = addSetting(new IntSetting("interval", 1, 1, 20));

    public final BoolSetting watermarkEnabled = addSetting(new BoolSetting("watermark", true));
    public final BoolSetting armorEnabled = addSetting(new BoolSetting("armor", true));
    public final BoolSetting totemsEnabled = addSetting(new BoolSetting("totems", true));
    public final BoolSetting coordsEnabled = addSetting(new BoolSetting("coordinates", true));
    public final BoolSetting freecamCords = addSetting(new BoolSetting("freecam spoof ", true));
    public final BoolSetting facingEnabled = addSetting(new BoolSetting("facing", true));
    public final BoolSetting fpsEnabled = addSetting(new BoolSetting("fps", true));
    public final BoolSetting pingEnabled = addSetting(new BoolSetting("ping", false));
    public final BoolSetting lagWarningEnabled = addSetting(new BoolSetting("lag warning", false));

    private int tickCounter = 0;
    private static final int PADDING = 2;

    private Text coordsText = Text.empty();
    private Text facingText = Text.empty();
    private Text watermarkText = Text.empty();
    private Text fpsText = Text.empty();
    private Text pingText = Text.empty();
    private boolean serverLagging = false;
    private int primaryRGB;


    public HUDModule() {
        super("hud","Displays in game hud.", Category.CLIENT, "ргв");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdate(PreTickEvent event) {
        tickCounter++;
        if (tickCounter % updateInterval.get() != 0) return;

        updateAllData();
    }

    private void updateAllData() {
        MinecraftClient mc = MINECRAFT;
        if (mc.world == null || mc.player == null) return;

        Color styled = getColorModule().getStyledPrimaryColor();
        primaryRGB = styled.getRGB() & 0x00FFFFFF;


        if (watermarkEnabled.get()) {
            watermarkText = Text.of(NAME + " " + VERSION);
        }

        if (coordsEnabled.get()) {

            double x;
            double y;
            double z;

            FreecamModule freecamModule = MODULE_MANAGER.getModule(FreecamModule.class);


            if (freecamCords.get() && freecamModule.isEnabled()){
                x = freecamModule.getX();
                y = freecamModule.getY();
                z = freecamModule.getZ();
            } else {
                x = mc.player.getX();
                y = mc.player.getY();
                z = mc.player.getZ();
            }


            coordsText = formatFancyCoords(x, y, z);

        }

        if (facingEnabled.get()) {
            facingText = getFacingText(mc);
        }

        if (fpsEnabled.get()) {
            fpsText = Text.literal("FPS: ").setStyle(Style.EMPTY.withColor(primaryRGB))
                    .append(Text.literal(Integer.toString(mc.getCurrentFps())).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
        }

        if (pingEnabled.get()) {
            int ping = PING_MANAGER.getPing();
            pingText = Text.literal("Ping: ").setStyle(Style.EMPTY.withColor(primaryRGB))
                    .append(Text.literal(ping < 0 ? "N/A" : Integer.toString(ping)).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));        }

        if (lagWarningEnabled.get()) {
            serverLagging = PING_MANAGER.isConnectionUnstable();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender2D(Render2DEvent event) {
        MinecraftClient mc = MINECRAFT;
        if (mc.world == null || mc.player == null) return;

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        float rawOffset = ChatAnimationHelper.getAnimationOffset();

        float animationOffset = (float) (14f - (rawOffset/1.428571428571429));

        renderTopLeft(event, primaryRGB);

        renderBottomLeft(event, primaryRGB, screenHeight, animationOffset);

        renderBottomRight(event, primaryRGB, screenWidth, screenHeight, animationOffset);

        if (lagWarningEnabled.get() && serverLagging) {
            drawLagWarning(event, screenWidth, screenHeight);
        }

        if (armorEnabled.get()) {
            renderArmorHUD(event);
        }

        if (totemsEnabled.get()) {
            renderTotems(event, screenWidth, screenHeight);
        }
    }

    private void renderTopLeft(Render2DEvent event, int color) {
        if (watermarkEnabled.get() && !watermarkText.getString().isEmpty()) {
            event.getDrawContext().drawText(MINECRAFT.textRenderer, watermarkText, PADDING, PADDING, color, true);
        }
    }

    private void renderBottomLeft(Render2DEvent event, int color, int screenHeight, float animationOffset) {
        int y = screenHeight - MINECRAFT.textRenderer.fontHeight - PADDING;
        y -= (int) animationOffset;

        if (coordsEnabled.get() && !coordsText.getString().isEmpty()) {
            event.getDrawContext().drawText(MINECRAFT.textRenderer, coordsText, PADDING, y, color, true);
            y -= MINECRAFT.textRenderer.fontHeight + 2;
        }

        if (facingEnabled.get() && !facingText.getString().isEmpty()) {
            event.getDrawContext().drawText(MINECRAFT.textRenderer, facingText, PADDING, y, color, true);
            y -= MINECRAFT.textRenderer.fontHeight + 2;
        }
    }

    private void renderBottomRight(Render2DEvent event, int color, int screenWidth, int screenHeight, float animationOffset) {
        int y = screenHeight - MINECRAFT.textRenderer.fontHeight;
        y -= (int) animationOffset;

        if (fpsEnabled.get() && !fpsText.getString().isEmpty()) {
            int width = MINECRAFT.textRenderer.getWidth(fpsText);
            event.getDrawContext().drawText(MINECRAFT.textRenderer, fpsText, screenWidth - width - PADDING, y, color, true);
            y -= MINECRAFT.textRenderer.fontHeight + 2;
        }

        if (pingEnabled.get() && !pingText.getString().isEmpty()) {
            int width = MINECRAFT.textRenderer.getWidth(pingText);
            event.getDrawContext().drawText(MINECRAFT.textRenderer, pingText, screenWidth - width - PADDING, y, color, true);
            y -= MINECRAFT.textRenderer.fontHeight + 2;
        }
    }

    private void renderArmorHUD(Render2DEvent event) {
        MinecraftClient mc = MINECRAFT;
        if (mc.player == null) return;

        double x = mc.getWindow().getScaledWidth() / 2 + 7 * mc.getWindow().getScaleFactor();
        double y = mc.getWindow().getScaledHeight() - 28 * mc.getWindow().getScaleFactor();

        ItemStack[] armor = new ItemStack[]{
                mc.player.getEquippedStack(EquipmentSlot.HEAD),
                mc.player.getEquippedStack(EquipmentSlot.CHEST),
                mc.player.getEquippedStack(EquipmentSlot.LEGS),
                mc.player.getEquippedStack(EquipmentSlot.FEET)
        };

        for (int i = 0; i < armor.length; i++) {
            ItemStack stack = armor[i];
            if (stack.isEmpty()) continue;

            double armorX = x + i * 18;
            double armorY = y;

            event.getDrawContext().drawItem(stack, (int) armorX, (int) armorY);
            event.getDrawContext().drawStackOverlay(MINECRAFT.textRenderer, stack, (int) armorX, (int) armorY);

            if (stack.isDamageable()) {
                float durability = (stack.getMaxDamage() - (float) stack.getDamage()) / stack.getMaxDamage();
                int percent = Math.round(durability * 100);
                int r = (int) ((1 - durability) * 255);
                int g = (int) (durability * 255);

                String durabilityText = Integer.toString(percent);
                event.getDrawContext().drawText(
                        MINECRAFT.textRenderer,
                        durabilityText,
                        (int) (armorX + 1),
                        (int) (armorY - 10),
                        new Color(r, g, 0).getRGB(),
                        true
                );
            }
        }
    }


    private void renderTotems(Render2DEvent event, int screenWidth, int screenHeight) {
        MinecraftClient mc = MINECRAFT;
        if (mc.player == null) return;

        int totemCount = 0;

        for (ItemStack stack : mc.player.getInventory().main) {
            if (stack.getItem() == net.minecraft.item.Items.TOTEM_OF_UNDYING) {
                totemCount += stack.getCount();
            }
        }

        ItemStack offHandStack = mc.player.getOffHandStack();
        if (offHandStack.getItem() == net.minecraft.item.Items.TOTEM_OF_UNDYING) {
            totemCount += offHandStack.getCount();
        }

        // if (totemCount == 0) return;

        int y = screenHeight - 52;

        int xCenter = screenWidth / 2;

        int iconSize = 16;
        int iconX = xCenter - iconSize / 2;
        int iconY = y;

        ItemStack totemStack = new ItemStack(net.minecraft.item.Items.TOTEM_OF_UNDYING);

        event.getDrawContext().drawItem(totemStack, iconX, iconY);

        String countStr = String.valueOf(totemCount);

        int textX = iconX + iconSize - 4;
        int textY = iconY + iconSize - 6;

        event.getDrawContext().drawText(
                mc.textRenderer,
                Text.literal(countStr).setStyle(Style.EMPTY.withColor(0xFFFFFF)),
                textX,
                textY,
                0xFFFFFF,
                true
        );
    }

    private void drawLagWarning(Render2DEvent event, int screenWidth, int screenHeight) {
        String warningText = "Server is lagging!";
        int textWidth = MINECRAFT.textRenderer.getWidth(warningText);
        int textHeight = MINECRAFT.textRenderer.fontHeight;

        int x = (screenWidth - textWidth) / 2;
        int y = ((screenHeight - textHeight) / 2) - 60;

        event.getDrawContext().drawText(MINECRAFT.textRenderer, warningText, x, y, primaryRGB, true);
    }

    private Text formatFancyCoords(double x, double y, double z) {
        MinecraftClient mc = MinecraftClient.getInstance();

        boolean isNether = mc.world != null && mc.world.getRegistryKey() == World.NETHER;
        boolean isOverworld = mc.world != null && mc.world.getRegistryKey() == World.OVERWORLD;

        double xAlt = isNether ? x * 8 : x / 8;
        double zAlt = isNether ? z * 8 : z / 8;

        Style primaryStyle = Style.EMPTY.withColor(primaryRGB);
        Style grayStyle = Style.EMPTY.withColor(Formatting.GRAY);
        Style whiteStyle = Style.EMPTY.withColor(Formatting.WHITE);

        MutableText result = Text.literal("XYZ ").setStyle(primaryStyle);
        result.append(formatNumberStyled(x, whiteStyle, grayStyle));
        result.append(Text.literal(" ").setStyle(primaryStyle));
        result.append(formatNumberStyled(y, whiteStyle, grayStyle));
        result.append(Text.literal(" ").setStyle(primaryStyle));
        result.append(formatNumberStyled(z, whiteStyle, grayStyle));

        if (isOverworld || isNether) {
            result.append(Text.literal(" [").setStyle(primaryStyle));
            result.append(formatNumberStyled(xAlt, whiteStyle, grayStyle));
            result.append(Text.literal(", ").setStyle(primaryStyle));
            result.append(formatNumberStyled(zAlt, whiteStyle, grayStyle));
            result.append(Text.literal("]").setStyle(primaryStyle));
        }
        return result;
    }

    private MutableText formatNumberStyled(double val, Style digitStyle, Style separatorStyle) {
        String formatted = formatNumber(val); // "123.0"
        int dotIndex = formatted.indexOf('.');

        if (dotIndex == -1) {
            return Text.literal(formatted).setStyle(digitStyle);
        }

        MutableText text = Text.literal(formatted.substring(0, dotIndex)).setStyle(digitStyle);
        text.append(Text.literal(".").setStyle(separatorStyle));
        text.append(Text.literal(formatted.substring(dotIndex + 1)).setStyle(digitStyle));

        return text;
    }


    private String formatNumber(double val) {
        double rounded = Math.round(val * 10.0) / 10.0;
        return String.format("%.1f", rounded).replace(',', '.');
    }


    private Text getFacingText(MinecraftClient mc) {
        if (mc.player == null) return Text.literal("Invalid").setStyle(Style.EMPTY.withColor(Formatting.RED));

        int facingIndex = MathHelper.floor(mc.player.getYaw() * 4.0F / 360.0F + 0.5D) & 3;

        String dir = switch (facingIndex) {
            case 0 -> "South";
            case 1 -> "West";
            case 2 -> "North";
            case 3 -> "East";
            default -> "Invalid";
        };

        String axis = switch (facingIndex) {
            case 0 -> "+Z";
            case 1 -> "-X";
            case 2 -> "-Z";
            case 3 -> "+X";
            default -> "?";
        };

        Style primaryStyle = Style.EMPTY.withColor(primaryRGB);
        Style whiteStyle = Style.EMPTY.withColor(Formatting.WHITE);

        MutableText text = Text.literal(dir).setStyle(primaryStyle);
        text.append(Text.literal(" [").setStyle(primaryStyle));
        text.append(Text.literal(axis).setStyle(whiteStyle));
        text.append(Text.literal("]").setStyle(primaryStyle));

        return text;
    }
    private ColorModule getColorModule() {
        return MODULE_MANAGER.getModule(ColorModule.class);
    }
}
