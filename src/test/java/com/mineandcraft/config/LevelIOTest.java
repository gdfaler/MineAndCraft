package com.mineandcraft.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelIOTest {

  @TempDir
  Path dir;

  @Test
  void newLevelHasNoPlayerPosition() {
    LevelIO.LevelData data = LevelIO.loadOrCreate(dir.resolve("new-world"));

    assertFalse(data.hasPlayerPosition());
  }

  @Test
  void levelRoundTrip() throws IOException {
    LevelIO.LevelData data = new LevelIO.LevelData();
    data.seed = -1234567890123L;
    data.playerX = 10.5f;
    data.playerY = 64f;
    data.playerZ = -3.25f;
    data.yaw = 180f;
    data.pitch = -20f;
    data.flying = true;
    data.selectedSlot = 6;

    LevelIO.save(dir, data);
    LevelIO.LevelData loaded = LevelIO.loadOrCreate(dir);

    assertEquals(data.seed, loaded.seed);
    assertTrue(loaded.hasPlayerPosition());
    assertEquals(10.5f, loaded.playerX);
    assertEquals(64f, loaded.playerY);
    assertEquals(-3.25f, loaded.playerZ);
    assertEquals(180f, loaded.yaw);
    assertEquals(-20f, loaded.pitch);
    assertTrue(loaded.flying);
    assertEquals(6, loaded.selectedSlot);
  }

  @Test
  void corruptSeedIsAnError() throws IOException {
    Files.writeString(dir.resolve("level.properties"), "seed=oops\n");

    assertThrows(IllegalStateException.class, () -> LevelIO.loadOrCreate(dir));
  }
}
