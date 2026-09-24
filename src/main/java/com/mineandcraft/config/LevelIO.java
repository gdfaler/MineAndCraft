package com.mineandcraft.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Random;

/** Метаданные сохранённого мира: сид и состояние игрока ({@code level.properties}). */
public final class LevelIO {

  public static final Path DEFAULT_WORLD_DIR = SettingsIO.DATA_DIR.resolve("world");

  /** Данные уровня. Позиция игрока равна NaN, если игрок ещё не сохранялся. */
  public static final class LevelData {
    public long seed;
    public float playerX = Float.NaN;
    public float playerY = Float.NaN;
    public float playerZ = Float.NaN;
    public float yaw;
    public float pitch;
    public boolean flying;
    public int selectedSlot;

    public boolean hasPlayerPosition() {
      return !Float.isNaN(playerX) && !Float.isNaN(playerY) && !Float.isNaN(playerZ);
    }
  }

  private LevelIO() {
  }

  /** Читает level.properties; если файла нет — создаёт новый уровень со случайным сидом. */
  public static LevelData loadOrCreate(Path worldDir) {
    LevelData data = new LevelData();
    Path file = worldDir.resolve("level.properties");

    if (!Files.exists(file)) {
      data.seed = new Random().nextLong();
      return data;
    }

    Properties properties = new Properties();
    try (InputStream in = Files.newInputStream(file)) {
      properties.load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Не удалось прочитать " + file + ": " + e.getMessage(), e);
    }

    String seed = properties.getProperty("seed");
    try {
      data.seed = Long.parseLong(seed);
    } catch (NumberFormatException | NullPointerException e) {
      throw new IllegalStateException("В " + file + " нет корректного сида мира", e);
    }

    data.playerX = SettingsIO.readFloat(properties, "player.x", Float.NaN);
    data.playerY = SettingsIO.readFloat(properties, "player.y", Float.NaN);
    data.playerZ = SettingsIO.readFloat(properties, "player.z", Float.NaN);
    data.yaw = SettingsIO.readFloat(properties, "player.yaw", 0f);
    data.pitch = SettingsIO.readFloat(properties, "player.pitch", 0f);
    data.flying = SettingsIO.readBoolean(properties, "player.flying", false);
    data.selectedSlot = SettingsIO.readInt(properties, "player.slot", 0);
    return data;
  }

  public static void save(Path worldDir, LevelData data) throws IOException {
    Properties properties = new Properties();
    properties.setProperty("seed", Long.toString(data.seed));
    if (data.hasPlayerPosition()) {
      properties.setProperty("player.x", Float.toString(data.playerX));
      properties.setProperty("player.y", Float.toString(data.playerY));
      properties.setProperty("player.z", Float.toString(data.playerZ));
    }
    properties.setProperty("player.yaw", Float.toString(data.yaw));
    properties.setProperty("player.pitch", Float.toString(data.pitch));
    properties.setProperty("player.flying", Boolean.toString(data.flying));
    properties.setProperty("player.slot", Integer.toString(data.selectedSlot));

    Files.createDirectories(worldDir);
    try (OutputStream out = Files.newOutputStream(worldDir.resolve("level.properties"))) {
      properties.store(out, "MineAndCraft world");
    }
  }
}
