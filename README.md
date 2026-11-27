# Sky's Terrain Smoother - Biomes O' Plenty

This add-on keeps Terrain Smoother working with supported Biomes O' Plenty
worlds and building pieces. Choose slabs, steps or corners in Terrain Smoother's
configuration. Only new, dry terrain is smoothed, including suitable cave floors.
Existing chunks are left alone.

Terrain Smoother's **Materials** config page also controls this add-on. The soil switch
covers origin grass. Sand and Gravel controls the coloured and mossy sands, and Stone covers
the natural sandstones and rock mappings. Mineralogy rocks need
both stone smoothing and Mineralogy support enabled. All groups start enabled. Save on
the main config page and restart Minecraft; existing pieces and recipes are unaffected.

## Minecraft 1.21.1

Use Forge 52.1.0 and Java 21 with these matching mods on both client and server:

- Biomes O' Plenty 21.1.0.14.
- TerraBlender 4.1.0.8 and GlitchCore 2.1.0.2, required by BOP.
- Sky's Grass Slabs 1.2.1.121011.
- Sky's Building Pieces 0.3.13.121011.
- Sky's Building Pieces BOP add-on 0.2.8.121011.
- Sky's Terrain Smoother 0.1.6.121011.

BOP now uses vanilla grass and dirt for its ordinary ground. Terrain Smoother
already handles those states in BOP biomes. The add-on smooths origin grass with
its own slabs, steps and corners. White sand, orange sand, black sand and mossy
black sand use falling slabs.
Their natural sandstones use BOP's existing slabs and the matching steps and corners
from Building Pieces BOP. The add-on also connects the older natural rock replacements. Mineralogy
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
mixed saves. Terrain Smoother now supplies coarse dirt slabs and their recipes
under the same IDs, so previously placed slabs and saved items still load.
Replacement pieces use the recipes from Minecraft and the content mods.

This does not restore every block or feature removed from BOP. Keep BOP worlds
on a supported Minecraft and loader route, and check a copy before upgrading.
There is no retrogen.

See [gameplay](docs/GAMEPLAY.md), [building from source](docs/BUILDING.md) and the
[changelog](CHANGELOG.md).

## Licence

[LGPL-2.1-only](LICENSE). See [NOTICE](NOTICE). BOP textures are not bundled.
