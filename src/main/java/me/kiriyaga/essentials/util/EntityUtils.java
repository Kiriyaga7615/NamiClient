package me.kiriyaga.essentials.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static me.kiriyaga.essentials.Essentials.MINECRAFT;

public class EntityUtils {

    public enum EntityTypeCategory {
        ALL,
        PLAYERS,
        OTHER_PLAYERS,
        HOSTILE,
        NEUTRAL,
        PASSIVE,
        DROPPED_ITEMS,
        END_CRYSTALS
    }

    public static List<Entity> getEntities(EntityTypeCategory category) {
        return switch (category) {
            case ALL -> getAllEntities();
            case PLAYERS -> getPlayers().stream().map(p -> (Entity)p).collect(Collectors.toList());
            case OTHER_PLAYERS -> getOtherPlayers().stream().map(p -> (Entity)p).collect(Collectors.toList());
            case HOSTILE -> getAllEntities().stream().filter(EntityUtils::isHostile).collect(Collectors.toList());
            case NEUTRAL -> getAllEntities().stream().filter(EntityUtils::isNeutral).collect(Collectors.toList());
            case PASSIVE -> getAllEntities().stream().filter(EntityUtils::isPassive).collect(Collectors.toList());
            case DROPPED_ITEMS -> getAllEntities().stream().filter(e -> e instanceof ItemEntity).collect(Collectors.toList());
            case END_CRYSTALS -> getAllEntities().stream().filter(e -> e instanceof EndCrystalEntity).collect(Collectors.toList());
        };
    }

    public static List<ItemStack> getDroppedItemStacks() {
        return getAllEntities().stream()
                .filter(e -> e instanceof ItemEntity)
                .map(e -> ((ItemEntity) e).getStack())
                .collect(Collectors.toList());
    }

    public static List<ItemEntity> getDroppedItems() {
        return getAllEntities().stream()
                .filter(e -> e instanceof ItemEntity)
                .map(e -> (ItemEntity) e)
                .collect(Collectors.toList());
    }


    public static List<Entity> getAllEntities() {
        ClientWorld world = MINECRAFT.world;
        return world != null
                ? StreamSupport.stream(world.getEntities().spliterator(), false).collect(Collectors.toList())
                : List.of();
    }

    public static List<PlayerEntity> getPlayers() {
        ClientWorld world = MINECRAFT.world;
        if (world == null) return List.of();

        return StreamSupport.stream(world.getEntities().spliterator(), false)
                .filter(e -> e instanceof PlayerEntity)
                .map(e -> (PlayerEntity) e)
                .collect(Collectors.toList());
    }

    public static List<PlayerEntity> getOtherPlayers() {
        ClientPlayerEntity self = MINECRAFT.player;
        return getPlayers().stream()
                .filter(p -> !p.isRemoved() && p != self)
                .collect(Collectors.toList());
    }

    /*
    Since 1.21.5 data about mod target`s is not accessible (also it wasnt accessible on some engine forrks)
    so its the easiest way to select them
    but its still bad, since its bases on agro, not on target

    TODO: someday write some logic from mc core, and track all mobs and actions with them, so we can 100% accurate track agro
     */
    public static boolean isHostile(Entity e) {
        if (e instanceof CreeperEntity ||
                e instanceof SkeletonEntity ||
                e instanceof StrayEntity ||
                e instanceof WitherSkeletonEntity ||
                (e.getClass() == ZombieEntity.class) ||
                e instanceof HuskEntity ||
                e instanceof DrownedEntity ||
                e instanceof VindicatorEntity ||
                e instanceof EvokerEntity ||
                e instanceof PillagerEntity ||
                e instanceof RavagerEntity ||
                e instanceof BlazeEntity ||
                e instanceof WitherEntity ||
                e instanceof EnderDragonEntity ||
                e instanceof ShulkerEntity ||
                e instanceof GuardianEntity ||
                e instanceof ElderGuardianEntity ||
                e instanceof GhastEntity ||
                e instanceof HoglinEntity ||
                e instanceof MagmaCubeEntity ||
                e instanceof SilverfishEntity ||
                e instanceof SlimeEntity ||
                e instanceof PhantomEntity ||
                e instanceof IllusionerEntity ||
                e instanceof WitchEntity) {
            return true;
        }

        if (isNeutralEntityType(e) && isAggressiveNow(e)) {
            return true;
        }

        return false;
    }

    public static boolean isNeutral(Entity e) {
        return isNeutralEntityType(e) && !isAggressiveNow(e);
    }

    private static boolean isNeutralEntityType(Entity e) {
        return e instanceof EndermanEntity ||
                e instanceof PiglinEntity ||
                e instanceof ZombifiedPiglinEntity ||
                e instanceof SpiderEntity ||
                e instanceof CaveSpiderEntity ||
                e instanceof PolarBearEntity ||
                (e instanceof WolfEntity && !((WolfEntity) e).isTamed()) ||
                e instanceof BeeEntity ||
                e instanceof GoatEntity ||
                (e instanceof IronGolemEntity && !((IronGolemEntity) e).isPlayerCreated());
    }

    public static boolean isPassive(Entity e) {
        return e instanceof PassiveEntity ||
                (e instanceof IronGolemEntity && ((IronGolemEntity) e).isPlayerCreated());
    }

    public static boolean isAggressiveNow(Entity e) {
        ClientPlayerEntity player = MINECRAFT.player;
        if (player == null || MINECRAFT.world == null) return false;

        long timeOfDay = MINECRAFT.world.getTimeOfDay() % 24000;
        boolean isNight = timeOfDay >= 13000 && timeOfDay <= 23000;

        if (e instanceof EndermanEntity enderman) return enderman.isAngry();
        if (e instanceof ZombifiedPiglinEntity piglin) return piglin.isAttacking();
        if (e instanceof PiglinEntity piglin) {
            return !isPlayerWearingGold(player) || (isPlayerWearingGold(player) && piglin.isAttacking());
        }
        if (e instanceof SpiderEntity spider) return spider.isAttacking() || isNight;
        if (e instanceof CaveSpiderEntity) return true;
        if (e instanceof PolarBearEntity bear) return bear.isAttacking();
        if (e instanceof WolfEntity wolf) return wolf.isAttacking();
        if (e instanceof BeeEntity bee) return bee.hasAngerTime();

        return false;
    }

    private static boolean isPlayerWearingGold(ClientPlayerEntity player) {
        return isGoldArmor(player.getEquippedStack(EquipmentSlot.HEAD)) ||
                isGoldArmor(player.getEquippedStack(EquipmentSlot.CHEST)) ||
                isGoldArmor(player.getEquippedStack(EquipmentSlot.LEGS)) ||
                isGoldArmor(player.getEquippedStack(EquipmentSlot.FEET));
    }

    private static boolean isGoldArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Items.GOLDEN_HELMET ||
                item == Items.GOLDEN_CHESTPLATE ||
                item == Items.GOLDEN_LEGGINGS ||
                item == Items.GOLDEN_BOOTS;
    }
}
