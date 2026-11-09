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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Nacho Chip Rocket. Click it and YOU are the rocket: it blasts you up into the sky
 * with a trail of cheesy nacho dust, then you float gently back down.
 */
public class NachoChipRocket extends Item {

	/** How long the rocket pushes you up: 2 seconds. */
	private static final int BOOST_TICKS = 40;

	/** How fast you go up. 1.2 is about 24 blocks a second. */
	private static final double UP_SPEED = 1.2;

	/** How long you float back down: 20 seconds, so you never go splat. */
	private static final int FLOAT_TICKS = 20 * 20;

	/** How long before you can ride another one: 5 seconds. */
	private static final int COOLDOWN_TICKS = 100;

	/** Nacho cheese orange, and chip yellow. */
	private static final int CHEESE = 0xFF8C1A;
	private static final int CHIP = 0xFFD23B;

	/** Players riding a rocket right now, and how many ticks of boost they have left. */
	private static final Map<UUID, Integer> RIDERS = new HashMap<>();

	public NachoChipRocket(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		if (!serverLevel.canSeeSky(player.blockPosition().above())) {
			serverPlayer.sendSystemMessage(Component.literal("You need open sky to ride a rocket!")
					.withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}
		if (RIDERS.containsKey(player.getUUID())) {
			return InteractionResult.FAIL; // already flying
		}

		RIDERS.put(player.getUUID(), BOOST_TICKS);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 2.0f, 0.6f);
		serverPlayer.sendSystemMessage(Component.literal("3... 2... 1... BLAST OFF! Hold on to your chips!")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

		ItemStack stack = player.getItemInHand(hand);
		player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	/** Every tick: push riders up, leave a cheesy trail, and float them down when the rocket runs out. */
	public static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Integer>> it = RIDERS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				it.remove(); // logged out mid-flight
				continue;
			}

			ServerLevel level = (ServerLevel) player.level();
			int left = entry.getValue() - 1;
			if (left <= 0) {
				it.remove();
				reachTheTop(player, level);
				continue;
			}
			entry.setValue(left);

			Vec3 motion = player.getDeltaMovement();
			player.setDeltaMovement(motion.x, UP_SPEED, motion.z);
			player.hurtMarked = true; // tells the game to send the new speed to the player right away
			player.resetFallDistance();

			// The rocket trail: nacho cheese, chip crumbs, and fire.
			double x = player.getX();
			double y = player.getY();
			double z = player.getZ();
			level.sendParticles(new DustParticleOptions(CHEESE, 1.5f), x, y - 0.3, z, 6, 0.25, 0.2, 0.25, 0.02);
			level.sendParticles(new DustParticleOptions(CHIP, 1.2f), x, y - 0.6, z, 4, 0.3, 0.3, 0.3, 0.02);
			level.sendParticles(ParticleTypes.FLAME, x, y - 0.5, z, 3, 0.15, 0.1, 0.15, 0.01);
			if (left % 8 == 0) {
				level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 1.2f);
			}
		}
	}

	/** The rocket runs out of chip fuel at the top: confetti, then a slow float back to the ground. */
	private static void reachTheTop(ServerPlayer player, ServerLevel level) {
		Confetti.burst(level, player.getX(), player.getY(), player.getZ(), 120, 4.0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 2.0f, 1.0f);
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, FLOAT_TICKS));
		player.resetFallDistance();
		player.sendSystemMessage(Component.literal("Chip level 0! Out of fuel. Floating back down...")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}
}
