package com.mineandcraft.ui;

import com.mineandcraft.graphics.FontRenderer;
import com.mineandcraft.graphics.ShaderProgram;
import com.mineandcraft.graphics.UiDrawer;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UiSlider {

  private final String label;
  private final float x;
  private final float y;
  private final float width;
  private final float min;
  private final float max;
  private final Supplier<Float> getter;
  private final Consumer<Float> setter;
  private final String format;

  private boolean dragging;

  public UiSlider(
      String label,
      float x,
      float y,
      float width,
      float min,
      float max,
      Supplier<Float> getter,
      Consumer<Float> setter,
      String format
  ) {
    this.label = label;
    this.x = x;
    this.y = y;
    this.width = width;
    this.min = min;
    this.max = max;
    this.getter = getter;
    this.setter = setter;
    this.format = format;
  }

  public void mousePressed(float mouseX, float mouseY) {
    if (containsTrack(mouseX, mouseY)) {
      dragging = true;
      applyValue(mouseX);
    }
  }

  public void mouseReleased() {
    dragging = false;
  }

  public void mouseDragged(float mouseX, float mouseY) {
    if (dragging) {
      applyValue(mouseX);
    }
  }

  public void render(ShaderProgram colorShader, ShaderProgram textShader, FontRenderer font) {
    float scale = 0.024f;
    font.drawText(textShader, label, x, y + 0.03f, scale, 0.92f, 0.92f, 0.95f, 1f);

    float trackY0 = y;
    float trackY1 = y + 0.018f;
    UiDrawer.fill(colorShader, x, trackY0, x + width, trackY1, 0.15f, 0.15f, 0.18f, 1f);

    float t = (getter.get() - min) / (max - min);
    float knobX = x + width * t;
    UiDrawer.fill(colorShader, x, trackY0, knobX, trackY1, 0.35f, 0.65f, 0.35f, 1f);
    UiDrawer.fill(colorShader, knobX - 0.008f, trackY0 - 0.006f, knobX + 0.008f, trackY1 + 0.006f, 0.9f, 0.9f, 0.95f, 1f);

    String valueText = String.format(format, getter.get());
    font.drawText(textShader, valueText, x + width + 0.02f, y + 0.002f, scale, 0.85f, 0.85f, 0.9f, 1f);
  }

  private void applyValue(float mouseX) {
    float t = (mouseX - x) / width;
    t = Math.max(0f, Math.min(1f, t));
    setter.accept(min + t * (max - min));
  }

  private boolean containsTrack(float mouseX, float mouseY) {
    return mouseX >= x && mouseX <= x + width + 0.08f && mouseY >= y - 0.01f && mouseY <= y + 0.03f;
  }
}
