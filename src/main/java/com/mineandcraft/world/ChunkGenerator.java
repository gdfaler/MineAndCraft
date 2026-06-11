package com.mineandcraft.world;

public class ChunkGenerator {

  private final long seed;

  public ChunkGenerator(long seed) {
    this.seed = seed;
  }

  public void generate(Chunk chunk, World world) {
    if (chunk.isGenerated()) {
      return;
    }

    int baseX = chunk.getChunkX() * World.CHUNK_SIZE;
    int baseZ = chunk.getChunkZ() * World.CHUNK_SIZE;

    for (int localX = 0; localX < World.CHUNK_SIZE; localX++) {
      for (int localZ = 0; localZ < World.CHUNK_SIZE; localZ++) {
        int worldX = baseX + localX;
        int worldZ = baseZ + localZ;
        int surface = world.getSurfaceHeight(worldX, worldZ);

        for (int y = 0; y < World.HEIGHT; y++) {
          byte block;

          if (y == 0) {
            block = Block.BEDROCK;
          } else if (y > surface) {
            block = Block.AIR;
          } else if (isCave(worldX, y, worldZ)) {
            block = Block.AIR;
          } else if (y == surface) {
            block = surface <= World.SEA_LEVEL + 1 ? Block.SAND : Block.GRASS;
          } else if (y >= surface - 4) {
            block = Block.DIRT;
          } else {
            block = Block.STONE;
          }

          chunk.setLocal(localX, y, localZ, block);
        }
      }
    }

    plantTrees(chunk, world, baseX, baseZ);
    chunk.setGenerated();
  }

  private void plantTrees(Chunk chunk, World world, int baseX, int baseZ) {
    for (int localX = 3; localX < World.CHUNK_SIZE - 3; localX++) {
      for (int localZ = 3; localZ < World.CHUNK_SIZE - 3; localZ++) {
        int worldX = baseX + localX;
        int worldZ = baseZ + localZ;

        if (!shouldPlantTree(worldX, worldZ)) {
          continue;
        }

        int surface = world.getSurfaceHeight(worldX, worldZ);
        if (surface < World.SEA_LEVEL + 2 || surface > World.HEIGHT - 10) {
          continue;
        }

        if (chunk.getLocal(localX, surface, localZ) != Block.GRASS) {
          continue;
        }

        int trunkHeight = 4 + hash(worldX, worldZ, 17) % 3;
        for (int y = 1; y <= trunkHeight; y++) {
          world.setBlockQuiet(worldX, surface + y, worldZ, Block.WOOD);
        }

        int crownY = surface + trunkHeight;
        for (int dx = -2; dx <= 2; dx++) {
          for (int dz = -2; dz <= 2; dz++) {
            for (int dy = 0; dy <= 2; dy++) {
              if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && dy > 1) {
                continue;
              }

              int lx = worldX + dx;
              int lz = worldZ + dz;
              int ly = crownY + dy;

              if (world.getBlock(lx, ly, lz) == Block.AIR) {
                world.setBlockQuiet(lx, ly, lz, Block.LEAVES);
              }
            }
          }
        }
      }
    }
  }

  private boolean isCave(int x, int y, int z) {
    if (y <= 1 || y > 48) {
      return false;
    }

    double nx = x * 0.062;
    double ny = y * 0.09;
    double nz = z * 0.062;

    double noise =
        Math.sin(nx + seed * 0.001) * Math.cos(ny) * Math.sin(nz)
            + Math.sin(nx * 0.45 + nz * 0.55 + seed * 0.002) * 0.55;

    return noise > 0.58;
  }

  private boolean shouldPlantTree(int x, int z) {
    return hash(x, z, 31) % 28 == 0;
  }

  private int hash(int x, int z, int salt) {
    long value = x * 7342871L ^ z * 912931L ^ seed ^ salt * 573571L;
    value = (value ^ (value >>> 33)) * 0xff51afd7ed558ccdL;
    value = (value ^ (value >>> 33)) * 0xc4ceb9fe1a85ec53L;
    value ^= value >>> 33;
    return (int) Math.abs(value);
  }
}
