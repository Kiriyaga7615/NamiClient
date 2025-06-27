package me.kiriyaga.essentials.feature.module.impl.client;

import me.kiriyaga.essentials.feature.module.Category;
import me.kiriyaga.essentials.feature.module.Module;
import me.kiriyaga.essentials.manager.EntityManager;
import me.kiriyaga.essentials.setting.impl.BoolSetting;
import me.kiriyaga.essentials.setting.impl.DoubleSetting;
import me.kiriyaga.essentials.setting.impl.EnumSetting;
import me.kiriyaga.essentials.setting.impl.IntSetting;

public class EntityManagerModule extends Module {

    public final IntSetting maxIdleTicks = addSetting(new IntSetting("max idle ticks", 500, 250, 750));
    public final DoubleSetting targetRange = addSetting(new DoubleSetting("target range", 3.0, 1.0, 6.0));
    public final DoubleSetting minTicksExisted = addSetting(new DoubleSetting("target age", 12, 0.0, 20.0));
    public final BoolSetting targetPlayers = addSetting(new BoolSetting("target players", true));
    public final BoolSetting targetHostiles = addSetting(new BoolSetting("target hostiles", true));
    public final BoolSetting targetNeutrals = addSetting(new BoolSetting("target neutrals", false));
    public final BoolSetting targetPassives = addSetting(new BoolSetting("target passives", false));
    public final EnumSetting<TargetPriority> priority = addSetting(new EnumSetting<>("priority", TargetPriority.DISTANCE));

    public enum TargetPriority {
        DISTANCE, HEALTH
    }

    public EntityManagerModule() {
        super("entity manager", "Allows you to config entity manager settings", Category.CLIENT, "entity", "entitymanager", "enity", "утешеньфтфпук");
    }
}
