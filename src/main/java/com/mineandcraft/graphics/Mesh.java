package com.mineandcraft.graphics;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class Mesh {

  public enum Layout {
    BLOCK(6),
    POSITION(3);

    private final int stride;

    Layout(int stride) {
      this.stride = stride;
    }
  }

  private final int vao;
  private final int vbo;
  private final Layout layout;
  private int vertexCount;

  public Mesh(Layout layout) {
    this.layout = layout;

    vao = glGenVertexArrays();
    vbo = glGenBuffers();

    glBindVertexArray(vao);
    glBindBuffer(GL_ARRAY_BUFFER, vbo);

    int stride = layout.stride * Float.BYTES;
    glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
    glEnableVertexAttribArray(0);

    if (layout == Layout.BLOCK) {
      glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
      glEnableVertexAttribArray(1);
      glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5 * Float.BYTES);
      glEnableVertexAttribArray(2);
    }

    glBindVertexArray(0);
  }

  public Mesh() {
    this(Layout.BLOCK);
  }

  public void upload(float[] vertices) {
    vertexCount = vertices.length / layout.stride;

    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
  }

  /** Загружает данные из буфера от его position до limit. */
  public void upload(FloatBuffer vertices) {
    vertexCount = vertices.remaining() / layout.stride;

    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
  }

  public int getVertexCount() {
    return vertexCount;
  }

  public void render() {
    if (vertexCount == 0) {
      return;
    }

    glBindVertexArray(vao);
    glDrawArrays(GL_TRIANGLES, 0, vertexCount);
    glBindVertexArray(0);
  }

  public void renderLines() {
    if (vertexCount == 0) {
      return;
    }

    glBindVertexArray(vao);
    glDrawArrays(GL_LINES, 0, vertexCount);
    glBindVertexArray(0);
  }

  public void delete() {
    glDeleteBuffers(vbo);
    glDeleteVertexArrays(vao);
  }
}
