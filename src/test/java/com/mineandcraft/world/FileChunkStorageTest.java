package com.mineandcraft.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileChunkStorageTest {

  @TempDir
  Path dir;

  @Test
  void savedChunkCanBeLoadedBack() throws IOException {
    FileChunkStorage storage = new FileChunkStorage(dir);
    byte[] data = new ChunkGenerator(3).generate(-2, 9).rawData();

    storage.save(-2, 9, data);

    assertArrayEquals(data, storage.load(-2, 9));
  }

  @Test
  void missingChunkReturnsNull() throws IOException {
    assertNull(new FileChunkStorage(dir).load(0, 0));
  }

  @Test
  void corruptFileIsReportedAsError() throws IOException {
    Files.createDirectories(dir.resolve("chunks"));
    Files.writeString(dir.resolve("chunks").resolve("c.0.0.dat"), "not a chunk");

    assertThrows(IOException.class, () -> new FileChunkStorage(dir).load(0, 0));
  }

  @Test
  void worldRegeneratesChunkWhenSaveIsCorrupt() throws IOException {
    Files.createDirectories(dir.resolve("chunks"));
    Files.writeString(dir.resolve("chunks").resolve("c.0.0.dat"), "not a chunk");

    World world = new World(3, new FileChunkStorage(dir), Runnable::run);
    world.loadArea(0, 0, 0);

    assertEquals(Block.BEDROCK, world.getBlock(0, 0, 0));
  }

  @Test
  void editedWorldPersistsAcrossInstances() {
    World first = new World(4, new FileChunkStorage(dir), Runnable::run);
    first.loadArea(0, 0, 1);
    int y = first.getTopY(1, 1);
    first.setBlock(1, y, 1, Block.COBBLESTONE);
    first.close();

    World second = new World(4, new FileChunkStorage(dir), Runnable::run);
    second.loadArea(0, 0, 1);
    assertEquals(Block.COBBLESTONE, second.getBlock(1, y, 1));
  }
}
