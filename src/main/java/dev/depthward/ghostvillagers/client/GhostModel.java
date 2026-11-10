package dev.depthward.ghostvillagers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.depthward.ghostvillagers.GhostVillagers;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.Villager;

public final class GhostModel extends HierarchicalModel<Villager> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(GhostVillagers.ID, "ghost"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    public float visibility = 1;

    public GhostModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
        this.head = root.getChild("head");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
            .texOffs(40, 16).addBox(-3, -2, -2, 4, 12, 4), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
            .texOffs(32, 48).addBox(-1, -2, -2, 4, 12, 4), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
            .texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-2, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
            .texOffs(16, 48).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(2, 12, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(Villager villager, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        if (villager.getUnhappyCounter() > 0) head.zRot = 0.25F * Mth.sin(age * 0.45F);
        // Classic (four-pixel-arm) player proportions; drift instead of walking.
        leftArm.zRot = -0.12F - Mth.cos(age * 0.06F) * 0.035F;
        rightArm.zRot = 0.12F + Mth.cos(age * 0.06F) * 0.035F;
        leftArm.xRot = Mth.sin(age * 0.045F) * 0.05F;
        rightArm.xRot = -leftArm.xRot;
        leftLeg.xRot = 0.04F + Mth.sin(age * 0.04F) * 0.025F;
        rightLeg.xRot = -leftLeg.xRot;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vertices, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        // Honor vanilla invisibility/outline alpha instead of making hidden mobs visible.
        super.renderToBuffer(pose, vertices, light, overlay, red, green, blue, alpha * 0.58F * visibility);
    }
}
