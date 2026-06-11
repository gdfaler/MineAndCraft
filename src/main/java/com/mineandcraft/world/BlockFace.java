package com.mineandcraft.world;

public enum BlockFace {
  TOP(0, 1, 0, 1.0f),
  BOTTOM(0, -1, 0, 0.5f),
  NORTH(0, 0, -1, 0.8f),
  SOUTH(0, 0, 1, 0.8f),
  EAST(1, 0, 0, 0.6f),
  WEST(-1, 0, 0, 0.6f);

  public final int dx;
  public final int dy;
  public final int dz;
  public final float shade;

  BlockFace(int dx, int dy, int dz, float shade) {
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
    this.shade = shade;
  }
}
