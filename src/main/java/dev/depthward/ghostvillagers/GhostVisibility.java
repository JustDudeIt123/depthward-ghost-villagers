package dev.depthward.ghostvillagers;

import dev.depthward.lanterns.HazardLights;
import dev.depthward.lanterns.LanternConfig;
import dev.depthward.lanterns.LanternEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GhostVisibility {
    public static final int FADE_TICKS = 30;

    private GhostVisibility() {}

    public static boolean isNight(Level level) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        // Fixed-time dimensions (Nether/End) have no ordinary night.
        return level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime()
            && time >= 13000 && time < 23000;
    }

    public static boolean canReveal(Villager villager) {
        if (!isNight(villager.level())) return false;
        if (!(villager.level() instanceof ServerLevel level)) return ((GhostState) villager).depthward$isRevealed();
        return illuminated(level, villager.position().add(0, 0.8, 0));
    }

    public static boolean illuminated(ServerLevel level, Vec3 point) {
        double heldRadius = Math.max(LanternConfig.radius(false, false), LanternConfig.radius(false, true));
        if (heldRadius > 0) {
            for (var holder : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(heldRadius + 1))) {
                if (holder.isSpectator() || !holder.isAlive() || !HazardLights.carriesLight(holder, false)) continue;
                double radius = LanternConfig.radius(false, holder.isInWater());
                if (within(point, holder.position().add(0, 0.8, 0), radius)) return true;
            }
        }
        double placedRadius = Math.max(LanternConfig.radius(true, false), LanternConfig.radius(true, true));
        if (placedRadius <= 0) return false;
        int minX = ((int) Math.floor(point.x - placedRadius)) >> 4;
        int maxX = ((int) Math.floor(point.x + placedRadius)) >> 4;
        int minZ = ((int) Math.floor(point.z - placedRadius)) >> 4;
        int maxZ = ((int) Math.floor(point.z + placedRadius)) >> 4;
        // Read only already-loaded nearby chunks. Never load a chunk to reveal a ghost.
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            var chunk = level.getChunkSource().getChunkNow(x, z);
            if (chunk == null) continue;
            for (var block : chunk.getBlockEntities().values()) {
                if (!(block instanceof LanternEntity lantern) || lantern.isRemoved() || lantern.fuel() <= 0) continue;
                var state = lantern.getBlockState();
                if (!state.getValue(BlockStateProperties.LIT)) continue;
                double radius = LanternConfig.radius(true, state.getValue(BlockStateProperties.WATERLOGGED));
                if (within(point, Vec3.atCenterOf(lantern.getBlockPos()), radius)) return true;
            }
        }
        return false;
    }

    private static boolean within(Vec3 point, Vec3 source, double radius) {
        // Same spherical approximation as the lantern mod's hazard protection.
        return radius > 0 && point.distanceToSqr(source) < radius * radius;
    }

    public static float smoothAlpha(float progress) {
        float clamped = Math.max(0, Math.min(1, progress));
        return clamped * clamped * (3 - 2 * clamped);
    }

    public static float advanceFade(float progress, boolean revealed) {
        float step = 1F / FADE_TICKS;
        return revealed ? Math.min(1, progress + step) : Math.max(0, progress - step);
    }

    public static void serverTick(Villager villager) {
        if (villager.level().isClientSide()) return;
        var state = (GhostState) villager;
        // Stagger the nearby-light query; trading is rechecked every tick.
        if (!isNight(villager.level()) || villager.tickCount % 5 == villager.getId() % 5 || villager.isTrading()) {
            boolean visible = canReveal(villager);
            state.depthward$setRevealed(visible);
            if (!visible && villager.getTradingPlayer() != null) {
                var player = villager.getTradingPlayer();
                player.closeContainer();
                villager.setTradingPlayer(null);
            }
        }
    }
}
