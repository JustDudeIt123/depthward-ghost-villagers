package dev.depthward.ghostvillagers;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import dev.depthward.ghostvillagers.client.GhostModel;
import dev.depthward.ghostvillagers.client.GhostRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GhostVillagers.ID, value = Dist.CLIENT)
public final class ClientSmoke {
    private static int frames;
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void verifyFade(net.minecraft.client.model.geom.ModelPart root) {
        for (float visibility : new float[]{0, 0.25F, 0.5F, 1}) {
            var model = new GhostModel(root);
            model.visibility = visibility;
            var builder = new com.mojang.blaze3d.vertex.BufferBuilder(4096);
            builder.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY);
            model.renderToBuffer(new com.mojang.blaze3d.vertex.PoseStack(), builder,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            var rendered = builder.end();
            try {
                var bytes = rendered.vertexBuffer();
                int stride = com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY.getVertexSize();
                int expected = (int) (255 * 0.58F * visibility);
                for (int vertex = 0; vertex < rendered.drawState().vertexCount(); vertex++) {
                    int alpha = Byte.toUnsignedInt(bytes.get(vertex * stride + 15));
                    check(Math.abs(alpha - expected) <= 1, "Model vertex alpha does not follow fade visibility");
                }
            } finally { rendered.release(); }
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.END
            && mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.setScreen(new TitleScreen());
        }
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) throws java.io.IOException {
        if (!(event.getScreen() instanceof TitleScreen)) return;
        var mc = Minecraft.getInstance();
        if (mc.getOverlay() != null) return;
        var root = mc.getEntityModels().bakeLayer(GhostModel.LAYER);
        if (frames == 0) verifyFade(root);
        check(root.getAllParts().count() == 7, "Expected human head, torso and four limbs; no nose");
        check(mc.getResourceManager().getResource(GhostRenderer.TEXTURE).isPresent(), "Missing spectral texture");
        for (var sound : java.util.List.of(SoundEvents.VILLAGER_AMBIENT, SoundEvents.VILLAGER_TRADE,
            SoundEvents.VILLAGER_YES, SoundEvents.VILLAGER_NO, SoundEvents.VILLAGER_HURT,
            SoundEvents.VILLAGER_DEATH, SoundEvents.VILLAGER_CELEBRATE)) {
            var resolved = mc.getSoundManager().getSoundEvent(sound.getLocation());
            check(resolved != null && resolved.getWeight() == 0, "Voice still has playable sounds: " + sound.getLocation());
        }
        var gui = event.getGuiGraphics();
        int width = event.getScreen().width, height = event.getScreen().height;
        gui.fill(0, 0, width, height, 0xFF192635);
        // Checkerboard behind the model makes real alpha blending visible.
        for (int x = 0; x < width; x += 20) for (int y = 0; y < height; y += 20)
            if ((x / 20 + y / 20) % 2 == 0) gui.fill(x, y, x + 20, y + 20, 0xFF26394D);
        gui.drawCenteredString(mc.font, "DepthWard: human ghost villager", width / 2, 12, 0xD7F7FF);
        gui.flush();
        var pose = gui.pose();
        pose.pushPose();
        pose.translate(width / 2.0, height * 0.36, 200);
        float scale = height * 0.33F;
        pose.scale(scale, scale, -scale);
        pose.mulPose(Axis.YP.rotationDegrees(-20));
        pose.mulPose(Axis.XP.rotationDegrees(-5));
        Lighting.setupForEntityInInventory();
        var model = new GhostModel(root);
        root.getChild("left_arm").zRot = -0.12F;
        root.getChild("right_arm").zRot = 0.12F;
        var buffers = mc.renderBuffers().bufferSource();
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(GhostRenderer.TEXTURE)),
            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        buffers.endBatch();
        pose.popPose();
        Lighting.setupFor3DItems();
        if (++frames == 12) {
            try (var screenshot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                screenshot.writeToFile(java.nio.file.Path.of("ghost-preview.png"));
            }
            System.out.println("[GHOST_CLIENT_SMOKE] PASS: human model, vertex alpha at four fade stages, texture and seven muted voices");
            mc.stop();
        }
    }
}
