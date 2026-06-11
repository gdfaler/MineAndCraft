package com.mineandcraft.graphics;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class UiDrawer {

  private UiDrawer() {
  }

  public static void begin() {
    glDisable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
  }

  public static void end() {
    glDisable(GL_BLEND);
    glEnable(GL_DEPTH_TEST);
  }

  public static void panel(ShaderProgram shader, float x0, float y0, float x1, float y1, float alpha) {
    fill(shader, x0, y0, x1, y1, 0.08f, 0.08f, 0.1f, alpha);
    outline(shader, x0, y0, x1, y1, 0.75f, 0.75f, 0.8f, 0.9f);
  }

  public static void fill(
      ShaderProgram shader,
      float x0, float y0, float x1, float y1,
      float r, float g, float b, float a
  ) {
    float[] vertices = {
        x0, y0,  x1, y0,  x1, y1,
        x0, y0,  x1, y1,  x0, y1,
    };

    draw(shader, vertices, r, g, b, a);
  }

  public static void outline(
      ShaderProgram shader,
      float x0, float y0, float x1, float y1,
      float r, float g, float b, float a
  ) {
    float t = 0.0025f;
    fill(shader, x0, y1 - t, x1, y1, r, g, b, a);
    fill(shader, x0, y0, x1, y0 + t, r, g, b, a);
    fill(shader, x0, y0, x0 + t, y1, r, g, b, a);
    fill(shader, x1 - t, y0, x1, y1, r, g, b, a);
  }

  private static void draw(ShaderProgram shader, float[] vertices, float r, float g, float b, float a) {
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
}
