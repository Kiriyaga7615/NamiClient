package me.kiriyaga.essentials.manager;

import me.kiriyaga.essentials.feature.command.Command;
import me.kiriyaga.essentials.event.SubscribeEvent;
import me.kiriyaga.essentials.event.impl.ChatMessageEvent;
import me.kiriyaga.essentials.feature.command.impl.*;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.text.PersonNameFormatter.Length;

import static me.kiriyaga.essentials.Essentials.*;

public class CommandManager {

    private final List<Command> commands = new ArrayList<>();
    private String prefix = ".";

    public void init() {
        EVENT_MANAGER.register(this);

        registerCommand(new HelpCommand());
        registerCommand(new ChangePrefixCommand());
        registerCommand(new NameCommand());
        registerCommand(new FovCommand());
        registerCommand(new GammaCommand());
        registerCommand(new DisconnectCommand());
        registerCommand(new ToggleCommand());
        registerCommand(new BindCommand());
        registerCommand(new SaveCommand());
        registerCommand(new LoadCommand());
        registerCommand(new YawCommand());
        registerCommand(new PitchCommand());
        registerCommand(new FriendCommand());


        LOGGER.info("Registered " + commands.size() + " commands");
    }

    public void registerCommand(Command command) {
        commands.add(command);
    }

    public void unregisterCommand(Command command) {
        commands.remove(command);
    }

    public List<Command> getCommands() {
        return commands;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        if (prefix != null && !prefix.isEmpty()) {
            this.prefix = prefix;
            LOGGER.info("Prefix changed to: " + prefix);
        } else {
            LOGGER.warn("Attempted to set empty or null prefix.");
        }
    }

    public List<String> getSuggestions(String input) {
        String[] parts = input.split("\\s+");
        String cmdName = parts.length > 0 ? parts[0] : "";

        String exactMatch = commands.stream()
            .filter(cmd -> (prefix + cmd.getName()).equals(cmdName))
            .map(cmd -> prefix + cmd.getName())
            .findFirst()
            .orElse("");

        if (exactMatch == cmdName) {
            return List.of(exactMatch);
        } else  if (cmdName.startsWith(prefix) && input.length() == 1) {
            return commands.stream()
                .map(cmd -> prefix + cmd.getName())
                .toList();
        } else {
            return commands.stream()
                .map(cmd -> prefix + cmd.getName())
                .filter(s -> s.startsWith(cmdName))
                .toList();
        }
    }


    @SubscribeEvent
    public void onChatMessage(ChatMessageEvent event) {
        String message = event.getMessage();

        if (!message.startsWith(prefix)) return;

        event.setCancelled(true);

        String[] parts = message.split("\\s+");
        String cmdName = parts.length > 0 ? parts[0] : "";
        String[] args = new String[0];
        if (parts.length > 1) {
            args = new String[parts.length - 1];
            System.arraycopy(parts, 1, args, 0, args.length);
        }

        for (Command cmd : commands) {
            if ((prefix + cmd.getName()).matches(cmdName)) {
                try {
                    cmd.execute(args);
                } catch (Exception e) {
                    LOGGER.error("Error executing command " + cmdName, e);
                }
                return;
            }
        }
    }
}
