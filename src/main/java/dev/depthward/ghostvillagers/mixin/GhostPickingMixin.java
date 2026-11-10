package dev.depthward.ghostvillagers.mixin;

import dev.depthward.ghostvillagers.GhostState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class GhostPickingMixin {
    @Inject(method = {"isPickable", "isPushable"}, at = @At("HEAD"), cancellable = true)
    private void depthward$hiddenGhost(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Villager villager && !((GhostState) villager).depthward$isRevealed()) cir.setReturnValue(false);
    }
}
