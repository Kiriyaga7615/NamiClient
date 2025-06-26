package me.kiriyaga.essentials.manager;

import me.kiriyaga.essentials.event.EventPriority;
import me.kiriyaga.essentials.event.SubscribeEvent;
import me.kiriyaga.essentials.event.impl.PacketSendEvent;
import me.kiriyaga.essentials.event.impl.PreTickEvent;
import me.kiriyaga.essentials.feature.module.impl.client.RotationManagerModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.function.Supplier;

import static me.kiriyaga.essentials.Essentials.*;

public class RotationManager {
    private final List<RotationRequest> requests = new ArrayList<>();
    private RotationRequest activeRequest = null;

    private float realYaw, realPitch;
    private float rotationYaw, rotationPitch;
    private float currentYawSpeed = 0f;
    private float currentPitchSpeed = 0f;

    private float rotationSpeed;
    private float rotationEaseFactor;
    private float rotationThreshold;
    private int ticksBeforeRelease;
    private float jitterAmount;
    private float jitterSpeed;

    private int tickCount = 0;
    private boolean returning = false;
    private int ticksHolding = 0;

    public float lastSentSpoofYaw;
    public float lastSentSpoofPitch;

    public float lastSentSpoofYaw2;
    public float lastSentSpoofPitch2;

    private boolean spoofing = false;

    public void init() {
        EVENT_MANAGER.register(this);
    }

    public void updateRealRotation(float yaw, float pitch) {
        realYaw = wrapDegrees(yaw);
        realPitch = MathHelper.clamp(pitch, -90f, 90f);
    }

    public void submitRequest(RotationRequest request) {
        boolean wasActive = activeRequest != null && Objects.equals(activeRequest.id, request.id);

        requests.removeIf(r -> Objects.equals(r.id, request.id));
        requests.add(request);
        requests.sort(Comparator.comparingInt(r -> -r.priority));

        if (wasActive) {
            activeRequest = request;
        }
    }


    public boolean isRequestCompleted(String id) {
        RotationRequest request = null;

        if (activeRequest != null && activeRequest.id.equals(id)) {
            request = activeRequest;
        } else {
            for (RotationRequest r : requests) {
                if (r.id.equals(id)) {
                    request = r;
                    break;
                }
            }
        }

        if (request == null) {
            return false;
        }

        float yawDiff = wrapDegrees(request.targetYaw - lastSentSpoofYaw);
        float pitchDiff = request.targetPitch - lastSentSpoofPitch;

        return Math.abs(yawDiff) <= rotationThreshold && Math.abs(pitchDiff) <= rotationThreshold;
    }


    public void cancelRequest(String id) {
        requests.removeIf(r -> Objects.equals(r.id, id));
    }

    public boolean isRotating() {
        return activeRequest != null || returning;
    }


    public float getRotationYaw() {
        return rotationYaw;
    }

    public float getRotationPitch() {
        return rotationPitch;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreTick(PreTickEvent event) {
        if (MINECRAFT.player == null) return;

        RotationManagerModule rotationModule = MODULE_MANAGER.getModule(RotationManagerModule.class);
        rotationSpeed = rotationModule.rotationSpeed.get().floatValue();
        rotationEaseFactor = rotationModule.rotationEaseFactor.get().floatValue();
        rotationThreshold = rotationModule.rotationThreshold.get().floatValue();
        ticksBeforeRelease = rotationModule.ticksBeforeRelease.get();
        jitterAmount = rotationModule.jitterAmount.get().floatValue();
        jitterSpeed = rotationModule.jitterSpeed.get().floatValue();

        updateRealRotation(MINECRAFT.player.getYaw(), MINECRAFT.player.getPitch());

        if (!requests.isEmpty()) {
            if (activeRequest == null || !requests.contains(activeRequest)) {
                activeRequest = requests.get(0);
                rotationYaw = realYaw;
                rotationPitch = realPitch;
                ticksHolding = 0;
                currentYawSpeed = 0f;
                currentPitchSpeed = 0f;
                returning = false;
            }

            boolean updated = false;
            if (activeRequest.shouldUpdate()) {
                float oldYaw = activeRequest.targetYaw;
                float oldPitch = activeRequest.targetPitch;
                activeRequest.updateTarget();
                if (Math.abs(wrapDegrees(oldYaw - activeRequest.targetYaw)) > 0.001f ||
                        Math.abs(oldPitch - activeRequest.targetPitch) > 0.001f) {
                    ticksHolding = 0;
                    updated = true;
                }
            }

            float yawDiff = wrapDegrees(activeRequest.targetYaw - rotationYaw);
            float pitchDiff = activeRequest.targetPitch - rotationPitch;

            boolean reached = Math.abs(yawDiff) <= rotationThreshold && Math.abs(pitchDiff) <= rotationThreshold;

            if (reached && !updated) {
                ticksHolding++;
                if (ticksHolding >= ticksBeforeRelease) {
                    requests.remove(activeRequest);
                    activeRequest = null;
                    ticksHolding = 0;
                    returning = true;
                }
            } else {
                ticksHolding = 0;

                currentYawSpeed = lerp(currentYawSpeed, yawDiff, rotationEaseFactor);
                currentPitchSpeed = lerp(currentPitchSpeed, pitchDiff, rotationEaseFactor);

                float clampedYawSpeed = MathHelper.clamp(currentYawSpeed, -rotationSpeed, rotationSpeed);
                float clampedPitchSpeed = MathHelper.clamp(currentPitchSpeed, -rotationSpeed, rotationSpeed);

                rotationYaw = wrapDegrees(rotationYaw + clampedYawSpeed);
                rotationPitch = MathHelper.clamp(rotationPitch + clampedPitchSpeed, -90f, 90f);
            }
        } else if (returning) {
            float yawDiff = wrapDegrees(realYaw - rotationYaw);
            float pitchDiff = realPitch - rotationPitch;

            currentYawSpeed = lerp(currentYawSpeed, yawDiff, rotationEaseFactor);
            currentPitchSpeed = lerp(currentPitchSpeed, pitchDiff, rotationEaseFactor);

            float clampedYawSpeed = MathHelper.clamp(currentYawSpeed, -rotationSpeed, rotationSpeed);
            float clampedPitchSpeed = MathHelper.clamp(currentPitchSpeed, -rotationSpeed, rotationSpeed);

            if (Math.abs(yawDiff) <= rotationThreshold) {
                rotationYaw = realYaw;
                currentYawSpeed = 0f;
            } else {
                rotationYaw = wrapDegrees(rotationYaw + clampedYawSpeed);
            }

            if (Math.abs(pitchDiff) <= rotationThreshold) {
                rotationPitch = realPitch;
                currentPitchSpeed = 0f;
            } else {
                rotationPitch = MathHelper.clamp(rotationPitch + clampedPitchSpeed, -90f, 90f);
            }

            boolean backReached = Math.abs(yawDiff) <= rotationThreshold && Math.abs(pitchDiff) <= rotationThreshold;

            if (backReached) {
                returning = false;
                rotationYaw = realYaw;
                rotationPitch = realPitch;
            }
        } else {
            rotationYaw = realYaw;
            rotationPitch = realPitch;
            currentYawSpeed = 0f;
            currentPitchSpeed = 0f;
        }

        if (isRotating()) {
            float jitterYawOffset = jitterAmount * (float) Math.sin(tickCount * jitterSpeed);
            float jitterPitchOffset = jitterYawOffset / 5f;

            rotationYaw = wrapDegrees(rotationYaw + jitterYawOffset);
            rotationPitch = MathHelper.clamp(rotationPitch + jitterPitchOffset, -90f, 90f);
        }

        // this should be here since if we standing still and not moving it will cause not sending packets at all
        if (isRotating()) {
            float spoofYaw = getRotationYaw();
            float spoofPitch = getRotationPitch();

            float yawDiff = Math.abs(wrapDegrees(spoofYaw - lastSentSpoofYaw2));
            float pitchDiff = Math.abs(spoofPitch - lastSentSpoofPitch2);

            if (yawDiff > 0.001f || pitchDiff > 0.001f) {
                lastSentSpoofYaw2 = spoofYaw;
                lastSentSpoofPitch2 = spoofPitch;

                Vec3d pos = MINECRAFT.player.getPos();

                MINECRAFT.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                        pos.x,
                        pos.y,
                        pos.z,
                        spoofYaw,
                        spoofPitch,
                        MINECRAFT.player.isOnGround(),
                        false
                ));

                MINECRAFT.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                        spoofYaw,
                        spoofPitch,
                        MINECRAFT.player.isOnGround(),
                        false
                ));
            }
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPacketSend(PacketSendEvent event) {
        if (spoofing) return;
        Packet<?> packet = event.getPacket();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (!isRotating()) return;

        float spoofYaw = getRotationYaw();
        float spoofPitch = getRotationPitch();

        lastSentSpoofYaw = spoofYaw;
        lastSentSpoofPitch = spoofPitch;

        Vec3d pos = mc.player.getPos();

        if (!(packet instanceof PlayerMoveC2SPacket)) return;

        spoofing = true;

        try {
            if (packet instanceof PlayerMoveC2SPacket.Full full) {
                event.cancel();
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                        full.getX(pos.x),
                        full.getY(pos.y),
                        full.getZ(pos.z),
                        spoofYaw,
                        spoofPitch,
                        full.isOnGround(),
                        full.horizontalCollision()
                ));

            }else if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround look) {
                event.cancel();
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                        spoofYaw,
                        spoofPitch,
                        look.isOnGround(),
                        look.horizontalCollision()
                ));
            }
        } finally {
            if (isRotating()){
                mc.player.setBodyYaw(spoofYaw);
                mc.player.setHeadYaw(spoofYaw);
            }

            spoofing = false; //  ebal rot rekursii
            // CHAT_MANAGER.sendRaw(String.format("Visual Look: yaw=%.2f pitch=%.2f (Spoofing active)", spoofYaw, spoofPitch));
        }
    }

    private float wrapDegrees(float angle) {
        angle %= 360f;
        if (angle >= 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }

    private float lerp(float from, float to, float factor) {
        return from + (to - from) * factor;
    }

    public static class RotationRequest {
        public final String id;
        public final int priority;
        private final boolean dynamic;
        private final Supplier<Float> yawSupplier;
        private final Supplier<Float> pitchSupplier;

        public float targetYaw;
        public float targetPitch;

        public RotationRequest(String id, int priority, float yaw, float pitch) {
            this.id = id;
            this.priority = priority;
            this.dynamic = false;
            this.targetYaw = yaw;
            this.targetPitch = pitch;
            this.yawSupplier = null;
            this.pitchSupplier = null;
        }

        public RotationRequest(String id, int priority, Supplier<Float> yawSupplier, Supplier<Float> pitchSupplier) {
            this.id = id;
            this.priority = priority;
            this.dynamic = true;
            this.yawSupplier = yawSupplier;
            this.pitchSupplier = pitchSupplier;
            updateTarget();
        }

        public boolean shouldUpdate() {
            return dynamic;
        }

        public void updateTarget() {
            if (dynamic && yawSupplier != null && pitchSupplier != null) {
                targetYaw = yawSupplier.get();
                targetPitch = pitchSupplier.get();
            }
        }
    }
}
