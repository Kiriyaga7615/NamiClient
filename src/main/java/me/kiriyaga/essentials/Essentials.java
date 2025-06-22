package me.kiriyaga.essentials;

import me.kiriyaga.essentials.feature.gui.ClickGuiScreen;
import me.kiriyaga.essentials.manager.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Essentials implements ClientModInitializer {
    public static String NAME = "2bEssentials";
    public static final String VERSION = "420.17";

    public static final MinecraftClient MINECRAFT = MinecraftClient.getInstance();

    public static final EventManager EVENT_MANAGER = new EventManager();
    public static final CommandManager COMMAND_MANAGER = new CommandManager();
    public static final ChatManager CHAT_MANAGER = new ChatManager();
    public static final ModuleManager MODULE_MANAGER = new ModuleManager();
    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();
    public static final FriendManager FRIEND_MANAGER = new FriendManager();
    public static final PingManager PING_MANAGER = new PingManager();
    public static final RotationManager ROTATION_MANAGER = new RotationManager();
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public static ClickGuiScreen CLICK_GUI = new ClickGuiScreen();



    @Override
    public void onInitializeClient() {
        COMMAND_MANAGER.init();
        MODULE_MANAGER.init();
        PING_MANAGER.init();
        ROTATION_MANAGER.init();

        CONFIG_MANAGER.load();
        FRIEND_MANAGER.load();

        LOGGER.info(NAME + " " + VERSION + " has been initialized");

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CONFIG_MANAGER.save();
        });

    }
}
