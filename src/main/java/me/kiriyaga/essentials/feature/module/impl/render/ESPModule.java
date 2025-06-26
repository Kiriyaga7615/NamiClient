package me.kiriyaga.essentials.feature.module.impl.render;

import me.kiriyaga.essentials.event.EventPriority;
import me.kiriyaga.essentials.event.SubscribeEvent;
import me.kiriyaga.essentials.event.impl.Render3DEvent;
import me.kiriyaga.essentials.feature.module.Category;
import me.kiriyaga.essentials.feature.module.Module;
import me.kiriyaga.essentials.feature.module.impl.client.ColorModule;
import me.kiriyaga.essentials.setting.impl.BoolSetting;
import me.kiriyaga.essentials.setting.impl.DoubleSetting;
import me.kiriyaga.essentials.util.EntityUtils;
import me.kiriyaga.essentials.util.render.RenderUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.awt.*;

import static me.kiriyaga.essentials.Essentials.*;

public class ESPModule extends Module {

    public final BoolSetting showPlayers = addSetting(new BoolSetting("players", true));
    public final BoolSetting showPeacefuls = addSetting(new BoolSetting("peacefuls", true));
    public final BoolSetting showNeutrals = addSetting(new BoolSetting("neutrals", true));
    public final BoolSetting showHostiles = addSetting(new BoolSetting("hostiles", true));
    public final BoolSetting showItems = addSetting(new BoolSetting("items", true));
    public final DoubleSetting lineWidth = addSetting(new DoubleSetting("line", 1.5, 0.5, 2.5));
    public final BoolSetting filled = addSetting(new BoolSetting("filled", false));

    private static final Color COLOR_PASSIVE = new Color(211, 211, 211, 255);
    private static final Color COLOR_NEUTRAL = new Color(255, 255, 0, 255);
    private static final Color COLOR_HOSTILE = new Color(255, 0, 0, 255);
    private static final Color COLOR_ITEM = new Color(211, 211, 211, 255);

    public ESPModule() {
        super("esp", "Draws boxes around entities", Category.RENDER, "esp", "WH", "boxes", "уыз");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender3D(Render3DEvent event) {
        if (MINECRAFT == null || MINECRAFT.world == null || MINECRAFT.player == null) return;

        MatrixStack matrices = event.getMatrices();
        ColorModule colorModule = MODULE_MANAGER.getModule(ColorModule.class);

        if (showPlayers.get()) {
            for (Entity player : ENTITY_MANAGER.getOtherPlayers()) {
                if (player.isRemoved()) continue;
                drawBox(player, colorModule.getStyledPrimaryColor(), matrices, event.getTickDelta());
            }
        }

        if (showPeacefuls.get()) {
            for (Entity entity : ENTITY_MANAGER.getPassive()) {
                if (!entity.isAlive()) continue;
                drawBox(entity, COLOR_PASSIVE, matrices, event.getTickDelta());
            }
        }

        if (showNeutrals.get()) {
            for (Entity entity : ENTITY_MANAGER.getNeutral()) {
                if (!entity.isAlive()) continue;
                drawBox(entity, COLOR_NEUTRAL, matrices, event.getTickDelta());
            }
        }

        if (showHostiles.get()) {
            for (Entity entity : ENTITY_MANAGER.getHostile()) {
                if (!entity.isAlive()) continue;
                drawBox(entity, COLOR_HOSTILE, matrices, event.getTickDelta());
            }
        }

        if (showItems.get()) {
            for (ItemEntity item :  ENTITY_MANAGER.getDroppedItems()) {
                if (item.isRemoved()) continue;
                drawBox(item, COLOR_ITEM, matrices, event.getTickDelta());
            }
        }
    }

    private void drawBox(Entity entity, Color color, MatrixStack matrices, float partialTicks) {
        double interpX = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * partialTicks;
        double interpY = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * partialTicks;
        double interpZ = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * partialTicks;

        Box box = entity.getBoundingBox().offset(
                interpX - entity.getX(),
                interpY - entity.getY(),
                interpZ - entity.getZ()
        );

        if (filled.get()) {
            RenderUtil.drawBoxFilled(matrices, box, new Color(color.getRed(), color.getGreen(), color.getBlue(), 75));
        } else {
            RenderUtil.drawBox(matrices, box, color, lineWidth.get());
        }
    }
}
