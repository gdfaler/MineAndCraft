package com.mineandcraft.graphics;

import com.mineandcraft.player.Hotbar;
import com.mineandcraft.world.Block;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL15.*;

public class Hud {

  private final int crosshairVao;
  private final int crosshairVbo;
  private final int quadVao;
  private final int quadVbo;

  public Hud() {
    float size = 0.008f;
    float gap = 0.003f;

    float[] crosshair = {
        -size, -gap,   size, gap,
         size, -gap,   size, gap,
        -size,  gap,  -size, gap,
         size,  gap,   size, gap,
    };

    crosshairVao = glGenVertexArrays();
    crosshairVbo = glGenBuffers();
    glBindVertexArray(crosshairVao);
    glBindBuffer(GL_ARRAY_BUFFER, crosshairVbo);
    glBufferData(GL_ARRAY_BUFFER, crosshair, GL_STATIC_DRAW);
    glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
    glEnableVertexAttribArray(0);
    glBindVertexArray(0);

    quadVao = glGenVertexArrays();
    quadVbo = glGenBuffers();
    glBindVertexArray(quadVao);
    glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
    glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
    glEnableVertexAttribArray(1);
    glBindVertexArray(0);
  }

  public void drawCrosshair(ShaderProgram shader) {
    shader.use();
    shader.setColor(1f, 1f, 1f, 0.95f);

    glDisable(GL_DEPTH_TEST);
    glBindVertexArray(crosshairVao);
    glDrawArrays(GL_LINES, 0, 4);
    glBindVertexArray(0);
    glEnable(GL_DEPTH_TEST);
  }

  public void drawHotbar(ShaderProgram colorShader, ShaderProgram texturedShader, TextureAtlas atlas, Hotbar hotbar) {
    float slotSize = 0.075f;
    float gap = 0.008f;
    float totalWidth = Hotbar.SIZE * slotSize + (Hotbar.SIZE - 1) * gap;
    float startX = -totalWidth * 0.5f;
    float y = -0.9f;

    glDisable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    for (int i = 0; i < Hotbar.SIZE; i++) {
      float x = startX + i * (slotSize + gap);
      float border = i == hotbar.getSelectedSlot() ? 0.004f : 0.002f;
      float borderAlpha = i == hotbar.getSelectedSlot() ? 1f : 0.55f;

      drawColoredQuad(colorShader, x - border, y - border, x + slotSize + border, y + slotSize + border,
          0f, 0f, 0f, 0.72f * borderAlpha);
      drawColoredQuad(colorShader, x, y, x + slotSize, y + slotSize, 0.55f, 0.55f, 0.55f, 0.92f);

      byte block = hotbar.getSlot(i);
      int icon = Block.getIconIndex(block);
      drawTexturedQuad(texturedShader, atlas, x + 0.01f, y + 0.01f, x + slotSize - 0.01f, y + slotSize - 0.01f,
          atlas.u0(icon), atlas.v0(icon), atlas.u1(icon), atlas.v1(icon));
    }

    glDisable(GL_BLEND);
    glEnable(GL_DEPTH_TEST);
  }

  public void drawBreakProgress(ShaderProgram shader, float progress) {
    if (progress <= 0f) {
      return;
    }

    float size = 0.035f;
    float filled = size * progress;

    shader.use();
    glDisable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    shader.setColor(1f, 1f, 1f, 0.85f);
    drawColoredQuad(shader, -size, -size - 0.06f, size, -size - 0.06f + 0.006f, 1f, 1f, 1f, 0.85f);
    shader.setColor(0.2f, 0.2f, 0.2f, 0.9f);
    drawColoredQuad(shader, -size, -size - 0.06f, -size + filled * 2f, -size - 0.06f + 0.006f, 0.2f, 0.2f, 0.2f, 0.9f);

    glDisable(GL_BLEND);
    glEnable(GL_DEPTH_TEST);
  }

  public void drawPauseOverlay(ShaderProgram shader) {
    shader.use();
    shader.setColor(0f, 0f, 0f, 0.45f);

    glDisable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    drawColoredQuad(shader, -1f, -1f, 1f, 1f, 0f, 0f, 0f, 0.45f);
    glDisable(GL_BLEND);
    glEnable(GL_DEPTH_TEST);
  }

  private void drawColoredQuad(
      ShaderProgram shader,
      float x0, float y0, float x1, float y1,
      float r, float g, float b, float a
  ) {
    float[] vertices = {
        x0, y0,  x1, y0,  x1, y1,
        x0, y0,  x1, y1,  x0, y1,
    };

    shader.use();
    shader.setColor(r, g, b, a);

    int vao = glGenVertexArrays();
    int vbo = glGenBuffers();

    glBindVertexArray(vao);
    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
    glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
    glEnableVertexAttribArray(0);
    glDrawArrays(GL_TRIANGLES, 0, 6);
    glBindVertexArray(0);

    glDeleteBuffers(vbo);
    glDeleteVertexArrays(vao);
  }

  private void drawTexturedQuad(
      ShaderProgram shader,
      TextureAtlas atlas,
      float x0, float y0, float x1, float y1,
      float u0, float v0, float u1, float v1
  ) {
    float[] vertices = {
        x0, y0, u0, v0,
        x1, y0, u1, v0,
        x1, y1, u1, v1,
        x0, y0, u0, v0,
        x1, y1, u1, v1,
        x0, y1, u0, v1,
    };

    shader.use();
    shader.setTextureUnit(0);
    shader.setColor(1f, 1f, 1f, 1f);
    glActiveTexture(GL_TEXTURE0);
    atlas.bind();

    glBindVertexArray(quadVao);
    glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
    glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
    glDrawArrays(GL_TRIANGLES, 0, 6);
    glBindVertexArray(0);
  }

  public void delete() {
    glDeleteBuffers(crosshairVbo);
    glDeleteVertexArrays(crosshairVao);
    glDeleteBuffers(quadVbo);
    glDeleteVertexArrays(quadVao);
  }
}
