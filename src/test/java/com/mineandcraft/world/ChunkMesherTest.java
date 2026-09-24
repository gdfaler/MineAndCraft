package com.mineandcraft.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkMesherTest {

  private static final int VERTICES_PER_FACE = 6;

  private final ChunkMesher mesher = new ChunkMesher();

  private static Chunk[][] neighborhood(Chunk center) {
    Chunk[][] result = new Chunk[3][3];
    result[1][1] = center;
    return result;
  }

  @Test
  void singleBlockHasSixFaces() {
    Chunk chunk = new Chunk(0, 0);
    chunk.setLocal(5, 10, 5, Block.STONE);

    mesher.build(neighborhood(chunk));

    assertEquals(6 * VERTICES_PER_FACE, mesher.vertexCount());
  }

  @Test
  void touchingBlocksHideSharedFaces() {
    Chunk chunk = new Chunk(0, 0);
    chunk.setLocal(5, 10, 5, Block.STONE);
    chunk.setLocal(6, 10, 5, Block.DIRT);

    mesher.build(neighborhood(chunk));

    assertEquals(10 * VERTICES_PER_FACE, mesher.vertexCount());
  }

  @Test
  void transparentNeighborsDoNotHideFaces() {
    Chunk chunk = new Chunk(0, 0);
    chunk.setLocal(5, 10, 5, Block.STONE);
    chunk.setLocal(6, 10, 5, Block.LEAVES);

    mesher.build(neighborhood(chunk));

    // Камень виден сквозь листву (6 граней), у листвы скрыта грань к камню (5 граней).
    assertEquals(11 * VERTICES_PER_FACE, mesher.vertexCount());
  }

  @Test
  void glassToGlassFacesAreSkipped() {
    Chunk chunk = new Chunk(0, 0);
    chunk.setLocal(5, 10, 5, Block.GLASS);
    chunk.setLocal(6, 10, 5, Block.GLASS);

    mesher.build(neighborhood(chunk));

    assertEquals(10 * VERTICES_PER_FACE, mesher.vertexCount());
  }

  @Test
  void neighborChunkHidesFaceOnBorder() {
    Chunk center = new Chunk(0, 0);
    Chunk east = new Chunk(1, 0);
    center.setLocal(World.CHUNK_SIZE - 1, 10, 5, Block.STONE);
    east.setLocal(0, 10, 5, Block.STONE);

    Chunk[][] around = neighborhood(center);
    around[2][1] = east;
    mesher.build(around);

    assertEquals(5 * VERTICES_PER_FACE, mesher.vertexCount());
  }

  @Test
  void worldBottomIsNotRendered() {
    Chunk chunk = new Chunk(0, 0);
    chunk.setLocal(3, 0, 3, Block.BEDROCK);

    mesher.build(neighborhood(chunk));

    assertEquals(5 * VERTICES_PER_FACE, mesher.vertexCount());
  }

  @Test
  void verticesUseWorldCoordinates() {
    Chunk chunk = new Chunk(-2, 3);
    chunk.setLocal(0, 10, 0, Block.STONE);

    mesher.build(neighborhood(chunk));

    float[] v = mesher.vertices();
    for (int i = 0; i < mesher.size(); i += ChunkMesher.FLOATS_PER_VERTEX) {
      assertTrue(v[i] == -32f || v[i] == -31f, "x = " + v[i]);
      assertTrue(v[i + 1] == 10f || v[i + 1] == 11f, "y = " + v[i + 1]);
      assertTrue(v[i + 2] == 48f || v[i + 2] == 49f, "z = " + v[i + 2]);
    }
  }

  @Test
  void ambientOcclusionDarkensCornerNextToWall() {
    Chunk chunk = new Chunk(0, 0);
    chunk.setLocal(5, 10, 5, Block.STONE);
    chunk.setLocal(6, 11, 5, Block.STONE);

    mesher.build(neighborhood(chunk));

    // Ищем вершины верхней грани нижнего блока (y = 11, x в [5, 6]).
    float min = Float.MAX_VALUE;
    float max = -Float.MAX_VALUE;
    float[] v = mesher.vertices();
    for (int i = 0; i < mesher.size(); i += ChunkMesher.FLOATS_PER_VERTEX) {
      boolean topOfLowerBlock = v[i + 1] == 11f && v[i] >= 5f && v[i] <= 6f && v[i + 2] >= 5f && v[i + 2] <= 6f;
      if (topOfLowerBlock && v[i + 5] <= BlockFace.TOP.shade) {
        min = Math.min(min, v[i + 5]);
        max = Math.max(max, v[i + 5]);
      }
    }

    assertTrue(min < max, "у стены вершины должны быть темнее: " + min + " / " + max);
  }
}
