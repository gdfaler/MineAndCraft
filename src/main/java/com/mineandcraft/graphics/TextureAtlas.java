package com.mineandcraft.graphics;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class TextureAtlas {

  public static final int TILE_SIZE = 16;
  public static final int COLS = 4;
  public static final int ROWS = 4;
  public static final int TILE_COUNT = COLS * ROWS;

  private final int id;

  public TextureAtlas() {
    id = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, id);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    int width = TILE_SIZE * COLS;
    int height = TILE_SIZE * ROWS;
    ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);

    copyTile(pixels, 0, loadResourceTile("textures/grass.png"));
    copyTile(pixels, 1, generateGrassSide());
    copyTile(pixels, 2, generateDirt());
    copyTile(pixels, 3, generateStone());
    copyTile(pixels, 4, generateWoodSide());
    copyTile(pixels, 5, generateWoodTop());
    copyTile(pixels, 6, generateLeaves());
    copyTile(pixels, 7, generateSand());
    copyTile(pixels, 8, generateBedrock());
    copyTile(pixels, 9, generatePlanks());

    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
    glBindTexture(GL_TEXTURE_2D, 0);
  }

  private static byte[] loadResourceTile(String resource) {
    STBImage.stbi_set_flip_vertically_on_load(true);

    try (MemoryStack stack = MemoryStack.stackPush()) {
      byte[] bytes = readResource(resource);
      ByteBuffer data = stack.malloc(bytes.length);
      data.put(bytes);
      data.flip();

      IntBuffer w = stack.mallocInt(1);
      IntBuffer h = stack.mallocInt(1);
      IntBuffer c = stack.mallocInt(1);

      ByteBuffer image = STBImage.stbi_load_from_memory(data, w, h, c, 4);
      if (image == null) {
        return generateDirt();
      }

      byte[] tile = new byte[TILE_SIZE * TILE_SIZE * 4];
      int srcW = w.get(0);
      int srcH = h.get(0);

      for (int y = 0; y < TILE_SIZE; y++) {
        for (int x = 0; x < TILE_SIZE; x++) {
          int sx = x * srcW / TILE_SIZE;
          int sy = y * srcH / TILE_SIZE;
          int srcIndex = (sy * srcW + sx) * 4;
          int dstIndex = (y * TILE_SIZE + x) * 4;

          tile[dstIndex] = image.get(srcIndex);
          tile[dstIndex + 1] = image.get(srcIndex + 1);
          tile[dstIndex + 2] = image.get(srcIndex + 2);
          tile[dstIndex + 3] = image.get(srcIndex + 3);
        }
      }

      STBImage.stbi_image_free(image);
      return tile;
    }
  }

  private static byte[] generateGrassSide() {
    byte[] grassTop = loadResourceTile("textures/grass.png");
    byte[] dirt = generateDirt();
    byte[] tile = new byte[TILE_SIZE * TILE_SIZE * 4];

    for (int y = 0; y < TILE_SIZE; y++) {
      for (int x = 0; x < TILE_SIZE; x++) {
        int index = (y * TILE_SIZE + x) * 4;
        byte[] source = y < 4 ? grassTop : dirt;

        tile[index] = source[index];
        tile[index + 1] = source[index + 1];
        tile[index + 2] = source[index + 2];
        tile[index + 3] = (byte) 255;
      }
    }

    return tile;
  }

  private static byte[] generateDirt() {
    return generateNoiseTile(118, 82, 48, 42);
  }

  private static byte[] generateStone() {
    byte[] tile = new byte[TILE_SIZE * TILE_SIZE * 4];
    Random random = new Random(7);

    for (int y = 0; y < TILE_SIZE; y++) {
      for (int x = 0; x < TILE_SIZE; x++) {
        int noise = random.nextInt(24) - 12;
        int index = (y * TILE_SIZE + x) * 4;
        int value = clamp(128 + noise, 0, 255);

        tile[index] = (byte) value;
        tile[index + 1] = (byte) value;
        tile[index + 2] = (byte) clamp(value + 4, 0, 255);
        tile[index + 3] = (byte) 255;
      }
    }

    return tile;
  }

  private static byte[] generateWoodSide() {
    byte[] tile = new byte[TILE_SIZE * TILE_SIZE * 4];
    Random random = new Random(11);

    for (int y = 0; y < TILE_SIZE; y++) {
      for (int x = 0; x < TILE_SIZE; x++) {
        int index = (y * TILE_SIZE + x) * 4;
        int stripe = (x + random.nextInt(2)) % 4 == 0 ? -18 : 0;

        tile[index] = (byte) clamp(102 + stripe, 0, 255);
        tile[index + 1] = (byte) clamp(76 + stripe, 0, 255);
        tile[index + 2] = (byte) clamp(44 + stripe / 2, 0, 255);
        tile[index + 3] = (byte) 255;
      }
    }

    return tile;
  }

  private static byte[] generateWoodTop() {
    byte[] tile = new byte[TILE_SIZE * TILE_SIZE * 4];
    int centerX = TILE_SIZE / 2;
    int centerY = TILE_SIZE / 2;

    for (int y = 0; y < TILE_SIZE; y++) {
      for (int x = 0; x < TILE_SIZE; x++) {
        int index = (y * TILE_SIZE + x) * 4;
        int dist = (int) Math.sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY));
        int ring = dist % 3 == 0 ? 12 : 0;

        tile[index] = (byte) clamp(140 + ring, 0, 255);
        tile[index + 1] = (byte) clamp(108 + ring, 0, 255);
        tile[index + 2] = (byte) clamp(62 + ring / 2, 0, 255);
        tile[index + 3] = (byte) 255;
      }
    }

    return tile;
  }

  private static byte[] generateLeaves() {
    byte[] tile = new byte[TILE_SIZE * TILE_SIZE * 4];
    Random random = new Random(19);

    for (int y = 0; y < TILE_SIZE; y++) {
      for (int x = 0; x < TILE_SIZE; x++) {
        int index = (y * TILE_SIZE + x) * 4;
        if (random.nextInt(5) == 0) {
          tile[index + 3] = 0;
          continue;
        }

        int noise = random.nextInt(20) - 10;
        tile[index] = (byte) clamp(55 + noise, 0, 255);
        tile[index + 1] = (byte) clamp(115 + noise, 0, 255);
        tile[index + 2] = (byte) clamp(35 + noise / 2, 0, 255);
        tile[index + 3] = (byte) 255;
      }
    }

    return tile;
  }

  private static byte[] generateSand() {
    return generateNoiseTile(210, 198, 128, 23);
  }

  private static byte[] generateBedrock() {
    byte[] tile = new byte[TILE_SIZE * TILE_SIZE * 4];
    Random random = new Random(3);

    for (int y = 0; y < TILE_SIZE; y++) {
      for (int x = 0; x < TILE_SIZE; x++) {
        int index = (y * TILE_SIZE + x) * 4;
        int dark = ((x + y) % 3 == 0) ? -25 : 0;

        tile[index] = (byte) clamp(55 + dark, 0, 255);
        tile[index + 1] = (byte) clamp(55 + dark, 0, 255);
        tile[index + 2] = (byte) clamp(55 + dark, 0, 255);
        tile[index + 3] = (byte) 255;
      }
    }

    return tile;
  }

  private static byte[] generatePlanks() {
    byte[] tile = new byte[TILE_SIZE * TILE_SIZE * 4];

    for (int y = 0; y < TILE_SIZE; y++) {
      for (int x = 0; x < TILE_SIZE; x++) {
        int index = (y * TILE_SIZE + x) * 4;
        int plank = (y / 4) % 2 == 0 ? 8 : -6;
        int seam = y % 4 == 0 ? -18 : 0;

        tile[index] = (byte) clamp(162 + plank + seam, 0, 255);
        tile[index + 1] = (byte) clamp(128 + plank + seam, 0, 255);
        tile[index + 2] = (byte) clamp(76 + plank / 2 + seam, 0, 255);
        tile[index + 3] = (byte) 255;
      }
    }

    return tile;
  }

  private static byte[] generateNoiseTile(int r, int g, int b, int seed) {
    byte[] tile = new byte[TILE_SIZE * TILE_SIZE * 4];
    Random random = new Random(seed);

    for (int y = 0; y < TILE_SIZE; y++) {
      for (int x = 0; x < TILE_SIZE; x++) {
        int noise = random.nextInt(28) - 14;
        int index = (y * TILE_SIZE + x) * 4;

        tile[index] = (byte) clamp(r + noise, 0, 255);
        tile[index + 1] = (byte) clamp(g + noise, 0, 255);
        tile[index + 2] = (byte) clamp(b + noise / 2, 0, 255);
        tile[index + 3] = (byte) 255;
      }
    }

    return tile;
  }

  private static void copyTile(ByteBuffer atlas, int tileIndex, byte[] tile) {
    int col = tileIndex % COLS;
    int row = tileIndex / COLS;
    int offsetX = col * TILE_SIZE;
    int offsetY = row * TILE_SIZE;
    int atlasWidth = TILE_SIZE * COLS;

    for (int y = 0; y < TILE_SIZE; y++) {
      for (int x = 0; x < TILE_SIZE; x++) {
        int src = (y * TILE_SIZE + x) * 4;
        int dst = ((offsetY + y) * atlasWidth + offsetX + x) * 4;

        atlas.put(dst, tile[src]);
        atlas.put(dst + 1, tile[src + 1]);
        atlas.put(dst + 2, tile[src + 2]);
        atlas.put(dst + 3, tile[src + 3]);
      }
    }
  }

  private static byte[] readResource(String classpathResource) {
    String path = classpathResource.startsWith("/") ? classpathResource : "/" + classpathResource;

    try (InputStream in = TextureAtlas.class.getResourceAsStream(path)) {
      if (in == null) {
        throw new RuntimeException("Resource not found: " + path);
      }
      return in.readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException("Failed to read texture: " + path, e);
    }
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  public void bind() {
    glBindTexture(GL_TEXTURE_2D, id);
  }

  public float u0(int atlasIndex) {
    return (atlasIndex % COLS) / (float) COLS;
  }

  public float u1(int atlasIndex) {
    return (atlasIndex % COLS + 1) / (float) COLS;
  }

  public float v0(int atlasIndex) {
    return (atlasIndex / COLS) / (float) ROWS;
  }

  public float v1(int atlasIndex) {
    return (atlasIndex / COLS + 1) / (float) ROWS;
  }
}
