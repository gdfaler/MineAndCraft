package com.mineandcraft.config;

import com.mineandcraft.world.World;

public class GameSettings {

  public static final float MIN_SENSITIVITY = 0.04f;
  public static final float MAX_SENSITIVITY = 0.28f;
  public static final float MIN_FOV = 60f;
  public static final float MAX_FOV = 100f;
  public static final int MIN_RENDER_DISTANCE = World.MIN_VIEW_DISTANCE;
  public static final int MAX_RENDER_DISTANCE = World.MAX_VIEW_DISTANCE;
  public static final float MIN_SPEED = 0.5f;
  public static final float MAX_SPEED = 2f;

  private float mouseSensitivity = 0.12f;
  private float fov = 70f;
  private int renderDistance = 8;
  private boolean fogEnabled = true;
  private boolean vsync = true;
  private boolean showDebug = false;
  private float speedMultiplier = 1f;
  private boolean rawMouseInput = true;

  public float getMouseSensitivity() {
    return mouseSensitivity;
  }

  public void setMouseSensitivity(float mouseSensitivity) {
    this.mouseSensitivity = clamp(mouseSensitivity, MIN_SENSITIVITY, MAX_SENSITIVITY);
  }

  public float getFov() {
    return fov;
  }

  public void setFov(float fov) {
    this.fov = clamp(fov, MIN_FOV, MAX_FOV);
  }

  public int getRenderDistance() {
    return renderDistance;
  }

  public void setRenderDistance(int renderDistance) {
    this.renderDistance = clamp(renderDistance, MIN_RENDER_DISTANCE, MAX_RENDER_DISTANCE);
  }

  public boolean isFogEnabled() {
    return fogEnabled;
  }

  public void setFogEnabled(boolean fogEnabled) {
    this.fogEnabled = fogEnabled;
  }

  public boolean isVsync() {
    return vsync;
  }

  public void setVsync(boolean vsync) {
    this.vsync = vsync;
  }

  public boolean isShowDebug() {
    return showDebug;
  }

  public void setShowDebug(boolean showDebug) {
    this.showDebug = showDebug;
  }

  public float getSpeedMultiplier() {
    return speedMultiplier;
  }

  public void setSpeedMultiplier(float speedMultiplier) {
    this.speedMultiplier = clamp(speedMultiplier, MIN_SPEED, MAX_SPEED);
  }

  public boolean isRawMouseInput() {
    return rawMouseInput;
  }

  public void setRawMouseInput(boolean rawMouseInput) {
    this.rawMouseInput = rawMouseInput;
  }

  private static float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
}
