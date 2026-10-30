# Sky's Terrain Smoother - Biomes O' Plenty

Keep the smoother slopes going when you reach a Biomes O' Plenty biome. This
add-on gives BOP grass and soil matching slabs, steps and corners for Sky's
Terrain Smoother to use in new terrain.

## BOP slopes and building pieces

The add-on covers BOP's eight grass variants and ordinary and coarse loamy,
sandy and silty dirt. Grass keeps its native textures, biome colours and
spreading rules. Moss and overgrown grass don't start spreading like ordinary
grass.

Choose the level in Terrain Smoother's configuration: slabs alone, slabs and
steps, or all three shapes. The same setting applies outdoors and on dry cave
floors. Existing chunks aren't changed; explore new terrain to see the results.

You can build with the pieces yourself too. Three matching full blocks in a
row make six slabs; two slabs side by side make a full block again. Two slabs
placed diagonally make four steps, and two steps side by side make four corners.
Stack two matching corners vertically in the grid to recover one step, or two
matching steps to recover one slab.
Matching pieces can also join in the world. Use a shovel on grass or ordinary
soil pieces to make paths with the same shape. Loamy, sandy and silty paths
keep their native BOP dirt sides. Flattening a grass step or corner also
flattens compatible grass directly underneath in the same shovel action.
The [gameplay guide](docs/GAMEPLAY.md) covers plant support and snow.

## Installation

For **Minecraft 1.12.2**, use **Forge 14.23.5.2859** and **Java 8**. Install
this add-on together with:

- **Biomes O' Plenty 7.0.1.2445**.
- **Sky's Terrain Smoother 0.1.0.112021**.
- **Sky's Grass Slabs 1.1.2.112021**.
- **Sky's Building Pieces 0.3.8.112021**.
- **[Sky's Building Pieces BOP add-on](https://github.com/MinecraftModDevelopmentMods/SkysBuildingPieces-BiomesOPlenty-Addon) 0.2.4.112021**.

All six main jars are needed on both the client and server.

The Building Pieces BOP add-on supplies the natural rock pieces used to smooth
supported BOP stone terrain, alongside this add-on's grass and soil pieces.
Install both add-ons for the complete BOP smoothing support.

## Existing worlds

Back up your world before adding or removing mods. This add-on doesn't reshape
terrain you've already explored. Its generated pieces remain ordinary blocks
after smoothing is turned off, so keep the add-on installed in worlds that
contain them.

For smoothing options and existing-world advice, see the
[Terrain Smoother guides](https://github.com/MinecraftModDevelopmentMods/SkysTerrainSmoother/tree/master-1.12.2/docs).
For development, see [building from source](docs/BUILDING.md).
Release history is in the [changelog](CHANGELOG.md).

## Licence

[LGPL-2.1-only](LICENSE). See [NOTICE](NOTICE) for credits.
BOP textures are read from the installed mod, not bundled here.
