package com.elduin.confetti_creepers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The Pie Rocket. Click it and a rocket blasts a pumpkin pie into space.
 * A few seconds later a GIANT pumpkin pie falls out of the sky in front of you.
 */
public class PieRocket extends Item {

	/** How far in front of you the giant pie lands. Far enough that it doesn't land on you. */
	private static final int LAND_DISTANCE = 16;

	/** How big the giant pie is. 6 makes it 13 blocks across. */
	private static final int PIE_RADIUS = 6;

	/** How long the pie stays up in space: 3 seconds. */
	private static final int SPACE_TICKS = 60;

	/** How high above the ground the giant pie starts falling from. */
	private static final int DROP_HEIGHT = 40;

	/** How many pumpkin pies you can grab when the giant one lands. */
	private static final int SLICES = 8;

	/** How long before you can launch another one: 5 seconds. */
	private static final int COOLDOWN_TICKS = 100;

	private static final BlockState CRUST = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
	//? if <26.2 {
	private static final BlockState FILLING = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
	private static final BlockState CREAM = Blocks.WHITE_WOOL.defaultBlockState();
	//?} else {
	/*private static final BlockState FILLING = Blocks.DYED_TERRACOTTA.orange().defaultBlockState();
	private static final BlockState CREAM = Blocks.WOOL.white().defaultBlockState();
	*///?}

	/** A pie on its way: first up in space, then falling back down. */
	private static final class Flight {
		final ServerLevel level;
		final BlockPos center;
		int spaceTicks = SPACE_TICKS;
		List<FallingBlockEntity> falling;
		int fallingTicks;

		Flight(ServerLevel level, BlockPos center) {
			this.level = level;
			this.center = center;
		}
	}

	private static final List<Flight> FLIGHTS = new ArrayList<>();

	public PieRocket(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		if (!serverLevel.canSeeSky(player.blockPosition().above())) {
			serverPlayer.sendSystemMessage(Component.literal("You need open sky to launch a rocket!")
					.withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}

		// Where the pie will land: straight ahead of where you're looking.
		Vec3 look = player.getLookAngle();
		Vec3 ahead = new Vec3(look.x, 0, look.z);
		ahead = ahead.lengthSqr() < 0.0001 ? Vec3.directionFromRotation(0, player.getYRot()) : ahead.normalize();
		BlockPos center = BlockPos.containing(player.getX() + ahead.x * LAND_DISTANCE, player.getY(),
				player.getZ() + ahead.z * LAND_DISTANCE);

		level.addFreshEntity(new FireworkRocketEntity(level, player.getX(), player.getY() + 1, player.getZ(),
				new ItemStack(Items.FIREWORK_ROCKET)));
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 2.0f, 0.8f);
		serverPlayer.sendSystemMessage(Component.literal("3... 2... 1... BLAST OFF! The pumpkin pie is going to space!")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

		FLIGHTS.add(new Flight(serverLevel, center));
		ItemStack stack = player.getItemInHand(hand);
		player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	/** Every tick: pies in space come back down, and falling pies land. */
	public static void tick(MinecraftServer server) {
		Iterator<Flight> it = FLIGHTS.iterator();
		while (it.hasNext()) {
			Flight flight = it.next();
			if (flight.falling == null) {
				if (--flight.spaceTicks <= 0) {
					dropPie(flight);
				}
				continue;
			}
			flight.fallingTicks++;
			boolean landed = flight.falling.stream().allMatch(FallingBlockEntity::isRemoved);
			if (landed || flight.fallingTicks > 400) {
				land(flight);
				it.remove();
			}
		}
	}

	/** Builds the giant pie up in the sky out of falling blocks. */
	private static void dropPie(Flight flight) {
		ServerLevel level = flight.level;
		BlockPos center = flight.center;

		int ground = level.getMinY();
		for (int dx = -PIE_RADIUS; dx <= PIE_RADIUS; dx++) {
			for (int dz = -PIE_RADIUS; dz <= PIE_RADIUS; dz++) {
				ground = Math.max(ground, level.getHeight(Heightmap.Types.MOTION_BLOCKING,
						center.getX() + dx, center.getZ() + dz));
			}
		}
		int sky = Math.min(ground + DROP_HEIGHT, level.getMaxY() - 5);

		flight.falling = new ArrayList<>();
		int rim = (PIE_RADIUS - 1) * (PIE_RADIUS - 1) + (PIE_RADIUS - 1);
		for (int dx = -PIE_RADIUS; dx <= PIE_RADIUS; dx++) {
			for (int dz = -PIE_RADIUS; dz <= PIE_RADIUS; dz++) {
				int d = dx * dx + dz * dz;
				if (d > PIE_RADIUS * PIE_RADIUS + PIE_RADIUS) {
					continue; // outside the circle
				}
				boolean edge = d > rim;
				BlockPos bottom = new BlockPos(center.getX() + dx, sky, center.getZ() + dz);
				// Bottom: all crust. Middle: crust around the edge, pumpkin inside. Top: just the crust edge.
				spawn(flight, bottom, CRUST);
				spawn(flight, bottom.above(), edge ? CRUST : FILLING);
				if (edge) {
					spawn(flight, bottom.above(2), CRUST);
				} else if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
					spawn(flight, bottom.above(2), CREAM); // a big blob of whipped cream on top
					if (dx == 0 && dz == 0) {
						spawn(flight, bottom.above(3), CREAM);
					}
				}
			}
		}

		level.playSound(null, center.getX(), ground, center.getZ(),
				SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, SoundSource.PLAYERS, 4.0f, 0.6f);
		for (ServerPlayer player : level.players()) {
			if (player.blockPosition().closerThan(center, 64)) {
				player.sendSystemMessage(Component.literal("Look up! A GIANT pumpkin pie is falling from space!")
						.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			}
		}
	}

	private static void spawn(Flight flight, BlockPos pos, BlockState state) {
		if (!flight.level.getBlockState(pos).isAir()) {
			return; // never squash something that's already up there
		}
		FallingBlockEntity block = FallingBlockEntity.fall(flight.level, pos, state);
		block.dropItem = false;
		flight.falling.add(block);
	}

	/** SPLAT! Confetti everywhere, and pumpkin pies to grab. */
	private static void land(Flight flight) {
		ServerLevel level = flight.level;
		double x = flight.center.getX() + 0.5;
		double z = flight.center.getZ() + 0.5;
		double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, flight.center.getX(), flight.center.getZ());

		Confetti.burst(level, x, y, z, 300, PIE_RADIUS * 2);
		level.playSound(null, x, y, z, SoundEvents.SLIME_BLOCK_FALL, SoundSource.BLOCKS, 4.0f, 0.5f);
		level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.BLOCKS, 3.0f, 1.0f);

		RandomSource random = level.getRandom();
		for (int i = 0; i < SLICES; i++) {
			ItemEntity slice = new ItemEntity(level, x, y + 0.5, z, new ItemStack(Items.PUMPKIN_PIE));
			slice.setDeltaMovement((random.nextDouble() - 0.5) * 0.4, 0.4, (random.nextDouble() - 0.5) * 0.4);
			level.addFreshEntity(slice);
		}

		for (ServerPlayer player : level.players()) {
			if (player.blockPosition().closerThan(flight.center, 64)) {
				player.sendSystemMessage(Component.literal("SPLAT! The giant pumpkin pie landed! Grab a slice!")
						.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			}
		}
	}
}
