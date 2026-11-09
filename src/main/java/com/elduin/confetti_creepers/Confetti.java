package com.elduin.confetti_creepers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Everything confetti: creeper pops, creeper phone calls, and Confetti Guy. */
public final class Confetti {

	/** Red, orange, yellow, green, blue, purple, pink, white. */
	private static final int[] COLORS = {
			0xFF3B3B, 0xFF9A1F, 0xFFF23B, 0x3BFF6A, 0x3BC8FF, 0x8A3BFF, 0xFF3BD8, 0xFFFFFF
	};

	/** How close you have to be to a creeper pop to get covered in confetti. */
	private static final double SPLASH_RANGE = 6.0;

	/** How long you stay Confetti Guy: 5 minutes. Another creeper pop starts it over. */
	private static final int CONFETTI_GUY_TICKS = 20 * 60 * 5;

	/** How many ticks the phone keeps ringing: 3 seconds. */
	private static final int RING_TICKS = 60;

	private static final Map<UUID, Integer> CONFETTI_GUYS = new HashMap<>();
	private static final Map<UUID, Vec3> LAST_SPOT = new HashMap<>();
	private static final Map<UUID, Integer> RINGING = new HashMap<>();

	private Confetti() {
	}

	/** Throws a cloud of colored confetti at a spot. */
	public static void burst(ServerLevel level, double x, double y, double z, int count, double size) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < count; i++) {
			int color = COLORS[random.nextInt(COLORS.length)];
			level.sendParticles(new DustParticleOptions(color, 1.2f),
					x + (random.nextDouble() - 0.5) * size,
					y + random.nextDouble() * size * 0.7,
					z + (random.nextDouble() - 0.5) * size,
					1, 0.05, 0.05, 0.05, 0.02);
		}
	}

	/** A creeper went off. No boom, no hole: just a confetti party. */
	public static void creeperPop(Creeper creeper, ServerLevel level) {
		double x = creeper.getX();
		double y = creeper.getY();
		double z = creeper.getZ();

		burst(level, x, y, z, 200, 5.0);
		level.sendParticles(ParticleTypes.FIREWORK, x, y + 1, z, 40, 1.0, 1.0, 1.0, 0.15);
		level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.HOSTILE, 2.0f, 1.0f);
		level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.HOSTILE, 2.0f, 1.0f);

		AABB area = creeper.getBoundingBox().inflate(SPLASH_RANGE);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
			makeConfettiGuy(player);
			Quests.add(player, Quests.POPS, 1);
		}

		creeper.discard();
	}

	/** You walked up to a creeper, so it calls you on the Minecraft World Wi-Fi. */
	public static void creeperCalls(Creeper creeper, ServerLevel level, ServerPlayer player) {
		player.sendSystemMessage(Component.literal("[Phone] Connected to Wi-Fi: Minecraft World")
				.withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.literal("[Phone] Incoming call from: Creeper")
				.withStyle(ChatFormatting.GREEN));
		player.sendSystemMessage(Component.literal("Creeper: Ssssss... WHAT THE @#$%&?!")
				.withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 2.0f);
		if (hasPhone(player)) {
			ring(player);
		}
		Quests.add(player, Quests.CALLED, 1);
	}

	private static boolean hasPhone(ServerPlayer player) {
		return player.getInventory().contains(stack -> stack.is(ModItems.PHONE));
	}

	/** Makes a player's phone ring for a few seconds. */
	public static void ring(ServerPlayer player) {
		RINGING.put(player.getUUID(), RING_TICKS);
	}

	/** Covers a player in confetti. */
	public static void makeConfettiGuy(ServerPlayer player) {
		boolean alreadyConfettiGuy = CONFETTI_GUYS.containsKey(player.getUUID());
		CONFETTI_GUYS.put(player.getUUID(), CONFETTI_GUY_TICKS);
		if (!alreadyConfettiGuy) {
			player.sendSystemMessage(Component.literal("You got covered in confetti! You are now CONFETTI GUY!")
					.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
		}
	}

	/** Every tick: Confetti Guys drop confetti wherever they walk, over and over and over. */
	public static void tick(MinecraftServer server) {
		tickRinging(server);
		Iterator<Map.Entry<UUID, Integer>> it = CONFETTI_GUYS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				continue; // logged out; the confetti waits for them
			}

			int left = entry.getValue() - 1;
			if (left <= 0) {
				it.remove();
				LAST_SPOT.remove(entry.getKey());
				player.sendSystemMessage(Component.literal("The last of the confetti fell off. Bye bye, Confetti Guy!")
						.withStyle(ChatFormatting.GRAY));
				continue;
			}
			entry.setValue(left);

			Vec3 here = player.position();
			Vec3 before = LAST_SPOT.put(entry.getKey(), here);
			boolean walking = before != null && before.distanceToSqr(here) > 0.0004;
			ServerLevel level = (ServerLevel) player.level();

			if (walking) {
				burst(level, here.x, here.y, here.z, 4, 1.0);
				Quests.walked(player, Math.sqrt(before.distanceToSqr(here)));
			} else if (left % 10 == 0) {
				burst(level, here.x, here.y + 0.5, here.z, 1, 0.8);
			}
		}
	}

	/** Every tick: ringing phones go ding-ding-ding. */
	private static void tickRinging(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Integer>> it = RINGING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			int left = entry.getValue() - 1;
			if (player == null || left <= 0) {
				it.remove();
				continue;
			}
			entry.setValue(left);
			if (left % 5 == 0) {
				float pitch = (left / 5) % 2 == 0 ? 1.6f : 2.0f;
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0f, pitch);
			}
		}
	}
}
