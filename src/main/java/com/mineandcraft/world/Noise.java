package com.mineandcraft.world;

import java.util.Random;

/**
 * Градиентный шум Перлина (2D и 3D) с таблицей перестановок, зависящей от сида.
 * Потокобезопасен: после создания состояние только читается.
 */
public final class Noise {

  private static final int[][] GRAD3 = {
      {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
      {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
      {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
  };

  private final int[] perm = new int[512];

  public Noise(long seed) {
    int[] p = new int[256];
    for (int i = 0; i < 256; i++) {
      p[i] = i;
    }

    Random random = new Random(seed);
    for (int i = 255; i > 0; i--) {
      int j = random.nextInt(i + 1);
      int tmp = p[i];
      p[i] = p[j];
      p[j] = tmp;
    }

    for (int i = 0; i < 512; i++) {
      perm[i] = p[i & 255];
    }
  }

  /** Значение примерно в диапазоне [-1, 1]. */
  public double noise(double x, double y) {
    int xi = fastFloor(x);
    int yi = fastFloor(y);
    double xf = x - xi;
    double yf = y - yi;
    xi &= 255;
    yi &= 255;

    double u = fade(xf);
    double v = fade(yf);

    int aa = perm[perm[xi] + yi];
    int ab = perm[perm[xi] + yi + 1];
    int ba = perm[perm[xi + 1] + yi];
    int bb = perm[perm[xi + 1] + yi + 1];

    double x1 = lerp(grad(aa, xf, yf, 0), grad(ba, xf - 1, yf, 0), u);
    double x2 = lerp(grad(ab, xf, yf - 1, 0), grad(bb, xf - 1, yf - 1, 0), u);
    return lerp(x1, x2, v) * 1.41;
  }

  /** Значение примерно в диапазоне [-1, 1]. */
  public double noise(double x, double y, double z) {
    int xi = fastFloor(x);
    int yi = fastFloor(y);
    int zi = fastFloor(z);
    double xf = x - xi;
    double yf = y - yi;
    double zf = z - zi;
    xi &= 255;
    yi &= 255;
    zi &= 255;

    double u = fade(xf);
    double v = fade(yf);
    double w = fade(zf);

    int a = perm[xi] + yi;
    int aa = perm[a] + zi;
    int ab = perm[a + 1] + zi;
    int b = perm[xi + 1] + yi;
    int ba = perm[b] + zi;
    int bb = perm[b + 1] + zi;

    double x1 = lerp(grad(perm[aa], xf, yf, zf), grad(perm[ba], xf - 1, yf, zf), u);
    double x2 = lerp(grad(perm[ab], xf, yf - 1, zf), grad(perm[bb], xf - 1, yf - 1, zf), u);
    double y1 = lerp(x1, x2, v);

    x1 = lerp(grad(perm[aa + 1], xf, yf, zf - 1), grad(perm[ba + 1], xf - 1, yf, zf - 1), u);
    x2 = lerp(grad(perm[ab + 1], xf, yf - 1, zf - 1), grad(perm[bb + 1], xf - 1, yf - 1, zf - 1), u);
    double y2 = lerp(x1, x2, v);

    return lerp(y1, y2, w);
  }

  /** Фрактальный шум (fBm), нормализованный примерно к [-1, 1]. */
  public double fractal(double x, double y, int octaves, double lacunarity, double persistence) {
    double sum = 0;
    double amplitude = 1;
    double frequency = 1;
    double norm = 0;

    for (int i = 0; i < octaves; i++) {
      sum += noise(x * frequency, y * frequency) * amplitude;
      norm += amplitude;
      amplitude *= persistence;
      frequency *= lacunarity;
    }

    return sum / norm;
  }

  private static double grad(int hash, double x, double y, double z) {
    int[] g = GRAD3[hash % 12];
    return g[0] * x + g[1] * y + g[2] * z;
  }

  private static double fade(double t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }

  private static double lerp(double a, double b, double t) {
    return a + t * (b - a);
  }

  private static int fastFloor(double value) {
    int i = (int) value;
    return value < i ? i - 1 : i;
  }
}
