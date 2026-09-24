package com.mineandcraft.engine;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

/** Окно GLFW с контекстом OpenGL 3.3 Core. Отслеживает размер окна и фреймбуфера (HiDPI). */
public class Window {

  private final Input input = new Input();

  private long handle;
  private int windowWidth;
  private int windowHeight;
  private int framebufferWidth;
  private int framebufferHeight;
  private boolean focused = true;
  private boolean focusLost;

  public Window(String title, int width, int height) {
    GLFWErrorCallback.createPrint(System.err).set();

    if (!GLFW.glfwInit()) {
      throw new IllegalStateException("Unable to initialize GLFW");
    }

    GLFW.glfwDefaultWindowHints();
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

    handle = GLFW.glfwCreateWindow(width, height, title, 0, 0);
    if (handle == 0) {
      GLFW.glfwTerminate();
      throw new IllegalStateException("Failed to create GLFW window (нужен OpenGL 3.3)");
    }

    GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
    if (mode != null) {
      GLFW.glfwSetWindowPos(handle, (mode.width() - width) / 2, (mode.height() - height) / 2);
    }

    GLFW.glfwMakeContextCurrent(handle);
    GL.createCapabilities();

    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer w = stack.mallocInt(1);
      IntBuffer h = stack.mallocInt(1);
      GLFW.glfwGetWindowSize(handle, w, h);
      windowWidth = w.get(0);
      windowHeight = h.get(0);
      GLFW.glfwGetFramebufferSize(handle, w, h);
      framebufferWidth = w.get(0);
      framebufferHeight = h.get(0);
    }
    GL11.glViewport(0, 0, framebufferWidth, framebufferHeight);

    GLFW.glfwSetWindowSizeCallback(handle, (win, w, h) -> {
      windowWidth = w;
      windowHeight = h;
    });
    GLFW.glfwSetFramebufferSizeCallback(handle, (win, w, h) -> {
      framebufferWidth = w;
      framebufferHeight = h;
      GL11.glViewport(0, 0, w, h);
    });
    GLFW.glfwSetWindowFocusCallback(handle, (win, isFocused) -> {
      // Паузу вызывает только реальная потеря фокуса, а не начальное состояние окна.
      if (focused && !isFocused) {
        focusLost = true;
        input.releaseAll();
      }
      focused = isFocused;
    });

    input.install(handle);
    GLFW.glfwShowWindow(handle);
    focused = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
  }

  public Input getInput() {
    return input;
  }

  public long getHandle() {
    return handle;
  }

  public boolean shouldClose() {
    return GLFW.glfwWindowShouldClose(handle);
  }

  public void requestClose() {
    GLFW.glfwSetWindowShouldClose(handle, true);
  }

  public void pollEvents() {
    GLFW.glfwPollEvents();
  }

  public void swapBuffers() {
    GLFW.glfwSwapBuffers(handle);
  }

  public void setVsync(boolean vsync) {
    GLFW.glfwSwapInterval(vsync ? 1 : 0);
  }

  public void setCursorCaptured(boolean captured) {
    GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, captured ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
    input.resetMouseDelta();
  }

  /** Необработанное движение мыши (без ускорения ОС), работает только при захваченном курсоре. */
  public void setRawMouseMotion(boolean enabled) {
    if (GLFW.glfwRawMouseMotionSupported()) {
      GLFW.glfwSetInputMode(handle, GLFW.GLFW_RAW_MOUSE_MOTION, enabled ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
    }
  }

  /** Возвращает true один раз после потери фокуса окном. */
  public boolean consumeFocusLost() {
    boolean lost = focusLost;
    focusLost = false;
    return lost;
  }

  public boolean isFocused() {
    return focused;
  }

  public boolean isMinimized() {
    return framebufferWidth == 0 || framebufferHeight == 0;
  }

  public int getWindowWidth() {
    return windowWidth;
  }

  public int getWindowHeight() {
    return windowHeight;
  }

  public float getAspectRatio() {
    return framebufferHeight == 0 ? 1f : (float) framebufferWidth / framebufferHeight;
  }

  public void destroy() {
    if (handle != 0) {
      org.lwjgl.glfw.Callbacks.glfwFreeCallbacks(handle);
      GLFW.glfwDestroyWindow(handle);
      handle = 0;
    }

    GLFW.glfwTerminate();
    GLFWErrorCallback callback = GLFW.glfwSetErrorCallback(null);
    if (callback != null) {
      callback.free();
    }
  }
}
