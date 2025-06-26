package me.kiriyaga.essentials.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.kiriyaga.essentials.feature.module.impl.render.FreecamModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.debug.ChunkBorderDebugRenderer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import static me.kiriyaga.essentials.Essentials.MODULE_MANAGER;

@Mixin(ChunkBorderDebugRenderer.class)
public abstract class MixinChunkBorderDebugRenderer {
    @Shadow
    @Final
    private MinecraftClient client;

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getChunkPos()Lnet/minecraft/util/math/ChunkPos;"))
    private ChunkPos render$getChunkPos(ChunkPos chunkPos) {
        FreecamModule freecamModule = MODULE_MANAGER.getModule(FreecamModule.class);

        if (!freecamModule.isEnabled()) return chunkPos;

        float delta = client.getRenderTickCounter().getTickDelta(true);

        double interpolatedX = freecamModule.prevPos.x + (freecamModule.pos.x - freecamModule.prevPos.x) * delta;
        double interpolatedZ = freecamModule.prevPos.z + (freecamModule.pos.z - freecamModule.prevPos.z) * delta;

        return new ChunkPos(
                ChunkSectionPos.getSectionCoord(MathHelper.floor(interpolatedX)),
                ChunkSectionPos.getSectionCoord(MathHelper.floor(interpolatedZ))
        );
    }
}
