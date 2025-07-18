package me.kiriyaga.nami.feature.command.impl;

import me.kiriyaga.nami.feature.command.Command;
import me.kiriyaga.nami.feature.command.RegisterCommand;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;

import static me.kiriyaga.nami.Nami.MC;

@RegisterCommand
public class DisconnectCommand extends Command {

    public DisconnectCommand() {
        super("disconnect", "Disconnects you from the server. Usage: Disconnect", "dis", "discnect", "dissconnect", "logout", "вшысщттусе");
    }

    @Override
    public void execute(String[] args) {
        if (MC.player != null && MC.getNetworkHandler() != null) {
            MC.getNetworkHandler().onDisconnect(new DisconnectS2CPacket(Text.empty()));
        }
    }
}
