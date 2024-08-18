package com.jelly.mightyminerv2.Util;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;

public class RenderUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void drawPoint(Vec3 vec, Color color) {
        drawBox(new AxisAlignedBB(vec.xCoord - 0.05, vec.yCoord - 0.05, vec.zCoord - 0.05,
                vec.xCoord + 0.05, vec.yCoord + 0.05, vec.zCoord + 0.05), color);
    }

    public static void drawBlockBox(BlockPos blockPos, Color color) {
        double x = blockPos.getX();
        double y = blockPos.getY();
        double z = blockPos.getZ();
        double x2 = x + 1;
        double y2 = y + 1;
        double z2 = z + 1;
        drawBox(new AxisAlignedBB(x, y, z, x2, y2, z2), color);
    }

    public static void drawBox(AxisAlignedBB aabb, Color color) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        RenderManager renderManager = mc.getRenderManager();
        aabb = aabb.offset(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ);

        float a = color.getAlpha() / 255.0F;
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        GlStateManager.color(r, g, b, a);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex();
        tessellator.draw();
        GlStateManager.color(r, g, b, a);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex();
        tessellator.draw();
        GlStateManager.color(r, g, b, a);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex();
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex();
        tessellator.draw();
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(2);
        RenderGlobal.drawSelectionBoundingBox(aabb);
        GL11.glLineWidth(1.0f);
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }

    public static void drawMultiLineText(ArrayList<String> lines, RenderGameOverlayEvent event, Color color, float scale) {
        ScaledResolution scaledResolution = event.resolution;
        int scaledWidth = scaledResolution.getScaledWidth();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) (scaledWidth / 2), 50, 0.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.scale(scale, scale, scale);
        FontRenderer fontRenderer = mc.fontRendererObj;
        int yOffset = 0;
        for (String line : lines) {
            fontRenderer.drawString(line, (-fontRenderer.getStringWidth(line) / 2f), yOffset, color.getRGB(), true);
            yOffset += fontRenderer.FONT_HEIGHT * 2;
        }

        GlStateManager.popMatrix();
    }

    public static void drawCenterTopText(String text, RenderGameOverlayEvent event, Color color) {
        drawCenterTopText(text, event, color, 3);
    }

    public static void drawCenterTopText(String text, RenderGameOverlayEvent event, Color color, float scale) {
        ScaledResolution scaledResolution = event.resolution;
        int scaledWidth = scaledResolution.getScaledWidth();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) (scaledWidth / 2), 50, 0.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.scale(scale, scale, scale);
        mc.fontRendererObj.drawString(text, (-mc.fontRendererObj.getStringWidth(text) / 2f), 0, color.getRGB(), true);
        GlStateManager.popMatrix();
    }

    public static void drawText(String str, double X, double Y, double Z, float scale) {
        float lScale = scale;
        FontRenderer fontRenderer = mc.fontRendererObj;

        double renderPosX = X - mc.getRenderManager().viewerPosX;
        double renderPosY = Y - mc.getRenderManager().viewerPosY;
        double renderPosZ = Z - mc.getRenderManager().viewerPosZ;

        double distance = Math.sqrt(renderPosX * renderPosX + renderPosY * renderPosY + renderPosZ * renderPosZ);
        double multiplier = Math.max(distance / 150f, 0.1f);
        lScale *= (float) (0.45f * multiplier);

        float xMultiplier = mc.gameSettings.thirdPersonView == 2 ? -1 : 1;

        GlStateManager.pushMatrix();
        GlStateManager.translate(renderPosX, renderPosY, renderPosZ);
        RenderManager renderManager = mc.getRenderManager();
        GlStateManager.rotate(-renderManager.playerViewY, 0, 1, 0);
        GlStateManager.rotate(renderManager.playerViewX * xMultiplier, 1, 0, 0);
        GlStateManager.scale(-lScale, -lScale, lScale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int textWidth = fontRenderer.getStringWidth(StringUtils.stripControlCodes((str)));

        float j = textWidth / 2f;
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.color(0, 0, 0, 0.5f);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(-j - 1, -1, 0).endVertex();
        worldrenderer.pos(-j - 1, 8, 0).endVertex();
        worldrenderer.pos(j + 1, 8, 0).endVertex();
        worldrenderer.pos(j + 1, -1, 0).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();

        fontRenderer.drawString(str, -textWidth / 2, 0, 553648127);
        GlStateManager.depthMask(true);
        fontRenderer.drawString(str, -textWidth / 2, 0, -1);

        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    public static void drawTracer(Vec3 from, Vec3 to, Color color) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        RenderManager renderManager = mc.getRenderManager();
        double renderPosX = to.xCoord - renderManager.viewerPosX;
        double renderPosY = to.yCoord - renderManager.viewerPosY;
        double renderPosZ = to.zCoord - renderManager.viewerPosZ;
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(from.xCoord - renderManager.viewerPosX, from.yCoord - renderManager.viewerPosY, from.zCoord - renderManager.viewerPosZ);
        GL11.glVertex3d(renderPosX, renderPosY, renderPosZ);
        GL11.glEnd();
        GL11.glLineWidth(1.0f);
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }

    public static void drawTracer(Vec3 to, Color color) {
        drawTracer(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ), to, color);
    }
}