# TierTagger+ (Fabric 1.21.11)

Shows players' PvP tiers next to their nametag and lets you look anyone up across 8 tierlists:
MCTiers, SubTiers, PvPTiers, StatTiers, TierTests, Mace SubModes (msm.macepvp.site), PvPHQ and MCPVP.

- **K**: lookup menu (your own profile, search, waving skin, all tierlists side by side)
- `/tiers`, `/tiers lookup <name>`, `/tiers enable|disable|toggle`, `/tiers config`, `/tiers clear`

## Build
    ./gradlew build          # jar in build/libs/
    ./gradlew runClient      # dev client

Requires Java 21. Mappings: Yarn 1.21.11+build.4, Fabric API 0.141.3+1.21.11.

## Icons
`tools/gen_icons.py` regenerates every icon, the icon font (`assets/tiertaggerplus/font/icons.json`)
and `render/IconMap.java`. It needs the uku3lig/TierTagger and PvPTiers/Tiers texture folders as inputs:

    python3 tools/gen_icons.py <TierTagger>/src/main/resources/assets/tier-tagger/textures \
        <Tiers>/src/main/resources/resourcepacks/tiers-resources/assets/minecraft .

## Where the data comes from
| List | Source |
|---|---|
| MCTiers | `mctiers.net/api/v2/profile/by-name/{name}` |
| SubTiers | `subtiers.net/api/v2/profile/by-name/{name}` |
| PvPTiers | `pvptiers.com/api/search_profile/{name}` |
| StatTiers | full leaderboard from the site's worker API (cached 10 min) |
| TierTests | data embedded in `tiertests.com/players/{name}` (no public API) |
| Mace SubModes | `msm.macepvp.site/api/v1/players/{name}` |
| PvPHQ | `pvphq.com/api/v1/players/by-name/{name}` |
| MCPVP | parsed from `mcpvp.com/@{name}` (no public API) |

## Credits / license
MCTiers & SubTiers icons: uku3lig/TierTagger (MPL-2.0). PvPTiers icons: PvPTiers/Tiers (GPL-3.0).
Other icons are original. This project is GPL-3.0-or-later.
