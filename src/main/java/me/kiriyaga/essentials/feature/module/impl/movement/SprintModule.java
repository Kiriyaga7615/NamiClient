package me.kiriyaga.essentials.feature.module.impl.movement;

import me.kiriyaga.essentials.event.EventPriority;
import me.kiriyaga.essentials.event.SubscribeEvent;
import me.kiriyaga.essentials.event.impl.PacketSendEvent;
import me.kiriyaga.essentials.event.impl.PreTickEvent;
import me.kiriyaga.essentials.feature.module.Category;
import me.kiriyaga.essentials.feature.module.Module;
import me.kiriyaga.essentials.mixin.PlayerInteractEntityC2SPacketAccessor;
import me.kiriyaga.essentials.setting.impl.BoolSetting;
import me.kiriyaga.essentials.setting.impl.EnumSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

import static me.kiriyaga.essentials.Essentials.MINECRAFT;

public class SprintModule extends Module {

    private final BoolSetting keepSprint = addSetting(new BoolSetting("keep sprint", true));

    public SprintModule() {
        super("sprint", "Automatically makes you sprint while moving forward.", Category.MOVEMENT);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onUpdateEvent(PreTickEvent event) {
        if (!isEnabled()) return;

        ClientPlayerEntity player = MINECRAFT.player;
        if (player == null || player.isSubmergedInWater() || player.isTouchingWater()) return;

                if (player.forwardSpeed > 0 && !player.hasVehicle()) {
                    player.setSprinting(true);
                } else {
                    player.setSprinting(false);
                }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPacketSend(PacketSendEvent event) {
        if (!isEnabled()) return;
        if (!(event.getPacket() instanceof PlayerInteractEntityC2SPacket packet)) return;

        PlayerInteractEntityC2SPacket.InteractTypeHandler handler = ((PlayerInteractEntityC2SPacketAccessor) packet).getTypeHandler();
        PlayerInteractEntityC2SPacket.InteractType type = handler.getType();

        if (type != PlayerInteractEntityC2SPacket.InteractType.ATTACK) return;


        if (!keepSprint.get()) {
            MINECRAFT.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(MINECRAFT.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            MINECRAFT.player.setSprinting(false);
        }
    }
}
