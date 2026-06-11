package com.mineandcraft.ui;

import com.mineandcraft.graphics.FontRenderer;
import com.mineandcraft.graphics.ShaderProgram;
import com.mineandcraft.graphics.UiDrawer;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UiToggle {

  private final String label;
  private final float x;
  private final float y;
  private final float width;
  private final float height;
  private final Supplier<Boolean> getter;
  private final Consumer<Boolean> setter;

  private boolean hovered;

  public UiToggle(
      String label,
      float x,
      float y,
      float width,
      float height,
      Supplier<Boolean> getter,
      Consumer<Boolean> setter
  ) {
    this.label = label;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.getter = getter;
    this.setter = setter;
  }

  public void updateHover(float mouseX, float mouseY) {
    hovered = contains(mouseX, mouseY);
  }

  public boolean click(float mouseX, float mouseY) {
    if (contains(mouseX, mouseY)) {
      setter.accept(!getter.get());
      return true;
    }

    return false;
  }

  public void render(ShaderProgram colorShader, ShaderProgram textShader, FontRenderer font) {
    boolean enabled = getter.get();
    float bg = enabled ? 0.28f : 0.16f;
    float accent = enabled ? 0.35f : 0.22f;

    UiDrawer.fill(colorShader, x, y, x + width, y + height, bg, bg + accent * 0.2f, bg, hovered ? 1f : 0.9f);
    UiDrawer.outline(colorShader, x, y, x + width, y + height, 0.8f, 0.8f, 0.85f, 0.85f);

    float scale = 0.024f;
    String state = enabled ? "Вкл" : "Выкл";
    String text = label + ": " + state;
    font.drawText(textShader, text, x + 0.015f, y + (height - scale) * 0.5f, scale, 1f, 1f, 1f, 1f);
  }

  private boolean contains(float mouseX, float mouseY) {
    return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
  }
}
