package com.mineandcraft.player;

import com.mineandcraft.world.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HotbarTest {

  @Test
  void scrollWrapsAround() {
    Hotbar hotbar = new Hotbar();

    hotbar.scroll(1);
    assertEquals(Hotbar.SIZE - 1, hotbar.getSelectedSlot());
    hotbar.scroll(-1);
    assertEquals(0, hotbar.getSelectedSlot());
  }

  @Test
  void selectIgnoresInvalidSlots() {
    Hotbar hotbar = new Hotbar();
    hotbar.selectSlot(4);
    hotbar.selectSlot(42);
    assertEquals(4, hotbar.getSelectedSlot());
  }

  @Test
  void pickSelectsExistingSlotOrReplacesCurrent() {
    Hotbar hotbar = new Hotbar();

    hotbar.pick(Block.STONE);
    assertEquals(Block.STONE, hotbar.getSelectedBlock());
    int stoneSlot = hotbar.getSelectedSlot();

    hotbar.pick(Block.IRON_ORE);
    assertEquals(stoneSlot, hotbar.getSelectedSlot());
    assertEquals(Block.IRON_ORE, hotbar.getSelectedBlock());

    hotbar.pick(Block.BEDROCK);
    assertEquals(Block.IRON_ORE, hotbar.getSelectedBlock(), "бедрок нельзя взять");
  }
}
