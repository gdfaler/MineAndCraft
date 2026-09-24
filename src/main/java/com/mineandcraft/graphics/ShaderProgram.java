package com.mineandcraft.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {

  private static final String BLOCK_VERTEX = """
      #version 330 core

      layout(location = 0) in vec3 aPos;
      layout(location = 1) in vec2 aTex;
      layout(location = 2) in float aShade;

      uniform mat4 uProjection;
      uniform mat4 uView;

      out vec2 vTex;
      out float vShade;
      out vec3 vWorldPos;

      void main() {
          vTex = aTex;
          vShade = aShade;
          vWorldPos = aPos;
          gl_Position = uProjection * uView * vec4(aPos, 1.0);
      }
      """;

  private static final String BLOCK_FRAGMENT = """
      #version 330 core

      in vec2 vTex;
      in float vShade;
      in vec3 vWorldPos;

      uniform sampler2D uTexture;
      uniform vec3 uCameraPos;
      uniform vec3 uFogColor;
      uniform float uFogEnabled;
      uniform float uFogStart;
      uniform float uFogEnd;

      out vec4 FragColor;

      void main() {
          vec4 color = texture(uTexture, vTex) * vShade;
          if (color.a < 0.1) {
              discard;
          }

          if (uFogEnabled > 0.5) {
              float dist = distance(vWorldPos.xz, uCameraPos.xz);
              float fog = clamp((dist - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0);
              FragColor = vec4(mix(color.rgb, uFogColor, fog), 1.0);
          } else {
              FragColor = vec4(color.rgb, 1.0);
          }
      }
      """;

  private static final String HUD_VERTEX = """
      #version 330 core

      layout(location = 0) in vec2 aPos;

      uniform vec2 uScale;

      void main() {
          gl_Position = vec4(aPos * uScale, 0.0, 1.0);
      }
      """;

  private static final String HUD_FRAGMENT = """
      #version 330 core

      uniform vec4 uColor;

      out vec4 FragColor;

      void main() {
          FragColor = uColor;
      }
      """;

  private static final String HUD_TEXTURED_VERTEX = """
      #version 330 core

      layout(location = 0) in vec2 aPos;
      layout(location = 1) in vec2 aTex;

      uniform vec2 uScale;

      out vec2 vTex;

      void main() {
          vTex = aTex;
          gl_Position = vec4(aPos * uScale, 0.0, 1.0);
      }
      """;

  private static final String HUD_TEXTURED_FRAGMENT = """
      #version 330 core

      in vec2 vTex;

      uniform sampler2D uTexture;
      uniform vec4 uColor;

      out vec4 FragColor;

      void main() {
          FragColor = texture(uTexture, vTex) * uColor;
      }
      """;

  private static final String LINE_VERTEX = """
      #version 330 core

      layout(location = 0) in vec3 aPos;

      uniform mat4 uProjection;
      uniform mat4 uView;
      uniform mat4 uModel;

      void main() {
          gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
      }
      """;

  private static final String LINE_FRAGMENT = """
      #version 330 core

      uniform vec4 uColor;

      out vec4 FragColor;

      void main() {
          FragColor = uColor;
      }
      """;

  private final int programId;
  private final int projectionLoc;
  private final int viewLoc;
  private final int textureLoc;
  private final int cameraPosLoc;
  private final int fogColorLoc;
  private final int fogEnabledLoc;
  private final int colorLoc;
  private final int modelLoc;
  private final int fogStartLoc;
  private final int fogEndLoc;
  private final int scaleLoc;
  private final float[] matrixBuffer = new float[16];

  private ShaderProgram(String vertex, String fragment) {
    int vertexShader = compile(vertex, GL_VERTEX_SHADER);
    int fragmentShader = compile(fragment, GL_FRAGMENT_SHADER);

    programId = glCreateProgram();
    glAttachShader(programId, vertexShader);
    glAttachShader(programId, fragmentShader);
    glLinkProgram(programId);

    if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
      throw new RuntimeException(glGetProgramInfoLog(programId));
    }

    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);

    projectionLoc = glGetUniformLocation(programId, "uProjection");
    viewLoc = glGetUniformLocation(programId, "uView");
    textureLoc = glGetUniformLocation(programId, "uTexture");
    cameraPosLoc = glGetUniformLocation(programId, "uCameraPos");
    fogColorLoc = glGetUniformLocation(programId, "uFogColor");
    fogEnabledLoc = glGetUniformLocation(programId, "uFogEnabled");
    colorLoc = glGetUniformLocation(programId, "uColor");
    modelLoc = glGetUniformLocation(programId, "uModel");
    fogStartLoc = glGetUniformLocation(programId, "uFogStart");
    fogEndLoc = glGetUniformLocation(programId, "uFogEnd");
    scaleLoc = glGetUniformLocation(programId, "uScale");

    if (scaleLoc >= 0) {
      glUseProgram(programId);
      glUniform2f(scaleLoc, 1f, 1f);
      glUseProgram(0);
    }
  }

  public static ShaderProgram createBlockShader() {
    return new ShaderProgram(BLOCK_VERTEX, BLOCK_FRAGMENT);
  }

  public static ShaderProgram createHudShader() {
    return new ShaderProgram(HUD_VERTEX, HUD_FRAGMENT);
  }

  public static ShaderProgram createHudTexturedShader() {
    return new ShaderProgram(HUD_TEXTURED_VERTEX, HUD_TEXTURED_FRAGMENT);
  }

  public static ShaderProgram createLineShader() {
    return new ShaderProgram(LINE_VERTEX, LINE_FRAGMENT);
  }

  private int compile(String source, int type) {
    int shader = glCreateShader(type);
    glShaderSource(shader, source);
    glCompileShader(shader);

    if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
      throw new RuntimeException(glGetShaderInfoLog(shader));
    }

    return shader;
  }

  public void use() {
    glUseProgram(programId);
  }

  public void setMatrices(Matrix4f projection, Matrix4f view) {
    if (projectionLoc >= 0) {
      glUniformMatrix4fv(projectionLoc, false, projection.get(matrixBuffer));
    }
    if (viewLoc >= 0) {
      glUniformMatrix4fv(viewLoc, false, view.get(matrixBuffer));
    }
  }

  public void setTextureUnit(int unit) {
    if (textureLoc >= 0) {
      glUniform1i(textureLoc, unit);
    }
  }

  public void setCameraPos(Vector3f cameraPos) {
    if (cameraPosLoc >= 0) {
      glUniform3f(cameraPosLoc, cameraPos.x, cameraPos.y, cameraPos.z);
    }
  }

  public void setFogColor(float r, float g, float b) {
    if (fogColorLoc >= 0) {
      glUniform3f(fogColorLoc, r, g, b);
    }
  }

  public void setFogEnabled(boolean enabled) {
    if (fogEnabledLoc >= 0) {
      glUniform1f(fogEnabledLoc, enabled ? 1f : 0f);
    }
  }

  public void setColor(float r, float g, float b, float a) {
    if (colorLoc >= 0) {
      glUniform4f(colorLoc, r, g, b, a);
    }
  }

  public void setModel(Matrix4f model) {
    if (modelLoc >= 0) {
      glUniformMatrix4fv(modelLoc, false, model.get(matrixBuffer));
    }
  }

  public void setFogRange(float start, float end) {
    if (fogStartLoc >= 0) {
      glUniform1f(fogStartLoc, start);
    }
    if (fogEndLoc >= 0) {
      glUniform1f(fogEndLoc, end);
    }
  }

  /** Масштаб для 2D-шейдеров: сохраняет пропорции интерфейса при любом соотношении сторон окна. */
  public void setScale(float x, float y) {
    if (scaleLoc >= 0) {
      glUniform2f(scaleLoc, x, y);
    }
  }

  public void delete() {
    glDeleteProgram(programId);
  }
}
