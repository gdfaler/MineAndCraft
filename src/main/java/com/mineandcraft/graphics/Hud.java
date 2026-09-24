package com.mineandcraft.graphics;

import com.mineandcraft.player.Hotbar;
import com.mineandcraft.world.Block;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_DST_COLOR;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_ZERO;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * Игровой HUD. Координаты заданы для эталонного соотношения сторон 16:9;
 * 2D-шейдеры масштабируют их так, чтобы пропорции сохранялись в любом окне.
 */
public final class Hud {

  /** Во сколько раз единица NDC по X больше, чем по Y, в эталонном окне 16:9. */
  private static final float X_PER_Y = 9f / 16f;

  private Hud() {
  }

  public static void drawCrosshair(ShaderProgram shader) {
    UiDrawer.begin();
    // Инверсия цвета под прицелом, как в Minecraft: прицел виден на любом фоне.
    glBlendFunc(GL_ONE_MINUS_DST_COLOR, GL_ZERO);

    float length = 0.022f;
    float thickness = 0.002f;
    UiDrawer.fill(shader, -length * X_PER_Y, -thickness, length * X_PER_Y, thickness, 1f, 1f, 1f, 1f);
    UiDrawer.fill(shader, -thickness * X_PER_Y, -length, thickness * X_PER_Y, -thickness, 1f, 1f, 1f, 1f);
    UiDrawer.fill(shader, -thickness * X_PER_Y, thickness, thickness * X_PER_Y, length, 1f, 1f, 1f, 1f);

    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    UiDrawer.end();
  }

  public static void drawHotbar(ShaderProgram colorShader, ShaderProgram texturedShader, TextureAtlas atlas, Hotbar hotbar) {
    float slotHeight = 0.13f;
    float slotWidth = slotHeight * X_PER_Y;
    float gap = 0.008f;
    float totalWidth = Hotbar.SIZE * slotWidth + (Hotbar.SIZE - 1) * gap;
    float startX = -totalWidth * 0.5f;
    float y = -0.96f;
    float inset = 0.018f;

    UiDrawer.begin();

    for (int i = 0; i < Hotbar.SIZE; i++) {
      float x = startX + i * (slotWidth + gap);
      boolean selected = i == hotbar.getSelectedSlot();

      UiDrawer.fill(colorShader, x, y, x + slotWidth, y + slotHeight, 0.15f, 0.15f, 0.15f, 0.6f);
      if (selected) {
        UiDrawer.outline(colorShader, x - 0.003f, y - 0.005f, x + slotWidth + 0.003f, y + slotHeight + 0.005f,
            1f, 1f, 1f, 1f);
      } else {
        UiDrawer.outline(colorShader, x, y, x + slotWidth, y + slotHeight, 0.05f, 0.05f, 0.05f, 0.8f);
      }
    }

    texturedShader.use();
    texturedShader.setTextureUnit(0);
    texturedShader.setColor(1f, 1f, 1f, 1f);
    glActiveTexture(GL_TEXTURE0);
    atlas.bind();

    for (int i = 0; i < Hotbar.SIZE; i++) {
      float x = startX + i * (slotWidth + gap);
      int icon = Block.getIconIndex(hotbar.getSlot(i));
      UiDrawer.texturedQuad(
          x + inset * X_PER_Y, y + inset, x + slotWidth - inset * X_PER_Y, y + slotHeight - inset,
          TextureAtlas.u0(icon), TextureAtlas.v0(icon), TextureAtlas.u1(icon), TextureAtlas.v1(icon)
      );
    }

    UiDrawer.end();
  }

  public static void drawBreakProgress(ShaderProgram shader, float progress) {
    if (progress <= 0f) {
      return;
    }

    float halfWidth = 0.06f * X_PER_Y;
    float y0 = -0.07f;
    float y1 = y0 + 0.012f;

    UiDrawer.begin();
    UiDrawer.fill(shader, -halfWidth, y0, halfWidth, y1, 0f, 0f, 0f, 0.6f);
    UiDrawer.fill(shader, -halfWidth, y0, -halfWidth + 2f * halfWidth * Math.min(1f, progress), y1,
        1f, 1f, 1f, 0.9f);
    UiDrawer.end();
  }
}
