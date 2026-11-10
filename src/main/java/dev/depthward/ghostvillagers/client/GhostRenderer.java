package dev.depthward.ghostvillagers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.depthward.ghostvillagers.GhostVillagers;
import dev.depthward.ghostvillagers.GhostState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.Villager;

public final class GhostRenderer extends MobRenderer<Villager, GhostModel> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(GhostVillagers.ID, "textures/entity/ghost_villager.png");

    public GhostRenderer(EntityRendererProvider.Context context) {
        super(context, new GhostModel(context.bakeLayer(GhostModel.LAYER)), 0.15F);
    }

    @Override public ResourceLocation getTextureLocation(Villager villager) { return TEXTURE; }

    @Override
    public boolean shouldRender(Villager villager, Frustum frustum, double x, double y, double z) {
        var state = (GhostState) villager;
        return (state.depthward$isRevealed() || state.depthward$fade(1) > 0)
            && super.shouldRender(villager, frustum, x, y, z);
    }

    @Override
    public void render(Villager villager, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float visibility = ((GhostState) villager).depthward$fade(partialTick);
        shadowRadius = 0.15F * visibility;
        if (visibility <= 0) return; // No body, leash, name or glowing outline while hidden.
        model.visibility = visibility;
        try { super.render(villager, yaw, partialTick, pose, buffers, light); }
        finally { model.visibility = 1; }
    }

    @Override
    protected boolean shouldShowName(Villager villager) {
        return ((GhostState) villager).depthward$isRevealed()
            && ((GhostState) villager).depthward$fade(1) >= 0.95F && super.shouldShowName(villager);
    }

    @Override
    protected RenderType getRenderType(Villager villager, boolean visible, boolean translucent, boolean glowing) {
        return super.getRenderType(villager, visible, translucent,
            glowing && ((GhostState) villager).depthward$fade(1) >= 1);
    }

    @Override
    protected void scale(Villager villager, PoseStack pose, float partialTick) {
        float size = villager.isBaby() ? 0.46875F : 0.9375F;
        pose.scale(size, size, size);
    }

    @Override
    protected void setupRotations(Villager villager, PoseStack pose, float age, float yaw, float partialTick) {
        super.setupRotations(villager, pose, age, yaw, partialTick);
        if (!villager.isSleeping()) {
            // Keep real collision/navigation at ground level for workstations,
            // beds and trading. Only the spectral body floats above that anchor.
            pose.translate(0, 0.28F + Mth.sin((age + villager.getId() * 7) * 0.08F) * 0.065F, 0);
        }
    }
}
