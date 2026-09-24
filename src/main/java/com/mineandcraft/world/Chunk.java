package com.mineandcraft.world;

public class Chunk {

  public static final int VOLUME = World.CHUNK_SIZE * World.HEIGHT * World.CHUNK_SIZE;

  private final int chunkX;
  private final int chunkZ;
  private final byte[] blocks;
  private boolean modified;

  public Chunk(int chunkX, int chunkZ) {
    this(chunkX, chunkZ, new byte[VOLUME]);
  }

  Chunk(int chunkX, int chunkZ, byte[] blocks) {
    if (blocks.length != VOLUME) {
      throw new IllegalArgumentException("Chunk data must have " + VOLUME + " bytes, got " + blocks.length);
    }

    this.chunkX = chunkX;
    this.chunkZ = chunkZ;
    this.blocks = blocks;
  }

  public int getChunkX() {
    return chunkX;
  }

  public int getChunkZ() {
    return chunkZ;
  }

  public long key() {
    return World.chunkKey(chunkX, chunkZ);
  }

  /** Чанк отличается от того, что выдал бы генератор, и его нужно сохранить. */
  public boolean isModified() {
    return modified;
  }

  void setModified(boolean modified) {
    this.modified = modified;
  }

  public byte getLocal(int localX, int localY, int localZ) {
    if (localX < 0 || localZ < 0
        || localX >= World.CHUNK_SIZE || localZ >= World.CHUNK_SIZE
        || localY < 0 || localY >= World.HEIGHT) {
      return Block.AIR;
    }

    return blocks[index(localX, localY, localZ)];
  }

  public void setLocal(int localX, int localY, int localZ, byte block) {
    if (localX < 0 || localZ < 0
        || localX >= World.CHUNK_SIZE || localZ >= World.CHUNK_SIZE
        || localY < 0 || localY >= World.HEIGHT) {
      return;
    }

    blocks[index(localX, localY, localZ)] = block;
  }

  /** Сырые данные для сохранения. Не изменять снаружи пакета. */
  byte[] rawData() {
    return blocks;
  }

  static int index(int localX, int localY, int localZ) {
    return (localY * World.CHUNK_SIZE + localZ) * World.CHUNK_SIZE + localX;
  }
}
