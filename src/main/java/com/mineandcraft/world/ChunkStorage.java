package com.mineandcraft.world;

import java.io.IOException;

/** Хранилище изменённых игроком чанков. Реализации должны быть потокобезопасными. */
public interface ChunkStorage {

  /** Возвращает сохранённые данные чанка или {@code null}, если чанк не сохранялся. */
  byte[] load(int chunkX, int chunkZ) throws IOException;

  void save(int chunkX, int chunkZ, byte[] data) throws IOException;
}
