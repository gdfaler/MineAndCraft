package com.mineandcraft.player;

import com.mineandcraft.TestWorlds;
import com.mineandcraft.world.Block;
import com.mineandcraft.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTest {

  private static final int GROUND = 20;
  private static final float DT = 1f / 60f;

  private static void simulate(Player player, World world, Player.Controls controls, float seconds) {
    for (float t = 0; t < seconds; t += DT) {
      player.update(world, controls, 1f, DT);
    }
  }

  @Test
  void fallsAndLandsOnGround() {
    World world = TestWorlds.flat(GROUND);
    Player player = new Player();
    player.setPosition(0.5f, GROUND + 10, 0.5f);

    simulate(player, world, new Player.Controls(), 3f);

    assertTrue(player.isOnGround());
    assertEquals(GROUND, player.getPosition().y, 0.01f);
  }

  @Test
  void fastFallDoesNotTunnelThroughThinFloor() {
    World world = TestWorlds.flat(GROUND);
    world.setBlock(0, 60, 0, Block.STONE);
    Player player = new Player();
    player.setPosition(0.5f, 120, 0.5f);

    // Шаг кадра намеренно большой: скорость падения достигает десятков блоков в секунду.
    for (int i = 0; i < 200; i++) {
      player.update(world, new Player.Controls(), 1f, 0.1f);
    }

    assertEquals(61f, player.getPosition().y, 0.01f);
  }

  @Test
  void wallStopsMovement() {
    World world = TestWorlds.flat(GROUND);
    for (int y = GROUND; y < GROUND + 3; y++) {
      for (int z = -3; z <= 3; z++) {
        world.setBlock(3, y, z, Block.STONE);
      }
    }

    Player player = new Player();
    player.setPosition(0.5f, GROUND, 0.5f);
    player.setRotation(90f, 0f); // yaw 90° — смотрим вдоль +X
    Player.Controls controls = new Player.Controls();
    controls.forward = 1;

    simulate(player, world, controls, 3f);

    float maxX = 3f - Player.HALF_WIDTH;
    assertTrue(player.getPosition().x <= maxX, "игрок прошёл сквозь стену: x = " + player.getPosition().x);
    assertTrue(player.getPosition().x > maxX - 0.05f, "игрок не дошёл до стены: x = " + player.getPosition().x);
  }

  @Test
  void jumpClearsOneBlockButNotTwo() {
    World world = TestWorlds.flat(GROUND);
    Player player = new Player();
    player.setPosition(0.5f, GROUND, 0.5f);
    simulate(player, world, new Player.Controls(), 0.2f);

    Player.Controls controls = new Player.Controls();
    controls.jump = true;
    float maxY = 0;
    for (int i = 0; i < 60; i++) {
      player.update(world, controls, 1f, DT);
      controls.jump = false;
      maxY = Math.max(maxY, player.getPosition().y);
    }

    float jumpHeight = maxY - GROUND;
    assertTrue(jumpHeight > 1.05f && jumpHeight < 2f, "высота прыжка " + jumpHeight);
  }

  @Test
  void cannotJumpInMidAir() {
    World world = TestWorlds.flat(GROUND);
    Player player = new Player();
    player.setPosition(0.5f, GROUND + 20, 0.5f);

    Player.Controls controls = new Player.Controls();
    controls.jump = true;
    player.update(world, controls, 1f, DT);

    assertFalse(player.isOnGround());
    assertTrue(player.getPosition().y < GROUND + 20);
  }

  @Test
  void flyingIgnoresGravityAndLandingEndsFlight() {
    World world = TestWorlds.flat(GROUND);
    Player player = new Player();
    player.setPosition(0.5f, GROUND + 5, 0.5f);
    player.setFlying(true);

    simulate(player, world, new Player.Controls(), 1f);
    assertEquals(GROUND + 5, player.getPosition().y, 0.01f);

    Player.Controls down = new Player.Controls();
    down.descend = true;
    simulate(player, world, down, 3f);

    assertFalse(player.isFlying());
    assertEquals(GROUND, player.getPosition().y, 0.01f);
  }

  @Test
  void doesNotMoveWhileChunkIsNotLoaded() {
    World world = TestWorlds.flat(GROUND);
    Player player = new Player();
    player.setPosition(10_000.5f, 50, 0.5f);

    simulate(player, world, new Player.Controls(), 1f);

    assertEquals(50f, player.getPosition().y);
  }

  @Test
  void pitchIsClampedAndYawWraps() {
    Player player = new Player();

    player.setRotation(370f, 120f);
    assertEquals(10f, player.getYaw(), 0.001f);
    assertTrue(player.getPitch() < 90f);

    player.setRotation(-30f, -120f);
    assertEquals(330f, player.getYaw(), 0.001f);
    assertTrue(player.getPitch() > -90f);
  }

  @Test
  void intersectsBlockUnderFeetOnlyWhenOverlapping() {
    Player player = new Player();
    player.setPosition(0.5f, GROUND, 0.5f);

    assertFalse(player.intersectsBlock(0, GROUND - 1, 0), "блок под ногами не пересекает игрока");
    assertTrue(player.intersectsBlock(0, GROUND, 0));
    assertTrue(player.intersectsBlock(0, GROUND + 1, 0));
    assertFalse(player.intersectsBlock(0, GROUND + 2, 0));
    assertFalse(player.intersectsBlock(1, GROUND, 0));
  }
}
