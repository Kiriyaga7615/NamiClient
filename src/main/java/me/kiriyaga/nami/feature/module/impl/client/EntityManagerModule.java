package me.kiriyaga.nami.feature.module.impl.client;

import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.setting.impl.BoolSetting;
import me.kiriyaga.nami.setting.impl.DoubleSetting;
import me.kiriyaga.nami.setting.impl.EnumSetting;
import me.kiriyaga.nami.setting.impl.IntSetting;

@RegisterModule
public class EntityManagerModule extends Module {

    public final IntSetting maxIdleTicks = addSetting(new IntSetting("max idle ticks", 500, 250, 750));
    public final DoubleSetting targetRange = addSetting(new DoubleSetting("target range", 7.0, 1.0, 16.0));
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
        super("entity manager", "Allows you to config entity manager settings", ModuleCategory.of("client"), "entity", "entitymanager", "enity", "утешеньфтфпук");
        if (!this.isEnabled())
            this.toggle();
    }

    @Override
    public void onDisable(){
        if (!this.isEnabled())
            this.toggle();
    }
}
