package com.mineandcraft.world;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Сохраняет каждый чанк в отдельный сжатый файл {@code chunks/c.<x>.<z>.dat}.
 * Запись идёт во временный файл с последующей атомарной заменой,
 * поэтому фоновый поток никогда не прочитает наполовину записанный чанк.
 */
public class FileChunkStorage implements ChunkStorage {

  private static final int MAGIC = 0x4D414343; // "MACC"
  private static final int VERSION = 1;

  private final Path directory;

  public FileChunkStorage(Path worldDirectory) {
    this.directory = worldDirectory.resolve("chunks");
  }

  @Override
  public byte[] load(int chunkX, int chunkZ) throws IOException {
    Path file = fileFor(chunkX, chunkZ);

    try (InputStream raw = Files.newInputStream(file);
         DataInputStream in = new DataInputStream(new GZIPInputStream(raw))) {
      if (in.readInt() != MAGIC) {
        throw new IOException("Not a chunk file: " + file);
      }

      int version = in.readInt();
      int height = in.readInt();
      if (version != VERSION || height != World.HEIGHT) {
        throw new IOException("Unsupported chunk format in " + file);
      }

      byte[] data = new byte[Chunk.VOLUME];
      in.readFully(data);
      return data;
    } catch (NoSuchFileException e) {
      return null;
    }
  }

  @Override
  public void save(int chunkX, int chunkZ, byte[] data) throws IOException {
    Files.createDirectories(directory);
    Path file = fileFor(chunkX, chunkZ);
    Path temp = directory.resolve(file.getFileName() + ".tmp");

    try (OutputStream raw = Files.newOutputStream(temp);
         DataOutputStream out = new DataOutputStream(new GZIPOutputStream(raw))) {
      out.writeInt(MAGIC);
      out.writeInt(VERSION);
      out.writeInt(World.HEIGHT);
      out.write(data);
    }

    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private Path fileFor(int chunkX, int chunkZ) {
    return directory.resolve("c." + chunkX + "." + chunkZ + ".dat");
  }
}
