package me.kiriyaga.essentials.mixin;

import me.kiriyaga.essentials.Essentials;
import me.kiriyaga.essentials.event.impl.PostTickEvent;
import me.kiriyaga.essentials.event.impl.PreTickEvent;
import me.kiriyaga.essentials.feature.module.impl.render.FreecamModule;
import me.kiriyaga.essentials.manager.RotationManager;
import me.kiriyaga.essentials.mixininterface.IClientPlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.kiriyaga.essentials.Essentials.*;

@Mixin(ClientPlayerEntity.class)
@Implements(@Interface(iface = IClientPlayerEntity.class, prefix = "accessor$"))
public abstract class MixinClientPlayerEntity implements IClientPlayerEntity {

    private boolean packetSentThisTick = false;
    private float lastSentRotYaw = Float.NaN;
    private float lastSentRotPitch = Float.NaN;

    @Shadow private double lastXClient;
    @Shadow private double lastYClient;
    @Shadow private double lastZClient;
    @Shadow private float lastYawClient;
    @Shadow private float lastPitchClient;
    @Shadow private int ticksSinceLastPositionPacketSent;
    @Shadow private boolean lastOnGround;
    @Shadow private boolean lastHorizontalCollision;

    @Shadow protected MinecraftClient client;

    @Shadow protected abstract boolean isCamera();

    @Shadow private void sendSprintingPacket() {}
    public double accessor$getLastXClient() { return lastXClient; }
    public void accessor$setLastXClient(double val) { this.lastXClient = val; }
    public double accessor$getLastYClient() { return lastYClient; }
    public void accessor$setLastYClient(double val) { this.lastYClient = val; }
    public double accessor$getLastZClient() { return lastZClient; }
    public void accessor$setLastZClient(double val) { this.lastZClient = val; }
    public float accessor$getLastYawClient() { return lastYawClient; }
    public void accessor$setLastYawClient(float val) { this.lastYawClient = val; }
    public float accessor$getLastPitchClient() { return lastPitchClient; }
    public void accessor$setLastPitchClient(float val) { this.lastPitchClient = val; }
    public int accessor$getTicksSinceLastPositionPacketSent() { return ticksSinceLastPositionPacketSent; }
    public void accessor$setTicksSinceLastPositionPacketSent(int val) { this.ticksSinceLastPositionPacketSent = val; }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void beforeSendMovementPackets(CallbackInfo ci) {
        packetSentThisTick = false;
    }

    @Inject(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V", ordinal = 0))
    private void onPacketSend1(CallbackInfo ci) {
        packetSentThisTick = true;
    }

    @Inject(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V", ordinal = 1))
    private void onPacketSend2(CallbackInfo ci) {
        packetSentThisTick = true;
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void injectSendMovementPackets(CallbackInfo ci) {
        if (!isCamera()) return;

        RotationManager rot = ROTATION_MANAGER;

        if (packetSentThisTick || !rot.isRotating()) return;

        float rotYaw = rot.getRotationYaw();
        float rotPitch = rot.getRotationPitch();

        boolean rotationChanged = rotYaw != lastSentRotYaw || rotPitch != lastSentRotPitch;

        if (rotationChanged) {
            this.client.getNetworkHandler().sendPacket(
                    new PlayerMoveC2SPacket.LookAndOnGround(rotYaw, rotPitch, this.lastOnGround, this.lastHorizontalCollision)
            );

            lastSentRotYaw = rotYaw;
            lastSentRotPitch = rotPitch;

            setTicksSinceLastPositionPacketSent(20);

            packetSentThisTick = true;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickHookPre(CallbackInfo ci) {

        EVENT_MANAGER.post(new PreTickEvent());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickHookPost(CallbackInfo ci) {

        EVENT_MANAGER.post(new PostTickEvent());
    }
}
