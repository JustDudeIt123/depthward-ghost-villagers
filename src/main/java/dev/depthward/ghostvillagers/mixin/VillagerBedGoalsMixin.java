package dev.depthward.ghostvillagers.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.*;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerGoalPackages.class)
public abstract class VillagerBedGoalsMixin {
 @Inject(method="getRestPackage",at=@At("HEAD"),cancellable=true)
 private static void noBedRest(VillagerProfession profession,float speed,CallbackInfoReturnable<ImmutableList<Pair<Integer,? extends BehaviorControl<? super Villager>>>> ci){
  ci.setReturnValue(VillagerGoalPackages.getIdlePackage(profession,speed));
 }
 @Redirect(method="getCorePackage",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/behavior/AcquirePoi;create(Ljava/util/function/Predicate;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;ZLjava/util/Optional;)Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;"))
 @SuppressWarnings("unchecked")
 private static BehaviorControl<PathfinderMob> noHomeReservation(Predicate<Holder<PoiType>> type,MemoryModuleType<GlobalPos> memory,boolean adults,Optional<Byte> event){
  if(memory==MemoryModuleType.HOME)return (BehaviorControl<PathfinderMob>)(BehaviorControl<?>)new DoNothing(20,40);
  return AcquirePoi.create(type,memory,adults,event);
 }
}
