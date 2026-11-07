# PrisonMinesX

An advanced **prison mines** plugin for Paper. It resets mines asynchronously through FastAsyncWorldEdit, supports several storage backends, and has a full in-game GUI and a developer API.

![Paper](https://img.shields.io/badge/Paper-1.20.4+-blue) ![Java](https://img.shields.io/badge/Java-21-orange) ![Build](https://img.shields.io/badge/build-Gradle-02303A)

## Features

- **Fast, lag-free resets** through FastAsyncWorldEdit, with a configurable reset animation
- **Weighted block composition** for each mine
- **Flexible reset triggers**: timers, cron-style schedules, a "% mined" threshold, and skipping empty resets
- **Spatial hashing** (chunk-based lookups) so servers with many mines stay fast
- **Mine GUI** for browsing, sorting and managing mines in game
- **Holograms** showing mine status and reset timers
- **Player features**: auto-pickup, fortune on every block, safe teleport out of a mine before it resets
- **Statistics** per mine and per player
- **Storage backends**: YAML, SQLite or MySQL (HikariCP connection pool)
- **PlaceholderAPI** expansion
- **Developer API** with events: `MineCreateEvent`, `MineDeleteEvent`, `MinePreResetEvent`, `MinePostResetEvent`, `MineBlockBreakEvent`, `MineRedefineEvent`, `MineStateChangeEvent`
- Every message is configurable in `messages.yml`

## Commands

All commands go through `/prisonmines` (alias `/pmines`).

| Subcommand | Description | Permission |
|---|---|---|
| `help` | Show help | `prisonminesx.cmd.help` |
| `gui` | Open the mine management GUI | `prisonminesx.cmd.gui` |
| `list` | List mines | `prisonminesx.cmd.list` |
| `info <mine>` | Show mine details | `prisonminesx.cmd.info` |
| `set <mine> ...` | Edit block composition | `prisonminesx.cmd.set` |
| `flags <mine>` | View or change mine flags | `prisonminesx.cmd.flags` |
| `timers` | Show reset timers | `prisonminesx.cmd.timers` |
| `stats` | Show mining statistics | `prisonminesx.cmd.stats` |
| `start` / `stop <mine>` | Start or stop a mine's reset cycle | `prisonminesx.cmd.start` / `.stop` |
| `tp <mine>` | Teleport to a mine | `prisonminesx.cmd.tp` |
| `reload` | Reload configuration | `prisonminesx.cmd.reload` |

`prisonminesx.admin` (or `prisonminesx.*`) grants everything. All permissions default to op.

## Configuration

- `config.yml`: global settings (performance, pagination, reset behaviour, storage backend)
- `messages.yml`: every player-facing message

To use MySQL, set `storage.type` to `mysql` and fill in the `storage.mysql` section.

## Requirements

- Paper 1.20.4+
- Java 21
- [FastAsyncWorldEdit](https://www.spigotmc.org/resources/fastasyncworldedit.13932/) (required)
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (optional)

## Building

```bash
./gradlew shadowJar
```

The shaded jar (HikariCP bundled in) goes to `build/libs/`.

## Developer API

```java
PrisonMinesAPI api = PrisonMinesAPI.getInstance();

@EventHandler
public void onReset(MinePostResetEvent event) {
    // react to a mine finishing its reset
}
```

## Project layout

```
src/main/java/com/sasha/prisonminesx/
├── PrisonMinesX.java    # Plugin entry point
├── api/                 # Public API and events
├── commands/            # /prisonmines and tab completion
├── gui/                 # Management GUI
├── hooks/               # PlaceholderAPI expansion
├── listeners/           # Mine, player, world, chat and GUI listeners
├── managers/            # Mine registry and holograms
├── models/              # Mine model
├── storage/             # YAML / SQLite / MySQL providers
├── tasks/               # Reset timers and player tracking
└── utils/               # FAWE reset engine, serialisation, time helpers
```
