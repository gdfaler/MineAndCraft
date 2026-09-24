package com.mineandcraft.world;

import com.mineandcraft.config.GameSettings;
import com.mineandcraft.graphics.Mesh;
import com.mineandcraft.graphics.ShaderProgram;
import com.mineandcraft.graphics.TextureAtlas;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class WorldRenderer implements World.Listener {

  /** Сколько чанков перестраивается за кадр (кроме срочных изменений от игрока). */
  private static final int REBUILD_BUDGET = 4;

  private final World world;
  private final TextureAtlas atlas;
  private final ChunkMesher mesher = new ChunkMesher();
  private final Map<Long, ChunkMesh> meshes = new HashMap<>();
  private final Mesh selectionMesh = new Mesh(Mesh.Layout.POSITION);
  private final Chunk[][] neighborhood = new Chunk[3][3];
  private final List<ChunkMesh> rebuildQueue = new ArrayList<>();
  private final FrustumIntersection frustum = new FrustumIntersection();
  private final Matrix4f viewProjection = new Matrix4f();
  private final Matrix4f model = new Matrix4f();
  private FloatBuffer uploadBuffer = MemoryUtil.memAllocFloat(1 << 16);

  private int visibleChunks;

  public WorldRenderer(World world, TextureAtlas atlas) {
    this.world = world;
    this.atlas = atlas;

    world.addListener(this);
    for (Chunk chunk : world.getLoadedChunks()) {
      chunkLoaded(chunk);
    }
    buildSelectionMesh();
  }

  @Override
  public void chunkLoaded(Chunk chunk) {
    meshes.computeIfAbsent(chunk.key(), k -> new ChunkMesh(chunk.getChunkX(), chunk.getChunkZ()));

    // Соседям теперь известно, что лежит за их границей: лишние грани на стыке пропадут.
    markDirty(chunk.getChunkX() - 1, chunk.getChunkZ(), false);
    markDirty(chunk.getChunkX() + 1, chunk.getChunkZ(), false);
    markDirty(chunk.getChunkX(), chunk.getChunkZ() - 1, false);
    markDirty(chunk.getChunkX(), chunk.getChunkZ() + 1, false);
  }

  @Override
  public void chunkUnloaded(Chunk chunk) {
    ChunkMesh mesh = meshes.remove(chunk.key());
    if (mesh != null) {
      mesh.delete();
    }
  }

  @Override
  public void blockChanged(int x, int y, int z) {
    int chunkX = Math.floorDiv(x, World.CHUNK_SIZE);
    int chunkZ = Math.floorDiv(z, World.CHUNK_SIZE);
    int localX = Math.floorMod(x, World.CHUNK_SIZE);
    int localZ = Math.floorMod(z, World.CHUNK_SIZE);

    // Блок на краю чанка влияет на грани и затенение соседних чанков (включая диагональные).
    int dxMin = localX == 0 ? -1 : 0;
    int dxMax = localX == World.CHUNK_SIZE - 1 ? 1 : 0;
    int dzMin = localZ == 0 ? -1 : 0;
    int dzMax = localZ == World.CHUNK_SIZE - 1 ? 1 : 0;

    for (int dx = dxMin; dx <= dxMax; dx++) {
      for (int dz = dzMin; dz <= dzMax; dz++) {
        markDirty(chunkX + dx, chunkZ + dz, true);
      }
    }
  }

  private void markDirty(int chunkX, int chunkZ, boolean urgent) {
    ChunkMesh mesh = meshes.get(World.chunkKey(chunkX, chunkZ));
    if (mesh != null) {
      mesh.markDirty(urgent);
    }
  }

  private boolean hasCardinalNeighbors(int chunkX, int chunkZ) {
    return world.isChunkLoaded(chunkX - 1, chunkZ)
        && world.isChunkLoaded(chunkX + 1, chunkZ)
        && world.isChunkLoaded(chunkX, chunkZ - 1)
        && world.isChunkLoaded(chunkX, chunkZ + 1);
  }

  private void rebuildDirtyChunks(Vector3f cameraPos) {
    rebuildQueue.clear();
    for (ChunkMesh mesh : meshes.values()) {
      if (mesh.isDirty() && hasCardinalNeighbors(mesh.getChunkX(), mesh.getChunkZ())) {
        rebuildQueue.add(mesh);
      }
    }

    if (rebuildQueue.isEmpty()) {
      return;
    }

    float camChunkX = cameraPos.x / World.CHUNK_SIZE - 0.5f;
    float camChunkZ = cameraPos.z / World.CHUNK_SIZE - 0.5f;
    rebuildQueue.sort((a, b) -> Float.compare(
        distanceSq(a, camChunkX, camChunkZ),
        distanceSq(b, camChunkX, camChunkZ)
    ));

    int budget = REBUILD_BUDGET;
    for (ChunkMesh mesh : rebuildQueue) {
      if (!mesh.isUrgent()) {
        if (budget <= 0) {
          continue;
        }
        budget--;
      }

      rebuild(mesh);
    }
  }

  private static float distanceSq(ChunkMesh mesh, float x, float z) {
    float dx = mesh.getChunkX() - x;
    float dz = mesh.getChunkZ() - z;
    return dx * dx + dz * dz;
  }

  private void rebuild(ChunkMesh mesh) {
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        neighborhood[dx + 1][dz + 1] = world.getChunk(mesh.getChunkX() + dx, mesh.getChunkZ() + dz);
      }
    }

    mesher.build(neighborhood);

    int size = mesher.size();
    if (uploadBuffer.capacity() < size) {
      uploadBuffer = MemoryUtil.memRealloc(uploadBuffer, Math.max(size, uploadBuffer.capacity() * 2));
    }
    uploadBuffer.clear();
    uploadBuffer.put(mesher.vertices(), 0, size).flip();
    mesh.upload(uploadBuffer);
  }

  private void buildSelectionMesh() {
    float e = 0.002f;
    float lo = -e;
    float hi = 1 + e;
    float[] cube = {
        lo, lo, lo, hi, lo, lo,   hi, lo, lo, hi, lo, hi,   hi, lo, hi, lo, lo, hi,   lo, lo, hi, lo, lo, lo,
        lo, hi, lo, hi, hi, lo,   hi, hi, lo, hi, hi, hi,   hi, hi, hi, lo, hi, hi,   lo, hi, hi, lo, hi, lo,
        lo, lo, lo, lo, hi, lo,   hi, lo, lo, hi, hi, lo,   hi, lo, hi, hi, hi, hi,   lo, lo, hi, lo, hi, hi,
    };

    selectionMesh.upload(cube);
  }

  public void render(
      ShaderProgram shader,
      Matrix4f projection,
      Matrix4f view,
      Vector3f cameraPos,
      GameSettings settings,
      float[] fogColor
  ) {
    rebuildDirtyChunks(cameraPos);

    float viewBlocks = world.getViewDistance() * World.CHUNK_SIZE;

    shader.use();
    shader.setMatrices(projection, view);
    shader.setTextureUnit(0);
    shader.setCameraPos(cameraPos);
    shader.setFogColor(fogColor[0], fogColor[1], fogColor[2]);
    shader.setFogEnabled(settings.isFogEnabled());
    shader.setFogRange(viewBlocks * 0.55f, viewBlocks * 0.95f);

    glActiveTexture(GL_TEXTURE0);
    atlas.bind();

    projection.mul(view, viewProjection);
    frustum.set(viewProjection);

    float maxDistance = viewBlocks + World.CHUNK_SIZE;
    float maxDistanceSq = maxDistance * maxDistance;
    visibleChunks = 0;

    for (ChunkMesh mesh : meshes.values()) {
      if (!mesh.isBuilt()) {
        continue;
      }

      float minX = mesh.getChunkX() * World.CHUNK_SIZE;
      float minZ = mesh.getChunkZ() * World.CHUNK_SIZE;
      float centerDx = minX + World.CHUNK_SIZE * 0.5f - cameraPos.x;
      float centerDz = minZ + World.CHUNK_SIZE * 0.5f - cameraPos.z;

      if (centerDx * centerDx + centerDz * centerDz > maxDistanceSq) {
        continue;
      }
      if (!frustum.testAab(minX, 0, minZ, minX + World.CHUNK_SIZE, World.HEIGHT, minZ + World.CHUNK_SIZE)) {
        continue;
      }

      mesh.render();
      visibleChunks++;
    }
  }

  public void renderSelection(ShaderProgram lineShader, Matrix4f projection, Matrix4f view, int x, int y, int z) {
    lineShader.use();
    lineShader.setMatrices(projection, view);
    lineShader.setModel(model.translation(x, y, z));
    lineShader.setColor(0f, 0f, 0f, 0.8f);

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    selectionMesh.renderLines();
    glDisable(GL_BLEND);
  }

  public int getVisibleChunkCount() {
    return visibleChunks;
  }

  public void delete() {
    for (ChunkMesh mesh : meshes.values()) {
      mesh.delete();
    }
    meshes.clear();
    selectionMesh.delete();
    MemoryUtil.memFree(uploadBuffer);
  }
}
