package com.mineandcraft.ui;

import com.mineandcraft.graphics.FontRenderer;
import com.mineandcraft.graphics.ShaderProgram;
import com.mineandcraft.graphics.UiDrawer;

public class UiButton {

  private final String label;
  private final Runnable action;
  private final float x;
  private final float y;
  private final float width;
  private final float height;

  private boolean hovered;

  public UiButton(String label, float x, float y, float width, float height, Runnable action) {
    this.label = label;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.action = action;
  }

  public void updateHover(float mouseX, float mouseY) {
    hovered = contains(mouseX, mouseY);
  }

  public boolean click(float mouseX, float mouseY) {
    if (contains(mouseX, mouseY)) {
      action.run();
      return true;
    }

    return false;
  }

  public void render(ShaderProgram colorShader, ShaderProgram textShader, FontRenderer font) {
    float bg = hovered ? 0.35f : 0.22f;
    UiDrawer.fill(colorShader, x, y, x + width, y + height, bg, bg, bg + 0.05f, 0.95f);
    UiDrawer.outline(colorShader, x, y, x + width, y + height, 0.85f, 0.85f, 0.9f, hovered ? 1f : 0.75f);

    float scale = 0.028f;
    float textWidth = font.measureText(label, scale);
    float textX = x + (width - textWidth) * 0.5f;
    float textY = y + (height - scale) * 0.5f;
    font.drawText(textShader, label, textX, textY, scale, 1f, 1f, 1f, 1f);
  }

  private boolean contains(float mouseX, float mouseY) {
    return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
  }
}
