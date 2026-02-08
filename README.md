# Mob Money

A simple, configurable Fabric mod that awards players money for killing mobs. Built to demonstrate integration with the [Common Economy API](https://github.com/Patbox/common-economy-api).

## Features

*   **Earn Money**: Get paid for hunting down hostile mobs.
*   **Configurable**:
    *   Set custom rewards per mob type (e.g., Zombies, Creepers, Ender Dragon).
    *   **Economy Provider Support**: Configure which economy mod to use (e.g., Savs Common Economy, Fuji, etc.).
*   **Common Economy API**: Works with any economy mod that implements the Common Economy API.

## Installation

1.  Install **Fabric Loader** and **Fabric API**.
2.  Install an economy mod that supports the Common Economy API (e.g., [Savs Common Economy](https://modrinth.com/mod/savs-common-economy)).
3.  Drop `mob-money-1.0.0.jar` into your `mods` folder.

## Configuration

The config file is located at `config/mob-money.json`. It will be generated upon first launch.

```json
{
  "economyProvider": "savs_common_economy",
  "currencyId": "dollar",
  "notificationMode": "CHAT",
  "mobPrices": {
    "minecraft:zombie": 5.0,
    "minecraft:skeleton": 5.0,
    "minecraft:creeper": 10.0,
    "minecraft:spider": 5.0,
    "minecraft:ender_dragon": 1000.0,
    "minecraft:wither": 500.0
  },
  "maxEarningsPerPeriod": 100.0,
  "earningPeriodDuration": 1200,
  "overflowMode": "DROP"
}
```

### Settings

*   `economyProvider`: The ID of the economy provider to use (e.g., `savs_common_economy`, `fuji`).
*   `currencyId`: The ID of the currency to award (e.g., `dollar`, `gold`).
*   `notificationMode`: How to notify players of earnings. Options: `CHAT`, `ACTION_BAR`, `NONE` (default: `CHAT`).
*   `mobPrices`: A **whitelist** of mobs and their reward values.
    *   **Only mobs listed here will award money.**
    *   Use the full entity ID (e.g., `minecraft:zombie`).
*   `maxEarningsPerPeriod`: The maximum amount a player can earn within the defined period. **Set to 0 to disable.** (Default: 100.0)
*   `earningPeriodDuration`: The length of the earning period in seconds. (Default: 1200 - 20 minutes)
*   `overflowMode`: Defines behavior when a kill exceeds the cap. Options:
    *   `DROP` (Default): If a kill pushes you over the cap, you get $0 for that kill.
    *   `PARTIAL`: You get the remaining amount up to the cap.
    *   `ALLOW`: If you are under the cap, you get the full reward (even if it exceeds the limit). Future kills give $0.

## Troubleshooting

If you are not receiving money:
1.  Check the server console/logs for messages starting with `[mob-money]`.
2.  Ensure you have an economy mod installed.
3.  Verify that `economyProvider` and `currencyId` in the config match the provider you are using.
    *   For **Savs Common Economy**: Provider `savs_common_economy`, Currency `dollar`.
    *   For **Fuji**: Provider `fuji`, Currency `gold` (or whatever is configured).
