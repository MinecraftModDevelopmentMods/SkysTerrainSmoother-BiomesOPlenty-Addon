# Changelog

## 0.1.0.112021

- Added Minecraft 1.12.2 support for BOP grass, soils and natural rock pieces.
- Requires the Building Pieces BOP add-on for the supported rock transitions.
- Shovels flatten BOP grass and ordinary soil pieces into matching paths.
  Grass steps and corners also flatten compatible grass directly beneath them.
- Added recipes to rebuild two matching corners into one step and two steps
  into one slab, with ingredient-based recipe-book unlocks.
- Kept recipe previews on normal item models while accepting saved orientations.
- Kept native grass beneath steps and corners, including snowy transitions.
- Preserved piece IDs and palettes when upgrading saved worlds. Smoothing still
  applies only to new terrain, at the selected level.

## 0.1.0.110021

- Adds slabs, steps and corners for Biomes O Plenty grass and soil.
- Smooths new terrain using the selected Terrain Smoother level.
- Keeps native soil types, textures, grass colours and spreading rules.
- Reuses natural rock pieces when the Building Pieces BOP add-on is installed.
