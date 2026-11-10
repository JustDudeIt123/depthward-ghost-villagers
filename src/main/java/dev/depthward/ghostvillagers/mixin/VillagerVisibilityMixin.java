package dev.depthward.ghostvillagers.mixin;

import dev.depthward.ghostvillagers.GhostState;
import dev.depthward.ghostvillagers.GhostVisibility;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerVisibilityMixin extends AbstractVillager implements GhostState {
    @Unique private static final EntityDataAccessor<Boolean> depthward$revealed = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.BOOLEAN);
    @Unique private float depthward$progress;
    @Unique private float depthward$previous;

    protected VillagerVisibilityMixin(EntityType<? extends AbstractVillager> type, Level level) { super(type, level); }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void depthward$define(CallbackInfo ci) { entityData.define(depthward$revealed, false); }

    @Override public boolean depthward$isRevealed() { return entityData.get(depthward$revealed); }
    @Override public void depthward$setRevealed(boolean revealed) { entityData.set(depthward$revealed, revealed); }

    @Override public float depthward$fade(float partialTick) {
        return GhostVisibility.smoothAlpha(depthward$previous + (depthward$progress - depthward$previous) * partialTick);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void depthward$tick(CallbackInfo ci) {
        var villager = (Villager) (Object) this;
        if (level().isClientSide()) {
            depthward$previous = depthward$progress;
            depthward$progress = GhostVisibility.advanceFade(depthward$progress, depthward$isRevealed());
        } else GhostVisibility.serverTick(villager);
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void depthward$blockHiddenTrade(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!GhostVisibility.canReveal((Villager) (Object) this)) cir.setReturnValue(InteractionResult.FAIL);
    }
}
