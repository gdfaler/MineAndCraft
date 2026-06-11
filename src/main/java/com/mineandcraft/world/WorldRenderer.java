package com.mineandcraft.world;

import com.mineandcraft.graphics.Mesh;
import com.mineandcraft.graphics.ShaderProgram;
import com.mineandcraft.graphics.TextureAtlas;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class WorldRenderer {

  private final World world;
  private final TextureAtlas atlas;
  private final Map<Integer, ChunkMesh> chunks = new HashMap<>();
  private final Mesh selectionMesh = new Mesh(Mesh.Layout.POSITION);

  public WorldRenderer(World world, TextureAtlas atlas) {
    this.world = world;
    this.atlas = atlas;

    int chunksX = World.SIZE / World.CHUNK_SIZE;
    int chunksZ = World.SIZE / World.CHUNK_SIZE;

    for (int cx = 0; cx < chunksX; cx++) {
      for (int cz = 0; cz < chunksZ; cz++) {
        chunks.put(World.chunkKey(cx, cz), new ChunkMesh(cx, cz));
      }
    }

    world.addChunkListener(this::markChunkDirty);
    rebuildAll();
    buildSelectionMesh();
  }

  private void markChunkDirty(int chunkKey) {
    ChunkMesh mesh = chunks.get(chunkKey);
    if (mesh != null) {
      mesh.markDirty();
    }
  }

  public void rebuildDirtyChunks() {
    for (ChunkMesh chunk : chunks.values()) {
      chunk.rebuildIfNeeded(world, atlas);
    }
  }

  private void rebuildAll() {
    for (ChunkMesh chunk : chunks.values()) {
      chunk.markDirty();
      chunk.rebuildIfNeeded(world, atlas);
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

  public void render(ShaderProgram shader, Matrix4f projection, Matrix4f view, Vector3f cameraPos) {
    rebuildDirtyChunks();

    shader.use();
    shader.setMatrices(projection, view);
    shader.setTextureUnit(0);
    shader.setCameraPos(cameraPos);
    shader.setFogColor(0.53f, 0.71f, 0.98f);

    glActiveTexture(GL_TEXTURE0);
    atlas.bind();

    for (ChunkMesh chunk : chunks.values()) {
      chunk.render();
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
