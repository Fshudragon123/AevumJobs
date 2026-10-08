# 🏛 AevumJobs

AevumJobs is the flagship AevumMC profession and progression system for Paper 26.2 / Java 25.

## Included
- Exactly 12 professions.
- Maximum 3 active professions.
- Progression preserved when leaving by default.
- Shared configurable XP curve with profession-specific action XP.
- Level 1–49 cash-only rewards.
- Level 50: $30,000 plus a unique configurable Master reward.
- SQLite persistence with async database executor and WAL mode.
- Cached player data and async leaderboard queries.
- Vault economy integration with graceful missing-economy handling.
- Optional PlaceholderAPI internal expansion.
- Player-only Warrior XP with repeated-victim cooldown protection.
- Hunter filters for NPC/fake/plugin-marked entities and configured invalid entity types.
- Placement tracking to reduce block place/break XP loops.
- Premium six-row dark/bronze/gold GUI flow.
- Player and admin commands with permission nodes.
- GitHub Actions build pipeline that produces an installable jar artifact.

## Requirements
Runtime: Paper 26.2, Java 25. Vault and an economy provider for cash rewards. PlaceholderAPI and Towny are optional.

Paper's current developer documentation states Java 25 is required for Paper 26.1+ and documents the newer 26.3 dependency version format. This project is intentionally pinned to 26.2.build.121 in pom.xml to match the AevumMC environment described for this plugin.

## Installation
Run mvn clean package with Java 25. Copy target/AevumJobs-1.0.0.jar into plugins/ and restart Paper.

The repository contains a GitHub Actions workflow under .github/workflows/build.yml. Every push to main builds the shaded jar and uploads it as the AevumJobs workflow artifact.

## Player commands
- /jobs
- /jobs menu
- /jobs stats
- /jobs leaderboard
- /jobs info <job>
- /jobs level <job>
- /jobs rewards <job>
- /jobs help

## Admin commands
- /jobs admin setlevel <player> <job> <level>
- /jobs admin addxp <player> <job> <amount>
- /jobs admin removexp <player> <job> <amount>
- /jobs admin reset <player> <job>
- /jobs admin reload

## Permissions
- aevumjobs.use
- aevumjobs.admin
- aevumjobs.admin.setlevel
- aevumjobs.admin.addxp
- aevumjobs.admin.removexp
- aevumjobs.admin.reset
- aevumjobs.admin.reload

## PlaceholderAPI
Internal expansion identifier: aevumjobs.
- %aevumjobs_<job>_level%
- %aevumjobs_<job>_xp%
- %aevumjobs_<job>_next_xp%
- %aevumjobs_<job>_progress%
- %aevumjobs_<job>_total_earned%
- %aevumjobs_<job>_actions%
- %aevumjobs_active_count%
- %aevumjobs_highest_level%
- %aevumjobs_total_xp%

PlaceholderAPI 2.12.3 includes 26.2 support.

## Profession actions
Miner: configured mining blocks.
Farmer: mature configured crops and configured planting blocks.
Lumberjack: configured logs and saplings.
Fisherman: successful fish catches.
Rancher: successful player-attributed breeding, with a configurable breeder cooldown.
Warrior: genuine player kills only, with repeated-victim anti-boosting.
Hunter: non-player entity kills that pass the configured NPC/fake-entity filters. By default every configured Hunter kill awards the same XP.
Builder: block placement with placement-loop tracking.
Alchemist: successful brewing events attributed to the last player to open the brewing stand.
Enchanter: successful enchanting.
Blacksmith: equipment crafting, anvil repair and smithing result collection.
Scholar: signed books and cartography result collection.

## Database
Player progression is stored in SQLite, not YAML. The schema includes per-profession level, current XP, lifetime XP, total earned, action count, active state, Level 50 reward state, and a reward journal table reserved for payout transaction auditing.

## Reward safety
Master item rewards require enough empty storage slots and have a persistent claim flag. The plugin never intentionally drops a Level 50 item because the inventory is full.

Vault does not define a universal idempotent transaction identifier, so external economy providers cannot offer a mathematically perfect cross-process two-phase commit. AevumJobs treats a successful Vault transaction as authoritative and records earned money in the job data afterward.

## Reload
/jobs admin reload reloads the main configuration without registering duplicate listeners or opening new database connections.

## Testing
Before production deployment, test the three-profession limit, leave/rejoin preservation, every action source, repeated-victim PvP, NPC filtering, Level 50 with full inventory, rapid GUI clicks, restart persistence, and missing Vault/PlaceholderAPI/Towny installations.
