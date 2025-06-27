package me.kiriyaga.essentials.mixin;

import me.kiriyaga.essentials.feature.module.impl.render.FreecamModule;
import me.kiriyaga.essentials.feature.module.impl.render.NoRenderModule;
import me.kiriyaga.essentials.mixininterface.IVec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.kiriyaga.essentials.Essentials.MODULE_MANAGER;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract void updateCrosshairTarget(float tickDelta);

    @Shadow
    public abstract void reset();

    @Shadow
    @Final
    private Camera camera;

    @Unique
    private final MatrixStack matrices = new MatrixStack();

    @Shadow
    protected abstract void bobView(MatrixStack matrices, float tickDelta);

    @Shadow
    protected abstract void tiltViewWhenHurt(MatrixStack matrices, float tickDelta);


    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        NoRenderModule noRender = MODULE_MANAGER.getModule(NoRenderModule.class);
        if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING && noRender.isEnabled() && noRender.isNoTotem()) {
            info.cancel();
        }
    }

    @Unique
    private boolean freecamSet = false;

    @Inject(method = "updateCrosshairTarget", at = @At("HEAD"), cancellable = true)
    private void updateTargetedEntityInvoke(float tickDelta, CallbackInfo info) {
        FreecamModule freecamModule = MODULE_MANAGER.getModule(FreecamModule.class);

        if (freecamModule.isEnabled() && client.getCameraEntity() != null && !freecamSet) {
            info.cancel();

            Entity cameraE = client.getCameraEntity();

            double x = cameraE.getX();
            double y = cameraE.getY();
            double z = cameraE.getZ();
            double lastX = cameraE.prevX;
            double lastY = cameraE.prevY;
            double lastZ = cameraE.prevZ;
            float yaw = cameraE.getYaw();
            float pitch = cameraE.getPitch();
            float lastYaw = cameraE.prevYaw;
            float lastPitch = cameraE.prevPitch;

            cameraE.setPos(freecamModule.getX(), freecamModule.getY() - cameraE.getEyeHeight(cameraE.getPose()), freecamModule.getZ());

            cameraE.prevX = freecamModule.prevPos.x;
            cameraE.prevY = freecamModule.prevPos.y - cameraE.getEyeHeight(cameraE.getPose());
            cameraE.prevZ = freecamModule.prevPos.z;

            cameraE.setYaw(freecamModule.yaw);
            cameraE.setPitch(freecamModule.pitch);
            cameraE.prevYaw = freecamModule.lastYaw;
            cameraE.prevPitch = freecamModule.lastPitch;

            freecamSet = true;
            updateCrosshairTarget(tickDelta);
            freecamSet = false;

            cameraE.setPos(x, y, z);
            cameraE.prevX = lastX;
            cameraE.prevY = lastY;
            cameraE.prevZ = lastZ;
            cameraE.setYaw(yaw);
            cameraE.setPitch(pitch);
            cameraE.prevYaw = lastYaw;
            cameraE.prevPitch = lastPitch;
        }
    }

}