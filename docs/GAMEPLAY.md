# BOP terrain pieces

All eight BOP grass variants and ordinary/coarse loamy, sandy and silty dirt
have horizontal slabs, steps and corners. Snow caps are visual and grass uses
the native BOP biome colours. Only a complete upper face supports plants.

Three full blocks in a row make six slabs. Two matching slabs in a horizontal
row make their original full block. Two slabs placed diagonally in the grid
make four steps; two steps side by side make four corners. Matching pieces can
also combine in the world. Unlike materials do not combine.

To rebuild a larger piece, put two matching corners in a vertical pair for one
step, or two matching steps in a vertical pair for one slab. Both recipes fit
the player's crafting grid. They keep the exact grass or soil type and work
with saved pieces in any orientation. The existing side-by-side cutting
recipes are unchanged.

The smoother affects only new dry Overworld surfaces and cave floors. The three levels use
slabs, then steps, then corners. Soil identities and metadata are preserved.
Special moss/overgrown grass does not acquire ordinary grass spreading.

## Shovel paths

Use a compatible shovel on an uncovered grass or ordinary soil slab, step or
corner to make a path. The footprint and orientation stay the same, with the
top lowered by one pixel. The whole action uses one durability point; creative
mode does not wear the shovel.

Loamy, sandy and silty pieces use matching BOP path variants. Origin and daisy
grass use the existing vanilla-soil path pieces. Decorative moss, overgrown
stone and netherrack, and coarse dirt do not turn into paths.

Flattening a grass step or corner also flattens compatible full grass directly
beneath it. The bottom of a ground-level piece meets the lowered support without
a gap. Other supporting materials are left alone.

Matching path corners, steps and slabs can join into larger path pieces or the
native full path block. Paths drop their matching dirt pieces, including with
Silk Touch, and solid cover turns a path back into matching dirt. Paths do not
spread grass, support plants or display the decorative snow cap.
