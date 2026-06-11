package com.mineandcraft.player;

import com.mineandcraft.engine.Raycast;
import com.mineandcraft.world.Block;
import com.mineandcraft.world.World;

public class BlockBreaker {

  private int targetX = Integer.MIN_VALUE;
  private int targetY;
  private int targetZ;
  private byte targetBlock;
  private float progress;

  public float getProgress() {
    return progress;
  }

  public void reset() {
    targetX = Integer.MIN_VALUE;
    progress = 0f;
  }

  public void update(World world, Raycast.Hit hit, boolean breaking, float deltaTime) {
    if (!breaking || hit == null || !hit.hit()) {
      reset();
      return;
    }

    byte block = world.getBlock(hit.blockX(), hit.blockY(), hit.blockZ());
    if (!Block.isBreakable(block)) {
      reset();
      return;
    }

    if (hit.blockX() != targetX || hit.blockY() != targetY || hit.blockZ() != targetZ) {
      targetX = hit.blockX();
      targetY = hit.blockY();
      targetZ = hit.blockZ();
      targetBlock = block;
      progress = 0f;
    }

    float hardness = Block.getHardness(targetBlock);
    if (hardness < 0f) {
      reset();
      return;
    }

    progress += deltaTime / hardness;

    if (progress >= 1f) {
      world.setBlock(targetX, targetY, targetZ, Block.AIR);
      reset();
    }
  }
}
