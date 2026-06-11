package com.mineandcraft.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SettingsIO {

  private static final Path SETTINGS_PATH = Path.of(System.getProperty("user.home"), ".mineandcraft", "settings.properties");

  private SettingsIO() {
  }

  public static void load(GameSettings settings) {
    if (!Files.exists(SETTINGS_PATH)) {
      return;
    }

    Properties properties = new Properties();
    try (InputStream in = Files.newInputStream(SETTINGS_PATH)) {
      properties.load(in);
    } catch (IOException e) {
      return;
    }

    settings.setMouseSensitivity(readFloat(properties, "mouseSensitivity", settings.getMouseSensitivity()));
    settings.setFov(readFloat(properties, "fov", settings.getFov()));
    settings.setRenderDistance(readInt(properties, "renderDistance", settings.getRenderDistance()));
    settings.setFogEnabled(readBoolean(properties, "fogEnabled", settings.isFogEnabled()));
    settings.setVsync(readBoolean(properties, "vsync", settings.isVsync()));
    settings.setShowDebug(readBoolean(properties, "showDebug", settings.isShowDebug()));
    settings.setSpeedMultiplier(readFloat(properties, "speedMultiplier", settings.getSpeedMultiplier()));
  }

  public static void save(GameSettings settings) {
    try {
      Files.createDirectories(SETTINGS_PATH.getParent());
    } catch (IOException e) {
      return;
    }

    Properties properties = new Properties();
    properties.setProperty("mouseSensitivity", Float.toString(settings.getMouseSensitivity()));
    properties.setProperty("fov", Float.toString(settings.getFov()));
    properties.setProperty("renderDistance", Integer.toString(settings.getRenderDistance()));
    properties.setProperty("fogEnabled", Boolean.toString(settings.isFogEnabled()));
    properties.setProperty("vsync", Boolean.toString(settings.isVsync()));
    properties.setProperty("showDebug", Boolean.toString(settings.isShowDebug()));
    properties.setProperty("speedMultiplier", Float.toString(settings.getSpeedMultiplier()));

    try (OutputStream out = Files.newOutputStream(SETTINGS_PATH)) {
      properties.store(out, "MineAndCraft settings");
    } catch (IOException ignored) {
    }
  }

  private static float readFloat(Properties properties, String key, float fallback) {
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

  private static int readInt(Properties properties, String key, int fallback) {
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

  private static boolean readBoolean(Properties properties, String key, boolean fallback) {
    String value = properties.getProperty(key);
    if (value == null) {
      return fallback;
    }

    return Boolean.parseBoolean(value);
  }
}
