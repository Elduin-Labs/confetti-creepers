# Runs as the hit mob, positioned at its eyes.
summon minecraft:item ~ ~1 ~ {Item:{id:"minecraft:porkchop",count:1}}
tag @s add pork_sword.popped
schedule function pork_sword:clear_tags 1t replace
