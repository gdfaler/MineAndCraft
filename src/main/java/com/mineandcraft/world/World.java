package com.mineandcraft.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongConsumer;

public class World {

  public static final int HEIGHT = 64;
  public static final int CHUNK_SIZE = 16;
  public static final int SEA_LEVEL = 20;
  private int viewDistance = 8;
  private int unloadDistance = 10;

  private final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
  private final List<LongConsumer> chunkListeners = new ArrayList<>();
  private final ChunkGenerator generator;

  public World() {
    this(2026L);
  }

  public World(long seed) {
    generator = new ChunkGenerator(seed);
  }

  public void addChunkListener(LongConsumer listener) {
    chunkListeners.add(listener);
  }

  public int getViewDistance() {
    return viewDistance;
  }

  public void setViewDistance(int viewDistance) {
    this.viewDistance = Math.max(4, Math.min(12, viewDistance));
    this.unloadDistance = this.viewDistance + 2;
  }

  public void updateChunksAround(float worldX, float worldZ) {
    int centerChunkX = Math.floorDiv((int) Math.floor(worldX), CHUNK_SIZE);
    int centerChunkZ = Math.floorDiv((int) Math.floor(worldZ), CHUNK_SIZE);

    for (int dx = -viewDistance; dx <= viewDistance; dx++) {
      for (int dz = -viewDistance; dz <= viewDistance; dz++) {
        if (dx * dx + dz * dz > viewDistance * viewDistance) {
          continue;
        }

        getOrCreateChunk(centerChunkX + dx, centerChunkZ + dz);
      }
    }

    unloadDistantChunks(centerChunkX, centerChunkZ);
  }

  private void unloadDistantChunks(int centerChunkX, int centerChunkZ) {
    Iterator<Map.Entry<Long, Chunk>> iterator = chunks.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry<Long, Chunk> entry = iterator.next();
      int chunkX = chunkXFromKey(entry.getKey());
      int chunkZ = chunkZFromKey(entry.getKey());
      int distance = Math.max(Math.abs(chunkX - centerChunkX), Math.abs(chunkZ - centerChunkZ));

      if (distance > unloadDistance) {
        iterator.remove();
      }
    }
  }

  public Chunk getOrCreateChunk(int chunkX, int chunkZ) {
    long key = chunkKey(chunkX, chunkZ);
    Chunk chunk = chunks.get(key);

    if (chunk != null) {
      return chunk;
    }

    chunk = new Chunk(chunkX, chunkZ);
    chunks.put(key, chunk);
    generator.generate(chunk, this);
    notifyChunkGenerated(chunkX, chunkZ);
    return chunk;
  }

  public Iterable<Chunk> getLoadedChunks() {
    return chunks.values();
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
    if (y < 0 || y >= HEIGHT) {
      return Block.AIR;
    }

    int chunkX = Math.floorDiv(x, CHUNK_SIZE);
    int chunkZ = Math.floorDiv(z, CHUNK_SIZE);
    Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));

    if (chunk == null || !chunk.isGenerated()) {
      return Block.AIR;
    }

    int localX = Math.floorMod(x, CHUNK_SIZE);
    int localZ = Math.floorMod(z, CHUNK_SIZE);
    return chunk.getLocal(localX, y, localZ);
  }

  public boolean setBlock(int x, int y, int z, byte block) {
    if (!setBlockQuiet(x, y, z, block)) {
      return false;
    }

    notifyChunk(x, z);
    return true;
  }

  boolean setBlockQuiet(int x, int y, int z, byte block) {
    if (y < 0 || y >= HEIGHT) {
      return false;
    }

    int chunkX = Math.floorDiv(x, CHUNK_SIZE);
    int chunkZ = Math.floorDiv(z, CHUNK_SIZE);
    Chunk chunk = getOrCreateChunk(chunkX, chunkZ);

    int localX = Math.floorMod(x, CHUNK_SIZE);
    int localZ = Math.floorMod(z, CHUNK_SIZE);

    if (chunk.getLocal(localX, y, localZ) == block) {
      return false;
    }

    if (chunk.getLocal(localX, y, localZ) == Block.BEDROCK && block != Block.BEDROCK) {
      return false;
    }

    chunk.setLocal(localX, y, localZ, block);
    return true;
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

  private void notifyChunkGenerated(int chunkX, int chunkZ) {
    markChunk(chunkX, chunkZ);
  }

  private void markChunk(int chunkX, int chunkZ) {
    long key = chunkKey(chunkX, chunkZ);

    for (LongConsumer listener : chunkListeners) {
      listener.accept(key);
    }
  }

  public static long chunkKey(int chunkX, int chunkZ) {
    return ((long) chunkX << 32) | (chunkZ & 0xffffffffL);
  }

  public static int chunkXFromKey(long key) {
    return (int) (key >> 32);
  }

  public static int chunkZFromKey(long key) {
    return (int) key;
  }
}
