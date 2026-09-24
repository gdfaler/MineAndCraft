package com.mineandcraft.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Хранит изменённые чанки в памяти, пока работает игра. Используется в тестах и как запасной вариант. */
public class MemoryChunkStorage implements ChunkStorage {

  private final Map<Long, byte[]> chunks = new ConcurrentHashMap<>();

  @Override
  public byte[] load(int chunkX, int chunkZ) {
    byte[] data = chunks.get(World.chunkKey(chunkX, chunkZ));
    return data == null ? null : data.clone();
  }

  @Override
  public void save(int chunkX, int chunkZ, byte[] data) {
    chunks.put(World.chunkKey(chunkX, chunkZ), data.clone());
  }

  public int size() {
    return chunks.size();
  }
}
