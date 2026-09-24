package com.mineandcraft;

import com.mineandcraft.config.LevelIO;
import com.mineandcraft.engine.Game;

import java.nio.file.Path;

public final class Main {

  private Main() {
  }

  /**
   * Аргументы (необязательные):
   * {@code --world <каталог>} — где хранить мир (по умолчанию ~/.mineandcraft/world),
   * {@code --seed <число>} — сид для нового мира.
   */
  public static void main(String[] args) {
    Path worldDir = LevelIO.DEFAULT_WORLD_DIR;
    Long seed = null;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--world" -> worldDir = Path.of(requireValue(args, ++i, "--world"));
        case "--seed" -> seed = parseSeed(requireValue(args, ++i, "--seed"));
        default -> {
          System.err.println("Неизвестный аргумент: " + args[i]);
          System.err.println("Использование: MineAndCraft [--world <каталог>] [--seed <число>]");
          System.exit(2);
        }
      }
    }

    new Game(worldDir, seed).run();
  }

  private static String requireValue(String[] args, int index, String option) {
    if (index >= args.length) {
      System.err.println("Для " + option + " не указано значение");
      System.exit(2);
    }
    return args[index];
  }

  /** Числовой сид используется как есть, любой другой текст — через его hashCode, как в Minecraft. */
  private static long parseSeed(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return value.hashCode();
    }
  }
}
