# Mirrora [简体中文](README.md)

Create realistic mirrors in Minecraft servers. Players standing in front
of the mirror can see their own real-time reflection (as well as all
other nearby players), including poses, equipment, facing direction, and
hand animations.

<p align="center">
  <img src="preview.png" width="760">
</p>

## Features

- The mirror exists as a fixed rectangular area in the world, attached to a wall, and remains after server restarts.
- Only players standing on the **front side** of the mirror and within the selected rectangular area and effective depth range will be reflected.
- All players inside the mirror area can see each other's reflections.
- Reflections synchronize position, direction, pose, equipment, hand animations, and actions.
- The mirror is only visible to players inside the area.

## How It Works

The plugin uses the `mannequin` entity introduced in Minecraft 1.21.9+
as the reflection carrier.

Using [packetevents](https://github.com/retrooper/packetevents), entity
packets are directly sent to clients. No real entities are created on
the server, and no resource packs or client mods are required.

## Dependencies

- [Paper](https://papermc.io/) 1.21.9 or higher
- [packetevents](https://github.com/retrooper/packetevents) 2.13.0 or higher

## Installation

1. Install packetevents into the `plugins/` directory.
2. Put `Mirrora.jar` into the `plugins/` directory.
3. Start the server.

## Usage

Creating mirrors requires `mirrora.admin` permission.

### 1. Get Selection Tool

```
/mirror wand
```

Obtain a "mirror selection tool" to select a wall area as the mirror range.

### 2. Select Two Points

Hold the selection tool:

- **Left click** a block on the wall as point 1
- **Right click** another block on the wall as point 2

The two points must be on walls with the **same facing direction** (for example, both on a north-facing wall), otherwise the mirror cannot be created.

The rectangle formed between the two points becomes the mirror area.

### 3. Create Mirror

```
/mirror create <id> [depth]
```

- `id`: Unique identifier of the mirror, used for later management (removing, listing)
- `depth` (optional): Effective distance in front of the mirror, measured in blocks. Default: 8, maximum: 32. Players must stand within this distance to see or be reflected.

Example:

```
/mirror create test-mirror 6
```

### 4. Manage Mirrors

```
/mirror list # List all created mirrors
/mirror remove <id> # Remove a specific mirror
```

## Commands & Permissions

| Command | Description | Permission |
| --- | --- | --- |
| `/mirror wand` | Get selection tool | `mirrora.admin` |
| `/mirror create <id> [depth]` | Create mirror | `mirrora.admin` |
| `/mirror remove <id>` | Remove mirror | `mirrora.admin` |
| `/mirror list` | List mirrors | `mirrora.admin` |

| Permission Node | Description | Default |
| --- | --- | --- |
| `mirrora.admin` | Allows creating, removing, and managing mirror areas | `op` |

After a mirror is created, any player standing inside the mirror area can see reflections without additional permissions.

## Data Storage

Mirror area information is stored in: `plugins/Mirrora/mirrors.yml`  
The plugin automatically loads mirror data on startup and immediately writes changes to disk when mirrors are created or removed.
