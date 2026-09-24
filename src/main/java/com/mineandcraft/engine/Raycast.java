package com.mineandcraft.engine;

import com.mineandcraft.world.Block;
import com.mineandcraft.world.World;

/** Поиск блока под прицелом алгоритмом DDA (Amanatides–Woo): проверяется каждый пересечённый блок. */
public final class Raycast {

  /**
   * @param blockX блок, в который попал луч
   * @param placeX соседний блок со стороны грани попадания — туда ставится новый блок
   */
  public record Hit(
      boolean hit,
      int blockX,
      int blockY,
      int blockZ,
      int placeX,
      int placeY,
      int placeZ
  ) {
    private static final Hit MISS = new Hit(false, 0, 0, 0, 0, 0, 0);

    public static Hit miss() {
      return MISS;
    }
  }

  private Raycast() {
  }

  public static Hit cast(World world, float ox, float oy, float oz, float dx, float dy, float dz, float maxDistance) {
    float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (length < 1e-6f) {
      return Hit.miss();
    }

    dx /= length;
    dy /= length;
    dz /= length;

    int x = (int) Math.floor(ox);
    int y = (int) Math.floor(oy);
    int z = (int) Math.floor(oz);

    if (Block.isSolid(world.getBlock(x, y, z))) {
      return new Hit(true, x, y, z, x, y, z);
    }

    int stepX = dx > 0 ? 1 : -1;
    int stepY = dy > 0 ? 1 : -1;
    int stepZ = dz > 0 ? 1 : -1;

    float tDeltaX = dx == 0 ? Float.POSITIVE_INFINITY : Math.abs(1f / dx);
    float tDeltaY = dy == 0 ? Float.POSITIVE_INFINITY : Math.abs(1f / dy);
    float tDeltaZ = dz == 0 ? Float.POSITIVE_INFINITY : Math.abs(1f / dz);

    float tMaxX = dx == 0 ? Float.POSITIVE_INFINITY : (dx > 0 ? x + 1 - ox : ox - x) * tDeltaX;
    float tMaxY = dy == 0 ? Float.POSITIVE_INFINITY : (dy > 0 ? y + 1 - oy : oy - y) * tDeltaY;
    float tMaxZ = dz == 0 ? Float.POSITIVE_INFINITY : (dz > 0 ? z + 1 - oz : oz - z) * tDeltaZ;

    while (true) {
      int prevX = x;
      int prevY = y;
      int prevZ = z;
      float t;

      if (tMaxX < tMaxY && tMaxX < tMaxZ) {
        t = tMaxX;
        x += stepX;
        tMaxX += tDeltaX;
      } else if (tMaxY < tMaxZ) {
        t = tMaxY;
        y += stepY;
        tMaxY += tDeltaY;
      } else {
        t = tMaxZ;
        z += stepZ;
        tMaxZ += tDeltaZ;
      }

      if (t > maxDistance) {
        return Hit.miss();
      }

      if (Block.isSolid(world.getBlock(x, y, z))) {
        return new Hit(true, x, y, z, prevX, prevY, prevZ);
      }
    }
  }
}
