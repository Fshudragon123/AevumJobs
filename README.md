# AevumAgriculture

**AevumMC • The Imperial Agrarian Ledger**  
A custom agriculture, food production and player-driven trade plugin for Paper 26.2 / Java 25.

AevumAgriculture turns farming into a complete economic profession: players purchase seeds, establish real crops in the world, harvest produce, process ingredients, cook edible food, register businesses and trade with other players or the Imperial Commodity Exchange.

## Release 1.0.0 at a glance

- **119 crop varieties** across grains, legumes, vegetables, roots, orchard fruits, berries, vines, nuts, herbs, spices, fibres, oilseeds and sugar crops.
- **79 custom food and ingredient items**, including genuinely edible food with configured hunger and saturation.
- **78 processing and cooking recipes** across eight production stations.
- **1,065 custom item model definitions and textures** in the Java 26.2 resource pack, including six visual growth stages per crop, custom seed and harvested-produce artwork, food, equipment and GUI icons.
- **A custom onyx-and-antique-gold 54-slot menu background** and the Imperial Agrarian Ledger GUI.
- **A complete seed shop**: all enabled crop seeds are available from the in-game Seed Exchange, with configurable prices.
- SQLite persistence for crops, farmer progression, businesses, stock and trade records.

## Fertiliser rule

**Bone meal is the only fertiliser or growth booster.** There is no compost, manure, fertiliser item, soil additive, chemical booster or alternate growth-boosting mechanic. Bone meal can be applied to a planted crop up to the configured application limit. Soil hydration affects crop growth efficiency but is not a fertiliser system.

## Requirements and integrations

- Paper 26.2 (API build `26.2.build.121-stable`)
- Java 25
- Vault + an economy provider such as EssentialsX Economy for the seed shop, commodity market, station purchases and business payments
- PlaceholderAPI optional
- Towny optional; used to attribute player businesses to the owner's current town
- AevumJobs and AevumReligion are optional soft integrations. Resource-pack delivery uses a distinct UUID and `Player.addResourcePack`, so this pack stacks instead of replacing AevumReligion's pack.

## Installation

1. Download `AevumAgriculture-1.0.0.jar` from the GitHub release.
2. Put the JAR directly inside the server's main `plugins/` directory, alongside other plugin JARs.
3. Start the server. The plugin creates `plugins/AevumAgriculture/` and extracts its bundled `aevum-agriculture.zip`.
4. Ensure Vault and an economy provider are installed if players should buy seeds, equipment and food, or use the commodity exchange.
5. Players must accept the AevumAgriculture resource-pack prompt. The pack is sent about five seconds after joining, after AevumReligion's normal join prompt, and is stacked separately.
6. Run `/agri` or `/farm` to open the Imperial Agrarian Ledger.

Do not place the JAR inside `plugins/AevumAgriculture/`. That directory is the plugin data folder, not the directory Paper scans for plugin JARs.

## Player gameplay

### Seeds and planting

- `/agri` opens the main Imperial Agrarian Ledger.
- Choose **The Imperial Seed Exchange** or use `/agri shop` / `/agri seeds`.
- Every enabled crop is listed. Left-click buys 1 seed, right-click buys 64, and shift-click buys 16. Prices are configured per crop in `crops.yml`.
- Plant a purchased seed by right-clicking an empty farmland block with the seed in the main hand.
- Crops use six distinct resource-pack-backed growth visuals and are persisted by world and block coordinates.
- Right-click the farmland beneath a mature crop with an empty/non-seed main hand to harvest it.
- Hold bone meal in the main hand and right-click an established crop to accelerate its growth. Bone meal is consumed and the configured per-crop limit is enforced.
- Certain orchard and vine crops listed in `growth.natural-regrowth-crops` regrow after harvesting. Other crops must be replanted.

Growth is timestamp-based. A crop continues to mature while its chunk is unloaded and across server restarts; on chunk load the visual is reconstructed at the correct stage. The default growth times are deliberately fast for multiplayer play and can be adjusted per crop.

### Processing and cooking

Purchase the specialist station kits from **Production Stations** in the Ledger. Place the station kit in the world and right-click the placed block to open its recipe menu. The station uses its configured base block as the placed object; the custom item model and menu artwork are supplied by the resource pack.

The eight stations are:

| Station | Main purpose |
|---|---|
| Stone Mill | Flour, meal and ground ingredients |
| Olive Press | Oils and juices |
| Baker's Oven | Breads, pastries, cakes and baked food |
| Cooking Hearth | Soups, stews, drinks and cooked meals |
| Drying Rack | Dried fruit, herbs and tea leaves |
| Fermentation Stand | Vinegars, pickles and preserved products |
| Preparation Table | Salads and assembled ingredients |
| Mortar & Pestle | Spice blends, cacao and coffee processing |

Recipe menus show their required ingredients and output. Crafting consumes the custom ingredients from the player's inventory and awards Agriculture XP. Custom edible foods use Paper's item food component, so they restore the configured hunger and saturation rather than being decorative items only.

### Commodity Exchange

The Imperial Commodity Exchange buys registered harvests, ingredients and finished foods through Vault. Reference prices come from `crops.yml` and `foods.yml`. NPC demand is limited per item over a rolling 24-hour window; recent sales reduce the current price to discourage infinite fixed-price selling. Players can view prices and their current inventory stock in `/agri market`.

### Player-run businesses

Each player can register one agricultural business:

- `farmstead`
- `bakery`
- `mill`
- `orchard`
- `kitchen`
- `grocer`

Examples:

```text
/agri business register bakery The Golden Loaf
/agri business stock wheat_loaf 32
/agri business price wheat_loaf 8.50
/agri business open
/agri business close
/agri business visit PlayerName
/agri directory
```

Business stock consists of custom harvested produce, ingredients and finished goods. Owners choose their prices and can open or close their storefront. Other players can browse the Business Directory, visit an open storefront and purchase its listed stock. When Towny is enabled, the owner's current town is recorded on the business profile.

## Commands

| Command | Function |
|---|---|
| `/agri` | Open the Ledger |
| `/agri shop` / `/agri seeds` | Open the complete seed shop |
| `/agri crops` | Browse the crop codex |
| `/agri recipes` / `/agri food` | Browse recipes |
| `/agri stations` | Buy station kits |
| `/agri market` | Open the Commodity Exchange |
| `/agri business` | Manage your business |
| `/agri directory` | Discover open player-run businesses |
| `/agri stats` | View farming progression and statistics |
| `/agri top` | View the farmer leaderboard |
| `/agri business register <type> <name>` | Register a business |
| `/agri business stock <item-id> [amount]` | Deposit custom goods into stock |
| `/agri business price <item-id> <price>` | Set a storefront price |
| `/agri business open` / `close` | Open or close your shop |
| `/agri business visit <player>` | Visit a player's storefront |

Admin commands:

```text
/agri admin reload
/agri admin give <player> <item-id> [amount]
/agri admin station give <player> <station-id>
/agri admin xp <player> <amount>
/agri admin stats
```

## Data files and configuration

```text
plugins/AevumAgriculture/
├── config.yml
├── crops.yml
├── foods.yml
├── recipes.yml
├── agriculture.db
└── aevum-agriculture.zip
```

- `crops.yml`: all crop definitions, regions, growth duration, seed prices, yields and seed return chance.
- `foods.yml`: all processed ingredients and custom edible foods, their item models, market prices, nutrition and saturation.
- `recipes.yml`: recipes, ingredients, output, production station and XP reward.
- `config.yml`: growth, farm limits, bone-meal-only boosting, station-kit prices, market demand, business registration and resource-pack delivery.
- `agriculture.db`: player crops, UUID-linked profiles, player businesses, business inventory and market transactions.

The connected GitHub repository is private, so its release assets are not a public download URL for Minecraft clients. The plugin embeds and extracts its pack, and the built-in HTTP host is enabled on port `25822` by default. Allocate a public TCP port in MintServers, then set `resource-pack.host.public-url` to `http://YOUR-PUBLIC-IP:YOUR-ALLOCATED-PORT`. The plugin serves `/aevum-agriculture.zip`. Do not use the private GitHub release URL for client delivery.

The pack overrides Minecraft's shared `generic_54` chest background. This gives the Ledger its onyx/bronze frame, but also skins other 54-slot chest-style plugin menus while the pack is enabled. Other inventory sizes are unaffected.

## Permissions

| Permission | Default |
|---|---|
| `aevumagriculture.use` | true |
| `aevumagriculture.plant` | true |
| `aevumagriculture.harvest` | true |
| `aevumagriculture.process` | true |
| `aevumagriculture.cook` | true |
| `aevumagriculture.business` | true |
| `aevumagriculture.market` | true |
| `aevumagriculture.admin` | op |
| `aevumagriculture.admin.reload` | op |
| `aevumagriculture.admin.give` | op |
| `aevumagriculture.admin.station` | op |
| `aevumagriculture.admin.stats` | op |
| `aevumagriculture.admin.xp` | op |

## PlaceholderAPI

Expansion identifier: `aevumagriculture`.

- `%aevumagriculture_level%`
- `%aevumagriculture_xp%`
- `%aevumagriculture_harvests%`
- `%aevumagriculture_crafts%`
- `%aevumagriculture_sales%`
- `%aevumagriculture_business%`

## Build and verification

```bash
python -m pip install pillow PyYAML
python tools/verify-source.py
python tools/build-resource-pack.py
mvn --batch-mode clean package
python tools/verify-plugin-jar.py target/AevumAgriculture-1.0.0.jar
```

Java 25 is required. The GitHub Actions workflow compiles the shaded plugin, verifies the embedded resource pack and publishes the JAR, standalone pack and source archive.

## Compatibility and future expansion

The initial release provides the full crop / seed shop / harvest / processing / edible food / business / market loop. The YAML catalogue is designed for extension without changing Java code. Further expansions can add climate and historical-season effects, crop breeding, food quality grades, business employees, national granaries, inter-nation supply contracts and larger agricultural events.

Existing data is stored in `agriculture.db`; do not delete the plugin data folder when updating.
