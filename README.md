# ItemCooldowns

[![Modrinth](https://img.shields.io/modrinth/v/item_cooldowns?label=Modrinth&style=flat-square)](https://modrinth.com/plugin/item_cooldowns)

Plugin that adds cooldowns to PvP items: **mace, spear, trident, end crystals and respawn anchors**. When a player tries to use an item during its cooldown, the action is cancelled and a chat message is shown.

## Features

- **Mace** — 45 second cooldown between attacks
- **Spear** (1.21.11+, all 7 variants) — 5 second cooldown between lunges (Jab/Charge) and attacks
- **Trident** — 5 second cooldown between throws, Riptide dashes and attacks
- **End Crystal** — 45 second cooldown per use
- **Respawn Anchor** — 45 second cooldown per use
- **Ender pearl style visual cooldown**: the inventory slot is covered by a translucent white square that shrinks over time — works on every item of that type in the inventory
- Chat message when a player tries to use an item during its cooldown
- Fully configurable: duration, action cancel, overlay, messages
- `pvp-only`: mace/spear/trident attacks only trigger cooldowns against players (default)
- World whitelist (disabled by default — works everywhere)

## Compatibility

- **Server software**: Paper, Spigot, Bukkit, Purpur, Pufferfish, Leaves and any other Bukkit fork. On Fabric/Quilt it works through Banner/CardBoard
- **Minecraft versions**: **1.21 – 26.x** (Java 21+). The spear only exists since 1.21.11 — on older versions that rule automatically disables itself
- **Folia**: the plugin uses no schedulers, fully compatible
- **LuckPerms**: plugin permissions are managed through LuckPerms

## Installation

1. Download `ItemCooldowns-1.00.jar`
2. Place it in the server's `plugins` folder
3. Restart the server
4. On first start, `config.yml` and the `lang/` folder (with `messages-en.yml` and `messages-ru.yml` templates) are created

## Permissions (LuckPerms)

Bypass permissions are **not granted by default to anyone, even operators** — grant them manually:

| Permission | Description |
|---|---|
| `itemcooldowns.bypass.mace` | Bypass mace cooldown |
| `itemcooldowns.bypass.spear` | Bypass spear cooldown |
| `itemcooldowns.bypass.trident` | Bypass trident cooldown |
| `itemcooldowns.bypass.end-crystal` | Bypass end crystal cooldown |
| `itemcooldowns.bypass.respawn-anchor` | Bypass respawn anchor cooldown |
| `itemcooldowns.bypass` | Bypass all cooldowns (parent permission) |
| `itemcooldowns.admin` | Access to `/itemcooldowns reload` (default: operators) |

Example:

```
/lp user Sooqwess permission set itemcooldowns.bypass.mace true
/lp group vip permission set itemcooldowns.bypass true
```

## Commands

| Command | Description |
|---|---|
| `/itemcooldowns help` (alias: `/icd`) | Help |
| `/itemcooldowns reload` | Reload config and locale files |

## Configuration

```yaml
locale: en            # message language: en, ru or your own

pvp-only: true        # attack cooldowns only against players

worlds:
  enabled: false      # enable world whitelist
  list:
    - world
    - world_pvp

messages:
  enabled: true       # chat messages
  notify-start: false # message when a cooldown starts

mace:
  enabled: true
  cooldown-seconds: 45   # cooldown length
  cancel-action: true    # cancel the action during cooldown
  overlay: true          # white cooldown overlay on the slot
  bypass-permission: itemcooldowns.bypass.mace
```

Identical sections exist for `spear`, `trident`, `end-crystal`, `respawn-anchor`.

## Localization

The `plugins/ItemCooldowns/lang/` folder contains `messages-en.yml` and `messages-ru.yml` (both are extracted on first start). To add your own language, copy a file as `messages-<code>.yml`, edit it and set `locale: <code>` in the config. Missing strings fall back to the bundled English template, then to Russian.

Placeholders in messages: `{item}` — item name, `{seconds}` — remaining seconds.

## Notes

- Cooldowns reset when a player leaves the server
- Crystals/anchors start the cooldown on right-click use (attempt)

## Building from source

```
mvn -DskipTests package
```

The jar appears in `target/`.

## License

MIT. Author: **Sooqwess**.
