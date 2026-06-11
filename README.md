# MineAndCraft

Воксельная sandbox-игра в духе Minecraft на Java + LWJGL 3.

## Требования

- **JDK 21+** (в проекте настроен Java 25)
- **Maven 3.9+**
- **Windows** (нативные библиотеки LWJGL подключены для Windows)

## Быстрый старт

```bash
mvn compile exec:java
```

Или в IntelliJ IDEA: запустите `com.mineandcraft.Main`.

## Управление

| Действие | Клавиша |
|----------|---------|
| Движение | `W` `A` `S` `D` |
| Прыжок | `Space` |
| Спринт | `Ctrl` |
| Ломать блок | Зажать **ЛКМ** |
| Ставить блок | **ПКМ** |
| Слот хотбара | `1`–`9` или **колёсико мыши** |
| Пауза | `Escape` |

## Структура проекта

```
src/main/java/com/mineandcraft/
├── Main.java                 # Точка входа
├── engine/                   # Игровой цикл, окно, камера, рейкаст
│   └── physics/              # Коллизии игрока
├── graphics/                 # Шейдеры, текстуры, HUD, меши
├── player/                   # Хотбар и управление инвентарём
└── world/                    # Блоки, генерация мира, чанки, рендер

src/main/resources/
└── textures/                 # Текстуры (grass.png и др.)
```

## Сборка JAR

```bash
mvn package
```

## Технологии

- [LWJGL 3](https://www.lwjgl.org/) — окно, OpenGL, загрузка изображений
- [JOML](https://github.com/JOML-CI/JOML) — математика (матрицы, векторы)
- OpenGL 3.3 Core — рендер мира и интерфейса

## Лицензия

Учебный проект. Не является копией Minecraft и не использует его ассеты.
