package dev.depthward.ghostvillagers.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerHostilesSensor.class)
public abstract class VillagerHostilesSensorMixin {
    @Inject(method = "isMatchingEntity", at = @At("HEAD"), cancellable = true)
    private void depthward$ignoreZombie(LivingEntity observer, LivingEntity candidate, CallbackInfoReturnable<Boolean> cir) {
        if (candidate instanceof Zombie) cir.setReturnValue(false);
    }
}
