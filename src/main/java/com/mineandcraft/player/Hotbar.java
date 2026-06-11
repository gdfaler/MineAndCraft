package com.mineandcraft.player;

import com.mineandcraft.world.Block;
import org.lwjgl.glfw.GLFW;

public class Hotbar {

  public static final int SIZE = 9;

  private final byte[] slots = {
      Block.GRASS,
      Block.DIRT,
      Block.STONE,
      Block.WOOD,
      Block.PLANKS,
      Block.SAND,
      Block.LEAVES,
      Block.STONE,
      Block.DIRT
  };

  private int selected;

  public byte getSelectedBlock() {
    return slots[selected];
  }

  public int getSelectedSlot() {
    return selected;
  }

  public byte getSlot(int index) {
    return slots[index];
  }

  public void scroll(int delta) {
    if (delta > 0) {
      selected = (selected + SIZE - 1) % SIZE;
    } else if (delta < 0) {
      selected = (selected + 1) % SIZE;
    }
  }

  public void selectSlot(int index) {
    if (index >= 0 && index < SIZE) {
      selected = index;
    }
  }

  public void handleKeys(long window) {
    for (int i = 0; i < SIZE; i++) {
      if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_1 + i) == GLFW.GLFW_PRESS) {
        selectSlot(i);
        sleepKey(window, GLFW.GLFW_KEY_1 + i);
      }
    }
  }

  private static void sleepKey(long window, int key) {
    while (GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS) {
      GLFW.glfwPollEvents();
    }
  }
}
