package com.mineandcraft.world;

public class Chunk {

  private final int chunkX;
  private final int chunkZ;
  private final byte[][][] blocks = new byte[World.CHUNK_SIZE][World.HEIGHT][World.CHUNK_SIZE];
  private boolean generated;

  public Chunk(int chunkX, int chunkZ) {
    this.chunkX = chunkX;
    this.chunkZ = chunkZ;
  }

  public int getChunkX() {
    return chunkX;
  }

  public int getChunkZ() {
    return chunkZ;
  }

  public boolean isGenerated() {
    return generated;
  }

  public void setGenerated() {
    generated = true;
  }

  public byte getLocal(int localX, int localY, int localZ) {
    if (localX < 0 || localZ < 0
        || localX >= World.CHUNK_SIZE || localZ >= World.CHUNK_SIZE
        || localY < 0 || localY >= World.HEIGHT) {
      return Block.AIR;
    }

    return blocks[localX][localY][localZ];
  }

  public void setLocal(int localX, int localY, int localZ, byte block) {
    if (localX < 0 || localZ < 0
        || localX >= World.CHUNK_SIZE || localZ >= World.CHUNK_SIZE
        || localY < 0 || localY >= World.HEIGHT) {
      return;
    }

    blocks[localX][localY][localZ] = block;
  }
}
