package dev.depthward.ghostvillagers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(GhostVillagers.ID)
@Mod.EventBusSubscriber(modid = GhostVillagers.ID)
public final class GhostVillagers {
    public static final String ID = "depthward_ghost_villagers";

    public static boolean ignoresTarget(LivingEntity attacker, LivingEntity target) {
        // Includes vanilla mobs and modded Mob subclasses. Player targeting is unchanged.
        return attacker instanceof Mob && target instanceof Villager;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent event) {
        if (event.getTarget() instanceof Villager villager && !GhostVisibility.canReveal(villager)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void playerDamage(LivingAttackEvent event) {
        if (event.getEntity() instanceof Villager villager
            && event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player
            && !GhostVisibility.canReveal(villager)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Villager villager && !GhostVisibility.canReveal(villager)) {
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getTarget() instanceof Villager villager && !GhostVisibility.canReveal(villager)) {
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    public static boolean isVillagerVoice(SoundEvent sound) {
        var id = sound.getLocation();
        if (!id.getNamespace().equals("minecraft")) return false;
        return switch (id.getPath()) {
            case "entity.villager.ambient", "entity.villager.celebrate", "entity.villager.death",
                 "entity.villager.hurt", "entity.villager.no", "entity.villager.trade", "entity.villager.yes" -> true;
            default -> false;
        };
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void sound(PlayLevelSoundEvent event) {
        // Also covers trade sounds played at the player, rather than the trader.
        // Workstation/tool sounds are intentionally retained.
        if (event.getSound() != null && isVillagerVoice(event.getSound().value())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void changeTarget(LivingChangeTargetEvent event) {
        // Covers direct setTarget calls as well as the vanilla target goals.
        if (ignoresTarget(event.getEntity(), event.getNewTarget())) {
            event.setNewTarget(null);
            ((Mob) event.getEntity()).getNavigation().stop();
        }
    }

    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Villager villager) {
            // Existing saves may still contain a hostile memory from before installation.
            var brain = villager.getBrain();
            if (brain.getMemory(MemoryModuleType.NEAREST_HOSTILE).orElse(null) instanceof Zombie) {
                brain.eraseMemory(MemoryModuleType.NEAREST_HOSTILE);
            }
        }
        if (event.getEntity() instanceof Mob mob) clearVillagerTarget(mob);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof Mob mob) clearVillagerTarget(mob);
    }

    private static void clearVillagerTarget(Mob mob) {
        boolean cleared = false;
        if (mob instanceof NeutralMob neutral && neutral.getPersistentAngerTarget() != null
            && mob.level() instanceof ServerLevel level && level.getEntity(neutral.getPersistentAngerTarget()) instanceof Villager) {
            neutral.stopBeingAngry();
            cleared = true;
        }
        if (mob.getTarget() instanceof Villager) {
            mob.setTarget(null);
            cleared = true;
        }
        var brain = mob.getBrain();
        if (brain.checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)
            && brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) instanceof Villager) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
            cleared = true;
        }
        if (brain.checkMemory(MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED)) {
            var angryAt = brain.getMemory(MemoryModuleType.ANGRY_AT).orElse(null);
            if (angryAt != null && mob.level() instanceof ServerLevel level && level.getEntity(angryAt) instanceof Villager) {
                brain.eraseMemory(MemoryModuleType.ANGRY_AT);
                cleared = true;
            }
        }
        if (cleared) mob.getNavigation().stop();
    }
}
