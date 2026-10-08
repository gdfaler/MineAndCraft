# MineAndCraft

[![Build](https://github.com/gdfaler/MineAndCraft/actions/workflows/build.yml/badge.svg)](https://github.com/gdfaler/MineAndCraft/actions/workflows/build.yml)
![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![OpenGL 3.3](https://img.shields.io/badge/OpenGL-3.3-5586A4?logo=opengl&logoColor=white)

A voxel sandbox in the spirit of Minecraft, written from scratch in Java with LWJGL and plain OpenGL. There's no game engine underneath: the world generator, renderer, physics and menus are all in this repo.

<!-- Screenshot goes here: drag an image into the GitHub editor and it inserts the link for you -->

## What's in it

- **An infinite world from a seed.** Plains, hills, mountains, beaches and forests on the surface. Spaghetti caves, bigger caverns deeper down, coal, iron and gravel underground.
- **13 block types**, including see-through glass and leaves. Every texture except the grass top is drawn by code at startup instead of being loaded from image files.
- **Rendering:** smooth ambient occlusion on block corners, distance fog, frustum culling.
- **Movement:** collisions, jumping, sprinting and a fly mode.
- **Mining takes time.** Every block has its own hardness. Leaves break almost instantly, ore takes a while, and bedrock doesn't break at all.
- **Saving.** The world and the player autosave every minute and when you quit. Settings carry over between runs.

## How it works

The parts I had the most fun with:

- **Chunk streaming.** The world is split into 16×128×16 chunks. Missing chunks are loaded from disk or generated on a pool of up to 4 background threads, nearest first, then handed to the main thread when they're ready.
- **Deterministic generation.** A chunk depends only on the seed and its coordinates. Trees that cross a chunk border still line up, and a chunk nobody touched never needs saving because it can be regenerated.
- **Meshing away from OpenGL.** `ChunkMesher` builds the geometry without touching the GPU, skips faces hidden between solid blocks, and flips each quad's diagonal based on its occlusion values so the shading doesn't streak. `WorldRenderer` uploads at most 4 rebuilt chunks per frame to keep the frame rate steady. Blocks you break or place update right away.
- **Small saves.** Only chunks you've changed get written to disk, one gzip file each.

## Controls

| Action | Key |
|---|---|
| Move | `W` `A` `S` `D` |
| Jump / fly up | `Space` |
| Fly down | `Shift` |
| Sprint | `Ctrl` |
| Toggle flying | `F` |
| Break block | hold left mouse |
| Place block | right mouse (hold to keep placing) |
| Pick the block you're looking at | middle mouse |
| Hotbar slot | `1`–`9` or mouse wheel |
| Debug info | `F3` |
| Pause menu | `Esc` |

The game pauses by itself when the window loses focus.

## Running it

You need **JDK 21+**, **Maven 3.9+** and a GPU with OpenGL 3.3. Maven picks the right LWJGL native libraries for Windows, Linux and macOS (Intel and Apple Silicon).

**Windows and Linux**

```bash
mvn compile exec:java
```

**macOS.** GLFW has to run on the process's first thread, so start the built jar with `-XstartOnFirstThread`:

```bash
mvn package -DskipTests
java -XstartOnFirstThread -jar target/MineAndCraft-0.1.0-all.jar
```

In IntelliJ IDEA, run `com.mineandcraft.Main`. On a Mac, add `-XstartOnFirstThread` to the VM options first.

### Options

| Option | What it does |
|---|---|
| `--seed <value>` | Seed for a new world. Numbers are used as-is, and any text works too |
| `--world <folder>` | Where the world is saved (default: `~/.mineandcraft/world`) |

With Maven: `mvn compile exec:java -Dexec.args="--seed 12345"`. With the jar, put them after the jar name.

## Settings and saves

`Esc` opens the pause menu with **Resume**, **Settings** and **Quit**. Settings has mouse sensitivity, FOV (60–100), render distance (4–16 chunks), movement speed, fog, VSync, the debug overlay and raw mouse input.

Everything is stored in `~/.mineandcraft`:

```
.mineandcraft/
├── settings.properties        # your settings
└── world/
    ├── level.properties       # seed, player position and rotation
    └── chunks/c.<x>.<z>.dat   # chunks you've changed (gzip)
```

To start a new world, delete the `world` folder or run with `--world <new folder>`.

## Tests

```bash
mvn verify
```

60 JUnit 5 tests in 11 classes cover world generation, meshing, raycasting, player physics, saving and settings. None of them need a GPU, so GitHub Actions runs them on Ubuntu and Windows on every push. `mvn verify` also builds the runnable jar.

## Project layout

```
src/main/java/com/mineandcraft/
├── Main.java        # entry point, command-line options
├── config/          # settings and world metadata
├── engine/          # game loop, window, input, raycasting
│   └── physics/     # player collisions
├── graphics/        # shaders, texture atlas, HUD, font, UI drawing
├── player/          # player, hotbar, block breaking
├── ui/              # pause menu and settings screen
└── world/           # blocks, generation, chunks, storage, mesher, renderer
```

## Built with

[LWJGL 3](https://www.lwjgl.org/) (GLFW, OpenGL, stb) · [JOML](https://github.com/JOML-CI/JOML) · JUnit 5 · Maven

---

Made by [Egor](https://github.com/gdfaler) to learn how voxel engines work. A fan project, not affiliated with Mojang or Microsoft, and it uses none of Minecraft's code or assets.
