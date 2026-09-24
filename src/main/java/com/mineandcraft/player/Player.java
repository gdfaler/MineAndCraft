package com.mineandcraft.player;

import com.mineandcraft.engine.Vec3;
import com.mineandcraft.engine.physics.PlayerCollision;
import com.mineandcraft.world.World;

/** Игрок: положение, ориентация взгляда и физика движения. Не зависит от GLFW. */
public class Player {

  public static final float EYE_HEIGHT = 1.62f;
  public static final float HEIGHT = 1.8f;
  public static final float WIDTH = 0.6f;
  public static final float HALF_WIDTH = WIDTH * 0.5f;

  private static final float WALK_SPEED = 4.3f;
  private static final float SPRINT_SPEED = 5.6f;
  private static final float FLY_SPEED = 10.9f;
  private static final float FLY_SPRINT_SPEED = 21.6f;
  private static final float JUMP_SPEED = 8.7f;
  private static final float GRAVITY = 28f;
  private static final float TERMINAL_VELOCITY = 60f;
  private static final float GROUND_ACCELERATION = 14f;
  private static final float AIR_ACCELERATION = 3f;

  /** Намерения движения за кадр: заполняются из ввода. */
  public static final class Controls {
    public float forward;
    public float strafe;
    public boolean jump;
    public boolean descend;
    public boolean sprint;

    public void clear() {
      forward = 0;
      strafe = 0;
      jump = false;
      descend = false;
      sprint = false;
    }
  }

  private final Vec3 position = new Vec3(0, 0, 0);
  private final Hotbar hotbar = new Hotbar();

  private float yaw;
  private float pitch;
  private float velX;
  private float velY;
  private float velZ;
  private boolean onGround;
  private boolean flying;

  public Vec3 getPosition() {
    return position;
  }

  public void setPosition(float x, float y, float z) {
    position.set(x, y, z);
    velX = 0;
    velY = 0;
    velZ = 0;
  }

  public Hotbar getHotbar() {
    return hotbar;
  }

  public float getYaw() {
    return yaw;
  }

  public float getPitch() {
    return pitch;
  }

  public void setRotation(float yaw, float pitch) {
    this.yaw = ((yaw % 360f) + 360f) % 360f;
    this.pitch = Math.max(-89.9f, Math.min(89.9f, pitch));
  }

  public void rotate(float deltaYaw, float deltaPitch) {
    setRotation(yaw + deltaYaw, pitch + deltaPitch);
  }

  public boolean isOnGround() {
    return onGround;
  }

  public boolean isFlying() {
    return flying;
  }

  public void setFlying(boolean flying) {
    this.flying = flying;
    velY = 0;
  }

  public void toggleFlying() {
    setFlying(!flying);
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
    double yawRad = Math.toRadians(yaw);
    double pitchRad = Math.toRadians(pitch);

    out[0] = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
    out[1] = (float) -Math.sin(pitchRad);
    out[2] = (float) (-Math.cos(yawRad) * Math.cos(pitchRad));
  }

  public boolean intersectsBlock(int x, int y, int z) {
    return PlayerCollision.intersectsBlock(position, HALF_WIDTH, HEIGHT, x, y, z);
  }

  public void update(World world, Controls controls, float speedMultiplier, float deltaTime) {
    // Пока чанк под игроком не загружен, не двигаемся — иначе провалимся в пустоту.
    if (!world.isLoadedAt((int) Math.floor(position.x), (int) Math.floor(position.z))) {
      return;
    }

    double yawRad = Math.toRadians(yaw);
    float forwardX = (float) Math.sin(yawRad);
    float forwardZ = (float) -Math.cos(yawRad);
    float rightX = (float) Math.cos(yawRad);
    float rightZ = (float) Math.sin(yawRad);

    float moveX = forwardX * controls.forward + rightX * controls.strafe;
    float moveZ = forwardZ * controls.forward + rightZ * controls.strafe;
    float length = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
    if (length > 1e-4f) {
      moveX /= length;
      moveZ /= length;
    }

    float speed;
    if (flying) {
      speed = controls.sprint ? FLY_SPRINT_SPEED : FLY_SPEED;
    } else {
      speed = controls.sprint && controls.forward > 0 ? SPRINT_SPEED : WALK_SPEED;
    }
    speed *= speedMultiplier;

    float acceleration = flying || onGround ? GROUND_ACCELERATION : AIR_ACCELERATION;
    float blend = 1f - (float) Math.exp(-acceleration * deltaTime);
    velX += (moveX * speed - velX) * blend;
    velZ += (moveZ * speed - velZ) * blend;

    if (flying) {
      float vertical = (controls.jump ? 1f : 0f) - (controls.descend ? 1f : 0f);
      velY += (vertical * speed - velY) * blend;
    } else {
      if (controls.jump && onGround) {
        velY = JUMP_SPEED;
      }
      velY = Math.max(-TERMINAL_VELOCITY, velY - GRAVITY * deltaTime);
    }

    if (PlayerCollision.move(world, position, PlayerCollision.AXIS_X, velX * deltaTime, HALF_WIDTH, HEIGHT)) {
      velX = 0;
    }
    if (PlayerCollision.move(world, position, PlayerCollision.AXIS_Z, velZ * deltaTime, HALF_WIDTH, HEIGHT)) {
      velZ = 0;
    }

    boolean falling = velY <= 0;
    boolean hitVertical = PlayerCollision.move(world, position, PlayerCollision.AXIS_Y, velY * deltaTime, HALF_WIDTH, HEIGHT);
    onGround = hitVertical && falling;
    if (hitVertical) {
      velY = 0;
    }

    if (flying && onGround) {
      // Приземлились в полёте — как в Minecraft, выходим из режима полёта.
      flying = false;
    }
  }
}
