package com.mineandcraft.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class FontRenderer {

  private record Glyph(float u0, float v0, float u1, float v1, float width, float height) {
  }

  private static final String CHARSET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.: -/%+"
          + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";

  private final int textureId;
  private final int textureWidth;
  private final int textureHeight;
  private final float pixelHeight;
  private final Map<Character, Glyph> glyphs = new HashMap<>();

  public FontRenderer(int fontSize) {
    Font font = new Font("SansSerif", Font.BOLD, fontSize);
    BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setFont(font);
    graphics.setColor(Color.WHITE);
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

    FontMetrics metrics = graphics.getFontMetrics();
    pixelHeight = metrics.getHeight();

    int x = 2;
    int y = metrics.getAscent() + 2;
    int rowHeight = metrics.getHeight() + 4;

    for (char character : CHARSET.toCharArray()) {
      int charWidth = Math.max(1, metrics.charWidth(character));
      if (x + charWidth + 4 >= image.getWidth()) {
        x = 2;
        y += rowHeight;
      }

      graphics.drawString(String.valueOf(character), x, y);
      glyphs.put(character, new Glyph(
          x / (float) image.getWidth(),
          (y - metrics.getAscent()) / (float) image.getHeight(),
          (x + charWidth) / (float) image.getWidth(),
          (y - metrics.getAscent() + metrics.getHeight()) / (float) image.getHeight(),
          charWidth,
          metrics.getHeight()
      ));

      x += charWidth + 4;
    }

    graphics.dispose();

    textureWidth = image.getWidth();
    textureHeight = image.getHeight();
    int[] pixels = new int[textureWidth * textureHeight];
    image.getRGB(0, 0, textureWidth, textureHeight, pixels, 0, textureWidth);

    ByteBuffer buffer = BufferUtils.createByteBuffer(textureWidth * textureHeight * 4);
    for (int pixel : pixels) {
      buffer.put((byte) ((pixel >> 16) & 0xFF));
      buffer.put((byte) ((pixel >> 8) & 0xFF));
      buffer.put((byte) (pixel & 0xFF));
      buffer.put((byte) ((pixel >> 24) & 0xFF));
    }
    buffer.flip();

    textureId = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, textureId);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textureWidth, textureHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
    glBindTexture(GL_TEXTURE_2D, 0);
  }

  public float measureText(String text, float scale) {
    float width = 0f;

    for (int i = 0; i < text.length(); i++) {
      Glyph glyph = glyphs.get(text.charAt(i));
      if (glyph != null) {
        width += glyph.width * scale;
      }
    }

    return width;
  }

  public void drawText(
      ShaderProgram shader,
      String text,
      float x,
      float y,
      float scale,
      float r,
      float g,
      float b,
      float a
  ) {
    if (text == null || text.isEmpty()) {
      return;
    }

    shader.use();
    shader.setTextureUnit(0);
    shader.setColor(r, g, b, a);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);

    float cursorX = x;
    float glyphScale = scale / pixelHeight;

    for (int i = 0; i < text.length(); i++) {
      Glyph glyph = glyphs.get(text.charAt(i));
      if (glyph == null) {
        continue;
      }

      float w = glyph.width * glyphScale;
      float h = glyph.height * glyphScale;
      drawGlyph(shader, cursorX, y, cursorX + w, y + h, glyph);
      cursorX += w;
    }
  }

  private void drawGlyph(ShaderProgram shader, float x0, float y0, float x1, float y1, Glyph glyph) {
    float[] vertices = {
        x0, y0, glyph.u0, glyph.v0,
        x1, y0, glyph.u1, glyph.v0,
        x1, y1, glyph.u1, glyph.v1,
        x0, y0, glyph.u0, glyph.v0,
        x1, y1, glyph.u1, glyph.v1,
        x0, y1, glyph.u0, glyph.v1,
    };

    int vao = glGenVertexArrays();
    int vbo = glGenBuffers();

    glBindVertexArray(vao);
    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
    glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
    glEnableVertexAttribArray(1);
    glDrawArrays(GL_TRIANGLES, 0, 6);
    glBindVertexArray(0);

    glDeleteBuffers(vbo);
    glDeleteVertexArrays(vao);
  }

  public void delete() {
    glDeleteTextures(textureId);
  }
}
