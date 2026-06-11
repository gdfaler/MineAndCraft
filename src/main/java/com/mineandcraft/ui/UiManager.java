package com.mineandcraft.ui;

import com.mineandcraft.config.GameSettings;
import com.mineandcraft.config.SettingsIO;
import com.mineandcraft.graphics.FontRenderer;
import com.mineandcraft.graphics.ShaderProgram;
import com.mineandcraft.graphics.UiDrawer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class UiManager {

  public enum Screen {
    GAME,
    PAUSE,
    SETTINGS
  }

  private final GameSettings settings;
  private final Runnable onResume;
  private final Runnable onQuit;
  private final Runnable onSettingsApplied;

  private Screen screen = Screen.GAME;
  private FontRenderer font;
  private final List<UiButton> pauseButtons = new ArrayList<>();
  private final List<UiButton> settingsButtons = new ArrayList<>();
  private final List<UiSlider> sliders = new ArrayList<>();
  private final List<UiToggle> toggles = new ArrayList<>();

  private boolean mousePressed;
  private float mouseX;
  private float mouseY;

  public UiManager(GameSettings settings, Runnable onResume, Runnable onQuit, Runnable onSettingsApplied) {
    this.settings = settings;
    this.onResume = onResume;
    this.onQuit = onQuit;
    this.onSettingsApplied = onSettingsApplied;
    buildUi();
  }

  public void initFont() {
    if (font == null) {
      font = new FontRenderer(28);
    }
  }

  private void buildUi() {
    pauseButtons.clear();
    settingsButtons.clear();
    sliders.clear();
    toggles.clear();

    pauseButtons.add(new UiButton("Продолжить", -0.18f, 0.08f, 0.36f, 0.09f, this::resume));
    pauseButtons.add(new UiButton("Настройки", -0.18f, -0.04f, 0.36f, 0.09f, this::openSettings));
    pauseButtons.add(new UiButton("Выйти", -0.18f, -0.16f, 0.36f, 0.09f, onQuit));

    settingsButtons.add(new UiButton("Назад", -0.18f, -0.78f, 0.36f, 0.08f, this::backToPause));
    settingsButtons.add(new UiButton("Сохранить", -0.18f, -0.88f, 0.36f, 0.08f, this::saveSettings));

    float sliderX = -0.34f;
    float sliderW = 0.48f;
    sliders.add(new UiSlider(
        "Чувствительность мыши",
        sliderX, 0.52f, sliderW,
        GameSettings.MIN_SENSITIVITY, GameSettings.MAX_SENSITIVITY,
        settings::getMouseSensitivity, settings::setMouseSensitivity, "%.2f"
    ));
    sliders.add(new UiSlider(
        "Поле зрения (FOV)",
        sliderX, 0.34f, sliderW,
        GameSettings.MIN_FOV, GameSettings.MAX_FOV,
        settings::getFov, settings::setFov, "%.0f"
    ));
    sliders.add(new UiSlider(
        "Дальность прорисовки",
        sliderX, 0.16f, sliderW,
        GameSettings.MIN_RENDER_DISTANCE, GameSettings.MAX_RENDER_DISTANCE,
        () -> (float) settings.getRenderDistance(),
        value -> settings.setRenderDistance(Math.round(value)),
        "%.0f чанков"
    ));
    sliders.add(new UiSlider(
        "Скорость движения",
        sliderX, -0.02f, sliderW,
        GameSettings.MIN_SPEED, GameSettings.MAX_SPEED,
        settings::getSpeedMultiplier, settings::setSpeedMultiplier, "%.1fx"
    ));

    toggles.add(new UiToggle(
        "Туман",
        sliderX, -0.22f, 0.68f, 0.07f,
        settings::isFogEnabled, settings::setFogEnabled
    ));
    toggles.add(new UiToggle(
        "Вертикальная синхронизация",
        sliderX, -0.34f, 0.68f, 0.07f,
        settings::isVsync, settings::setVsync
    ));
    toggles.add(new UiToggle(
        "Отладочная информация",
        sliderX, -0.46f, 0.68f, 0.07f,
        settings::isShowDebug, settings::setShowDebug
    ));
  }

  public Screen getScreen() {
    return screen;
  }

  public boolean isMenuOpen() {
    return screen != Screen.GAME;
  }

  public void togglePause() {
    if (screen == Screen.GAME) {
      screen = Screen.PAUSE;
    } else if (screen == Screen.PAUSE) {
      resume();
    } else {
      backToPause();
    }
  }

  public void resume() {
    screen = Screen.GAME;
    onResume.run();
  }

  public void openSettings() {
    screen = Screen.SETTINGS;
  }

  public void backToPause() {
    screen = Screen.PAUSE;
    onSettingsApplied.run();
  }

  private void saveSettings() {
    SettingsIO.save(settings);
    onSettingsApplied.run();
  }

  public void handleKey(long window, int key) {
    if (key == GLFW.GLFW_KEY_ESCAPE) {
      togglePause();
      sleepKey(window, GLFW.GLFW_KEY_ESCAPE);
    }
  }

  public void updateMouse(long window, int windowWidth, int windowHeight) {
    if (!isMenuOpen()) {
      mousePressed = false;
      return;
    }

    double[] x = new double[1];
    double[] y = new double[1];
    GLFW.glfwGetCursorPos(window, x, y);

    mouseX = toNdcX((float) x[0], windowWidth);
    mouseY = toNdcY((float) y[0], windowHeight);

    List<UiButton> buttons = screen == Screen.PAUSE ? pauseButtons : settingsButtons;
    for (UiButton button : buttons) {
      button.updateHover(mouseX, mouseY);
    }

    for (UiToggle toggle : toggles) {
      toggle.updateHover(mouseX, mouseY);
    }

    boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

    if (down && !mousePressed) {
      for (UiButton button : buttons) {
        if (button.click(mouseX, mouseY)) {
          break;
        }
      }

      if (screen == Screen.SETTINGS) {
        for (UiToggle toggle : toggles) {
          toggle.click(mouseX, mouseY);
        }

        for (UiSlider slider : sliders) {
          slider.mousePressed(mouseX, mouseY);
        }
      }
    }

    if (!down) {
      for (UiSlider slider : sliders) {
        slider.mouseReleased();
      }
    } else if (screen == Screen.SETTINGS) {
      for (UiSlider slider : sliders) {
        slider.mouseDragged(mouseX, mouseY);
      }
    }

    mousePressed = down;
  }

  public void render(ShaderProgram colorShader, ShaderProgram textShader) {
    renderMenu(colorShader, textShader);
  }

  private void renderMenu(ShaderProgram colorShader, ShaderProgram textShader) {
    if (!isMenuOpen() || font == null) {
      return;
    }

    UiDrawer.begin();
    UiDrawer.fill(colorShader, -1f, -1f, 1f, 1f, 0f, 0f, 0f, 0.55f);

    if (screen == Screen.PAUSE) {
      renderPause(colorShader, textShader);
    } else {
      renderSettings(colorShader, textShader);
    }

    UiDrawer.end();
  }

  private void renderPause(ShaderProgram colorShader, ShaderProgram textShader) {
    UiDrawer.panel(colorShader, -0.28f, -0.24f, 0.28f, 0.24f, 0.92f);

    float titleScale = 0.05f;
    String title = "ПАУЗА";
    float titleWidth = font.measureText(title, titleScale);
    font.drawText(textShader, title, -titleWidth * 0.5f, 0.18f, titleScale, 1f, 1f, 1f, 1f);

    for (UiButton button : pauseButtons) {
      button.render(colorShader, textShader, font);
    }
  }

  private void renderSettings(ShaderProgram colorShader, ShaderProgram textShader) {
    UiDrawer.panel(colorShader, -0.42f, -0.95f, 0.42f, 0.62f, 0.94f);

    float titleScale = 0.045f;
    String title = "НАСТРОЙКИ";
    float titleWidth = font.measureText(title, titleScale);
    font.drawText(textShader, title, -titleWidth * 0.5f, 0.56f, titleScale, 1f, 1f, 1f, 1f);

    for (UiSlider slider : sliders) {
      slider.render(colorShader, textShader, font);
    }

    for (UiToggle toggle : toggles) {
      toggle.render(colorShader, textShader, font);
    }

    for (UiButton button : settingsButtons) {
      button.render(colorShader, textShader, font);
    }
  }

  public void drawDebug(ShaderProgram colorShader, ShaderProgram textShader, String text) {
    if (!settings.isShowDebug() || font == null) {
      return;
    }

    UiDrawer.begin();
    UiDrawer.fill(colorShader, -0.99f, 0.82f, -0.45f, 0.98f, 0f, 0f, 0f, 0.45f);
    font.drawText(textShader, text, -0.97f, 0.84f, 0.022f, 0.95f, 0.95f, 0.95f, 1f);
    UiDrawer.end();
  }

  public void delete() {
    if (font != null) {
      font.delete();
      font = null;
    }
  }

  private static float toNdcX(float pixelX, int width) {
    return pixelX / width * 2f - 1f;
  }

  private static float toNdcY(float pixelY, int height) {
    return 1f - pixelY / height * 2f;
  }

  private static void sleepKey(long window, int key) {
    while (GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS) {
      GLFW.glfwPollEvents();
    }
  }
}
