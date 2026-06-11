package com.mineandcraft.world;

public final class Block {

  public static final byte AIR = 0;
  public static final byte DIRT = 1;
  public static final byte GRASS = 2;
  public static final byte STONE = 3;
  public static final byte WOOD = 4;
  public static final byte LEAVES = 5;
  public static final byte SAND = 6;
  public static final byte BEDROCK = 7;
  public static final byte PLANKS = 8;

  private Block() {
  }

  public static boolean isSolid(byte block) {
    return block != AIR;
  }

  public static boolean isBreakable(byte block) {
    return block != AIR && block != BEDROCK;
  }

  public static float getHardness(byte block) {
    return switch (block) {
      case LEAVES -> 0.15f;
      case GRASS, DIRT, SAND -> 0.45f;
      case WOOD, PLANKS -> 0.75f;
      case STONE -> 1.4f;
      default -> -1f;
    };
  }

  public static int getAtlasIndex(byte block, BlockFace face) {
    return switch (block) {
      case GRASS -> switch (face) {
        case TOP -> Atlas.GRASS_TOP;
        case BOTTOM -> Atlas.DIRT;
        default -> Atlas.GRASS_SIDE;
      };
      case DIRT -> Atlas.DIRT;
      case STONE -> Atlas.STONE;
      case WOOD -> switch (face) {
        case TOP, BOTTOM -> Atlas.LOG_TOP;
        default -> Atlas.LOG_SIDE;
      };
      case LEAVES -> Atlas.LEAVES;
      case SAND -> Atlas.SAND;
      case BEDROCK -> Atlas.BEDROCK;
      case PLANKS -> Atlas.PLANKS;
      default -> Atlas.DIRT;
    };
  }

  public static int getIconIndex(byte block) {
    return switch (block) {
      case GRASS -> Atlas.GRASS_TOP;
      case DIRT -> Atlas.DIRT;
      case STONE -> Atlas.STONE;
      case WOOD -> Atlas.LOG_SIDE;
      case LEAVES -> Atlas.LEAVES;
      case SAND -> Atlas.SAND;
      case PLANKS -> Atlas.PLANKS;
      default -> Atlas.DIRT;
    };
  }

  public static final class Atlas {
    public static final int GRASS_TOP = 0;
    public static final int GRASS_SIDE = 1;
    public static final int DIRT = 2;
    public static final int STONE = 3;
    public static final int LOG_SIDE = 4;
    public static final int LOG_TOP = 5;
    public static final int LEAVES = 6;
    public static final int SAND = 7;
    public static final int BEDROCK = 8;
    public static final int PLANKS = 9;

    private Atlas() {
    }
  }
}
