package com.mineandcraft.engine.physics;

import com.mineandcraft.engine.Vec3;
import com.mineandcraft.world.Block;
import com.mineandcraft.world.World;

public final class PlayerCollision {

  private static final float SKIN = 0.001f;
  private static final int MAX_DEPENETRATION_STEPS = 8;

  private PlayerCollision() {
  }

  public record VerticalResult(float velocityY, boolean onGround) {
  }

  public static void resolveHorizontal(World world, Vec3 position, float halfWidth, float height) {
    for (int step = 0; step < MAX_DEPENETRATION_STEPS; step++) {
      if (!resolveHorizontalStep(world, position, halfWidth, height)) {
        break;
      }
    }
  }

  public static VerticalResult resolveVertical(
      World world,
      Vec3 position,
      float velocityY,
      float halfWidth,
      float height
  ) {
    float velY = velocityY;
    boolean onGround = false;

    for (int x = blockMinX(position.x, halfWidth); x <= blockMaxX(position.x, halfWidth); x++) {
      for (int z = blockMinZ(position.z, halfWidth); z <= blockMaxZ(position.z, halfWidth); z++) {
        for (int y = blockMinY(position.y); y <= blockMaxY(position.y, height); y++) {
          if (!Block.isSolid(world.getBlock(x, y, z))) {
            continue;
          }

          float playerBottom = position.y;
          float playerTop = position.y + height;
          float blockTop = y + 1f;
          float blockBottom = y;

          if (playerTop <= blockBottom + SKIN || playerBottom >= blockTop - SKIN) {
            continue;
          }

          float pushUp = blockTop - playerBottom;
          float pushDown = playerTop - blockBottom;

          if (velY <= 0f && pushUp < pushDown) {
            position.y = blockTop + SKIN;
            velY = 0f;
            onGround = true;
          } else if (velY > 0f && pushDown < pushUp) {
            position.y = blockBottom - height - SKIN;
            velY = 0f;
          }
        }
      }
    }

    return new VerticalResult(velY, onGround);
  }

  public static void depenetrate(World world, Vec3 position, float halfWidth, float height) {
    for (int step = 0; step < MAX_DEPENETRATION_STEPS; step++) {
      float bestOverlap = Float.MAX_VALUE;
      int bestX = 0;
      int bestY = 0;
      int bestZ = 0;
      int bestAxis = -1;

      for (int x = blockMinX(position.x, halfWidth); x <= blockMaxX(position.x, halfWidth); x++) {
        for (int z = blockMinZ(position.z, halfWidth); z <= blockMaxZ(position.z, halfWidth); z++) {
          for (int y = blockMinY(position.y); y <= blockMaxY(position.y, height); y++) {
            if (!Block.isSolid(world.getBlock(x, y, z))) {
              continue;
            }

            float overlapX = Math.min(position.x + halfWidth - x, x + 1f - (position.x - halfWidth));
            float overlapZ = Math.min(position.z + halfWidth - z, z + 1f - (position.z - halfWidth));
            float overlapY = Math.min(position.y + height - y, y + 1f - position.y);

            if (overlapX <= 0f || overlapZ <= 0f || overlapY <= 0f) {
              continue;
            }

            if (overlapX < bestOverlap) {
              bestOverlap = overlapX;
              bestX = x;
              bestY = y;
              bestZ = z;
              bestAxis = 0;
            }
            if (overlapZ < bestOverlap) {
              bestOverlap = overlapZ;
              bestX = x;
              bestY = y;
              bestZ = z;
              bestAxis = 2;
            }
            if (overlapY < bestOverlap) {
              bestOverlap = overlapY;
              bestX = x;
              bestY = y;
              bestZ = z;
              bestAxis = 1;
            }
          }
        }
      }

      if (bestAxis < 0) {
        return;
      }

      switch (bestAxis) {
        case 0 -> position.x += position.x < bestX + 0.5f
            ? -bestOverlap - SKIN
            : bestOverlap + SKIN;
        case 2 -> position.z += position.z < bestZ + 0.5f
            ? -bestOverlap - SKIN
            : bestOverlap + SKIN;
        case 1 -> {
          if (position.y + height * 0.5f < bestY + 0.5f) {
            position.y = bestY - height - SKIN;
          } else {
            position.y = bestY + 1f + SKIN;
          }
        }
        default -> {
          return;
        }
      }
    }
  }

  public static boolean isInsideSolid(World world, Vec3 position, float halfWidth, float height) {
    for (int x = blockMinX(position.x, halfWidth); x <= blockMaxX(position.x, halfWidth); x++) {
      for (int z = blockMinZ(position.z, halfWidth); z <= blockMaxZ(position.z, halfWidth); z++) {
        for (int y = blockMinY(position.y); y <= blockMaxY(position.y, height); y++) {
          if (Block.isSolid(world.getBlock(x, y, z))) {
            return true;
          }
        }
      }
    }

    return false;
  }

  private static boolean resolveHorizontalStep(World world, Vec3 position, float halfWidth, float height) {
    boolean resolved = false;

    for (int x = blockMinX(position.x, halfWidth); x <= blockMaxX(position.x, halfWidth); x++) {
      for (int z = blockMinZ(position.z, halfWidth); z <= blockMaxZ(position.z, halfWidth); z++) {
        for (int y = blockMinY(position.y); y <= blockMaxY(position.y, height); y++) {
          if (!Block.isSolid(world.getBlock(x, y, z))) {
            continue;
          }

          float overlapX = Math.min(position.x + halfWidth - x, x + 1f - (position.x - halfWidth));
          float overlapZ = Math.min(position.z + halfWidth - z, z + 1f - (position.z - halfWidth));
          float overlapY = Math.min(position.y + height - y, y + 1f - position.y);

          if (overlapX <= 0f || overlapZ <= 0f || overlapY <= 0f) {
            continue;
          }

          if (overlapX <= overlapZ) {
            position.x += position.x < x + 0.5f ? -overlapX - SKIN : overlapX + SKIN;
          } else {
            position.z += position.z < z + 0.5f ? -overlapZ - SKIN : overlapZ + SKIN;
          }

          resolved = true;
        }
      }
    }

    return resolved;
  }

  private static int blockMinX(float x, float halfWidth) {
    return (int) Math.floor(x - halfWidth);
  }

  private static int blockMaxX(float x, float halfWidth) {
    return (int) Math.floor(x + halfWidth);
  }

  private static int blockMinZ(float z, float halfWidth) {
    return (int) Math.floor(z - halfWidth);
  }

  private static int blockMaxZ(float z, float halfWidth) {
    return (int) Math.floor(z + halfWidth);
  }

  private static int blockMinY(float y) {
    return (int) Math.floor(y);
  }

  private static int blockMaxY(float y, float height) {
    return (int) Math.floor(y + height);
  }
}
