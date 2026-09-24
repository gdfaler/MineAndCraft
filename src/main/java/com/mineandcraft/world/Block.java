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
  public static final byte COBBLESTONE = 9;
  public static final byte GLASS = 10;
  public static final byte COAL_ORE = 11;
  public static final byte IRON_ORE = 12;
  public static final byte GRAVEL = 13;

  private Block() {
  }

  /** Блок занимает объём: с ним сталкивается игрок и в него упирается луч. */
  public static boolean isSolid(byte block) {
    return block != AIR;
  }

  /** Блок полностью закрывает соседнюю грань, поэтому её можно не рисовать. */
  public static boolean isOpaque(byte block) {
    return block != AIR && block != LEAVES && block != GLASS;
  }

  public static boolean isBreakable(byte block) {
    return block != AIR && block != BEDROCK;
  }

  public static float getHardness(byte block) {
    return switch (block) {
      case LEAVES -> 0.15f;
      case GLASS -> 0.25f;
      case GRASS, DIRT, SAND, GRAVEL -> 0.45f;
      case WOOD, PLANKS -> 0.75f;
      case STONE, COBBLESTONE -> 1.4f;
      case COAL_ORE, IRON_ORE -> 1.8f;
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
      case COBBLESTONE -> Atlas.COBBLESTONE;
      case GLASS -> Atlas.GLASS;
      case COAL_ORE -> Atlas.COAL_ORE;
      case IRON_ORE -> Atlas.IRON_ORE;
      case GRAVEL -> Atlas.GRAVEL;
      default -> Atlas.DIRT;
    };
  }

  public static int getIconIndex(byte block) {
    return switch (block) {
      case WOOD -> Atlas.LOG_SIDE;
      default -> getAtlasIndex(block, BlockFace.TOP);
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
    public static final int COBBLESTONE = 10;
    public static final int GLASS = 11;
    public static final int COAL_ORE = 12;
    public static final int IRON_ORE = 13;
    public static final int GRAVEL = 14;

    private Atlas() {
    }
  }
}
