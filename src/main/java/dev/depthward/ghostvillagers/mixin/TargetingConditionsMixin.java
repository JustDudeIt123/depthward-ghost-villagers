package dev.depthward.ghostvillagers.mixin;

import dev.depthward.ghostvillagers.GhostVillagers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void depthward$ignoreGhost(LivingEntity source, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (GhostVillagers.ignoresTarget(source, target)) cir.setReturnValue(false);
    }
}
