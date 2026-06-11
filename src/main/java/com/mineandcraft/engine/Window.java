package com.mineandcraft.engine;

import com.mineandcraft.graphics.Hud;
import com.mineandcraft.graphics.ShaderProgram;
import com.mineandcraft.graphics.TextureAtlas;
import com.mineandcraft.player.BlockBreaker;
import com.mineandcraft.world.Block;
import com.mineandcraft.world.World;
import com.mineandcraft.world.WorldRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class Window {

  private static final float FOV = 70f;
  private static final float NEAR = 0.1f;
  private static final float FAR = 320f;
  private static final float REACH = 5f;

  private final int width = 1280;
  private final int height = 720;

  private final World world;
  private final Camera camera;

  private long window;
  private WorldRenderer worldRenderer;
  private TextureAtlas atlas;
  private ShaderProgram blockShader;
  private ShaderProgram lineShader;
  private ShaderProgram hudShader;
  private ShaderProgram hudTexturedShader;
  private Hud hud;
  private final BlockBreaker blockBreaker = new BlockBreaker();

  private final Matrix4f projection = new Matrix4f();
  private final Matrix4f view = new Matrix4f();
  private final Vector3f cameraPos = new Vector3f();
  private final float[] lookDirection = new float[3];

  private boolean rightClickQueued;

  public Window(World world, Camera camera) {
    this.world = world;
    this.camera = camera;
  }

  public void run() {
    try {
      init();
      loop();
    } finally {
      cleanup();
    }
  }

  private void init() {
    GLFWErrorCallback.createPrint(System.err).set();

    if (!GLFW.glfwInit()) {
      throw new IllegalStateException("Unable to initialize GLFW");
    }

    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);

    window = GLFW.glfwCreateWindow(width, height, "MineAndCraft", 0, 0);
    if (window == 0) {
      throw new RuntimeException("Failed to create GLFW window");
    }

    GLFW.glfwMakeContextCurrent(window);
    GLFW.glfwSwapInterval(1);
    GL.createCapabilities();

    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    GLFW.glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
      if (action != GLFW.GLFW_PRESS || camera.isPaused()) {
        return;
      }

      if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
        rightClickQueued = true;
      }
    });

    GLFW.glfwSetScrollCallback(window, (w, xOffset, yOffset) -> {
      if (!camera.isPaused()) {
        camera.getHotbar().scroll((int) yOffset);
      }
    });

    GL11.glEnable(GL11.GL_DEPTH_TEST);
    GL11.glEnable(GL11.GL_CULL_FACE);
    GL11.glCullFace(GL11.GL_BACK);
    GL11.glClearColor(0.53f, 0.81f, 0.98f, 1f);

    atlas = new TextureAtlas();
    blockShader = ShaderProgram.createBlockShader();
    lineShader = ShaderProgram.createLineShader();
    hudShader = ShaderProgram.createHudShader();
    hudTexturedShader = ShaderProgram.createHudTexturedShader();
    hud = new Hud();
    worldRenderer = new WorldRenderer(world, atlas);

    updateProjection();
    camera.setWorld(world);

    int spawnX = World.SIZE / 2;
    int spawnZ = World.SIZE / 2;
    float spawnY = findTopY(spawnX, spawnZ);
    camera.position.set(spawnX + 0.5f, spawnY, spawnZ + 0.5f);
  }

  private float findTopY(int x, int z) {
    for (int y = World.HEIGHT - 1; y >= 0; y--) {
      if (Block.isSolid(world.getBlock(x, y, z))) {
        return y + 1.01f;
      }
    }
    return 20f;
  }

  private void updateProjection() {
    projection.identity().perspective(
        (float) Math.toRadians(FOV),
        (float) width / height,
        NEAR,
        FAR
    );
  }

  private void loop() {
    double lastFrame = GLFW.glfwGetTime();

    while (!GLFW.glfwWindowShouldClose(window)) {
      double now = GLFW.glfwGetTime();
      float deltaTime = (float) (now - lastFrame);
      lastFrame = now;

      if (deltaTime > 0.05f) {
        deltaTime = 0.05f;
      }

      GLFW.glfwPollEvents();

      camera.update(window, deltaTime);
      handleBlockInteraction(deltaTime);

      render();

      GLFW.glfwSwapBuffers(window);
    }
  }

  private Raycast.Hit currentTarget() {
    camera.getLookDirection(lookDirection);
    return Raycast.cast(
        world,
        camera.getEyeX(),
        camera.getEyeY(),
        camera.getEyeZ(),
        lookDirection[0],
        lookDirection[1],
        lookDirection[2],
        REACH
    );
  }

  private void handleBlockInteraction(float deltaTime) {
    if (camera.isPaused()) {
      rightClickQueued = false;
      blockBreaker.reset();
      return;
    }

    Raycast.Hit hit = currentTarget();
    boolean breaking = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    blockBreaker.update(world, hit, breaking, deltaTime);

    if (!hit.hit()) {
      rightClickQueued = false;
      return;
    }

    if (rightClickQueued) {
      int placeX = hit.placeX();
      int placeY = hit.placeY();
      int placeZ = hit.placeZ();

      if (!camera.intersectsBlock(placeX, placeY, placeZ)
          && world.getBlock(placeX, placeY, placeZ) == Block.AIR) {
        world.setBlock(placeX, placeY, placeZ, camera.getSelectedBlock());
      }

      rightClickQueued = false;
    }
  }

  private void render() {
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

    cameraPos.set(camera.getEyeX(), camera.getEyeY(), camera.getEyeZ());
    camera.getLookDirection(lookDirection);
    view.identity().lookAt(
        cameraPos.x,
        cameraPos.y,
        cameraPos.z,
        cameraPos.x + lookDirection[0],
        cameraPos.y + lookDirection[1],
        cameraPos.z + lookDirection[2],
        0f,
        1f,
        0f
    );

    worldRenderer.render(blockShader, projection, view, cameraPos);

    if (camera.isPaused()) {
      hud.drawPauseOverlay(hudShader);
      return;
    }

    Raycast.Hit hit = currentTarget();
    if (hit.hit()) {
      worldRenderer.renderSelection(lineShader, projection, view, hit.blockX(), hit.blockY(), hit.blockZ());
    }

    hud.drawCrosshair(hudShader);
    hud.drawBreakProgress(hudShader, blockBreaker.getProgress());
    hud.drawHotbar(hudShader, hudTexturedShader, atlas, camera.getHotbar());
  }

  private void cleanup() {
    if (hud != null) {
      hud.delete();
    }

    GLFW.glfwDestroyWindow(window);
    GLFW.glfwTerminate();

    GLFWErrorCallback callback = GLFW.glfwSetErrorCallback(null);
    if (callback != null) {
      callback.free();
    }
  }
}
