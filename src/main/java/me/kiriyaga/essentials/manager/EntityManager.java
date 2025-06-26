package me.kiriyaga.essentials.manager;

import me.kiriyaga.essentials.Essentials;
import me.kiriyaga.essentials.event.EventPriority;
import me.kiriyaga.essentials.event.SubscribeEvent;
import me.kiriyaga.essentials.event.impl.PreTickEvent;
import me.kiriyaga.essentials.feature.module.impl.client.EntityManagerModule;
import me.kiriyaga.essentials.setting.impl.BoolSetting;
import me.kiriyaga.essentials.setting.impl.DoubleSetting;
import me.kiriyaga.essentials.setting.impl.EnumSetting;
import me.kiriyaga.essentials.util.EntityUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static me.kiriyaga.essentials.Essentials.MINECRAFT;
import static me.kiriyaga.essentials.Essentials.MODULE_MANAGER;

public class EntityManager {

    private int maxIdleTicks = 500;
    private int idleTicksCounter = 0;

    private List<Entity> allEntities = List.of();
    private List<PlayerEntity> players = List.of();
    private List<PlayerEntity> otherPlayers = List.of();
    private List<Entity> hostile = List.of();
    private List<Entity> neutral = List.of();
    private List<Entity> passive = List.of();
    private List<ItemEntity> droppedItems = List.of();
    private List<Entity> endCrystals = List.of();

    private EntityManagerModule entityManagerModule = null;

    public void init() {
        Essentials.EVENT_MANAGER.register(this);
    }

    public void markRequested() {
        idleTicksCounter = 0;
    }

    public Entity getTarget() {
        markRequested();

        List<Entity> candidates = allEntities.stream()
                .filter(e -> e != MINECRAFT.player)
                .filter(e -> e instanceof LivingEntity && e.isAlive())
                .filter(e -> {
                    if (e.age < entityManagerModule.minTicksExisted.get().intValue()) return false;
                    double distSq = e.squaredDistanceTo(MINECRAFT.player);
                    return distSq <= entityManagerModule.targetRange.get() * entityManagerModule.targetRange.get();
                })
                .filter(e ->
                        (entityManagerModule.targetPlayers.get() && e instanceof PlayerEntity)
                                || (entityManagerModule.targetHostiles.get() && EntityUtils.isHostile(e))
                                || (entityManagerModule.targetNeutrals.get() && EntityUtils.isNeutral(e))
                                || (entityManagerModule.targetPassives.get() && EntityUtils.isPassive(e)))
                .collect(Collectors.toList());

        Comparator<Entity> comparator = switch (entityManagerModule.priority.get()) {
            case HEALTH -> Comparator.comparingDouble(e -> ((LivingEntity) e).getHealth());
            case DISTANCE -> Comparator.comparingDouble(e -> e.squaredDistanceTo(MINECRAFT.player));
        };

        Optional<Entity> result = candidates.stream().min(comparator);
        return result.orElse(null);
    }

    public List<Entity> getAllEntities() {
        markRequested();
        return allEntities;
    }

    public List<PlayerEntity> getPlayers() {
        markRequested();
        return players;
    }

    public List<PlayerEntity> getOtherPlayers() {
        markRequested();
        return otherPlayers;
    }

    public List<Entity> getHostile() {
        markRequested();
        return hostile;
    }

    public List<Entity> getNeutral() {
        markRequested();
        return neutral;
    }

    public List<Entity> getPassive() {
        markRequested();
        return passive;
    }

    public List<ItemEntity> getDroppedItems() {
        markRequested();
        return droppedItems;
    }

    public List<Entity> getEndCrystals() {
        markRequested();
        return endCrystals;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreTick(PreTickEvent event) {
        if (MINECRAFT.world == null) {
            clearData();
            idleTicksCounter = 0;
            return;
        }

        entityManagerModule = MODULE_MANAGER.getModule(EntityManagerModule.class);

        maxIdleTicks = MODULE_MANAGER.getModule(EntityManagerModule.class).maxIdleTicks.get();

        if (idleTicksCounter < maxIdleTicks) {
            updateAll();
            idleTicksCounter++;
        } else {
            clearData();
        }
    }

    private void updateAll() {
        allEntities = EntityUtils.getAllEntities();
        players = EntityUtils.getPlayers();
        otherPlayers = EntityUtils.getOtherPlayers();

        hostile = allEntities.stream().filter(EntityUtils::isHostile).toList();
        neutral = allEntities.stream().filter(EntityUtils::isNeutral).toList();
        passive = allEntities.stream().filter(EntityUtils::isPassive).toList();
        droppedItems = allEntities.stream()
                .filter(e -> e instanceof ItemEntity)
                .map(e -> (ItemEntity) e)
                .toList();
        endCrystals = allEntities.stream()
                .filter(e -> e instanceof net.minecraft.entity.decoration.EndCrystalEntity)
                .toList();
    }

    private void clearData() {
        allEntities = List.of();
        players = List.of();
        otherPlayers = List.of();
        hostile = List.of();
        neutral = List.of();
        passive = List.of();
        droppedItems = List.of();
        endCrystals = List.of();
    }
}