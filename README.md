<p align="center">
  <img src="docs/sorters-satchel-emblem.png" alt="Sorter's Satchel emblem" width="220">
</p>

<h1 align="center">Sorter's Satchel</h1>

<p align="center">
  Deposit matching inventory items into nearby chests with one crafted satchel.
</p>

<p align="center">
  <a href="https://github.com/JXYZP-G/-SortersSatchel/actions/workflows/build.yml"><img src="https://github.com/JXYZP-G/-SortersSatchel/actions/workflows/build.yml/badge.svg" alt="Build status"></a>
  <img src="https://img.shields.io/badge/Paper-26.2-blue" alt="Paper 26.2">
  <img src="https://img.shields.io/badge/Java-25-orange" alt="Java 25">
  <img src="https://img.shields.io/badge/license-MIT-green" alt="MIT license">
</p>

## What it does

Sorter's Satchel is a lightweight Paper plugin that replaces unreliable mob-based sorting with a player-controlled deposit tool.

Craft the satchel, right-click it to open its inventory menu, and press **Deposit nearby items**. Items from your main inventory are transferred into nearby loaded chests that already contain an exactly matching item.

- Matches complete item data, not only the material.
- Fills existing partial stacks before using empty slots.
- Prefers chests with the most matching occupied slots.
- Continues into another matching chest when the first becomes full.
- Leaves unmatched items and overflow in your inventory.
- Preserves armor, offhand items, the satchel, and—by default—the complete hotbar.
- Searches loaded chunks only; using the satchel never loads distant chunks.
- Requires no client mod or resource pack.

## Requirements

- Paper 26.2
- Java 25

## Installation

1. Download the latest jar from [Releases](https://github.com/JXYZP-G/-SortersSatchel/releases) or a successful [Actions build](https://github.com/JXYZP-G/-SortersSatchel/actions).
2. Place the jar in the server's `plugins` directory.
3. Restart the server.
4. Craft a Sorter's Satchel.

The old SmarterCopperGolems plugin is not required.

## Crafting recipe

| Leather | Ender Pearl | Leather |
|:---:|:---:|:---:|
| Copper Ingot | Bundle | Copper Ingot |
| Leather | Hopper | Leather |

The crafted bundle has a custom name, lore, glow, and persistent identity. Renaming an ordinary bundle does not turn it into a satchel.

## Usage

1. Keep the Sorter's Satchel anywhere in your inventory.
2. Right-click it.
3. Click the green **Deposit nearby items** button.
4. Items with an existing destination stack are deposited immediately.

A destination chest must already contain at least one exactly matching item. Named items, enchantments, potion data, durability, and other components are respected.

## Configuration

The configuration is created at `plugins/SortersSatchel/config.yml`:

```yaml
# Maximum straight-line distance from the player to a destination chest.
search-radius: 16.0

# Preserve the complete hotbar by default.
include-hotbar: false

messages:
  deposited: "<green>Deposited <amount> items into <chests> nearby chests."
  nothing: "<yellow>No inventory items had matching stacks in nearby chests."
  received: "<green>You received a Sorter's Satchel."
```

The search radius is limited to 1–64 blocks. Set `include-hotbar: true` if players should also deposit hotbar items.

## Commands and permissions

| Command | Purpose | Permission |
|---|---|---|
| `/sortersatchel open` | Opens the menu when the player owns a satchel | `sortersatchel.use` |
| `/sortersatchel give` | Gives the executing operator a satchel | `sortersatchel.admin` |

`sortersatchel.use` is granted by default. `sortersatchel.admin` defaults to server operators.

## Building

```bash
gradle clean build
```

The compiled jar is written to `build/libs/`.

## License

Sorter's Satchel is available under the [MIT License](LICENSE).
