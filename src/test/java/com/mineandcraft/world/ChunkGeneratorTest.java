package com.mineandcraft.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ChunkGeneratorTest {

  @Test
  void generationIsDeterministic() {
    Chunk a = new ChunkGenerator(99).generate(3, -7);
    Chunk b = new ChunkGenerator(99).generate(3, -7);

    assertArrayEquals(a.rawData(), b.rawData());
  }

  @Test
  void seedChangesTerrain() {
    ChunkGenerator a = new ChunkGenerator(1);
    ChunkGenerator b = new ChunkGenerator(2);
    int differentColumns = 0;

    for (int x = 0; x < 64; x++) {
      if (a.surfaceHeight(x * 7, x * 3) != b.surfaceHeight(x * 7, x * 3)) {
        differentColumns++;
      }
    }

    assertTrue(differentColumns > 32, "рельеф почти не зависит от сида: " + differentColumns);
  }

  @Test
  void bottomLayerIsBedrockAndSurfaceIsWithinWorld() {
    ChunkGenerator generator = new ChunkGenerator(5);
    Chunk chunk = generator.generate(0, 0);

    for (int x = 0; x < World.CHUNK_SIZE; x++) {
      for (int z = 0; z < World.CHUNK_SIZE; z++) {
        assertEquals(Block.BEDROCK, chunk.getLocal(x, 0, z));

        int surface = generator.surfaceHeight(x, z);
        assertTrue(surface > World.SEA_LEVEL - 30 && surface < World.HEIGHT - 10, "высота " + surface);
      }
    }
  }

  @Test
  void terrainHasVariedHeights() {
    ChunkGenerator generator = new ChunkGenerator(11);
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;

    for (int x = -500; x <= 500; x += 10) {
      for (int z = -500; z <= 500; z += 10) {
        int height = generator.surfaceHeight(x, z);
        min = Math.min(min, height);
        max = Math.max(max, height);
      }
    }

    assertTrue(max - min >= 15, "слишком плоский мир: " + min + " .. " + max);
  }

  @Test
  void worldContainsTreesCavesAndOres() {
    ChunkGenerator generator = new ChunkGenerator(2026);
    int wood = 0;
    int coal = 0;
    int caveAir = 0;

    for (int cx = -3; cx <= 3; cx++) {
      for (int cz = -3; cz <= 3; cz++) {
        Chunk chunk = generator.generate(cx, cz);
        for (int x = 0; x < World.CHUNK_SIZE; x++) {
          for (int z = 0; z < World.CHUNK_SIZE; z++) {
            int surface = generator.surfaceHeight(cx * World.CHUNK_SIZE + x, cz * World.CHUNK_SIZE + z);
            for (int y = 0; y < World.HEIGHT; y++) {
              byte block = chunk.getLocal(x, y, z);
              if (block == Block.WOOD) {
                wood++;
              } else if (block == Block.COAL_ORE) {
                coal++;
              } else if (block == Block.AIR && y < surface - 5) {
                caveAir++;
              }
            }
          }
        }
      }
    }

    assertTrue(wood > 0, "нет деревьев");
    assertTrue(coal > 0, "нет угля");
    assertTrue(caveAir > 0, "нет пещер");
  }

  @Test
  void treesStandOnGrass() {
    ChunkGenerator generator = new ChunkGenerator(2026);

    for (int cx = -2; cx <= 2; cx++) {
      for (int cz = -2; cz <= 2; cz++) {
        Chunk chunk = generator.generate(cx, cz);
        for (int x = 0; x < World.CHUNK_SIZE; x++) {
          for (int z = 0; z < World.CHUNK_SIZE; z++) {
            for (int y = 1; y < World.HEIGHT; y++) {
              if (chunk.getLocal(x, y, z) == Block.WOOD && chunk.getLocal(x, y - 1, z) != Block.WOOD) {
                assertEquals(Block.GRASS, chunk.getLocal(x, y - 1, z),
                    "ствол висит в воздухе в чанке " + cx + "," + cz + " на " + x + "," + y + "," + z);
              }
            }
          }
        }
      }
    }
  }

  @Test
  void treeCrownsContinueAcrossChunkBorders() {
    // Крона дерева, стоящего у края чанка, должна продолжаться в соседнем чанке, а не обрезаться.
    ChunkGenerator generator = new ChunkGenerator(2026);
    int checked = 0;

    for (int cx = -6; cx <= 6; cx++) {
      for (int cz = -6; cz <= 6; cz++) {
        Chunk chunk = generator.generate(cx, cz);
        Chunk east = null;

        for (int z = 0; z < World.CHUNK_SIZE; z++) {
          for (int y = 2; y < World.HEIGHT - 1; y++) {
            int edge = World.CHUNK_SIZE - 1;
            boolean trunkTop = chunk.getLocal(edge, y, z) == Block.WOOD
                && chunk.getLocal(edge, y - 1, z) == Block.WOOD
                && chunk.getLocal(edge, y + 1, z) != Block.WOOD;
            if (!trunkTop) {
              continue;
            }

            if (east == null) {
              east = generator.generate(cx + 1, cz);
            }
            byte neighbor = east.getLocal(0, y, z);
            assertTrue(neighbor == Block.LEAVES || neighbor == Block.WOOD,
                "крона обрезана на границе чанков " + cx + "," + cz + " z=" + z + " y=" + y);
            checked++;
          }
        }
      }
    }

    assumeTrue(checked > 0, "в выборке нет деревьев на краю чанка");
  }
}
