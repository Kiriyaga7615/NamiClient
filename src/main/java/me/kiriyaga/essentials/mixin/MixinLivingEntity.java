package me.kiriyaga.essentials.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.kiriyaga.essentials.feature.module.impl.client.RotationManagerModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.kiriyaga.essentials.Essentials.MODULE_MANAGER;
import static me.kiriyaga.essentials.Essentials.ROTATION_MANAGER;


@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {

    private float originalYaw, originalBodyYaw, originalHeadYaw;

    public MixinLivingEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void travelPreHook(Vec3d movementInput, CallbackInfo ci) {
        if ((Object)this != MinecraftClient.getInstance().player || !ROTATION_MANAGER.isRotating() || !MODULE_MANAGER.getModule(RotationManagerModule.class).moveFix.get()) return;

        originalYaw = this.getYaw();
        originalBodyYaw = ((LivingEntityAccessor) this).getBodyYaw();
        originalHeadYaw = ((LivingEntityAccessor) this).getHeadYaw();

        float spoofYaw = ROTATION_MANAGER.lastSentSpoofYaw;

        this.setYaw(spoofYaw);
        ((LivingEntityAccessor) this).setBodyYaw(spoofYaw);
        ((LivingEntityAccessor) this).setHeadYaw(spoofYaw);
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void travelPostHook(Vec3d movementInput, CallbackInfo ci) {
        if ((Object)this != MinecraftClient.getInstance().player || !ROTATION_MANAGER.isRotating() || !MODULE_MANAGER.getModule(RotationManagerModule.class).moveFix.get()) return;

        this.setYaw(originalYaw);
        ((LivingEntityAccessor) this).setBodyYaw(originalBodyYaw);
        ((LivingEntityAccessor) this).setHeadYaw(originalHeadYaw);
    }

    @ModifyVariable(method = "travel", at = @At("HEAD"), ordinal = 0)
    private Vec3d modifyMovementInput(Vec3d movementInput) {
        if ((Object)this != MinecraftClient.getInstance().player || !ROTATION_MANAGER.isRotating() || !MODULE_MANAGER.getModule(RotationManagerModule.class).moveFix.get()) return movementInput;
        if (movementInput.lengthSquared() < 1e-4) return movementInput;

        float realYaw = originalYaw;
        float spoofYaw = ROTATION_MANAGER.lastSentSpoofYaw;

        float clampedSpoofYaw = findClosestValidYaw(spoofYaw);

        Vec3d globalMovement = localToGlobal(movementInput, realYaw);

        Vec3d clampedLocalMovement = globalToLocal(globalMovement, clampedSpoofYaw);

        return clampedLocalMovement;
    }

    private float findClosestValidYaw(float yaw) {
        float[] allowedYawAngles = new float[]{0, 45, 90, 135, 180, 225, 270, 315};
        float bestYaw = allowedYawAngles[0];
        float minDiff = Float.MAX_VALUE;
        for (float allowedYaw : allowedYawAngles) {
            float diff = Math.abs(((allowedYaw - yaw + 540f) % 360f) - 180f);
            if (diff < minDiff) {
                minDiff = diff;
                bestYaw = allowedYaw;
            }
        }
        return bestYaw;
    }

    private Vec3d localToGlobal(Vec3d localVec, float yaw) {
        double rad = Math.toRadians(yaw);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double x = localVec.x * cos - localVec.z * sin;
        double z = localVec.z * cos + localVec.x * sin;

        return new Vec3d(x, localVec.y, z);
    }

    private Vec3d globalToLocal(Vec3d globalVec, float yaw) {
        double rad = Math.toRadians(yaw);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double x = globalVec.x * cos + globalVec.z * sin;
        double z = globalVec.z * cos - globalVec.x * sin;

        return new Vec3d(x, globalVec.y, z);
    }

    @ModifyExpressionValue(
            method = "jump",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"
            )
    )
    private float jumpFix(float originalYaw) {
        if ((Object)this != MinecraftClient.getInstance().player) return originalYaw;
        return ROTATION_MANAGER.getRotationYaw();
    }
}
