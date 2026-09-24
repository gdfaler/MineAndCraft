package com.mineandcraft.engine;

import org.lwjgl.glfw.GLFW;

/**
 * Состояние клавиатуры и мыши, собранное через колбэки GLFW.
 * «Нажатия» (wasPressed) и смещения мыши живут один кадр и сбрасываются в {@link #endFrame()}.
 */
public class Input {

  private final boolean[] keysDown = new boolean[GLFW.GLFW_KEY_LAST + 1];
  private final boolean[] keysPressed = new boolean[GLFW.GLFW_KEY_LAST + 1];
  private final boolean[] buttonsDown = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];
  private final boolean[] buttonsPressed = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];

  private double cursorX;
  private double cursorY;
  private double mouseDeltaX;
  private double mouseDeltaY;
  private boolean ignoreNextMove = true;
  private double scroll;

  public void install(long window) {
    GLFW.glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
      if (key < 0 || key > GLFW.GLFW_KEY_LAST) {
        return;
      }
      if (action == GLFW.GLFW_PRESS) {
        keysDown[key] = true;
        keysPressed[key] = true;
      } else if (action == GLFW.GLFW_RELEASE) {
        keysDown[key] = false;
      }
    });

    GLFW.glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
      if (button < 0 || button > GLFW.GLFW_MOUSE_BUTTON_LAST) {
        return;
      }
      if (action == GLFW.GLFW_PRESS) {
        buttonsDown[button] = true;
        buttonsPressed[button] = true;
      } else if (action == GLFW.GLFW_RELEASE) {
        buttonsDown[button] = false;
      }
    });

    GLFW.glfwSetCursorPosCallback(window, (w, x, y) -> {
      if (ignoreNextMove) {
        ignoreNextMove = false;
      } else {
        mouseDeltaX += x - cursorX;
        mouseDeltaY += y - cursorY;
      }
      cursorX = x;
      cursorY = y;
    });

    GLFW.glfwSetScrollCallback(window, (w, xOffset, yOffset) -> scroll += yOffset);
  }

  public void endFrame() {
    java.util.Arrays.fill(keysPressed, false);
    java.util.Arrays.fill(buttonsPressed, false);
    mouseDeltaX = 0;
    mouseDeltaY = 0;
    scroll = 0;
  }

  /** Сбрасывает накопленное смещение: после смены режима курсора GLFW присылает скачок координат. */
  public void resetMouseDelta() {
    mouseDeltaX = 0;
    mouseDeltaY = 0;
    ignoreNextMove = true;
  }

  /** Отпускает все клавиши, например при потере фокуса окна. */
  public void releaseAll() {
    java.util.Arrays.fill(keysDown, false);
    java.util.Arrays.fill(buttonsDown, false);
  }

  public boolean isKeyDown(int key) {
    return key >= 0 && key < keysDown.length && keysDown[key];
  }

  public boolean wasKeyPressed(int key) {
    return key >= 0 && key < keysPressed.length && keysPressed[key];
  }

  public boolean isMouseDown(int button) {
    return buttonsDown[button];
  }

  public boolean wasMousePressed(int button) {
    return buttonsPressed[button];
  }

  public double getMouseDeltaX() {
    return mouseDeltaX;
  }

  public double getMouseDeltaY() {
    return mouseDeltaY;
  }

  public double getCursorX() {
    return cursorX;
  }

  public double getCursorY() {
    return cursorY;
  }

  public double getScroll() {
    return scroll;
  }
}
