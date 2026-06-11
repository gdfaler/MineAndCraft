package com.mineandcraft.world;

import com.mineandcraft.graphics.Mesh;
import com.mineandcraft.graphics.TextureAtlas;

import java.util.ArrayList;
import java.util.List;

public class ChunkMesh {

  private final int chunkX;
  private final int chunkZ;
  private final Mesh mesh = new Mesh();
  private boolean dirty = true;

  public ChunkMesh(int chunkX, int chunkZ) {
    this.chunkX = chunkX;
    this.chunkZ = chunkZ;
  }

  public void markDirty() {
    dirty = true;
  }

  public void rebuildIfNeeded(World world, TextureAtlas atlas) {
    if (!dirty) {
      return;
    }

    List<Float> vertices = new ArrayList<>();
    int startX = chunkX * World.CHUNK_SIZE;
    int startZ = chunkZ * World.CHUNK_SIZE;

    for (int x = startX; x < startX + World.CHUNK_SIZE; x++) {
      for (int z = startZ; z < startZ + World.CHUNK_SIZE; z++) {
        for (int y = 0; y < World.HEIGHT; y++) {
          byte block = world.getBlock(x, y, z);
          if (!Block.isSolid(block)) {
            continue;
          }

          addBlockFaces(vertices, world, atlas, block, x, y, z);
        }
      }
    }

    float[] data = new float[vertices.size()];
    for (int i = 0; i < data.length; i++) {
      data[i] = vertices.get(i);
    }

    mesh.upload(data);
    dirty = false;
  }

  private void addBlockFaces(
      List<Float> vertices,
      World world,
      TextureAtlas atlas,
      byte block,
      int x,
      int y,
      int z
  ) {
    if (isAir(world, x, y + 1, z)) {
      addFace(vertices, atlas, block, BlockFace.TOP, x, y, z);
    }
    if (isAir(world, x, y - 1, z)) {
      addFace(vertices, atlas, block, BlockFace.BOTTOM, x, y, z);
    }
    if (isAir(world, x, y, z - 1)) {
      addFace(vertices, atlas, block, BlockFace.NORTH, x, y, z);
    }
    if (isAir(world, x, y, z + 1)) {
      addFace(vertices, atlas, block, BlockFace.SOUTH, x, y, z);
    }
    if (isAir(world, x + 1, y, z)) {
      addFace(vertices, atlas, block, BlockFace.EAST, x, y, z);
    }
    if (isAir(world, x - 1, y, z)) {
      addFace(vertices, atlas, block, BlockFace.WEST, x, y, z);
    }
  }

  private boolean isAir(World world, int x, int y, int z) {
    return !Block.isSolid(world.getBlock(x, y, z));
  }

  private void addFace(
      List<Float> vertices,
      TextureAtlas atlas,
      byte block,
      BlockFace face,
      int x,
      int y,
      int z
  ) {
    int tile = Block.getAtlasIndex(block, face);
    float u0 = atlas.u0(tile);
    float u1 = atlas.u1(tile);
    float v0 = atlas.v0(tile);
    float v1 = atlas.v1(tile);
    float shade = face.shade;

    float x0 = x;
    float y0 = y;
    float z0 = z;
    float x1 = x + 1f;
    float y1 = y + 1f;
    float z1 = z + 1f;

    switch (face) {
      case TOP -> addQuad(vertices, shade,
          x0, y1, z0, u0, v0,
          x1, y1, z0, u1, v0,
          x1, y1, z1, u1, v1,
          x0, y1, z1, u0, v1);
      case BOTTOM -> addQuad(vertices, shade,
          x0, y0, z1, u0, v0,
          x1, y0, z1, u1, v0,
          x1, y0, z0, u1, v1,
          x0, y0, z0, u0, v1);
      case NORTH -> addQuad(vertices, shade,
          x1, y0, z0, u0, v0,
          x1, y1, z0, u0, v1,
          x0, y1, z0, u1, v1,
          x0, y0, z0, u1, v0);
      case SOUTH -> addQuad(vertices, shade,
          x0, y0, z1, u0, v0,
          x0, y1, z1, u0, v1,
          x1, y1, z1, u1, v1,
          x1, y0, z1, u1, v0);
      case EAST -> addQuad(vertices, shade,
          x1, y0, z1, u0, v0,
          x1, y1, z1, u0, v1,
          x1, y1, z0, u1, v1,
          x1, y0, z0, u1, v0);
      case WEST -> addQuad(vertices, shade,
          x0, y0, z0, u0, v0,
          x0, y1, z0, u0, v1,
          x0, y1, z1, u1, v1,
          x0, y0, z1, u1, v0);
    }
  }

  private void addQuad(
      List<Float> vertices,
      float shade,
      float x1, float y1, float z1, float u1, float v1,
      float x2, float y2, float z2, float u2, float v2,
      float x3, float y3, float z3, float u3, float v3,
      float x4, float y4, float z4, float u4, float v4
  ) {
    addVertex(vertices, x1, y1, z1, u1, v1, shade);
    addVertex(vertices, x3, y3, z3, u3, v3, shade);
    addVertex(vertices, x2, y2, z2, u2, v2, shade);

    addVertex(vertices, x1, y1, z1, u1, v1, shade);
    addVertex(vertices, x4, y4, z4, u4, v4, shade);
    addVertex(vertices, x3, y3, z3, u3, v3, shade);
  }

  private void addVertex(List<Float> vertices, float x, float y, float z, float u, float v, float shade) {
    vertices.add(x);
    vertices.add(y);
    vertices.add(z);
    vertices.add(u);
    vertices.add(v);
    vertices.add(shade);
  }

  public void render() {
    mesh.render();
  }
}
