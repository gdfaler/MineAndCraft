package com.mineandcraft.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsIOTest {

  @TempDir
  Path dir;

  @Test
  void settingsRoundTrip() {
    GameSettings settings = new GameSettings();
    settings.setFov(90f);
    settings.setRenderDistance(11);
    settings.setMouseSensitivity(0.2f);
    settings.setFogEnabled(false);
    settings.setVsync(false);
    settings.setShowDebug(true);
    settings.setSpeedMultiplier(1.5f);
    settings.setRawMouseInput(false);

    Path file = dir.resolve("nested").resolve("settings.properties");
    SettingsIO.save(settings, file);

    GameSettings loaded = new GameSettings();
    SettingsIO.load(loaded, file);

    assertEquals(90f, loaded.getFov());
    assertEquals(11, loaded.getRenderDistance());
    assertEquals(0.2f, loaded.getMouseSensitivity(), 1e-6);
    assertFalse(loaded.isFogEnabled());
    assertFalse(loaded.isVsync());
    assertTrue(loaded.isShowDebug());
    assertEquals(1.5f, loaded.getSpeedMultiplier(), 1e-6);
    assertFalse(loaded.isRawMouseInput());
  }

  @Test
  void invalidAndOutOfRangeValuesFallBackOrClamp() throws IOException {
    Path file = dir.resolve("settings.properties");
    Files.writeString(file, "fov=abc\nrenderDistance=999\nmouseSensitivity=-5\n");

    GameSettings loaded = new GameSettings();
    SettingsIO.load(loaded, file);

    assertEquals(new GameSettings().getFov(), loaded.getFov());
    assertEquals(GameSettings.MAX_RENDER_DISTANCE, loaded.getRenderDistance());
    assertEquals(GameSettings.MIN_SENSITIVITY, loaded.getMouseSensitivity());
  }

  @Test
  void missingFileKeepsDefaults() {
    GameSettings loaded = new GameSettings();
    SettingsIO.load(loaded, dir.resolve("absent.properties"));

    assertEquals(new GameSettings().getFov(), loaded.getFov());
  }
}
