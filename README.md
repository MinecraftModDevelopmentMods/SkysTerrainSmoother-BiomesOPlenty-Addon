# Sky's Terrain Smoother - Biomes O Plenty

For Minecraft 1.10.2, Forge 12.18.3.2511 and Java 8.
Requires Biomes O Plenty 5.0, Sky's Grass Slabs 1.1 and Sky's Terrain Smoother 0.1
(which also requires Sky's Building Pieces 0.3).

Adds matching slabs, steps and corners for BOP grass and soil surfaces. New
Overworld chunks use them at the smoothing level selected in Terrain Smoother.
Existing terrain is never retrofitted. BOP grass keeps its own texture, tint,
soil relationship and spreading behaviour. Native full blocks are reused.

The Building Pieces BOP add-on is optional; when present, its existing natural
rock pieces are reused. No BOP textures are bundled. Back up worlds before
adding or removing mods that provide blocks.

See [Gameplay](docs/GAMEPLAY.md) and [Building](docs/BUILDING.md).

[Release preparation](docs/RELEASING.md) covers the CI checks and publication safeguards.

Licensed under LGPL-2.1-only. See LICENSE and NOTICE.
