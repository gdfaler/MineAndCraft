package com.mineandcraft;

import com.mineandcraft.engine.Camera;
import com.mineandcraft.engine.Window;
import com.mineandcraft.world.World;

public final class Main {

  private Main() {
  }

  public static void main(String[] args) {
    World world = new World();
    Camera camera = new Camera();

    Window window = new Window(world, camera);
    window.run();
  }
}
