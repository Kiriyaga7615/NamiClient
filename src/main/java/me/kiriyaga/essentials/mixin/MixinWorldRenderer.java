package me.kiriyaga.essentials.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import me.kiriyaga.essentials.event.impl.Render2DEvent;
import me.kiriyaga.essentials.event.impl.Render3DEvent;
import me.kiriyaga.essentials.feature.module.impl.render.FreecamModule;
import me.kiriyaga.essentials.feature.module.impl.render.NoWeatherModule;
import me.kiriyaga.essentials.util.MatrixCache;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

import static me.kiriyaga.essentials.Essentials.*;

@Mixin(WorldRenderer.class)

public abstract class MixinWorldRenderer {

    @Shadow @Final private Set<BlockEntity> noCullingBlockEntities;

    @Shadow protected abstract void translucencySort(Vec3d cameraPos);

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderTail(
            ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        float tickDelta = tickCounter.getTickDelta(true);

        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

        EVENT_MANAGER.post(new Render3DEvent(matrices, tickDelta, camera, positionMatrix, projectionMatrix));

        matrices.pop();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void captureMatrices(
            ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        MatrixCache.positionMatrix = new Matrix4f(positionMatrix);
        MatrixCache.projectionMatrix = new Matrix4f(projectionMatrix);
        MatrixCache.camera = camera;
    }

    @WrapWithCondition(method = "method_62216", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WeatherRendering;renderPrecipitation(Lnet/minecraft/world/World;Lnet/minecraft/client/render/VertexConsumerProvider;IFLnet/minecraft/util/math/Vec3d;)V"))
    private boolean shouldRenderPrecipitation(WeatherRendering instance, World world, VertexConsumerProvider vertexConsumers, int ticks, float tickProgress, Vec3d pos) {
        NoWeatherModule noWeatherModule = MODULE_MANAGER.getModule(NoWeatherModule.class);
        return !noWeatherModule.isEnabled();
    }


    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"), index = 3)
    private boolean renderSetupTerrainModifyArg(boolean spectator) {
        FreecamModule freecamModule = MODULE_MANAGER.getModule(FreecamModule.class);
        return freecamModule.isEnabled() || spectator;
    }
}
