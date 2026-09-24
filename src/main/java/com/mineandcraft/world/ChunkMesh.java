package com.mineandcraft.world;

import com.mineandcraft.graphics.Mesh;

import java.nio.FloatBuffer;

/** GPU-меш одного чанка. Живёт, пока чанк загружен; после выгрузки обязательно вызвать {@link #delete()}. */
public class ChunkMesh {

  private final int chunkX;
  private final int chunkZ;
  private final Mesh mesh = new Mesh();
  private boolean dirty = true;
  private boolean urgent;
  private boolean built;

  public ChunkMesh(int chunkX, int chunkZ) {
    this.chunkX = chunkX;
    this.chunkZ = chunkZ;
  }

  public int getChunkX() {
    return chunkX;
  }

  public int getChunkZ() {
    return chunkZ;
  }

  public void markDirty(boolean urgent) {
    dirty = true;
    this.urgent |= urgent;
  }

  public boolean isDirty() {
    return dirty;
  }

  /** Изменение от игрока: перестраивается в этом же кадре вне общего лимита. */
  public boolean isUrgent() {
    return urgent;
  }

  public boolean isBuilt() {
    return built;
  }

  public void upload(FloatBuffer vertices) {
    mesh.upload(vertices);
    dirty = false;
    urgent = false;
    built = true;
  }

  public int getVertexCount() {
    return mesh.getVertexCount();
  }

  public void render() {
    mesh.render();
  }

  public void delete() {
    mesh.delete();
  }
}
