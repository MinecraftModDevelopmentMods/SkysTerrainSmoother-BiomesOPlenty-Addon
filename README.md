# Sky's Terrain Smoother - Biomes O' Plenty

This add-on keeps Terrain Smoother working with supported Biomes O' Plenty
worlds and building pieces. Choose slabs, steps or corners in Terrain Smoother's
configuration. Only new, dry terrain is smoothed, including suitable cave floors.
Existing chunks are left alone.

## Minecraft 26.3

Use Forge 66.0.2 and Java 25 with these matching mods on both client and server:

- Biomes O' Plenty 26.3.0.0.16.
- TerraBlender 26.3.0.0.6 and GlitchCore 26.3.0.0.1, required by BOP.
- Sky's Grass Slabs 1.2.0.2603001.
- Sky's Building Pieces 0.3.9.2603001.
- Sky's Building Pieces BOP add-on 0.2.5.2603001.
- Sky's Terrain Smoother 0.1.0.2603001.

BOP now uses vanilla grass and dirt for its ordinary ground. Terrain Smoother
already handles those states in BOP biomes. This add-on also connects the
remaining natural rock replacements supplied by Building Pieces BOP. Mineralogy
is optional; its existing rock mapping takes priority when it provides the rock.

## Upgrading an older BOP world

Back up the world and keep both BOP add-ons installed during the upgrade.
BOP removed its old grass, dirt and path families after Minecraft 1.12. Saved
slabs, steps and corners become the closest available pieces while keeping
their shape, orientation, stack counts and custom item data:

- Loamy, sandy, silty, origin and daisy grass become ordinary grass pieces.
- The ordinary dirt variants become dirt pieces. Coarse variants stay coarse dirt.
- Paths become the matching vanilla-soil path pieces.
- Spectral moss and overgrown stone become mossy cobblestone pieces.
- Overgrown netherrack becomes netherrack pieces.

The old registry IDs remain available as recovery entries for unopened chunks and
mixed saves. Coarse dirt keeps its own slab under an existing palette ID; the
other old entries are hidden. The original cutting and combining recipes now
make the replacement pieces. The content mods keep their normal recipes and
placement behaviour.

This does not restore every block or feature removed from BOP. Keep BOP worlds
on a supported Minecraft and loader route, and check a copy before upgrading.
There is no retrogen.

See [gameplay](docs/GAMEPLAY.md), [building from source](docs/BUILDING.md) and the
[changelog](CHANGELOG.md).

## Licence

[LGPL-2.1-only](LICENSE). See [NOTICE](NOTICE). BOP textures are not bundled.
