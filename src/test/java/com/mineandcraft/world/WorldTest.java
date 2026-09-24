package com.mineandcraft.world;

import com.mineandcraft.TestWorlds;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldTest {

  @Test
  void updateLoadsChunksAroundPlayerAndUnloadsDistantOnes() {
    World world = TestWorlds.generated(1);
    world.setViewDistance(4);

    world.update(0, 0);
    assertTrue(world.isChunkLoaded(0, 0));
    assertTrue(world.isChunkLoaded(5, 0), "радиус загрузки = дальность + 1");
    assertFalse(world.isChunkLoaded(6, 0));

    world.update(40 * World.CHUNK_SIZE, 0);
    assertFalse(world.isChunkLoaded(0, 0), "дальний чанк должен выгрузиться");
    assertTrue(world.isChunkLoaded(40, 0));
  }

  @Test
  void viewDistanceIsClamped() {
    World world = TestWorlds.generated(1);

    world.setViewDistance(100);
    assertEquals(World.MAX_VIEW_DISTANCE, world.getViewDistance());
    world.setViewDistance(0);
    assertEquals(World.MIN_VIEW_DISTANCE, world.getViewDistance());
  }

  @Test
  void setBlockChangesWorldAndNotifiesListeners() {
    World world = TestWorlds.flat(10);
    List<String> events = new ArrayList<>();
    world.addListener(new World.Listener() {
      @Override
      public void blockChanged(int x, int y, int z) {
        events.add(x + "," + y + "," + z);
      }
    });

    assertTrue(world.setBlock(-3, 12, 5, Block.PLANKS));
    assertEquals(Block.PLANKS, world.getBlock(-3, 12, 5));
    assertEquals(List.of("-3,12,5"), events);

    assertFalse(world.setBlock(-3, 12, 5, Block.PLANKS), "повторная установка того же блока — не изменение");
    assertEquals(1, events.size());
  }

  @Test
  void bedrockCannotBeReplaced() {
    World world = TestWorlds.generated(1);
    world.loadArea(0, 0, 1);

    assertEquals(Block.BEDROCK, world.getBlock(0, 0, 0));
    assertFalse(world.setBlock(0, 0, 0, Block.AIR));
    assertEquals(Block.BEDROCK, world.getBlock(0, 0, 0));
  }

  @Test
  void blocksOutsideWorldOrUnloadedChunksAreAirAndCannotBeSet() {
    World world = TestWorlds.flat(10);

    assertEquals(Block.AIR, world.getBlock(0, -1, 0));
    assertEquals(Block.AIR, world.getBlock(0, World.HEIGHT, 0));
    assertEquals(Block.AIR, world.getBlock(10_000, 5, 0));
    assertFalse(world.setBlock(10_000, 5, 0, Block.STONE));
    assertFalse(world.setBlock(0, World.HEIGHT, 0, Block.STONE));
  }

  @Test
  void playerEditsSurviveChunkUnloadAndReload() {
    MemoryChunkStorage storage = new MemoryChunkStorage();
    World world = new World(7, storage, Runnable::run);
    world.setViewDistance(4);
    world.update(0, 0);

    int y = world.getTopY(3, 3);
    assertTrue(world.setBlock(3, y, 3, Block.GLASS));

    world.update(50 * World.CHUNK_SIZE, 0);
    assertFalse(world.isChunkLoaded(0, 0));
    assertEquals(1, storage.size(), "изменённый чанк должен сохраниться при выгрузке");

    world.update(0, 0);
    assertEquals(Block.GLASS, world.getBlock(3, y, 3), "изменение потерялось после повторной загрузки");
  }

  @Test
  void unmodifiedChunksAreNotSaved() {
    MemoryChunkStorage storage = new MemoryChunkStorage();
    World world = new World(7, storage, Runnable::run);
    world.setViewDistance(4);
    world.update(0, 0);
    world.update(50 * World.CHUNK_SIZE, 0);
    world.close();

    assertEquals(0, storage.size());
  }

  @Test
  void listenersSeeLoadAndUnloadEvents() {
    World world = TestWorlds.generated(1);
    world.setViewDistance(4);
    List<Long> loaded = new ArrayList<>();
    List<Long> unloaded = new ArrayList<>();
    world.addListener(new World.Listener() {
      @Override
      public void chunkLoaded(Chunk chunk) {
        loaded.add(chunk.key());
      }

      @Override
      public void chunkUnloaded(Chunk chunk) {
        unloaded.add(chunk.key());
      }
    });

    world.update(0, 0);
    assertEquals(world.getLoadedChunkCount(), loaded.size());

    world.update(50 * World.CHUNK_SIZE, 0);
    assertTrue(unloaded.contains(World.chunkKey(0, 0)));
  }

  @Test
  void chunkKeyRoundTripsNegativeCoordinates() {
    long key = World.chunkKey(-5, 123_456);
    assertEquals(-5, World.chunkXFromKey(key));
    assertEquals(123_456, World.chunkZFromKey(key));

    key = World.chunkKey(7, -1);
    assertEquals(7, World.chunkXFromKey(key));
    assertEquals(-1, World.chunkZFromKey(key));
  }
}
