package com.mineandcraft.engine;

import com.mineandcraft.config.GameSettings;
import com.mineandcraft.config.LevelIO;
import com.mineandcraft.config.SettingsIO;
import com.mineandcraft.graphics.Hud;
import com.mineandcraft.graphics.ShaderProgram;
import com.mineandcraft.graphics.TextureAtlas;
import com.mineandcraft.graphics.UiDrawer;
import com.mineandcraft.player.BlockBreaker;
import com.mineandcraft.player.Hotbar;
import com.mineandcraft.player.Player;
import com.mineandcraft.ui.UiManager;
import com.mineandcraft.world.Block;
import com.mineandcraft.world.FileChunkStorage;
import com.mineandcraft.world.World;
import com.mineandcraft.world.WorldRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.file.Path;

/** Игровой цикл: связывает окно, ввод, мир, игрока, рендер и интерфейс. */
public class Game {

  private static final float NEAR = 0.05f;
  private static final float REACH = 5f;
  private static final float PLACE_REPEAT_DELAY = 0.22f;
  private static final float AUTOSAVE_INTERVAL = 60f;
  private static final float MAX_FRAME_TIME = 0.05f;
  private static final float REFERENCE_ASPECT = 16f / 9f;
  private static final float[] SKY_COLOR = {0.62f, 0.8f, 1.0f};

  private final Path worldDir;
  private final Long seedOverride;
  private final GameSettings settings = new GameSettings();
  private final Player player = new Player();
  private final Player.Controls controls = new Player.Controls();
  private final BlockBreaker blockBreaker = new BlockBreaker();

  private final Matrix4f projection = new Matrix4f();
  private final Matrix4f view = new Matrix4f();
  private final Vector3f cameraPos = new Vector3f();
  private final float[] lookDirection = new float[3];

  private LevelIO.LevelData level;
  private Window window;
  private World world;
  private WorldRenderer worldRenderer;
  private TextureAtlas atlas;
  private ShaderProgram blockShader;
  private ShaderProgram lineShader;
  private ShaderProgram hudShader;
  private ShaderProgram hudTexturedShader;
  private UiManager uiManager;

  private Raycast.Hit target = Raycast.Hit.miss();
  private boolean cursorCaptured;
  private float placeCooldown;
  private float autosaveTimer;
  private float fpsSmoothed = 60f;

  /**
   * @param worldDir     каталог сохранения мира
   * @param seedOverride сид для нового мира или {@code null} для случайного; на существующий мир не влияет
   */
  public Game(Path worldDir, Long seedOverride) {
    this.worldDir = worldDir;
    this.seedOverride = seedOverride;
  }

  public void run() {
    try {
      init();
      loop();
    } finally {
      shutdown();
    }
  }

  private void init() {
    SettingsIO.load(settings);
    level = LevelIO.loadOrCreate(worldDir);
    if (!level.hasPlayerPosition() && seedOverride != null) {
      level.seed = seedOverride;
    }

    window = new Window("MineAndCraft", 1280, 720);

    GL11.glEnable(GL11.GL_DEPTH_TEST);
    GL11.glEnable(GL11.GL_CULL_FACE);
    GL11.glCullFace(GL11.GL_BACK);
    GL11.glClearColor(SKY_COLOR[0], SKY_COLOR[1], SKY_COLOR[2], 1f);

    atlas = new TextureAtlas();
    blockShader = ShaderProgram.createBlockShader();
    lineShader = ShaderProgram.createLineShader();
    hudShader = ShaderProgram.createHudShader();
    hudTexturedShader = ShaderProgram.createHudTexturedShader();

    uiManager = new UiManager(settings, this::syncCursor, window::requestClose, this::applySettings);
    uiManager.initFont();

    world = new World(level.seed, new FileChunkStorage(worldDir));
    worldRenderer = new WorldRenderer(world, atlas);
    applySettings();

    spawnPlayer();
    saveLevel();
    syncCursor();
  }

  private void spawnPlayer() {
    float x = level.hasPlayerPosition() ? level.playerX : 0.5f;
    float z = level.hasPlayerPosition() ? level.playerZ : 0.5f;

    // Синхронно грузим чанки вокруг игрока, чтобы он не провалился, пока работают фоновые потоки.
    world.loadArea(x, z, 2);

    float y = level.hasPlayerPosition()
        ? level.playerY
        : world.getTopY((int) Math.floor(x), (int) Math.floor(z));

    player.setPosition(x, y, z);
    player.setRotation(level.yaw, level.pitch);
    player.setFlying(level.flying);
    player.getHotbar().selectSlot(level.selectedSlot);
  }

  private void applySettings() {
    window.setVsync(settings.isVsync());
    window.setRawMouseMotion(settings.isRawMouseInput());
    world.setViewDistance(settings.getRenderDistance());
  }

  /** Захватывает курсор в игре и отпускает его в меню. */
  private void syncCursor() {
    boolean capture = !uiManager.isMenuOpen();
    if (capture != cursorCaptured) {
      window.setCursorCaptured(capture);
      cursorCaptured = capture;
    }
  }

  private void loop() {
    Input input = window.getInput();
    double lastFrame = GLFW.glfwGetTime();

    while (!window.shouldClose()) {
      double now = GLFW.glfwGetTime();
      float frameTime = (float) (now - lastFrame);
      lastFrame = now;
      if (frameTime > 0f) {
        fpsSmoothed = fpsSmoothed * 0.95f + (1f / frameTime) * 0.05f;
      }
      float deltaTime = Math.min(frameTime, MAX_FRAME_TIME);

      window.pollEvents();
      handleGlobalKeys(input);

      Vec3 position = player.getPosition();
      world.update(position.x, position.z);

      if (uiManager.isMenuOpen()) {
        uiManager.updateMouse(input, window.getWindowWidth(), window.getWindowHeight(), uiScaleX(), uiScaleY());
        blockBreaker.reset();
      } else {
        updatePlayer(input, deltaTime);
        handleBlockInteraction(input, deltaTime);
      }

      autosave(deltaTime);

      if (window.isMinimized()) {
        sleepQuietly(50);
      } else {
        render();
        window.swapBuffers();
      }

      input.endFrame();
    }
  }

  private void handleGlobalKeys(Input input) {
    if (input.wasKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
      uiManager.togglePause();
    }
    if (input.wasKeyPressed(GLFW.GLFW_KEY_F3)) {
      settings.setShowDebug(!settings.isShowDebug());
    }
    if (window.consumeFocusLost()) {
      uiManager.pause();
    }

    syncCursor();
  }

  private void updatePlayer(Input input, float deltaTime) {
    float sensitivity = settings.getMouseSensitivity();
    player.rotate((float) input.getMouseDeltaX() * sensitivity, (float) input.getMouseDeltaY() * sensitivity);

    for (int i = 0; i < Hotbar.SIZE; i++) {
      if (input.wasKeyPressed(GLFW.GLFW_KEY_1 + i)) {
        player.getHotbar().selectSlot(i);
      }
    }
    if (input.getScroll() != 0) {
      player.getHotbar().scroll((int) Math.signum(input.getScroll()));
    }
    if (input.wasKeyPressed(GLFW.GLFW_KEY_F)) {
      player.toggleFlying();
    }

    controls.clear();
    if (input.isKeyDown(GLFW.GLFW_KEY_W)) {
      controls.forward += 1;
    }
    if (input.isKeyDown(GLFW.GLFW_KEY_S)) {
      controls.forward -= 1;
    }
    if (input.isKeyDown(GLFW.GLFW_KEY_D)) {
      controls.strafe += 1;
    }
    if (input.isKeyDown(GLFW.GLFW_KEY_A)) {
      controls.strafe -= 1;
    }
    controls.jump = input.isKeyDown(GLFW.GLFW_KEY_SPACE);
    controls.descend = input.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || input.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    controls.sprint = input.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || input.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);

    player.update(world, controls, settings.getSpeedMultiplier(), deltaTime);

    // Упали за пределы мира (например, в незагруженный чанк) — возвращаемся на поверхность.
    Vec3 position = player.getPosition();
    if (position.y < -32f) {
      int x = (int) Math.floor(position.x);
      int z = (int) Math.floor(position.z);
      player.setPosition(position.x, world.getTopY(x, z), position.z);
    }
  }

  private void handleBlockInteraction(Input input, float deltaTime) {
    player.getLookDirection(lookDirection);
    target = Raycast.cast(
        world,
        player.getEyeX(), player.getEyeY(), player.getEyeZ(),
        lookDirection[0], lookDirection[1], lookDirection[2],
        REACH
    );

    blockBreaker.update(world, target, input.isMouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT), deltaTime);

    if (target.hit() && input.wasMousePressed(GLFW.GLFW_MOUSE_BUTTON_MIDDLE)) {
      player.getHotbar().pick(world.getBlock(target.blockX(), target.blockY(), target.blockZ()));
    }

    placeCooldown = Math.max(0f, placeCooldown - deltaTime);
    boolean placePressed = input.wasMousePressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    boolean placeHeld = input.isMouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT) && placeCooldown <= 0f;

    if (target.hit() && (placePressed || placeHeld)) {
      int x = target.placeX();
      int y = target.placeY();
      int z = target.placeZ();

      if (!player.intersectsBlock(x, y, z) && world.getBlock(x, y, z) == Block.AIR) {
        world.setBlock(x, y, z, player.getHotbar().getSelectedBlock());
      }
      placeCooldown = PLACE_REPEAT_DELAY;
    }
  }

  private void autosave(float deltaTime) {
    autosaveTimer += deltaTime;
    if (autosaveTimer >= AUTOSAVE_INTERVAL) {
      autosaveTimer = 0f;
      world.saveAll();
      saveLevel();
    }
  }

  private void saveLevel() {
    Vec3 position = player.getPosition();
    level.playerX = position.x;
    level.playerY = position.y;
    level.playerZ = position.z;
    level.yaw = player.getYaw();
    level.pitch = player.getPitch();
    level.flying = player.isFlying();
    level.selectedSlot = player.getHotbar().getSelectedSlot();

    try {
      LevelIO.save(worldDir, level);
    } catch (IOException e) {
      System.err.println("Не удалось сохранить мир: " + e.getMessage());
    }
  }

  private float uiScaleX() {
    float aspect = window.getAspectRatio();
    return aspect > REFERENCE_ASPECT ? REFERENCE_ASPECT / aspect : 1f;
  }

  private float uiScaleY() {
    float aspect = window.getAspectRatio();
    return aspect < REFERENCE_ASPECT ? aspect / REFERENCE_ASPECT : 1f;
  }

  private void render() {
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

    float far = (world.getViewDistance() + 2) * World.CHUNK_SIZE * 1.5f;
    projection.setPerspective((float) Math.toRadians(settings.getFov()), window.getAspectRatio(), NEAR, far);

    cameraPos.set(player.getEyeX(), player.getEyeY(), player.getEyeZ());
    player.getLookDirection(lookDirection);
    view.setLookAt(
        cameraPos.x, cameraPos.y, cameraPos.z,
        cameraPos.x + lookDirection[0], cameraPos.y + lookDirection[1], cameraPos.z + lookDirection[2],
        0f, 1f, 0f
    );

    worldRenderer.render(blockShader, projection, view, cameraPos, settings, SKY_COLOR);

    boolean menuOpen = uiManager.isMenuOpen();
    if (!menuOpen && target.hit()) {
      worldRenderer.renderSelection(lineShader, projection, view, target.blockX(), target.blockY(), target.blockZ());
    }

    float scaleX = uiScaleX();
    float scaleY = uiScaleY();
    hudShader.use();
    hudShader.setScale(scaleX, scaleY);
    hudTexturedShader.use();
    hudTexturedShader.setScale(scaleX, scaleY);

    if (!menuOpen) {
      Hud.drawCrosshair(hudShader);
      Hud.drawBreakProgress(hudShader, blockBreaker.getProgress());
      Hud.drawHotbar(hudShader, hudTexturedShader, atlas, player.getHotbar());
    }

    if (settings.isShowDebug()) {
      uiManager.drawDebug(hudShader, hudTexturedShader, debugText());
    }

    uiManager.render(hudShader, hudTexturedShader);
  }

  private String debugText() {
    Vec3 p = player.getPosition();
    String targetText = target.hit()
        ? String.format("%d %d %d", target.blockX(), target.blockY(), target.blockZ())
        : "-";

    return String.format(
        "FPS: %.0f%nXYZ: %.1f / %.1f / %.1f%nЧанк: %d %d   Взгляд: %.0f / %.0f%n"
            + "Чанки: %d (видно %d, в очереди %d)%nБлок: %s   Полёт: %s%nСид: %d",
        fpsSmoothed,
        p.x, p.y, p.z,
        Math.floorDiv((int) Math.floor(p.x), World.CHUNK_SIZE),
        Math.floorDiv((int) Math.floor(p.z), World.CHUNK_SIZE),
        player.getYaw(), player.getPitch(),
        world.getLoadedChunkCount(), worldRenderer.getVisibleChunkCount(), world.getPendingChunkCount(),
        targetText,
        player.isFlying() ? "да" : "нет",
        world.getSeed()
    );
  }

  private void shutdown() {
    if (world != null) {
      saveLevel();
      world.close();
    }
    SettingsIO.save(settings);

    if (window == null) {
      return;
    }

    if (worldRenderer != null) {
      worldRenderer.delete();
    }
    if (uiManager != null) {
      uiManager.delete();
    }
    UiDrawer.delete();
    for (ShaderProgram shader : new ShaderProgram[] {blockShader, lineShader, hudShader, hudTexturedShader}) {
      if (shader != null) {
        shader.delete();
      }
    }
    if (atlas != null) {
      atlas.delete();
    }

    window.destroy();
  }

  private static void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
