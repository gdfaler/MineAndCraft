package com.mineandcraft.engine;

import com.mineandcraft.config.GameSettings;
import com.mineandcraft.engine.physics.PlayerCollision;
import com.mineandcraft.player.Hotbar;
import com.mineandcraft.world.World;
import org.lwjgl.glfw.GLFW;

public class Camera {

  public static final float EYE_HEIGHT = 1.62f;
  public static final float PLAYER_HEIGHT = 1.8f;
  public static final float PLAYER_WIDTH = 0.6f;

  private static final float BASE_WALK_SPEED = 4.3f;
  private static final float BASE_SPRINT_SPEED = 6.8f;
  private static final float JUMP_SPEED = 8.5f;
  private static final float GRAVITY = 24f;

  public final Vec3 position = new Vec3(0, 20, 0);

  public float yaw;
  public float pitch;

  private GameSettings settings;
  private World world;
  private final Hotbar hotbar = new Hotbar();

  private double lastX = 640;
  private double lastY = 360;
  private boolean firstMouse = true;

  private float velY;
  private boolean onGround;

  public void setWorld(World world) {
    this.world = world;
  }

  public void setSettings(GameSettings settings) {
    this.settings = settings;
  }

  public Hotbar getHotbar() {
    return hotbar;
  }

  public byte getSelectedBlock() {
    return hotbar.getSelectedBlock();
  }

  public void update(long window, float deltaTime, boolean allowInput) {
    if (!allowInput) {
      return;
    }

    mouseLook(window);
    applyMovement(window, deltaTime);
    hotbar.handleKeys(window);
  }

  public void resetMouse() {
    firstMouse = true;
  }

  private void mouseLook(long window) {
    double[] x = new double[1];
    double[] y = new double[1];
    GLFW.glfwGetCursorPos(window, x, y);

    if (firstMouse) {
      lastX = x[0];
      lastY = y[0];
      firstMouse = false;
      return;
    }

    double dx = x[0] - lastX;
    double dy = lastY - y[0];
    lastX = x[0];
    lastY = y[0];

    float sensitivity = settings != null ? settings.getMouseSensitivity() : 0.12f;
    yaw += (float) dx * sensitivity;
    pitch -= (float) dy * sensitivity;

    if (pitch > 89f) {
      pitch = 89f;
    }
    if (pitch < -89f) {
      pitch = -89f;
    }
  }

  private void applyMovement(long window, float deltaTime) {
    float halfWidth = PLAYER_WIDTH * 0.5f;
    float speedMultiplier = settings != null ? settings.getSpeedMultiplier() : 1f;

    float forwardX = (float) Math.sin(Math.toRadians(yaw));
    float forwardZ = -(float) Math.cos(Math.toRadians(yaw));
    float rightX = (float) Math.cos(Math.toRadians(yaw));
    float rightZ = (float) Math.sin(Math.toRadians(yaw));

    float moveX = 0;
    float moveZ = 0;

    if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
      moveX += forwardX;
      moveZ += forwardZ;
    }
    if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
      moveX -= forwardX;
      moveZ -= forwardZ;
    }
    if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
      moveX -= rightX;
      moveZ -= rightZ;
    }
    if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
      moveX += rightX;
      moveZ += rightZ;
    }

    float length = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
    if (length > 0f) {
      moveX /= length;
      moveZ /= length;
    }

    boolean sprinting = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
        || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    float speed = (sprinting ? BASE_SPRINT_SPEED : BASE_WALK_SPEED) * speedMultiplier * deltaTime;

    position.x += moveX * speed;
    position.z += moveZ * speed;
    PlayerCollision.resolveHorizontal(world, position, halfWidth, PLAYER_HEIGHT);

    if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS
        && onGround
        && !PlayerCollision.isInsideSolid(world, position, halfWidth, PLAYER_HEIGHT)) {
      velY = JUMP_SPEED;
      onGround = false;
    }

    velY -= GRAVITY * deltaTime;
    position.y += velY * deltaTime;

    PlayerCollision.VerticalResult vertical = PlayerCollision.resolveVertical(
        world,
        position,
        velY,
        halfWidth,
        PLAYER_HEIGHT
    );
    velY = vertical.velocityY();
    onGround = vertical.onGround();

    PlayerCollision.depenetrate(world, position, halfWidth, PLAYER_HEIGHT);
  }

  public float getEyeX() {
    return position.x;
  }

  public float getEyeY() {
    return position.y + EYE_HEIGHT;
  }

  public float getEyeZ() {
    return position.z;
  }

  public void getLookDirection(float[] out) {
    float yawRad = (float) Math.toRadians(yaw);
    float pitchRad = (float) Math.toRadians(pitch);

    out[0] = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
    out[1] = (float) -Math.sin(pitchRad);
    out[2] = (float) (-Math.cos(yawRad) * Math.cos(pitchRad));
  }

  public boolean intersectsBlock(int x, int y, int z) {
    float halfWidth = PLAYER_WIDTH * 0.5f;
    float minX = position.x - halfWidth;
    float maxX = position.x + halfWidth;
    float minY = position.y;
    float maxY = position.y + PLAYER_HEIGHT;
    float minZ = position.z - halfWidth;
    float maxZ = position.z + halfWidth;

    return x + 1f > minX && x < maxX
        && y + 1f > minY && y < maxY
        && z + 1f > minZ && z < maxZ;
  }
}
