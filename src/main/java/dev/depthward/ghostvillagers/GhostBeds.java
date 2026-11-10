package dev.depthward.ghostvillagers;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Release old bed tickets instead of repeatedly reserving beds and erasing only memory. */
@Mod.EventBusSubscriber(modid=GhostVillagers.ID)
public final class GhostBeds {
 public static void clearHome(Villager villager){
  if(villager.level().isClientSide())return;
  if(villager.getBrain().hasMemoryValue(MemoryModuleType.HOME)){
   villager.releasePoi(MemoryModuleType.HOME);
   villager.getBrain().eraseMemory(MemoryModuleType.HOME);
   villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
   villager.getNavigation().stop();
  }
  if(villager.isSleeping())villager.stopSleeping();
 }
 @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(event.getEntity() instanceof Villager v)clearHome(v);}
 @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){if(event.getEntity() instanceof Villager v && v.tickCount%20==0)clearHome(v);}
 private GhostBeds(){}
}
