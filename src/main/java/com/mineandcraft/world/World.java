package com.mineandcraft.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;

public class World {

  public static final int SIZE = 128;
  public static final int HEIGHT = 64;
  public static final int CHUNK_SIZE = 16;
  public static final int SEA_LEVEL = 20;

  private final byte[][][] blocks = new byte[SIZE][HEIGHT][SIZE];
  private final List<IntConsumer> chunkListeners = new ArrayList<>();
  private final Random random = new Random(2026);

  public World() {
    generate();
  }

  public void addChunkListener(IntConsumer listener) {
    chunkListeners.add(listener);
  }

  private void generate() {
    for (int x = 0; x < SIZE; x++) {
      for (int z = 0; z < SIZE; z++) {
        int surface = getSurfaceHeight(x, z);

        for (int y = 0; y < HEIGHT; y++) {
          if (y == 0) {
            blocks[x][y][z] = Block.BEDROCK;
          } else if (y > surface) {
            blocks[x][y][z] = Block.AIR;
          } else if (y == surface) {
            blocks[x][y][z] = surface <= SEA_LEVEL + 1 ? Block.SAND : Block.GRASS;
          } else if (y >= surface - 4) {
            blocks[x][y][z] = Block.DIRT;
          } else {
            blocks[x][y][z] = Block.STONE;
          }
        }
      }
    }

    carveCaves();
    plantTrees();
  }

  private void carveCaves() {
    for (int i = 0; i < 60; i++) {
      float x = random.nextInt(SIZE);
      float y = 6 + random.nextInt(28);
      float z = random.nextInt(SIZE);

      int length = 18 + random.nextInt(24);
      float dx = random.nextFloat() * 2f - 1f;
      float dy = random.nextFloat() * 0.4f - 0.2f;
      float dz = random.nextFloat() * 2f - 1f;

      float lengthXZ = (float) Math.sqrt(dx * dx + dz * dz);
      if (lengthXZ < 0.01f) {
        continue;
      }

      dx /= lengthXZ;
      dz /= lengthXZ;

      for (int step = 0; step < length; step++) {
        carveSphere((int) x, (int) y, (int) z, 1 + random.nextInt(2));

        x += dx;
        y += dy;
        z += dz;

        dx += (random.nextFloat() - 0.5f) * 0.35f;
        dy += (random.nextFloat() - 0.5f) * 0.15f;
        dz += (random.nextFloat() - 0.5f) * 0.35f;

        lengthXZ = (float) Math.sqrt(dx * dx + dz * dz);
        if (lengthXZ > 0.01f) {
          dx /= lengthXZ;
          dz /= lengthXZ;
        }

        if (y < 2 || y > HEIGHT - 6) {
          break;
        }
      }
    }
  }

  private void carveSphere(int cx, int cy, int cz, int radius) {
    for (int x = cx - radius; x <= cx + radius; x++) {
      for (int y = cy - radius; y <= cy + radius; y++) {
        for (int z = cz - radius; z <= cz + radius; z++) {
          if (x < 0 || z < 0 || x >= SIZE || z >= SIZE || y <= 0 || y >= HEIGHT) {
            continue;
          }

          int dx = x - cx;
          int dy = y - cy;
          int dz = z - cz;
          if (dx * dx + dy * dy + dz * dz <= radius * radius + 1) {
            if (blocks[x][y][z] != Block.BEDROCK) {
              setBlockInternal(x, y, z, Block.AIR);
            }
          }
        }
      }
    }
  }

  private void plantTrees() {
    for (int x = 3; x < SIZE - 3; x++) {
      for (int z = 3; z < SIZE - 3; z++) {
        if (random.nextInt(28) != 0) {
          continue;
        }

        int surface = getSurfaceHeight(x, z);
        if (surface < SEA_LEVEL + 2 || surface > HEIGHT - 10) {
          continue;
        }

        if (getBlock(x, surface, z) != Block.GRASS) {
          continue;
        }

        int trunkHeight = 4 + random.nextInt(3);
        for (int y = 1; y <= trunkHeight; y++) {
          setBlockInternal(x, surface + y, z, Block.WOOD);
        }

        int crownY = surface + trunkHeight;
        for (int dx = -2; dx <= 2; dx++) {
          for (int dz = -2; dz <= 2; dz++) {
            for (int dy = 0; dy <= 2; dy++) {
              if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && dy > 1) {
                continue;
              }

              int lx = x + dx;
              int lz = z + dz;
              int ly = crownY + dy;

              if (getBlock(lx, ly, lz) == Block.AIR) {
                setBlockInternal(lx, ly, lz, Block.LEAVES);
              }
            }
          }
        }
      }
    }
  }

  public int getHeight(int x, int z) {
    return getSurfaceHeight(x, z);
  }

  public int getSurfaceHeight(int x, int z) {
    double height = 0;
    double amplitude = 1;
    double frequency = 0.035;
    double total = 0;

    for (int octave = 0; octave < 4; octave++) {
      double nx = x * frequency;
      double nz = z * frequency;

      double sample =
          Math.sin(nx) * Math.cos(nz * 1.17)
              + Math.sin(nx * 0.55 + nz * 0.73) * 0.65
              + Math.cos(nx * 1.9 + nz * 0.41) * 0.35;

      height += sample * amplitude;
      total += amplitude;
      amplitude *= 0.5;
      frequency *= 2.1;
    }

    int surface = (int) (height / total * 8 + 22);

    if (surface < 4) {
      surface = 4;
    }
    if (surface > HEIGHT - 3) {
      surface = HEIGHT - 3;
    }

    return surface;
  }

  public byte getBlock(int x, int y, int z) {
    if (x < 0 || z < 0 || x >= SIZE || z >= SIZE || y < 0 || y >= HEIGHT) {
      return Block.AIR;
    }

    return blocks[x][y][z];
  }

  public boolean setBlock(int x, int y, int z, byte block) {
    if (x < 0 || z < 0 || x >= SIZE || z >= SIZE || y < 0 || y >= HEIGHT) {
      return false;
    }

    if (blocks[x][y][z] == block) {
      return false;
    }

    if (blocks[x][y][z] == Block.BEDROCK && block != Block.BEDROCK) {
      return false;
    }

    blocks[x][y][z] = block;
    notifyChunk(x, z);
    return true;
  }

  private void setBlockInternal(int x, int y, int z, byte block) {
    blocks[x][y][z] = block;
  }

  private void notifyChunk(int x, int z) {
    int chunkX = Math.floorDiv(x, CHUNK_SIZE);
    int chunkZ = Math.floorDiv(z, CHUNK_SIZE);

    markChunk(chunkX, chunkZ);
    markChunk(chunkX - 1, chunkZ);
    markChunk(chunkX + 1, chunkZ);
    markChunk(chunkX, chunkZ - 1);
    markChunk(chunkX, chunkZ + 1);
  }

  private void markChunk(int chunkX, int chunkZ) {
    if (chunkX < 0 || chunkZ < 0) {
      return;
    }
    if (chunkX >= SIZE / CHUNK_SIZE || chunkZ >= SIZE / CHUNK_SIZE) {
      return;
    }

    int key = chunkKey(chunkX, chunkZ);
    for (IntConsumer listener : chunkListeners) {
      listener.accept(key);
    }
  }

  public static int chunkKey(int chunkX, int chunkZ) {
    return (chunkX << 16) | (chunkZ & 0xFFFF);
  }

  public static int chunkXFromKey(int key) {
    return (short) (key >> 16);
  }

  public static int chunkZFromKey(int key) {
    return (short) key;
  }
}
