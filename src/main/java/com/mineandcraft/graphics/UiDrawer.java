package com.mineandcraft.graphics;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * Рисование 2D-примитивов интерфейса в координатах NDC. Использует два общих VAO/VBO
 * (цветной и текстурный), которые создаются при первом использовании и переиспользуются.
 */
public final class UiDrawer {

  private static int colorVao;
  private static int colorVbo;
  private static int texturedVao;
  private static int texturedVbo;
  private static FloatBuffer staging;
  private static final float[] QUAD = new float[12];
  private static final float[] TEXTURED_QUAD = new float[24];

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
    float[] v = QUAD;
    v[0] = x0; v[1] = y0;  v[2] = x1; v[3] = y0;   v[4] = x1; v[5] = y1;
    v[6] = x0; v[7] = y0;  v[8] = x1; v[9] = y1;   v[10] = x0; v[11] = y1;

    shader.use();
    shader.setColor(r, g, b, a);
    drawColored(v, 6);
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

  /** Текстурированный прямоугольник; текстура и шейдер должны быть уже привязаны. */
  public static void texturedQuad(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1) {
    float[] v = TEXTURED_QUAD;
    int i = 0;
    i = putVertex(v, i, x0, y0, u0, v0);
    i = putVertex(v, i, x1, y0, u1, v0);
    i = putVertex(v, i, x1, y1, u1, v1);
    i = putVertex(v, i, x0, y0, u0, v0);
    i = putVertex(v, i, x1, y1, u1, v1);
    putVertex(v, i, x0, y1, u0, v1);
    drawTextured(v, 6);
  }

  static int putVertex(float[] target, int index, float x, float y, float u, float v) {
    target[index] = x;
    target[index + 1] = y;
    target[index + 2] = u;
    target[index + 3] = v;
    return index + 4;
  }

  static void drawColored(float[] vertices, int vertexCount) {
    if (colorVao == 0) {
      colorVao = glGenVertexArrays();
      colorVbo = glGenBuffers();
      glBindVertexArray(colorVao);
      glBindBuffer(GL_ARRAY_BUFFER, colorVbo);
      glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
      glEnableVertexAttribArray(0);
    }

    glBindVertexArray(colorVao);
    glBindBuffer(GL_ARRAY_BUFFER, colorVbo);
    upload(vertices, vertexCount * 2);
    glDrawArrays(GL_TRIANGLES, 0, vertexCount);
    glBindVertexArray(0);
  }

  /** Вершины в формате x, y, u, v. */
  static void drawTextured(float[] vertices, int vertexCount) {
    if (texturedVao == 0) {
      texturedVao = glGenVertexArrays();
      texturedVbo = glGenBuffers();
      glBindVertexArray(texturedVao);
      glBindBuffer(GL_ARRAY_BUFFER, texturedVbo);
      glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
      glEnableVertexAttribArray(0);
      glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
      glEnableVertexAttribArray(1);
    }

    glBindVertexArray(texturedVao);
    glBindBuffer(GL_ARRAY_BUFFER, texturedVbo);
    upload(vertices, vertexCount * 4);
    glDrawArrays(GL_TRIANGLES, 0, vertexCount);
    glBindVertexArray(0);
  }

  private static void upload(float[] vertices, int floatCount) {
    if (staging == null || staging.capacity() < floatCount) {
      if (staging != null) {
        MemoryUtil.memFree(staging);
      }
      staging = MemoryUtil.memAllocFloat(Math.max(1024, floatCount * 2));
    }

    staging.clear();
    staging.put(vertices, 0, floatCount).flip();
    glBufferData(GL_ARRAY_BUFFER, staging, GL_STREAM_DRAW);
  }

  public static void delete() {
    if (colorVao != 0) {
      glDeleteBuffers(colorVbo);
      glDeleteVertexArrays(colorVao);
      colorVao = 0;
    }
    if (texturedVao != 0) {
      glDeleteBuffers(texturedVbo);
      glDeleteVertexArrays(texturedVao);
      texturedVao = 0;
    }
    if (staging != null) {
      MemoryUtil.memFree(staging);
      staging = null;
    }
  }
}
