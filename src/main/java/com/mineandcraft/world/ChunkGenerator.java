package com.mineandcraft.world;

/**
 * Детерминированный генератор ландшафта. Результат зависит только от сида и координат чанка,
 * поэтому генерацию можно безопасно выполнять в фоновых потоках.
 */
public class ChunkGenerator {

  private static final int TREE_CELL = 5;
  private static final int TREE_RADIUS = 2;

  private final long seed;
  private final Noise continentNoise;
  private final Noise hillNoise;
  private final Noise mountainNoise;
  private final Noise caveNoiseA;
  private final Noise caveNoiseB;
  private final Noise cavernNoise;
  private final Noise oreNoise;
  private final Noise forestNoise;

  public ChunkGenerator(long seed) {
    this.seed = seed;
    continentNoise = new Noise(seed);
    hillNoise = new Noise(seed * 31 + 1);
    mountainNoise = new Noise(seed * 31 + 2);
    caveNoiseA = new Noise(seed * 31 + 3);
    caveNoiseB = new Noise(seed * 31 + 4);
    cavernNoise = new Noise(seed * 31 + 5);
    oreNoise = new Noise(seed * 31 + 6);
    forestNoise = new Noise(seed * 31 + 7);
  }

  public long getSeed() {
    return seed;
  }

  public Chunk generate(int chunkX, int chunkZ) {
    Chunk chunk = new Chunk(chunkX, chunkZ);
    int baseX = chunkX * World.CHUNK_SIZE;
    int baseZ = chunkZ * World.CHUNK_SIZE;

    for (int localX = 0; localX < World.CHUNK_SIZE; localX++) {
      for (int localZ = 0; localZ < World.CHUNK_SIZE; localZ++) {
        int worldX = baseX + localX;
        int worldZ = baseZ + localZ;
        int surface = surfaceHeight(worldX, worldZ);

        for (int y = 0; y <= surface; y++) {
          chunk.setLocal(localX, y, localZ, terrainBlock(worldX, y, worldZ, surface));
        }
      }
    }

    plantTrees(chunk, baseX, baseZ);
    return chunk;
  }

  /** Высота верхнего блока ландшафта (без учёта пещер и деревьев). */
  public int surfaceHeight(int x, int z) {
    double continent = continentNoise.fractal(x / 320.0, z / 320.0, 3, 2.0, 0.5);
    double hills = hillNoise.fractal(x / 72.0, z / 72.0, 4, 2.0, 0.5);
    double ridge = Math.max(0, mountainNoise.fractal(x / 180.0, z / 180.0, 2, 2.0, 0.5) + 0.1);

    double height = World.SEA_LEVEL + 4
        + continent * 14
        + hills * (6 + ridge * 10)
        + ridge * ridge * 70;

    return clamp((int) Math.round(height), 4, World.HEIGHT - 12);
  }

  private byte terrainBlock(int x, int y, int z, int surface) {
    if (y == 0) {
      return Block.BEDROCK;
    }
    if (y <= 3 && hash(x, y, z, 101) % 4 <= 3 - y) {
      return Block.BEDROCK;
    }
    if (isCave(x, y, z, surface)) {
      return Block.AIR;
    }

    boolean beach = surface <= World.SEA_LEVEL + 1;

    if (y == surface) {
      if (surface <= World.SEA_LEVEL - 3) {
        return Block.GRAVEL;
      }
      return beach ? Block.SAND : Block.GRASS;
    }
    if (y >= surface - 3) {
      return beach ? Block.SAND : Block.DIRT;
    }

    return oreOrStone(x, y, z);
  }

  private byte oreOrStone(int x, int y, int z) {
    if (y < 80 && oreNoise.noise(x / 4.0, y / 4.0, z / 4.0) > 0.62) {
      return Block.COAL_ORE;
    }
    if (y < 44 && oreNoise.noise(x / 3.0 + 500, y / 3.0, z / 3.0) > 0.68) {
      return Block.IRON_ORE;
    }
    if (oreNoise.noise(x / 9.0 - 500, y / 9.0, z / 9.0) > 0.7) {
      return Block.GRAVEL;
    }
    return Block.STONE;
  }

  boolean isCave(int x, int y, int z, int surface) {
    if (y <= 3 || y > surface) {
      return false;
    }

    // «Спагетти»-пещеры: пересечение двух изоповерхностей шума даёт длинные извилистые туннели.
    double a = caveNoiseA.noise(x / 42.0, y / 28.0, z / 42.0);
    double b = caveNoiseB.noise(x / 42.0, y / 28.0, z / 42.0);
    if (a * a + b * b < 0.006) {
      return true;
    }

    // Крупные полости только глубоко под поверхностью.
    if (y < surface - 10) {
      return cavernNoise.noise(x / 28.0, y / 16.0, z / 28.0) > 0.6;
    }

    return false;
  }

  private void plantTrees(Chunk chunk, int baseX, int baseZ) {
    int minCellX = Math.floorDiv(baseX - TREE_RADIUS, TREE_CELL);
    int maxCellX = Math.floorDiv(baseX + World.CHUNK_SIZE + TREE_RADIUS, TREE_CELL);
    int minCellZ = Math.floorDiv(baseZ - TREE_RADIUS, TREE_CELL);
    int maxCellZ = Math.floorDiv(baseZ + World.CHUNK_SIZE + TREE_RADIUS, TREE_CELL);

    // Сначала листва всех деревьев, затем стволы: так ствол соседнего дерева не перекроется кроной.
    for (int pass = 0; pass < 2; pass++) {
      for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
        for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
          int treeX = cellX * TREE_CELL + hash(cellX, 0, cellZ, 11) % TREE_CELL;
          int treeZ = cellZ * TREE_CELL + hash(cellX, 0, cellZ, 13) % TREE_CELL;

          if (!hasTree(cellX, cellZ, treeX, treeZ)) {
            continue;
          }

          int surface = surfaceHeight(treeX, treeZ);
          int trunkHeight = 4 + hash(treeX, 0, treeZ, 17) % 3;

          if (pass == 0) {
            placeCrown(chunk, baseX, baseZ, treeX, surface + trunkHeight, treeZ);
          } else {
            for (int y = 1; y <= trunkHeight; y++) {
              setIfInside(chunk, baseX, baseZ, treeX, surface + y, treeZ, Block.WOOD, true);
            }
          }
        }
      }
    }
  }

  private boolean hasTree(int cellX, int cellZ, int treeX, int treeZ) {
    double forest = forestNoise.fractal(treeX / 120.0, treeZ / 120.0, 2, 2.0, 0.5);
    int chance = forest > 0.25 ? 2 : forest > 0.0 ? 5 : 14;
    if (hash(cellX, 1, cellZ, 19) % chance != 0) {
      return false;
    }

    int surface = surfaceHeight(treeX, treeZ);
    if (surface <= World.SEA_LEVEL + 1 || surface > World.HEIGHT - 12) {
      return false;
    }

    // Под деревом должна быть трава, а не вход в пещеру.
    return !isCave(treeX, surface, treeZ, surface) && !isCave(treeX, surface - 1, treeZ, surface);
  }

  private void placeCrown(Chunk chunk, int baseX, int baseZ, int x, int topY, int z) {
    for (int dy = -1; dy <= 1; dy++) {
      int radius = dy == 1 ? 1 : TREE_RADIUS;

      for (int dx = -radius; dx <= radius; dx++) {
        for (int dz = -radius; dz <= radius; dz++) {
          boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
          if (corner && (dy == 1 || hash(x + dx, topY + dy, z + dz, 23) % 2 == 0)) {
            continue;
          }

          setIfInside(chunk, baseX, baseZ, x + dx, topY + dy, z + dz, Block.LEAVES, false);
        }
      }
    }

    setIfInside(chunk, baseX, baseZ, x, topY + 2, z, Block.LEAVES, false);
  }

  private static void setIfInside(Chunk chunk, int baseX, int baseZ, int x, int y, int z, byte block, boolean overwrite) {
    int localX = x - baseX;
    int localZ = z - baseZ;
    if (localX < 0 || localZ < 0 || localX >= World.CHUNK_SIZE || localZ >= World.CHUNK_SIZE
        || y < 0 || y >= World.HEIGHT) {
      return;
    }

    if (overwrite || chunk.getLocal(localX, y, localZ) == Block.AIR) {
      chunk.setLocal(localX, y, localZ, block);
    }
  }

  private int hash(int x, int y, int z, int salt) {
    long value = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L
        ^ seed ^ salt * 0x27D4EB2F165667C5L;
    value = (value ^ (value >>> 33)) * 0xff51afd7ed558ccdL;
    value = (value ^ (value >>> 33)) * 0xc4ceb9fe1a85ec53L;
    value ^= value >>> 33;
    return (int) (value & 0x7fffffff);
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
}
