package com.mineandcraft.world;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Хранит загруженные чанки вокруг игрока. Чанки загружаются (с диска или генератором)
 * в фоновых потоках, а все изменения мира и уведомления происходят в главном потоке.
 */
public class World implements AutoCloseable {

  public static final int HEIGHT = 128;
  public static final int CHUNK_SIZE = 16;
  public static final int SEA_LEVEL = 40;

  public static final int MIN_VIEW_DISTANCE = 4;
  public static final int MAX_VIEW_DISTANCE = 16;

  /** Слушатель событий мира. Все методы вызываются в главном потоке. */
  public interface Listener {
    default void chunkLoaded(Chunk chunk) {
    }

    default void chunkUnloaded(Chunk chunk) {
    }

    default void blockChanged(int x, int y, int z) {
    }
  }

  private final ChunkGenerator generator;
  private final ChunkStorage storage;
  private final Executor executor;
  private final ExecutorService ownedExecutor;
  private final int maxPending;

  private final Map<Long, Chunk> chunks = new HashMap<>();
  private final Set<Long> pending = new HashSet<>();
  private final Queue<Chunk> ready = new ConcurrentLinkedQueue<>();
  private final List<Listener> listeners = new ArrayList<>();

  private int viewDistance = 8;
  private int centerChunkX;
  private int centerChunkZ;

  /** Мир с фоновой генерацией в пуле потоков. */
  public World(long seed, ChunkStorage storage) {
    this(seed, storage, createPool());
  }

  private World(long seed, ChunkStorage storage, ExecutorService pool) {
    this.generator = new ChunkGenerator(seed);
    this.storage = storage;
    this.executor = pool;
    this.ownedExecutor = pool;
    this.maxPending = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
  }

  /** Мир с указанным исполнителем задач. {@code Runnable::run} даёт синхронную загрузку (для тестов). */
  public World(long seed, ChunkStorage storage, Executor executor) {
    this.generator = new ChunkGenerator(seed);
    this.storage = storage;
    this.executor = executor;
    this.ownedExecutor = null;
    this.maxPending = Integer.MAX_VALUE;
  }

  private static ExecutorService createPool() {
    int threads = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
    AtomicInteger counter = new AtomicInteger();
    return Executors.newFixedThreadPool(threads, runnable -> {
      Thread thread = new Thread(runnable, "chunk-worker-" + counter.incrementAndGet());
      thread.setDaemon(true);
      thread.setPriority(Thread.NORM_PRIORITY - 1);
      return thread;
    });
  }

  public long getSeed() {
    return generator.getSeed();
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public int getViewDistance() {
    return viewDistance;
  }

  public void setViewDistance(int viewDistance) {
    this.viewDistance = Math.max(MIN_VIEW_DISTANCE, Math.min(MAX_VIEW_DISTANCE, viewDistance));
  }

  /** Радиус загрузки данных на 1 больше дальности прорисовки: мешу нужны соседние чанки. */
  private int loadDistance() {
    return viewDistance + 1;
  }

  private int unloadDistance() {
    return viewDistance + 3;
  }

  /**
   * Вызывается каждый кадр: выгружает дальние чанки, ставит в очередь недостающие
   * (ближние в первую очередь) и принимает готовые из фоновых потоков.
   */
  public void update(float worldX, float worldZ) {
    centerChunkX = Math.floorDiv((int) Math.floor(worldX), CHUNK_SIZE);
    centerChunkZ = Math.floorDiv((int) Math.floor(worldZ), CHUNK_SIZE);

    unloadDistantChunks();
    requestMissingChunks();
    acceptReadyChunks();
  }

  /** Синхронно загружает чанки в радиусе вокруг точки. Используется при старте, чтобы игрок не упал в пустоту. */
  public void loadArea(float worldX, float worldZ, int radius) {
    int cx = Math.floorDiv((int) Math.floor(worldX), CHUNK_SIZE);
    int cz = Math.floorDiv((int) Math.floor(worldZ), CHUNK_SIZE);
    centerChunkX = cx;
    centerChunkZ = cz;

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        long key = chunkKey(cx + dx, cz + dz);
        if (!chunks.containsKey(key) && !pending.contains(key)) {
          addChunk(loadOrGenerate(cx + dx, cz + dz));
        }
      }
    }
  }

  private void requestMissingChunks() {
    int radius = loadDistance();
    List<long[]> missing = new ArrayList<>();

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        int distanceSq = dx * dx + dz * dz;
        if (distanceSq > radius * radius) {
          continue;
        }

        long key = chunkKey(centerChunkX + dx, centerChunkZ + dz);
        if (!chunks.containsKey(key) && !pending.contains(key)) {
          missing.add(new long[] {key, distanceSq});
        }
      }
    }

    missing.sort((a, b) -> Long.compare(a[1], b[1]));

    for (long[] entry : missing) {
      if (pending.size() >= maxPending) {
        break;
      }

      long key = entry[0];
      int chunkX = chunkXFromKey(key);
      int chunkZ = chunkZFromKey(key);
      pending.add(key);
      executor.execute(() -> ready.add(loadOrGenerate(chunkX, chunkZ)));
    }
  }

  private void acceptReadyChunks() {
    Chunk chunk;
    while ((chunk = ready.poll()) != null) {
      long key = chunk.key();
      pending.remove(key);

      if (chunks.containsKey(key) || chebyshevDistance(chunk.getChunkX(), chunk.getChunkZ()) > unloadDistance()) {
        continue;
      }

      addChunk(chunk);
    }
  }

  private void addChunk(Chunk chunk) {
    chunks.put(chunk.key(), chunk);
    for (Listener listener : listeners) {
      listener.chunkLoaded(chunk);
    }
  }

  private void unloadDistantChunks() {
    Iterator<Chunk> iterator = chunks.values().iterator();

    while (iterator.hasNext()) {
      Chunk chunk = iterator.next();
      if (chebyshevDistance(chunk.getChunkX(), chunk.getChunkZ()) <= unloadDistance()) {
        continue;
      }

      saveIfModified(chunk);
      iterator.remove();
      for (Listener listener : listeners) {
        listener.chunkUnloaded(chunk);
      }
    }
  }

  private int chebyshevDistance(int chunkX, int chunkZ) {
    return Math.max(Math.abs(chunkX - centerChunkX), Math.abs(chunkZ - centerChunkZ));
  }

  /** Выполняется в фоновом потоке: только чтение хранилища и чистая генерация. */
  private Chunk loadOrGenerate(int chunkX, int chunkZ) {
    try {
      byte[] data = storage.load(chunkX, chunkZ);
      if (data != null) {
        return new Chunk(chunkX, chunkZ, data);
      }
    } catch (IOException | RuntimeException e) {
      System.err.println("Не удалось загрузить чанк " + chunkX + ", " + chunkZ + ": " + e.getMessage()
          + ". Чанк будет сгенерирован заново.");
    }

    return generator.generate(chunkX, chunkZ);
  }

  private void saveIfModified(Chunk chunk) {
    if (!chunk.isModified()) {
      return;
    }

    try {
      storage.save(chunk.getChunkX(), chunk.getChunkZ(), chunk.rawData());
      chunk.setModified(false);
    } catch (IOException e) {
      System.err.println("Не удалось сохранить чанк " + chunk.getChunkX() + ", " + chunk.getChunkZ()
          + ": " + e.getMessage());
    }
  }

  public void saveAll() {
    for (Chunk chunk : chunks.values()) {
      saveIfModified(chunk);
    }
  }

  @Override
  public void close() {
    saveAll();

    if (ownedExecutor != null) {
      ownedExecutor.shutdownNow();
      try {
        ownedExecutor.awaitTermination(2, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public Collection<Chunk> getLoadedChunks() {
    return Collections.unmodifiableCollection(chunks.values());
  }

  public int getLoadedChunkCount() {
    return chunks.size();
  }

  public int getPendingChunkCount() {
    return pending.size();
  }

  public Chunk getChunk(int chunkX, int chunkZ) {
    return chunks.get(chunkKey(chunkX, chunkZ));
  }

  public boolean isChunkLoaded(int chunkX, int chunkZ) {
    return chunks.containsKey(chunkKey(chunkX, chunkZ));
  }

  /** Загружен ли чанк, содержащий мировую точку (x, z). */
  public boolean isLoadedAt(int x, int z) {
    return isChunkLoaded(Math.floorDiv(x, CHUNK_SIZE), Math.floorDiv(z, CHUNK_SIZE));
  }

  public int getSurfaceHeight(int x, int z) {
    return generator.surfaceHeight(x, z);
  }

  /** Возвращает Y первого свободного блока над самым верхним твёрдым блоком столбца. */
  public int getTopY(int x, int z) {
    for (int y = HEIGHT - 1; y >= 0; y--) {
      if (Block.isSolid(getBlock(x, y, z))) {
        return y + 1;
      }
    }

    return getSurfaceHeight(x, z) + 1;
  }

  public byte getBlock(int x, int y, int z) {
    if (y < 0 || y >= HEIGHT) {
      return Block.AIR;
    }

    Chunk chunk = chunks.get(chunkKey(Math.floorDiv(x, CHUNK_SIZE), Math.floorDiv(z, CHUNK_SIZE)));
    if (chunk == null) {
      return Block.AIR;
    }

    return chunk.getLocal(Math.floorMod(x, CHUNK_SIZE), y, Math.floorMod(z, CHUNK_SIZE));
  }

  /**
   * Ставит блок. Возвращает false, если чанк не загружен, координата вне мира,
   * блок уже такой или это попытка заменить бедрок.
   */
  public boolean setBlock(int x, int y, int z, byte block) {
    if (y < 0 || y >= HEIGHT) {
      return false;
    }

    Chunk chunk = chunks.get(chunkKey(Math.floorDiv(x, CHUNK_SIZE), Math.floorDiv(z, CHUNK_SIZE)));
    if (chunk == null) {
      return false;
    }

    int localX = Math.floorMod(x, CHUNK_SIZE);
    int localZ = Math.floorMod(z, CHUNK_SIZE);
    byte current = chunk.getLocal(localX, y, localZ);

    if (current == block || current == Block.BEDROCK) {
      return false;
    }

    chunk.setLocal(localX, y, localZ, block);
    chunk.setModified(true);

    for (Listener listener : listeners) {
      listener.blockChanged(x, y, z);
    }
    return true;
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
