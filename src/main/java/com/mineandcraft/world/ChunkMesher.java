package com.mineandcraft.world;

import com.mineandcraft.graphics.TextureAtlas;

import java.util.Arrays;

/**
 * Строит вершины меша чанка: x, y, z, u, v, яркость (FLOATS_PER_VERTEX float на вершину).
 * Рисуются только грани, соседствующие с непрозрачными блоками, вершины затеняются
 * ambient occlusion. Класс не обращается к OpenGL и не потокобезопасен (переиспользует буферы).
 */
public class ChunkMesher {

  public static final int FLOATS_PER_VERTEX = 6;

  private static final int S = World.CHUNK_SIZE;
  private static final int PS = S + 2;
  private static final int PH = World.HEIGHT + 2;
  private static final float[] AO_LEVELS = {0.45f, 0.65f, 0.82f, 1.0f};

  /**
   * Углы каждой грани (в порядке обхода многоугольника) и UV для них.
   * Порядок совпадает с BlockFace: TOP, BOTTOM, NORTH, SOUTH, EAST, WEST.
   */
  private static final int[][][] CORNERS = {
      {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}},
      {{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}},
      {{1, 0, 0}, {0, 0, 0}, {0, 1, 0}, {1, 1, 0}},
      {{0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}},
      {{1, 0, 1}, {1, 0, 0}, {1, 1, 0}, {1, 1, 1}},
      {{0, 0, 0}, {0, 0, 1}, {0, 1, 1}, {0, 1, 0}},
  };

  /** Для каждого угла: 0 — u0/v0, 1 — u1/v1. */
  private static final int[][][] UVS = {
      {{0, 0}, {0, 1}, {1, 1}, {1, 0}},
      {{0, 0}, {0, 1}, {1, 1}, {1, 0}},
      {{0, 0}, {1, 0}, {1, 1}, {0, 1}},
      {{0, 0}, {1, 0}, {1, 1}, {0, 1}},
      {{0, 0}, {1, 0}, {1, 1}, {0, 1}},
      {{0, 0}, {1, 0}, {1, 1}, {0, 1}},
  };

  private static final BlockFace[] FACES = BlockFace.values();
  private static final int[] SPLIT_A = {0, 1, 2, 0, 2, 3};
  private static final int[] SPLIT_B = {1, 2, 3, 1, 3, 0};

  private final byte[] padded = new byte[PS * PH * PS];
  private float[] out = new float[1 << 16];
  private int size;

  private final int[] ao = new int[4];

  /**
   * @param neighborhood 3x3 чанков, индекс [dx + 1][dz + 1]; центр обязателен,
   *                     отсутствующие соседи считаются воздухом
   */
  public void build(Chunk[][] neighborhood) {
    Chunk center = neighborhood[1][1];
    fillPadded(neighborhood);
    size = 0;

    int originX = center.getChunkX() * S;
    int originZ = center.getChunkZ() * S;

    for (int y = 0; y < World.HEIGHT; y++) {
      for (int z = 0; z < S; z++) {
        for (int x = 0; x < S; x++) {
          byte block = at(x, y, z);
          if (block == Block.AIR) {
            continue;
          }

          for (int f = 0; f < FACES.length; f++) {
            BlockFace face = FACES[f];
            byte neighbor = at(x + face.dx, y + face.dy, z + face.dz);
            if (!isFaceVisible(block, neighbor)) {
              continue;
            }

            emitFace(block, f, face, x, y, z, originX, originZ);
          }
        }
      }
    }
  }

  public float[] vertices() {
    return out;
  }

  /** Количество заполненных float в {@link #vertices()}. */
  public int size() {
    return size;
  }

  public int vertexCount() {
    return size / FLOATS_PER_VERTEX;
  }

  private static boolean isFaceVisible(byte block, byte neighbor) {
    if (Block.isOpaque(neighbor)) {
      return false;
    }
    // Стекло к стеклу не рисуем, иначе видны внутренние стенки у блоков стекла.
    return !(block == Block.GLASS && neighbor == Block.GLASS);
  }

  private void emitFace(byte block, int f, BlockFace face, int x, int y, int z, int originX, int originZ) {
    int tile = Block.getAtlasIndex(block, face);
    float u0 = TextureAtlas.u0(tile);
    float u1 = TextureAtlas.u1(tile);
    float v0 = TextureAtlas.v0(tile);
    float v1 = TextureAtlas.v1(tile);

    int[][] corners = CORNERS[f];
    int[][] uvs = UVS[f];
    for (int i = 0; i < 4; i++) {
      ao[i] = ambientOcclusion(face, x, y, z, corners[i]);
    }

    ensureCapacity(6 * FLOATS_PER_VERTEX);

    // Делим четырёхугольник по более светлой диагонали, чтобы тени не тянулись полосой.
    int[] order = ao[0] + ao[2] >= ao[1] + ao[3] ? SPLIT_A : SPLIT_B;

    for (int i : order) {
      int[] corner = corners[i];
      out[size++] = originX + x + corner[0];
      out[size++] = y + corner[1];
      out[size++] = originZ + z + corner[2];
      out[size++] = uvs[i][0] == 0 ? u0 : u1;
      out[size++] = uvs[i][1] == 0 ? v0 : v1;
      out[size++] = face.shade * AO_LEVELS[ao[i]];
    }
  }

  /** 0 — максимально затенённый угол, 3 — без затенения. */
  private int ambientOcclusion(BlockFace face, int x, int y, int z, int[] corner) {
    int nx = x + face.dx;
    int ny = y + face.dy;
    int nz = z + face.dz;

    // Смещения вдоль двух осей, лежащих в плоскости грани, в сторону угла.
    int ax = 0;
    int ay = 0;
    int az = 0;
    int bx = 0;
    int by = 0;
    int bz = 0;

    if (face.dy != 0) {
      ax = corner[0] == 1 ? 1 : -1;
      bz = corner[2] == 1 ? 1 : -1;
    } else if (face.dz != 0) {
      ax = corner[0] == 1 ? 1 : -1;
      by = corner[1] == 1 ? 1 : -1;
    } else {
      az = corner[2] == 1 ? 1 : -1;
      by = corner[1] == 1 ? 1 : -1;
    }

    boolean side1 = Block.isOpaque(at(nx + ax, ny + ay, nz + az));
    boolean side2 = Block.isOpaque(at(nx + bx, ny + by, nz + bz));
    if (side1 && side2) {
      return 0;
    }

    boolean diagonal = Block.isOpaque(at(nx + ax + bx, ny + ay + by, nz + az + bz));
    return 3 - ((side1 ? 1 : 0) + (side2 ? 1 : 0) + (diagonal ? 1 : 0));
  }

  private void ensureCapacity(int extra) {
    if (size + extra > out.length) {
      out = Arrays.copyOf(out, Math.max(out.length * 2, size + extra));
    }
  }

  /** Блок в локальных координатах центрального чанка, допускаются значения от -1 до S. */
  private byte at(int x, int y, int z) {
    return padded[((y + 1) * PS + (z + 1)) * PS + (x + 1)];
  }

  private void fillPadded(Chunk[][] neighborhood) {
    Arrays.fill(padded, Block.AIR);

    for (int py = 0; py < PH; py++) {
      int y = py - 1;

      for (int pz = 0; pz < PS; pz++) {
        int z = pz - 1;
        int chunkDz = z < 0 ? -1 : z >= S ? 1 : 0;

        for (int px = 0; px < PS; px++) {
          int x = px - 1;
          int chunkDx = x < 0 ? -1 : x >= S ? 1 : 0;
          int index = (py * PS + pz) * PS + px;

          if (y < 0) {
            // Под миром считаем камень: нижние грани бедрока никогда не видны.
            padded[index] = Block.STONE;
            continue;
          }
          if (y >= World.HEIGHT) {
            continue;
          }

          Chunk chunk = neighborhood[chunkDx + 1][chunkDz + 1];
          if (chunk != null) {
            padded[index] = chunk.getLocal(x - chunkDx * S, y, z - chunkDz * S);
          }
        }
      }
    }
  }
}
