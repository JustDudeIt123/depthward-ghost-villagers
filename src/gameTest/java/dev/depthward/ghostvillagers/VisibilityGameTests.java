package dev.depthward.ghostvillagers;

import dev.depthward.lanterns.Fuel;
import dev.depthward.lanterns.Lanterns;
import dev.depthward.lanterns.LanternEntity;
import dev.depthward.lanterns.LanternConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(GhostVillagers.ID)
@PrefixGameTestTemplate(false)
public final class VisibilityGameTests {
    @GameTest(template = "empty", batch = "visibility")
    public static void nightLightAndServerInteractionRules(GameTestHelper t) {
        var level = t.getLevel();
        long previousTime = level.getDayTime();
        var villager = t.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        var holder = t.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 2));
        holder.setNoAi(true);
        boolean[] closed = {false};
        var player = new net.minecraftforge.common.util.FakePlayer(level,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "GhostVisibility")) {
            @Override public void closeContainer() { closed[0] = true; }
        };
        var lantern = new ItemStack(Lanterns.LANTERN.get());
        Fuel.refill(lantern);
        var state = (GhostState) villager;
        try {
            level.setDayTime(18000);
            t.assertTrue(!state.depthward$isRevealed() && !villager.isPickable() && !villager.isPushable(), "Ghost must start hidden and untargetable");
            t.assertTrue(!GhostVisibility.canReveal(villager), "Night alone cannot reveal a ghost");
            t.assertTrue(!villager.interact(player, InteractionHand.MAIN_HAND).consumesAction()
                && !villager.isTrading(), "Direct interaction must not open trading without light");
            var hit = new net.minecraftforge.event.entity.player.AttackEntityEvent(player, villager);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(hit);
            t.assertTrue(hit.isCanceled(), "Melee must be blocked on the server");
            var arrowHit = new net.minecraftforge.event.entity.living.LivingAttackEvent(villager, level.damageSources().playerAttack(player), 4);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(arrowHit);
            t.assertTrue(arrowHit.isCanceled(), "Player-owned damage must be blocked while hidden");
            var click = new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, villager);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(click);
            t.assertTrue(click.isCanceled(), "Interaction packet must be rejected on the server");

            holder.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TORCH));
            t.assertTrue(!GhostVisibility.canReveal(villager), "Vanilla light items cannot reveal ghosts");
            holder.setItemSlot(EquipmentSlot.MAINHAND, lantern);
            t.assertTrue(GhostVisibility.canReveal(villager), "Active held lantern must reveal nearby ghost at night");
            villager.tickCount = villager.getId() % 5;
            GhostVisibility.serverTick(villager);
            t.assertTrue(state.depthward$isRevealed() && villager.isPickable() && villager.isPushable(), "Server flag must enable visible interactions");
            hit = new net.minecraftforge.event.entity.player.AttackEntityEvent(player, villager);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(hit);
            t.assertTrue(!hit.isCanceled(), "Visible ghost must allow interaction");

            holder.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            holder.setItemSlot(EquipmentSlot.OFFHAND, lantern);
            t.assertTrue(GhostVisibility.canReveal(villager), "Offhand lantern must work");
            Fuel.setEnabled(lantern, false);
            t.assertTrue(!GhostVisibility.canReveal(villager), "Disabled lantern must not reveal ghost");
            Fuel.setEnabled(lantern, true);
            lantern.getOrCreateTag().putInt("Fuel", 0);
            t.assertTrue(!GhostVisibility.canReveal(villager), "Empty lantern must not reveal ghost");
            Fuel.refill(lantern);
            holder.setPos(villager.getX() + 70, villager.getY(), villager.getZ());
            t.assertTrue(!GhostVisibility.canReveal(villager), "Distant lantern must not reveal ghost");
            holder.setPos(villager.getX() + 1, villager.getY(), villager.getZ());
            for (long daytime : new long[]{0, 6000, 12999, 23000, 23999}) {
                level.setDayTime(daytime);
                t.assertTrue(!GhostVisibility.canReveal(villager), "Day and daylight edges must stay hidden: " + daytime);
            }
            for (long nighttime : new long[]{13000, 18000, 22999, 37000}) {
                level.setDayTime(nighttime);
                t.assertTrue(GhostVisibility.canReveal(villager), "Night interval must reveal lit ghost: " + nighttime);
            }
            level.setDayTime(18000);
            villager.setTradingPlayer(player);
            var menu = new net.minecraft.world.inventory.MerchantMenu(1, player.getInventory(), villager);
            t.assertTrue(menu.stillValid(player), "Lit nighttime trading menu must be valid");
            Fuel.setEnabled(lantern, false);
            t.assertTrue(!menu.stillValid(player), "Menu must reject transactions immediately when light goes out, before the next entity tick");
            GhostVisibility.serverTick(villager);
            t.assertTrue(closed[0] && !villager.isTrading() && !state.depthward$isRevealed(), "Light loss must close menu, end existing trade and hide ghost immediately");
        } finally {
            level.setDayTime(previousTime);
            villager.discard();
            holder.discard();
        }
        t.succeed();
    }

    @GameTest(template = "empty", batch = "visibility")
    public static void placedLanternUsesConfiguredRadiusAndFuel(GameTestHelper t) {
        var level = t.getLevel();
        long time = level.getDayTime();
        var villager = t.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        var pos = new BlockPos(4, 2, 2);
        t.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE);
        t.setBlock(pos, Lanterns.BLOCK.get().defaultBlockState());
        var be = (LanternEntity) level.getBlockEntity(t.absolutePos(pos));
        var stack = new ItemStack(Lanterns.LANTERN.get());
        Fuel.refill(stack);
        try {
            level.setDayTime(18000);
            be.setFuel(stack);
            t.assertTrue(GhostVisibility.canReveal(villager), "Nearby placed lantern must reveal ghost");
            var originalPosition = villager.position();
            villager.setPos(originalPosition.x + LanternConfig.radius(true, false) + 5, originalPosition.y, originalPosition.z);
            t.assertTrue(!GhostVisibility.canReveal(villager), "Beyond configured radius must remain hidden");
            villager.setPos(originalPosition);
            Fuel.setEnabled(stack, false);be.setFuel(stack);
            t.assertTrue(!GhostVisibility.canReveal(villager), "Disabled placed lantern cannot reveal ghost");
            Fuel.setEnabled(stack, true);stack.getOrCreateTag().putInt("Fuel", 0);be.setFuel(stack);
            t.assertTrue(!GhostVisibility.canReveal(villager), "Empty placed lantern cannot reveal ghost");
        } finally {
            level.setDayTime(time);
            t.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
            villager.discard();
        }
        t.succeed();
    }

    @GameTest(template = "empty", batch = "visibility")
    public static void fadeIsSmoothAndReversesWithoutJumping(GameTestHelper t) {
        t.assertTrue(GhostVisibility.smoothAlpha(0) == 0 && GhostVisibility.smoothAlpha(1) == 1, "Fade must reach both endpoints");
        float progress = 0, previousAlpha = 0;
        for (int i = 0; i < GhostVisibility.FADE_TICKS; i++) {
            progress = GhostVisibility.advanceFade(progress, true);
            float alpha = GhostVisibility.smoothAlpha(progress);
            t.assertTrue(alpha >= previousAlpha && alpha - previousAlpha <= 0.051F, "Appearance must be monotonic without abrupt alpha steps");
            previousAlpha = alpha;
        }
        t.assertTrue(progress >= 0.999F, "Fade-in must finish in 1.5 seconds");
        progress = 0.5F;
        float reversed = GhostVisibility.advanceFade(progress, false);
        t.assertTrue(reversed < progress && reversed > 0.4F, "Direction reversal must continue from current opacity");
        for (int i = 0; i < GhostVisibility.FADE_TICKS; i++) progress = GhostVisibility.advanceFade(progress, false);
        t.assertTrue(progress == 0 && GhostVisibility.smoothAlpha(progress) == 0, "Hidden ghost must become completely transparent");
        t.succeed();
    }
}
