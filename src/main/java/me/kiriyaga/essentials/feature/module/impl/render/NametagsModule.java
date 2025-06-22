package me.kiriyaga.essentials.feature.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import me.kiriyaga.essentials.event.EventPriority;
import me.kiriyaga.essentials.event.SubscribeEvent;
import me.kiriyaga.essentials.event.impl.Render2DEvent;
import me.kiriyaga.essentials.feature.module.Category;
import me.kiriyaga.essentials.feature.module.Module;
import me.kiriyaga.essentials.feature.module.impl.client.ColorModule;
import me.kiriyaga.essentials.setting.impl.BoolSetting;
import me.kiriyaga.essentials.setting.impl.EnumSetting;
import me.kiriyaga.essentials.util.EntityUtils;
import me.kiriyaga.essentials.util.MatrixCache;
import me.kiriyaga.essentials.util.NametagFormatter;
import me.kiriyaga.essentials.util.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.Camera;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.*;

import static me.kiriyaga.essentials.Essentials.MINECRAFT;
import static me.kiriyaga.essentials.Essentials.MODULE_MANAGER;

public class NametagsModule extends Module {

    public final BoolSetting showPlayers = addSetting(new BoolSetting("players", true));
    public final BoolSetting showAnimals = addSetting(new BoolSetting("peacefuls", false));
    public final BoolSetting showEnemies = addSetting(new BoolSetting("hostiles", false));
    public final BoolSetting showItems = addSetting(new BoolSetting("items", true));
    public final BoolSetting showEquipment = addSetting(new BoolSetting("show equipment", true));
    public final EnumSetting<TextFormat> formatting = addSetting(new EnumSetting<>("format", TextFormat.None));
    public final BoolSetting showBackground = addSetting(new BoolSetting("background", true));

    private final NametagFormatter formatter = new NametagFormatter(this);
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public enum TextFormat {
        None, Bold, Italic, Both
    }

    public NametagsModule() {
        super("nametags", "Draws names above entities", Category.RENDER, "nametag", "nmtags", "names", "тфьуефпы");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender2D(Render2DEvent event) {
        if (mc == null || mc.world == null || mc.player == null) return;

        DrawContext drawContext = event.getDrawContext();
        MatrixStack matrices = drawContext.getMatrices();
        Camera camera = mc.gameRenderer.getCamera();

        FreecamModule freecamModule = MODULE_MANAGER.getModule(FreecamModule.class);
        ColorModule colorModule = MODULE_MANAGER.getModule(ColorModule.class);

        int color = new Color(colorModule.getStyledPrimaryColor().getRed(), colorModule.getStyledPrimaryColor().getGreen(), colorModule.getStyledPrimaryColor().getBlue(), 255).getRGB();
        if (showPlayers.get()) {
            for (PlayerEntity player : EntityUtils.getPlayers()) {
                if ((player == mc.player && !freecamModule.isEnabled()) || player.isRemoved())
                    continue;

                renderNametag2D(player, formatter.formatPlayer(player), color, camera, drawContext, MatrixCache.positionMatrix, MatrixCache.projectionMatrix, event.getRenderTickCounter().getTickDelta(true));
            }
        }

        if (showAnimals.get()) {
            for (Entity animal : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.PASSIVE)) {
                if (animal.isRemoved()) continue;
                renderNametag2D(animal, formatter.formatEntity(animal), 0xFFAAAAAA, camera, drawContext, MatrixCache.positionMatrix, MatrixCache.projectionMatrix, event.getRenderTickCounter().getTickDelta(true));
            }
        }

        if (showEnemies.get()) {
            for (Entity hostile : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.HOSTILE)) {
                if (hostile.isRemoved()) continue;
                renderNametag2D(hostile, formatter.formatEntity(hostile), 0xFFFF5555, camera, drawContext, MatrixCache.positionMatrix, MatrixCache.projectionMatrix, event.getRenderTickCounter().getTickDelta(true));
            }
        }

        if (showItems.get()) {
            for (ItemEntity item : EntityUtils.getDroppedItems()) {
                if (item.isRemoved() || item.getStack().isEmpty()) continue;
                renderNametag2D(item, formatter.formatItem(item), 0xFFFF5555, camera, drawContext, MatrixCache.positionMatrix, MatrixCache.projectionMatrix, event.getRenderTickCounter().getTickDelta(true));
            }
        }
    }

    private void renderNametag2D(Entity entity, Text text, int color,
                                 Camera camera, DrawContext drawContext,
                                 Matrix4f positionMatrix, Matrix4f projectionMatrix, float tickDelta) {

        double x = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * tickDelta;
        double y = (entity.lastRenderY + (entity.getY() - entity.lastRenderY) * tickDelta) + entity.getHeight() + 0.2;
        double z = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * tickDelta;

        Vec3d interpolated = new Vec3d(x, y, z);

        Vec3d projected = worldToScreen(interpolated, camera, positionMatrix, projectionMatrix);
        if (projected == null) return;

        int xScreen = (int) projected.x;
        int yScreen = (int) projected.y;

        int textWidth = mc.textRenderer.getWidth(text);
        int textHeight = mc.textRenderer.fontHeight;

        int bgPadding = 1;
        if (showBackground.get()) {
            int bgColor = (180 << 24) | 0x000000;

            int x1 = xScreen - textWidth / 2 - bgPadding;
            int y1 = yScreen - bgPadding;
            int x2 = xScreen + textWidth / 2 + bgPadding;
            int y2 = yScreen + textHeight + bgPadding -1;

            drawContext.fill(x1, y1, x2, y2, bgColor);

            MatrixStack matrixStack = drawContext.getMatrices();
            matrixStack.push();
            RenderUtil.rect(matrixStack, x1-1, y1-1, x2, y2, color, 1.0f); // thanks mojang

            matrixStack.pop();
        }


        drawContext.drawText(mc.textRenderer, text, xScreen - textWidth / 2, yScreen, color, false);

        if (entity instanceof PlayerEntity && showEquipment.get()) {
            renderItemRow2D((PlayerEntity) entity, drawContext, projected);
        }
    }


    private void renderItemRow2D(PlayerEntity player, DrawContext drawContext, Vec3d baseScreenPos) {
        ItemStack[] items = new ItemStack[] {
                player.getOffHandStack(),
                player.getEquippedStack(EquipmentSlot.HEAD),
                player.getEquippedStack(EquipmentSlot.CHEST),
                player.getEquippedStack(EquipmentSlot.LEGS),
                player.getEquippedStack(EquipmentSlot.FEET),
                player.getMainHandStack()
        };

        double spacing = 16;
        double totalWidth = (items.length - 1) * spacing;
        double startX = baseScreenPos.x - totalWidth / 2.0;
        int y = (int) baseScreenPos.y - 14;

        for (int i = 0; i < items.length; i++) {
            ItemStack stack = items[i];
            if (stack.isEmpty()) continue;
            int x = (int) (startX + i * spacing);

            drawContext.drawItem(stack, x - 8, y - 8);
            drawContext.drawStackOverlay(MINECRAFT.textRenderer, stack ,x - 8, y-8);
        }
    }


    private Vec3d worldToScreen(Vec3d worldPos, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix) {
        Vec3d camPos = camera.getPos();

        float relX = (float) (worldPos.x - camPos.x);
        float relY = (float) (worldPos.y - camPos.y);
        float relZ = (float) (worldPos.z - camPos.z);

        Vector4f vec = new Vector4f(relX, relY, relZ, 1.0f);

        Matrix4f viewProjection = new Matrix4f();
        projectionMatrix.mul(positionMatrix, viewProjection);
        vec.mul(viewProjection);

        if (vec.w <= 0.0f) return null;

        vec.div(vec.w);

        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

        float screenX = (vec.x * 0.5f + 0.5f) * screenWidth;
        float screenY = (1.0f - (vec.y * 0.5f + 0.5f)) * screenHeight;

        return new Vec3d(screenX, screenY, vec.z);
    }


}
