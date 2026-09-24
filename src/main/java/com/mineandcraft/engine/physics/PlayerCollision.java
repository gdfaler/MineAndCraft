package com.mineandcraft.engine.physics;

import com.mineandcraft.engine.Vec3;
import com.mineandcraft.world.Block;
import com.mineandcraft.world.World;

/**
 * Перемещение AABB игрока с коллизиями: движение раскладывается по осям, каждая ось
 * проходится шагами не длиннее MAX_STEP, чтобы на большой скорости не проскочить сквозь блок.
 */
public final class PlayerCollision {

  public static final int AXIS_X = 0;
  public static final int AXIS_Y = 1;
  public static final int AXIS_Z = 2;

  private static final float EPSILON = 0.001f;
  private static final float MAX_STEP = 0.45f;

  private PlayerCollision() {
  }

  /**
   * Сдвигает позицию по оси и возвращает true, если движение упёрлось в блок.
   * Позиция — центр основания AABB (ноги игрока).
   */
  public static boolean move(World world, Vec3 position, int axis, float delta, float halfWidth, float height) {
    boolean collided = false;
    float remaining = delta;

    while (remaining != 0f && !collided) {
      float step = Math.max(-MAX_STEP, Math.min(MAX_STEP, remaining));
      remaining -= step;
      collided = moveStep(world, position, axis, step, halfWidth, height);
    }

    return collided;
  }

  private static boolean moveStep(World world, Vec3 position, int axis, float step, float halfWidth, float height) {
    set(position, axis, get(position, axis) + step);

    float minX = position.x - halfWidth;
    float maxX = position.x + halfWidth;
    float minY = position.y;
    float maxY = position.y + height;
    float minZ = position.z - halfWidth;
    float maxZ = position.z + halfWidth;

    // По осям, вдоль которых не движемся, чуть сужаем коробку: иначе стоящий вплотную блок
    // считался бы пересечением.
    int x0 = floor(minX + (axis == AXIS_X ? 0 : EPSILON));
    int x1 = floor(maxX - (axis == AXIS_X ? 0 : EPSILON));
    int y0 = floor(minY + (axis == AXIS_Y ? 0 : EPSILON));
    int y1 = floor(maxY - (axis == AXIS_Y ? 0 : EPSILON));
    int z0 = floor(minZ + (axis == AXIS_Z ? 0 : EPSILON));
    int z1 = floor(maxZ - (axis == AXIS_Z ? 0 : EPSILON));

    boolean collided = false;
    float limit = step > 0 ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;

    for (int x = x0; x <= x1; x++) {
      for (int y = y0; y <= y1; y++) {
        for (int z = z0; z <= z1; z++) {
          if (!Block.isSolid(world.getBlock(x, y, z))) {
            continue;
          }

          int blockCoord = axis == AXIS_X ? x : axis == AXIS_Y ? y : z;
          collided = true;
          limit = step > 0 ? Math.min(limit, blockCoord) : Math.max(limit, blockCoord + 1);
        }
      }
    }

    if (!collided) {
      return false;
    }

    float extentBelow = axis == AXIS_Y ? 0 : halfWidth;
    float extentAbove = axis == AXIS_Y ? height : halfWidth;
    set(position, axis, step > 0 ? limit - extentAbove - EPSILON : limit + extentBelow + EPSILON);
    return true;
  }

  public static boolean intersectsSolid(World world, Vec3 position, float halfWidth, float height) {
    int x0 = floor(position.x - halfWidth + EPSILON);
    int x1 = floor(position.x + halfWidth - EPSILON);
    int y0 = floor(position.y + EPSILON);
    int y1 = floor(position.y + height - EPSILON);
    int z0 = floor(position.z - halfWidth + EPSILON);
    int z1 = floor(position.z + halfWidth - EPSILON);

    for (int x = x0; x <= x1; x++) {
      for (int y = y0; y <= y1; y++) {
        for (int z = z0; z <= z1; z++) {
          if (Block.isSolid(world.getBlock(x, y, z))) {
            return true;
          }
        }
      }
    }

    return false;
  }

  /** Пересекается ли AABB игрока с единичным блоком (x, y, z). */
  public static boolean intersectsBlock(Vec3 position, float halfWidth, float height, int x, int y, int z) {
    return x + 1f > position.x - halfWidth && x < position.x + halfWidth
        && y + 1f > position.y && y < position.y + height
        && z + 1f > position.z - halfWidth && z < position.z + halfWidth;
  }

  private static float get(Vec3 v, int axis) {
    return axis == AXIS_X ? v.x : axis == AXIS_Y ? v.y : v.z;
  }

  private static void set(Vec3 v, int axis, float value) {
    switch (axis) {
      case AXIS_X -> v.x = value;
      case AXIS_Y -> v.y = value;
      default -> v.z = value;
    }
  }

  private static int floor(float value) {
    return (int) Math.floor(value);
  }
}
