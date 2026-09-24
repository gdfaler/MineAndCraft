package com.mineandcraft.player;

import com.mineandcraft.engine.Raycast;
import com.mineandcraft.world.Block;
import com.mineandcraft.world.World;

public class BlockBreaker {

  /** Пауза после разрушения, чтобы при зажатой кнопке блоки не исчезали очередью. */
  private static final float COOLDOWN = 0.15f;

  private int targetX = Integer.MIN_VALUE;
  private int targetY;
  private int targetZ;
  private float progress;
  private float cooldown;

  public float getProgress() {
    return progress;
  }

  public void reset() {
    targetX = Integer.MIN_VALUE;
    progress = 0f;
  }

  /** Возвращает true, если в этом кадре блок был разрушен. */
  public boolean update(World world, Raycast.Hit hit, boolean breaking, float deltaTime) {
    cooldown = Math.max(0f, cooldown - deltaTime);

    if (!breaking || hit == null || !hit.hit()) {
      reset();
      return false;
    }

    byte block = world.getBlock(hit.blockX(), hit.blockY(), hit.blockZ());
    float hardness = Block.getHardness(block);
    if (!Block.isBreakable(block) || hardness < 0f) {
      reset();
      return false;
    }

    if (hit.blockX() != targetX || hit.blockY() != targetY || hit.blockZ() != targetZ) {
      targetX = hit.blockX();
      targetY = hit.blockY();
      targetZ = hit.blockZ();
      progress = 0f;
    }

    if (cooldown > 0f) {
      return false;
    }

    progress += deltaTime / hardness;

    if (progress >= 1f) {
      world.setBlock(targetX, targetY, targetZ, Block.AIR);
      reset();
      cooldown = COOLDOWN;
      return true;
    }

    return false;
  }
}
