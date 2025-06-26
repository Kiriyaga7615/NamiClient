package me.kiriyaga.essentials.feature.module.impl.world;

import me.kiriyaga.essentials.event.EventPriority;
import me.kiriyaga.essentials.event.SubscribeEvent;
import me.kiriyaga.essentials.event.impl.PacketReceiveEvent;
import me.kiriyaga.essentials.feature.module.Category;
import me.kiriyaga.essentials.feature.module.Module;

import me.kiriyaga.essentials.setting.impl.BoolSetting;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static me.kiriyaga.essentials.Essentials.*;

public class JoinAnnounceModule extends Module {

    public final BoolSetting everyone = addSetting(new BoolSetting("everyone", false));
    public final BoolSetting friends = addSetting(new BoolSetting("friends", true));

    public JoinAnnounceModule() {
        super("join announce", "Announces in chat when a player joins the server.", Category.WORLD, "joinannounce", "joins", "announce", "ощштфттщгтсу");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled()) return;

        if (event.getPacket() instanceof PlayerListS2CPacket joinPacket) {
            if (joinPacket.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                for (var entry : joinPacket.getEntries()) {
                    String playerName = entry.profile().getName();
                    if (playerName == null) continue;

                    boolean isFriend = FRIEND_MANAGER.isFriend(playerName);

                    if ((everyone.get() && !isFriend) || (friends.get() && isFriend)) {
                        String message = "§a[+] §7" + playerName;
                        CHAT_MANAGER.sendPersistent(playerName, message);
                    }
                }
            }
        } else if (event.getPacket() instanceof PlayerRemoveS2CPacket leavePacket) {
            for (var playerInfo : leavePacket.profileIds()) {
                var info = MINECRAFT.getNetworkHandler().getPlayerListEntry(playerInfo);
                if (info == null) continue;

                String playerName = info.getProfile().getName();
                if (playerName == null) continue;

                boolean isFriend = FRIEND_MANAGER.isFriend(playerName);

                if ((everyone.get() && !isFriend) || (friends.get() && isFriend)) {
                    String message = "§c[-] §7" + playerName;
                    CHAT_MANAGER.sendPersistent(playerName, message);
                }
            }
        }
    }
}
