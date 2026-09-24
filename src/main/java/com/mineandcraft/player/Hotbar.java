package com.mineandcraft.player;

import com.mineandcraft.world.Block;

public class Hotbar {

  public static final int SIZE = 9;

  private final byte[] slots = {
      Block.GRASS,
      Block.DIRT,
      Block.STONE,
      Block.COBBLESTONE,
      Block.WOOD,
      Block.PLANKS,
      Block.GLASS,
      Block.SAND,
      Block.LEAVES
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

  /** Прокрутка колёсика: вверх — предыдущий слот, вниз — следующий, по кругу. */
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

  /**
   * «Выбор блока» средней кнопкой: если блок уже есть в хотбаре — переключаемся на него,
   * иначе кладём его в текущий слот.
   */
  public void pick(byte block) {
    if (!Block.isBreakable(block)) {
      return;
    }

    for (int i = 0; i < SIZE; i++) {
      if (slots[i] == block) {
        selected = i;
        return;
      }
    }

    slots[selected] = block;
  }
}
