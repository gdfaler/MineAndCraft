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
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,:;!?()[]<>=_'\" -/%+#|—×"
          + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";

  private final int textureId;
  private final int textureWidth;
  private final int textureHeight;
  private final float pixelHeight;
  private final Map<Character, Glyph> glyphs = new HashMap<>();
  private float[] batch = new float[24 * 64];

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
        width += glyph.width * scale / pixelHeight;
      }
    }

    return width;
  }

  /** Рисует текст; левый нижний угол первой строки в (x, y). '\n' переносит строку вниз. */
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

    int needed = text.length() * 24;
    if (batch.length < needed) {
      batch = new float[needed];
    }

    float cursorX = x;
    float cursorY = y;
    float glyphScale = scale / pixelHeight;
    int index = 0;

    for (int i = 0; i < text.length(); i++) {
      char character = text.charAt(i);
      if (character == '\n') {
        cursorX = x;
        cursorY -= scale * 1.15f;
        continue;
      }

      Glyph glyph = glyphs.get(character);
      if (glyph == null) {
        continue;
      }

      float w = glyph.width * glyphScale;
      float h = glyph.height * glyphScale;
      float x1 = cursorX + w;
      float y1 = cursorY + h;

      // В атласе строки идут сверху вниз (v0 — верх глифа), а ось Y экрана направлена вверх.
      index = UiDrawer.putVertex(batch, index, cursorX, cursorY, glyph.u0, glyph.v1);
      index = UiDrawer.putVertex(batch, index, x1, cursorY, glyph.u1, glyph.v1);
      index = UiDrawer.putVertex(batch, index, x1, y1, glyph.u1, glyph.v0);
      index = UiDrawer.putVertex(batch, index, cursorX, cursorY, glyph.u0, glyph.v1);
      index = UiDrawer.putVertex(batch, index, x1, y1, glyph.u1, glyph.v0);
      index = UiDrawer.putVertex(batch, index, cursorX, y1, glyph.u0, glyph.v0);
      cursorX = x1;
    }

    if (index > 0) {
      UiDrawer.drawTextured(batch, index / 4);
    }
  }

  public void delete() {
    glDeleteTextures(textureId);
  }
}
