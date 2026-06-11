package com.mineandcraft.world;

import com.mineandcraft.config.GameSettings;
import com.mineandcraft.graphics.Mesh;
import com.mineandcraft.graphics.ShaderProgram;
import com.mineandcraft.graphics.TextureAtlas;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class WorldRenderer {

  private final World world;
  private final TextureAtlas atlas;
  private final Map<Long, ChunkMesh> meshes = new HashMap<>();
  private final Mesh selectionMesh = new Mesh(Mesh.Layout.POSITION);

  public WorldRenderer(World world, TextureAtlas atlas) {
    this.world = world;
    this.atlas = atlas;

    world.addChunkListener(this::markChunkDirty);
    buildSelectionMesh();
  }

  public void updateAround(float worldX, float worldZ) {
    world.updateChunksAround(worldX, worldZ);

    Set<Long> activeKeys = new HashSet<>();
    for (Chunk chunk : world.getLoadedChunks()) {
      long key = World.chunkKey(chunk.getChunkX(), chunk.getChunkZ());
      activeKeys.add(key);
      meshes.computeIfAbsent(key, k -> new ChunkMesh(chunk.getChunkX(), chunk.getChunkZ()));
    }

    meshes.keySet().removeIf(key -> {
      if (!activeKeys.contains(key)) {
        return true;
      }
      return false;
    });
  }

  private void markChunkDirty(long chunkKey) {
    ChunkMesh mesh = meshes.get(chunkKey);
    if (mesh != null) {
      mesh.markDirty();
    }
  }

  public void rebuildDirtyChunks() {
    for (ChunkMesh mesh : meshes.values()) {
      mesh.rebuildIfNeeded(world, atlas);
    }
  }

  private void buildSelectionMesh() {
    float e = 0.002f;
    float[] cube = {
        -e, -e, -e,   1 + e, -e, -e,
         1 + e, -e, -e,   1 + e, 1 + e, -e,
         1 + e, 1 + e, -e,   -e, 1 + e, -e,
        -e, 1 + e, -e,   -e, -e, -e,

        -e, -e, 1 + e,   1 + e, -e, 1 + e,
         1 + e, -e, 1 + e,   1 + e, 1 + e, 1 + e,
         1 + e, 1 + e, 1 + e,   -e, 1 + e, 1 + e,
        -e, 1 + e, 1 + e,   -e, -e, 1 + e,

        -e, 1 + e, -e,   1 + e, 1 + e, -e,
         1 + e, 1 + e, -e,   1 + e, 1 + e, 1 + e,
         1 + e, 1 + e, 1 + e,   -e, 1 + e, 1 + e,
        -e, 1 + e, 1 + e,   -e, 1 + e, -e,

        -e, -e, -e,   1 + e, -e, -e,
         1 + e, -e, -e,   1 + e, -e, 1 + e,
         1 + e, -e, 1 + e,   -e, -e, 1 + e,
        -e, -e, 1 + e,   -e, -e, -e,

         1 + e, -e, -e,   1 + e, 1 + e, -e,
         1 + e, 1 + e, -e,   1 + e, 1 + e, 1 + e,
         1 + e, 1 + e, 1 + e,   1 + e, -e, 1 + e,
         1 + e, -e, 1 + e,   1 + e, -e, -e,

        -e, -e, -e,   -e, 1 + e, -e,
        -e, 1 + e, -e,   -e, 1 + e, 1 + e,
        -e, 1 + e, 1 + e,   -e, -e, 1 + e,
        -e, -e, 1 + e,   -e, -e, -e,
    };

    selectionMesh.upload(cube);
  }

  public void render(
      ShaderProgram shader,
      Matrix4f projection,
      Matrix4f view,
      Vector3f cameraPos,
      GameSettings settings
  ) {
    updateAround(cameraPos.x, cameraPos.z);
    rebuildDirtyChunks();

    shader.use();
    shader.setMatrices(projection, view);
    shader.setTextureUnit(0);
    shader.setCameraPos(cameraPos);
    shader.setFogColor(0.53f, 0.71f, 0.98f);
    shader.setFogEnabled(settings.isFogEnabled());

    glActiveTexture(GL_TEXTURE0);
    atlas.bind();

    for (ChunkMesh mesh : meshes.values()) {
      mesh.render();
    }
  }

  public void renderSelection(ShaderProgram lineShader, Matrix4f projection, Matrix4f view, int x, int y, int z) {
    lineShader.use();
    lineShader.setMatrices(projection, view);
    lineShader.setModel(new Matrix4f().translation(x, y, z));
    lineShader.setColor(0f, 0f, 0f, 0.9f);

    glLineWidth(2f);
    glDisable(GL_DEPTH_TEST);
    selectionMesh.render();
    glEnable(GL_DEPTH_TEST);
  }
}
