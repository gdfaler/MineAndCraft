package com.mineandcraft;

import com.mineandcraft.world.Block;
import com.mineandcraft.world.Chunk;
import com.mineandcraft.world.MemoryChunkStorage;
import com.mineandcraft.world.World;

/** Миры для тестов: синхронная загрузка чанков, данные только в памяти. */
public final class TestWorlds {

  private TestWorlds() {
  }

  public static World generated(long seed) {
    return new World(seed, new MemoryChunkStorage(), Runnable::run);
  }

  /** Плоский мир из камня: блоки с y < groundY — камень, выше — воздух. Загружено 5x5 чанков вокруг (0, 0). */
  public static World flat(int groundY) {
    World world = generated(1L);
    world.loadArea(0, 0, 2);

    for (Chunk chunk : world.getLoadedChunks()) {
      for (int x = 0; x < World.CHUNK_SIZE; x++) {
        for (int z = 0; z < World.CHUNK_SIZE; z++) {
          for (int y = 0; y < World.HEIGHT; y++) {
            chunk.setLocal(x, y, z, y < groundY ? Block.STONE : Block.AIR);
          }
        }
      }
    }

    return world;
  }
}
