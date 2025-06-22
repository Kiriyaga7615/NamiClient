package me.kiriyaga.essentials.manager;

import me.kiriyaga.essentials.feature.module.Category;
import me.kiriyaga.essentials.feature.module.Module;
import me.kiriyaga.essentials.feature.module.impl.client.*;
import me.kiriyaga.essentials.feature.module.impl.combat.AuraModule;
import me.kiriyaga.essentials.feature.module.impl.combat.AutoLogModule;
import me.kiriyaga.essentials.feature.module.impl.combat.AutoTotemModule;
import me.kiriyaga.essentials.feature.module.impl.movement.GuiMoveModule;
import me.kiriyaga.essentials.feature.module.impl.movement.SprintModule;
import me.kiriyaga.essentials.feature.module.impl.render.*;
import me.kiriyaga.essentials.feature.module.impl.world.AutoFovModule;
import me.kiriyaga.essentials.feature.module.impl.world.AutoGammaModule;
import me.kiriyaga.essentials.feature.module.impl.world.JoinAnnounceModule;

import java.util.ArrayList;
import java.util.List;

import static me.kiriyaga.essentials.Essentials.LOGGER;
import static me.kiriyaga.essentials.Essentials.MODULE_MANAGER;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public void init() {
        /// RENDER ///
        registerModule(new NametagsModule());
        registerModule(new ESPModule());
        registerModule(new SearchModule());
        registerModule(new NoRenderModule());
        registerModule(new FreeLookModule());
        registerModule(new FreecamModule());
        registerModule(new NoWeatherModule());
        /// CLIENT ///
        registerModule(new ColorModule());
        registerModule(new ClickGuiModule());
        registerModule(new HUDModule());
        registerModule(new PingManagerModule());
        registerModule(new RotationManagerModule());

        /// COMBAT ///
        registerModule(new AutoTotemModule());
        registerModule(new AuraModule());
        registerModule(new AutoLogModule());
        /// MOVEMENT ///
        registerModule(new SprintModule());
        registerModule(new GuiMoveModule());
        /// TRAVEL ///
        //registerModule(new EbounceModule());
        /// WORLD ///
        registerModule(new JoinAnnounceModule());
        registerModule(new AutoFovModule());
        registerModule(new AutoGammaModule());

        LOGGER.info("Registered " + modules.size() + " modules");
    }

    public void registerModule(Module module) {
        modules.add(module);
    }

    public void unregisterModule(Module module) {
        modules.remove(module);
        module.setEnabled(false);
    }

    public List<Module> getModules() {
        return modules;
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (clazz.isInstance(module)) {
                return clazz.cast(module);
            }
        }
        return null;
    }

    public Module getModuleByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public List<Module> getModulesByCategory(Category category) {
        List<Module> modules = new ArrayList<>();
        for (Module mod : MODULE_MANAGER.getModules()) {
            if (mod.getCategory() == category) {
                modules.add(mod);
            }
        }
        modules.sort((m1, m2) -> m1.getName().compareToIgnoreCase(m2.getName()));
        return modules;
    }
}
