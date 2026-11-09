# Runs as the player who hit a mob with the Pork Sword.
# Revoke first so the next hit can trigger it again.
advancement revoke @s only pork_sword:hit_with_pork_sword

# The mob that was just hit has HurtTime 10 for this one tick.
# The tag stops a sweep attack from dropping two porkchops on the same mob.
execute as @e[type=!player,distance=..8,nbt={HurtTime:10s},tag=!pork_sword.popped,sort=nearest,limit=1] at @s anchored eyes positioned ^ ^ ^ run function pork_sword:drop
