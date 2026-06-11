package com.mineandcraft.engine;

import com.mineandcraft.world.Block;
import com.mineandcraft.world.World;

public final class Raycast {

  public record Hit(
      boolean hit,
      int blockX,
      int blockY,
      int blockZ,
      int placeX,
      int placeY,
      int placeZ
  ) {
    public static Hit miss() {
      return new Hit(false, 0, 0, 0, 0, 0, 0);
    }
  }

  private Raycast() {
  }

  public static Hit cast(World world, float ox, float oy, float oz, float dx, float dy, float dz, float maxDistance) {
    float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (length < 1e-5f) {
      return Hit.miss();
    }

    dx /= length;
    dy /= length;
    dz /= length;

    int lastX = Integer.MIN_VALUE;
    int lastY = Integer.MIN_VALUE;
    int lastZ = Integer.MIN_VALUE;

    float x = ox;
    float y = oy;
    float z = oz;
    float distance = 0f;
    float step = 0.05f;

    while (distance <= maxDistance) {
      int blockX = floor(x);
      int blockY = floor(y);
      int blockZ = floor(z);

      if (Block.isSolid(world.getBlock(blockX, blockY, blockZ))) {
        if (lastX == Integer.MIN_VALUE) {
          return Hit.miss();
        }

        return new Hit(true, blockX, blockY, blockZ, lastX, lastY, lastZ);
      }

      lastX = blockX;
      lastY = blockY;
      lastZ = blockZ;

      x += dx * step;
      y += dy * step;
      z += dz * step;
      distance += step;
    }

    return Hit.miss();
  }

  private static int floor(float value) {
    return (int) Math.floor(value);
  }
}
