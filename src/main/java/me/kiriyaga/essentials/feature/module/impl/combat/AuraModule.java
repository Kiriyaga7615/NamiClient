package me.kiriyaga.essentials.feature.module.impl.combat;

import me.kiriyaga.essentials.event.EventPriority;
import me.kiriyaga.essentials.event.SubscribeEvent;
import me.kiriyaga.essentials.event.impl.PreTickEvent;
import me.kiriyaga.essentials.event.impl.Render3DEvent;
import me.kiriyaga.essentials.feature.module.Category;
import me.kiriyaga.essentials.feature.module.Module;
import me.kiriyaga.essentials.feature.module.impl.client.ColorModule;
import me.kiriyaga.essentials.manager.RotationManager;
import me.kiriyaga.essentials.setting.impl.BoolSetting;
import me.kiriyaga.essentials.setting.impl.DoubleSetting;
import me.kiriyaga.essentials.setting.impl.IntSetting;
import me.kiriyaga.essentials.util.render.RenderUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.*;

import java.awt.*;

import static me.kiriyaga.essentials.Essentials.*;

public class AuraModule extends Module {

    public final DoubleSetting rotateRange = addSetting(new DoubleSetting("rotate", 4.6, 1.0, 10.0));
    public final DoubleSetting attackRange = addSetting(new DoubleSetting("attack", 4.0, 1.0, 6.0));
    public final BoolSetting swordOnly = addSetting(new BoolSetting("weap only", false));
    public final BoolSetting render = addSetting(new BoolSetting("render", true));
    public final BoolSetting tpsSync = addSetting(new BoolSetting("tps sync", true));
    public final BoolSetting multiTask = addSetting(new BoolSetting("multitask", false));
    private final IntSetting rotationPriority = addSetting(new IntSetting("rotation", 5, 1, 30));
    public final DoubleSetting preRotate = addSetting(new DoubleSetting("pre rotate", 0.2, 0.0, 1.0));

    private Entity currentTarget = null;

    public AuraModule() {
        super("aura", "Attack entities for you.", Category.COMBAT, "killaura", "ara", "killara", "фгкф");
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onTick(PreTickEvent event) {
        if (MINECRAFT.player == null || MINECRAFT.world == null) return;
        if (!multiTask.get() && MINECRAFT.player.isUsingItem()) return;

        if (swordOnly.get()) {
            ItemStack stack = MINECRAFT.player.getMainHandStack();
            if (!(stack.getItem() instanceof AxeItem || stack.isIn(ItemTags.SWORDS))) return;
        }

        Entity target = ENTITY_MANAGER.getTarget();
        if (target == null) {
            currentTarget = null;
            return;
        }

        double eyeDistance = getClosestEyeDistance(MINECRAFT.player.getEyePos(), target.getBoundingBox());

        boolean inRotateRange = eyeDistance <= rotateRange.get();
        boolean inAttackRange = eyeDistance <= attackRange.get();

        if (!inRotateRange) {
            currentTarget = null;
            return;
        }

        currentTarget = target;

        float cooldown = MINECRAFT.player.getAttackCooldownProgress(0f);
        float tps = 20f;
        if (tpsSync.get() && MINECRAFT.getServer() != null) {
            double tickTimeMs = MINECRAFT.getServer().getAverageTickTime() / 1_000_000.0;
            tps = (float) Math.min(20.0, 1000.0 / tickTimeMs);
        }

        float ticksUntilReady = (1.0f - cooldown) * tps;
        if (ticksUntilReady <= preRotate.get() * tps) {
            boolean alreadyRequested = ROTATION_MANAGER.hasRequest(AuraModule.class.getName());

            if (!alreadyRequested) {
                ROTATION_MANAGER.submitRequest(new RotationManager.RotationRequest(
                        AuraModule.class.getName(),
                        rotationPriority.get(),
                        () -> {
                            Vec3d lookPos = getClosestPointToEye(MINECRAFT.player.getEyePos(), target.getBoundingBox());
                            return (float) getYawToVec(MINECRAFT.player, lookPos);
                        },
                        () -> {
                            Vec3d lookPos = getClosestPointToEye(MINECRAFT.player.getEyePos(), target.getBoundingBox());
                            return (float) getPitchToVec(MINECRAFT.player, lookPos);
                        }
                ));
            }
        }


        if (!inAttackRange) return;
        if (cooldown < (tpsSync.get() ? 1.0f * (20f / tps) : 1.0f)) return;
        if (!ROTATION_MANAGER.isRequestCompleted(AuraModule.class.getName())) return;

        MINECRAFT.interactionManager.attackEntity(MINECRAFT.player, target);
        MINECRAFT.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender3D(Render3DEvent event) {
        if (!render.get() || currentTarget == null) return;

        double eyeDistance = getClosestEyeDistance(MINECRAFT.player.getEyePos(), currentTarget.getBoundingBox());
        if (eyeDistance > rotateRange.get()) return;

        ColorModule colorModule = MODULE_MANAGER.getModule(ColorModule.class);
        drawBox(currentTarget, colorModule.getStyledPrimaryColor(), event.getMatrices(), event.getTickDelta());
    }

    private void drawBox(Entity entity, Color color, MatrixStack matrices, float partialTicks) {
        double interpX = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * partialTicks;
        double interpY = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * partialTicks;
        double interpZ = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * partialTicks;
        Box box = entity.getBoundingBox().offset(interpX - entity.getX(), interpY - entity.getY(), interpZ - entity.getZ());
        RenderUtil.drawBoxFilled(matrices, box, new Color(color.getRed(), color.getGreen(), color.getBlue(), 75));
    }

    private static int getYawToVec(Entity from, Vec3d to) {
        double dx = to.x - from.getX();
        double dz = to.z - from.getZ();
        return wrapDegrees((int) Math.round(Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
    }

    private static int getPitchToVec(Entity from, Vec3d to) {
        Vec3d eyePos = from.getEyePos();
        double dx = to.x - eyePos.x;
        double dy = to.y - eyePos.y;
        double dz = to.z - eyePos.z;
        return (int) Math.round(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    private static int wrapDegrees(int angle) {
        angle %= 360;
        if (angle >= 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    private static Vec3d getClosestPointToEye(Vec3d eyePos, Box box) {
        double x = MathHelper.clamp(eyePos.x, box.minX, box.maxX);
        double y = MathHelper.clamp(eyePos.y, box.minY, box.maxY);
        double z = MathHelper.clamp(eyePos.z, box.minZ, box.maxZ);
        return new Vec3d(x, y, z);
    }

    private static double getClosestEyeDistance(Vec3d eyePos, Box box) {
        Vec3d closest = getClosestPointToEye(eyePos, box);
        return eyePos.distanceTo(closest);
    }
}
