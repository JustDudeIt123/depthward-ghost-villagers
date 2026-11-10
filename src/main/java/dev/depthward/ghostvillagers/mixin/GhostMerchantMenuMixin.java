package dev.depthward.ghostvillagers.mixin;

import dev.depthward.ghostvillagers.GhostVisibility;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantMenu.class)
public abstract class GhostMerchantMenuMixin {
    @Shadow @Final private Merchant trader;

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void depthward$validateLight(Player player, CallbackInfoReturnable<Boolean> cir) {
        // Container click packets validate this before executing any transaction,
        // including the interval between light loss and the next villager tick.
        if (trader instanceof Villager villager && !GhostVisibility.canReveal(villager)) cir.setReturnValue(false);
    }
}
