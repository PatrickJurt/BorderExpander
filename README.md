# BorderExpander

Paper plugin that starts the world border at size `1` and expands it when players discover new items.

## Commands
- `/borderexpander items` or `/be items`: opens a double-chest GUI with items the player has not discovered yet (alphabetically sorted, bottom row reserved for page buttons).
- `/borderexpander stats` or `/be stats`: shows how many unique items each player has discovered.

## Behavior
- Every globally new item expands the border by `+1` until `1000` global items are found.
- After `1000` items, each additional global item expands by `+10`.
- After `1000` items, an extra border bonus of `+10` blocks per average XP level of two lategame players is applied.
- Each player-unique item discovery broadcasts `<player> obtained <item>` and plays the level-up sound for all online players.
- A one-time lategame announcement is broadcast when `1000` global items are reached.
