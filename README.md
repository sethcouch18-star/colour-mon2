# Colormons (Cobblemon 1.8.1, NeoForge 1.21.1)

## Install
Put colormons-1.0.0.jar in your mods folder beside Cobblemon 1.8.1 (NeoForge) and Kotlin for Forge.
Look at one of your own Pokemon and press C.

## Groups
0 Primary, 1 Secondary, 2 Tertiary  -  the three biggest colours in the texture.
3 Eyes, 4 Outline  -  found automatically, or set by a mask.
5 Custom  -  reserved, not filled automatically.

Mask: assets/colormons/masks/<species>.png, same size as the texture.
Red = Primary, green = Secondary, blue = Tertiary, yellow = Eyes, black = untouched.

## Finishes
Default, Glossy, Matte, Shiny (Shiny swaps in the _shiny texture).

## Config
config/colormons.json holds colours per Pokemon, the four quick-save palettes and
allowOthers (false by default, so only your own Pokemon open the menu).

## Not in this build
The preview panel is an empty box, pixel painting is not wired up, and colours are
local to your game (no multiplayer).
