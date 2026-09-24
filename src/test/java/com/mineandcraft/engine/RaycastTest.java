package com.mineandcraft.engine;

import com.mineandcraft.TestWorlds;
import com.mineandcraft.world.Block;
import com.mineandcraft.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaycastTest {

  private static final int GROUND = 20;

  @Test
  void lookingDownHitsGroundAndPlacesOnTop() {
    World world = TestWorlds.flat(GROUND);

    Raycast.Hit hit = Raycast.cast(world, 0.5f, GROUND + 1.6f, 0.5f, 0, -1, 0, 5);

    assertTrue(hit.hit());
    assertEquals(0, hit.blockX());
    assertEquals(GROUND - 1, hit.blockY());
    assertEquals(0, hit.blockZ());
    assertEquals(GROUND, hit.placeY());
  }

  @Test
  void horizontalRayHitsWallAndPlacesInFront() {
    World world = TestWorlds.flat(GROUND);
    world.setBlock(3, GROUND, -1, Block.STONE);

    Raycast.Hit hit = Raycast.cast(world, 0.5f, GROUND + 0.5f, -0.5f, 1, 0, 0, 5);

    assertTrue(hit.hit());
    assertEquals(3, hit.blockX());
    assertEquals(-1, hit.blockZ());
    assertEquals(2, hit.placeX());
    assertEquals(GROUND, hit.placeY());
    assertEquals(-1, hit.placeZ());
  }

  @Test
  void blockBeyondReachIsMissed() {
    World world = TestWorlds.flat(GROUND);
    world.setBlock(8, GROUND, 0, Block.STONE);

    assertFalse(Raycast.cast(world, 0.5f, GROUND + 0.5f, 0.5f, 1, 0, 0, 5).hit());
  }

  @Test
  void placePositionIsAdjacentToHitFaceForDiagonalRay() {
    World world = TestWorlds.flat(GROUND);

    for (int i = 0; i < 200; i++) {
      float dx = (float) Math.cos(i * 0.37);
      float dz = (float) Math.sin(i * 0.37);
      Raycast.Hit hit = Raycast.cast(world, 0.3f, GROUND + 1.62f, 0.7f, dx, -0.6f, dz, 5);

      assertTrue(hit.hit());
      int distance = Math.abs(hit.blockX() - hit.placeX())
          + Math.abs(hit.blockY() - hit.placeY())
          + Math.abs(hit.blockZ() - hit.placeZ());
      assertEquals(1, distance, "место установки должно соседствовать с гранью, а не с углом");
      assertEquals(Block.AIR, world.getBlock(hit.placeX(), hit.placeY(), hit.placeZ()));
    }
  }

  @Test
  void rayDoesNotSkipThinDiagonalGaps() {
    World world = TestWorlds.flat(GROUND);
    // Блок по диагонали: пошаговый луч с шагом 0.05 мог «проскочить» угол.
    world.setBlock(1, GROUND, 1, Block.STONE);

    Raycast.Hit hit = Raycast.cast(world, 0.5f, GROUND + 0.5f, 0.5f, 1, 0, 1.0001f, 5);

    assertTrue(hit.hit());
    assertEquals(1, hit.blockX());
    assertEquals(1, hit.blockZ());
  }

  @Test
  void zeroDirectionIsMiss() {
    World world = TestWorlds.flat(GROUND);
    assertFalse(Raycast.cast(world, 0.5f, GROUND + 1f, 0.5f, 0, 0, 0, 5).hit());
  }
}
