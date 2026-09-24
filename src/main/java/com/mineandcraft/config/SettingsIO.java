package com.mineandcraft.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SettingsIO {

  /** Каталог данных игры: {@code ~/.mineandcraft}. */
  public static final Path DATA_DIR = Path.of(System.getProperty("user.home"), ".mineandcraft");
  private static final Path SETTINGS_PATH = DATA_DIR.resolve("settings.properties");

  private SettingsIO() {
  }

  public static void load(GameSettings settings) {
    load(settings, SETTINGS_PATH);
  }

  public static void load(GameSettings settings, Path path) {
    if (!Files.exists(path)) {
      return;
    }

    Properties properties = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      properties.load(in);
    } catch (IOException e) {
      System.err.println("Не удалось прочитать настройки " + path + ": " + e.getMessage());
      return;
    }

    settings.setMouseSensitivity(readFloat(properties, "mouseSensitivity", settings.getMouseSensitivity()));
    settings.setFov(readFloat(properties, "fov", settings.getFov()));
    settings.setRenderDistance(readInt(properties, "renderDistance", settings.getRenderDistance()));
    settings.setFogEnabled(readBoolean(properties, "fogEnabled", settings.isFogEnabled()));
    settings.setVsync(readBoolean(properties, "vsync", settings.isVsync()));
    settings.setShowDebug(readBoolean(properties, "showDebug", settings.isShowDebug()));
    settings.setSpeedMultiplier(readFloat(properties, "speedMultiplier", settings.getSpeedMultiplier()));
    settings.setRawMouseInput(readBoolean(properties, "rawMouseInput", settings.isRawMouseInput()));
  }

  public static void save(GameSettings settings) {
    save(settings, SETTINGS_PATH);
  }

  public static void save(GameSettings settings, Path path) {

    Properties properties = new Properties();
    properties.setProperty("mouseSensitivity", Float.toString(settings.getMouseSensitivity()));
    properties.setProperty("fov", Float.toString(settings.getFov()));
    properties.setProperty("renderDistance", Integer.toString(settings.getRenderDistance()));
    properties.setProperty("fogEnabled", Boolean.toString(settings.isFogEnabled()));
    properties.setProperty("vsync", Boolean.toString(settings.isVsync()));
    properties.setProperty("showDebug", Boolean.toString(settings.isShowDebug()));
    properties.setProperty("speedMultiplier", Float.toString(settings.getSpeedMultiplier()));
    properties.setProperty("rawMouseInput", Boolean.toString(settings.isRawMouseInput()));

    try {
      Files.createDirectories(path.getParent());
      try (OutputStream out = Files.newOutputStream(path)) {
        properties.store(out, "MineAndCraft settings");
      }
    } catch (IOException e) {
      System.err.println("Не удалось сохранить настройки " + path + ": " + e.getMessage());
    }
  }

  static float readFloat(Properties properties, String key, float fallback) {
    String value = properties.getProperty(key);
    if (value == null) {
      return fallback;
    }

    try {
      return Float.parseFloat(value);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  static int readInt(Properties properties, String key, int fallback) {
    String value = properties.getProperty(key);
    if (value == null) {
      return fallback;
    }

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  static boolean readBoolean(Properties properties, String key, boolean fallback) {
    String value = properties.getProperty(key);
    if (value == null) {
      return fallback;
    }

    return Boolean.parseBoolean(value);
  }
}
