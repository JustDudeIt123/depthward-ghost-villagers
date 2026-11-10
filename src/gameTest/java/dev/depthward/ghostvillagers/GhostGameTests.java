package dev.depthward.ghostvillagers;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(GhostVillagers.ID)
@PrefixGameTestTemplate(false)
public final class GhostGameTests {
    @GameTest(template = "empty")
    public static void villagerVoicesAreMutedButOtherSoundsRemain(GameTestHelper t) {
        for (var sound : java.util.List.of(net.minecraft.sounds.SoundEvents.VILLAGER_AMBIENT,
            net.minecraft.sounds.SoundEvents.VILLAGER_TRADE, net.minecraft.sounds.SoundEvents.VILLAGER_YES,
            net.minecraft.sounds.SoundEvents.VILLAGER_NO, net.minecraft.sounds.SoundEvents.VILLAGER_HURT,
            net.minecraft.sounds.SoundEvents.VILLAGER_DEATH, net.minecraft.sounds.SoundEvents.VILLAGER_CELEBRATE)) {
            var event = new net.minecraftforge.event.PlayLevelSoundEvent.AtPosition(t.getLevel(),
                new net.minecraft.world.phys.Vec3(0, 0, 0), net.minecraft.core.Holder.direct(sound),
                net.minecraft.sounds.SoundSource.NEUTRAL, 1, 1);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            t.assertTrue(event.isCanceled(), "Villager voice must be muted: " + sound.getLocation());
        }
        t.assertTrue(!GhostVillagers.isVillagerVoice(net.minecraft.sounds.SoundEvents.WANDERING_TRADER_AMBIENT), "Wandering trader must keep its voice");
        t.assertTrue(!GhostVillagers.isVillagerVoice(net.minecraft.sounds.SoundEvents.VILLAGER_WORK_LIBRARIAN), "Workstation sounds must remain");
        t.succeed();
    }

    @GameTest(template = "empty")
    public static void mobsCannotAcquireOrBeAssignedVillagers(GameTestHelper t) {
        var villager = t.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(t.getLevel());
        for (var type : java.util.List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
            EntityType.ZOMBIFIED_PIGLIN, EntityType.PILLAGER, EntityType.RAVAGER, EntityType.VEX,
            EntityType.SKELETON, EntityType.SPIDER, EntityType.PIGLIN, EntityType.HOGLIN, EntityType.WARDEN, EntityType.IRON_GOLEM)) {
            var zombie = (net.minecraft.world.entity.Mob) t.spawn(type, new BlockPos(3, 2, 2));
            t.assertTrue(!TargetingConditions.forCombat().test(zombie, villager), type + " must reject villager before starting pursuit");
            zombie.setTarget(villager);
            t.assertTrue(zombie.getTarget() == null, type + " must reject direct target assignment");
            zombie.setTarget(player);
            t.assertTrue(zombie.getTarget() == player, "Player targeting must remain unchanged");
            zombie.discard();
        }
        var piglin = t.spawn(EntityType.PIGLIN, new BlockPos(3, 2, 2));
        piglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, villager);
        piglin.getBrain().setMemory(MemoryModuleType.ANGRY_AT, villager.getUUID());
        GhostVillagers.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(piglin));
        t.assertTrue(piglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty(), "Saved brain attack target must be cleared");
        t.assertTrue(piglin.getBrain().getMemory(MemoryModuleType.ANGRY_AT).isEmpty(), "Villager anger must be cleared");
        piglin.discard();
        villager.discard();
        t.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void sensorAndRealZombieAiIgnoreGhosts(GameTestHelper t) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            t.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            t.setBlock(new BlockPos(x, 5, z), Blocks.STONE);
            if (x == 0 || x == 4 || z == 0 || z == 4) {
                for (int y = 2; y < 5; y++) t.setBlock(new BlockPos(x, y, z), Blocks.GLASS);
            }
        }
        var villager = t.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        var zombie = t.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 2));
        var husk = t.spawn(EntityType.HUSK, new BlockPos(1, 2, 2));
        var drowned = t.spawn(EntityType.DROWNED, new BlockPos(2, 2, 3));
        float health = villager.getHealth();
        t.onEachTick(() -> {
            t.assertTrue(zombie.getTarget() != villager && husk.getTarget() != villager && drowned.getTarget() != villager,
                "Real target selectors must not pursue ghosts");
            t.assertTrue(villager.getHealth() == health, "Nearby zombies must not attack the villager");
        });
        t.runAtTickTime(100, () -> {
            t.assertTrue(villager.getBrain().getMemory(MemoryModuleType.NEAREST_HOSTILE).isEmpty(),
                "Unexpected hostile memory: " + villager.getBrain().getMemory(MemoryModuleType.NEAREST_HOSTILE));
            t.assertTrue(!villager.getBrain().isActive(Activity.PANIC), "Villager must not panic around zombies");
            t.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void otherHostilesAreStillDetected(GameTestHelper t) {
        var villager = t.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        var pillager = t.spawn(EntityType.PILLAGER, new BlockPos(3, 2, 2));
        villager.getBrain().setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, java.util.List.of(pillager));
        villager.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            new net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities(villager, java.util.List.of(pillager)));
        var sensor = SensorType.VILLAGER_HOSTILES.create();
        // Sensor scan has a randomized initial phase; cover a complete scan interval.
        for (int i = 0; i < 40; i++) sensor.tick(t.getLevel(), villager);
        t.assertTrue(villager.getBrain().getMemory(MemoryModuleType.NEAREST_HOSTILE).orElse(null) == pillager,
            "Pillagers must still be perceived as hostile");
        pillager.discard();
        villager.discard();
        t.succeed();
    }

    @GameTest(template = "empty")
    public static void savedVillagerKeepsTradesAndCanRestock(GameTestHelper t) {
        var villager = t.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        villager.setVillagerData(villager.getVillagerData().setType(VillagerType.TAIGA).setProfession(VillagerProfession.LIBRARIAN).setLevel(3));
        villager.setVillagerXp(85);
        villager.setCustomName(net.minecraft.network.chat.Component.literal("Ghost trader"));
        var offer = new MerchantOffer(new ItemStack(Items.EMERALD, 5), new ItemStack(Items.BOOK), 12, 3, 0.05F);
        offer.increaseUses();
        villager.getOffers().clear();
        villager.getOffers().add(offer);
        var data = new CompoundTag();
        villager.saveWithoutId(data);
        var loaded = EntityType.VILLAGER.create(t.getLevel());
        loaded.load(data);
        t.assertTrue(loaded.getVillagerData().getProfession() == VillagerProfession.LIBRARIAN
            && loaded.getVillagerData().getType() == VillagerType.TAIGA
            && loaded.getVillagerData().getLevel() == 3, "Profession, biome and level must survive reload");
        t.assertTrue(loaded.getVillagerXp() == 85, "Experience must survive reload");
        t.assertTrue(loaded.getCustomName().equals(villager.getCustomName()), "Custom name must survive reload");
        t.assertTrue(loaded.getOffers().size() == 1 && loaded.getOffers().get(0).getUses() == 1, "Offers and uses must survive reload");
        loaded.restock();
        t.assertTrue(loaded.getOffers().get(0).getUses() == 0, "Vanilla restocking must still work");
        t.assertTrue(!loaded.isNoAi() && !loaded.isNoGravity(), "Vanilla work/bed navigation must remain enabled");
        villager.discard();
        t.succeed();
    }
}
