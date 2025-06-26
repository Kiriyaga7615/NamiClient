package me.kiriyaga.essentials.feature.module.impl.client;

import me.kiriyaga.essentials.feature.module.Category;
import me.kiriyaga.essentials.feature.module.Module;
import me.kiriyaga.essentials.setting.impl.BoolSetting;
import me.kiriyaga.essentials.setting.impl.DoubleSetting;
import me.kiriyaga.essentials.setting.impl.IntSetting;

public class RotationManagerModule extends Module {
    public final DoubleSetting rotationSpeed = addSetting(new DoubleSetting("speed", 25, 1, 60));
    public final DoubleSetting rotationEaseFactor = addSetting(new DoubleSetting("ease", 0.9, 0.05, 1));
    public final DoubleSetting rotationThreshold = addSetting(new DoubleSetting("threshold", 0, 0, 15));
    public final IntSetting ticksBeforeRelease = addSetting(new IntSetting("hold", 30, 00, 120));
    public final BoolSetting moveFix = addSetting(new BoolSetting("move fix", true));
    public final DoubleSetting jitterAmount = addSetting(new DoubleSetting("jitter amount", 2.0, 0, 15));
    public final DoubleSetting jitterSpeed = addSetting(new DoubleSetting("jitter speed", 0.3, 0.015, 1));

    public RotationManagerModule() {
        super("rotation manager", "Allows you to config rotation manager settings.", Category.CLIENT, "rotate", "rotationmanager", "roate", "toationmanager", "кщефеу");
    }
}
