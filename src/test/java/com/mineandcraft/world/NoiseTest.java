package com.mineandcraft.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoiseTest {

  @Test
  void sameSeedGivesSameValues() {
    Noise a = new Noise(42);
    Noise b = new Noise(42);

    for (int i = 0; i < 100; i++) {
      assertEquals(a.noise(i * 0.37, i * 0.11), b.noise(i * 0.37, i * 0.11));
      assertEquals(a.noise(i * 0.37, i * 0.11, i * 0.53), b.noise(i * 0.37, i * 0.11, i * 0.53));
    }
  }

  @Test
  void differentSeedsGiveDifferentValues() {
    Noise a = new Noise(1);
    Noise b = new Noise(2);
    int differences = 0;

    for (int i = 0; i < 100; i++) {
      if (a.noise(i * 0.37, i * 0.11) != b.noise(i * 0.37, i * 0.11)) {
        differences++;
      }
    }

    assertTrue(differences > 90, "шум почти не зависит от сида: " + differences);
  }

  @Test
  void valuesStayInExpectedRangeAndVary() {
    Noise noise = new Noise(7);
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;

    for (int x = 0; x < 200; x++) {
      for (int z = 0; z < 200; z++) {
        double value = noise.fractal(x * 0.05, z * 0.05, 4, 2.0, 0.5);
        min = Math.min(min, value);
        max = Math.max(max, value);
      }
    }

    assertTrue(min >= -1.5 && max <= 1.5, "выход за диапазон: " + min + " .. " + max);
    assertTrue(max - min > 0.5, "шум слишком ровный: " + min + " .. " + max);
  }

  @Test
  void isNotPeriodicOverShortDistances() {
    Noise noise = new Noise(3);
    assertNotEquals(noise.noise(0.5, 0.5), noise.noise(0.5 + 2 * Math.PI, 0.5));
  }
}
