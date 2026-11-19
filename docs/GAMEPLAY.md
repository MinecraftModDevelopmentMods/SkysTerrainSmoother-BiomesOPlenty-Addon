# Pieces and smoothing

Current BOP versions use vanilla grass and dirt instead of its old soil families. Those
surfaces use the normal Terrain Smoother rules at all three levels, in BOP
biomes too. Existing terrain is left alone.

The **Materials** page in Terrain Smoother's config also controls this add-on.
**Dirt and grass** includes its coarse dirt, and **Stone and rocks** controls
its rock mappings. You can turn stone smoothing off and keep grass and soil
transitions. Mineralogy rocks also need Mineralogy support enabled. All groups
are enabled by default. Save on the main config screen and restart Minecraft.

Saved BOP slabs, steps and corners are converted to the replacements listed
in the README. They keep their orientation, and then follow the owning content
mod's placement, joining, snow, grass, shovel and drop behaviour. Decorative
moss and overgrown rock become rock pieces and do not spread grass.

The original recipe identities now cut and combine those replacement pieces.
Two matching corners in a vertical pair make a step; two matching steps make a
slab; two slabs side by side make their full block. Unlike materials do not join.
