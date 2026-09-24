package com.mineandcraft.player;

import com.mineandcraft.TestWorlds;
import com.mineandcraft.engine.Raycast;
import com.mineandcraft.world.Block;
import com.mineandcraft.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockBreakerTest {

  @Test
  void blockBreaksAfterItsHardnessTime() {
    World world = TestWorlds.flat(10);
    BlockBreaker breaker = new BlockBreaker();
    Raycast.Hit hit = new Raycast.Hit(true, 0, 9, 0, 0, 10, 0);
    float hardness = Block.getHardness(Block.STONE);

    boolean broken = false;
    float time = 0;
    while (!broken && time < 10) {
      broken = breaker.update(world, hit, true, 0.05f);
      time += 0.05f;
    }

    assertTrue(broken);
    assertEquals(hardness, time, 0.1f);
    assertEquals(Block.AIR, world.getBlock(0, 9, 0));
  }

  @Test
  void releasingButtonResetsProgress() {
    World world = TestWorlds.flat(10);
    BlockBreaker breaker = new BlockBreaker();
    Raycast.Hit hit = new Raycast.Hit(true, 0, 9, 0, 0, 10, 0);

    breaker.update(world, hit, true, 0.5f);
    assertTrue(breaker.getProgress() > 0);

    breaker.update(world, hit, false, 0.05f);
    assertEquals(0f, breaker.getProgress());
  }

  @Test
  void bedrockIsUnbreakable() {
    World world = TestWorlds.flat(10);
    world.getChunk(0, 0).setLocal(0, 9, 0, Block.BEDROCK);
    BlockBreaker breaker = new BlockBreaker();
    Raycast.Hit hit = new Raycast.Hit(true, 0, 9, 0, 0, 10, 0);

    for (int i = 0; i < 100; i++) {
      assertFalse(breaker.update(world, hit, true, 0.1f));
    }
    assertEquals(Block.BEDROCK, world.getBlock(0, 9, 0));
  }
}
