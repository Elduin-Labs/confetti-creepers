package com.elduin.confetti_creepers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Confetti Missions: little jobs to do, each with a prize. Type /quests to see them. */
public final class Quests {

	/** Things we count. More than one mission can count the same thing. */
	public static final String CALLED = "called";
	public static final String PRANK_CALLS = "prank_calls";
	public static final String POPS = "pops";
	public static final String CONFETTI_WALK = "confetti_walk";

	private record Quest(String id, String name, String task, String stat, int goal,
			Item reward, int rewardCount, String rewardName) {
	}

	/** The missions, in the order they show up in the list. */
	private static final List<Quest> QUESTS = List.of(
			new Quest("hello", "Hello?", "Get called by 3 creepers", CALLED, 3,
					Items.CAKE, 1, "1 Cake"),
			new Quest("prank_caller", "Prank Caller", "Call a creeper with your phone 5 times", PRANK_CALLS, 5,
					Items.FIREWORK_ROCKET, 16, "16 Firework Rockets"),
			new Quest("party_popper", "Party Popper", "Be there when 5 creepers pop", POPS, 5,
					Items.DIAMOND, 3, "3 Diamonds"),
			new Quest("confetti_trail", "Confetti Trail", "Walk 500 blocks as Confetti Guy", CONFETTI_WALK, 500,
					Items.GOLDEN_APPLE, 2, "2 Golden Apples"),
			new Quest("party_master", "Party Master", "Be there when 25 creepers pop", POPS, 25,
					Items.TOTEM_OF_UNDYING, 1, "1 Totem of Undying")
	);

	/** How often the Confetti Trail mission tells you how far you've walked. */
	private static final int WALK_UPDATE_EVERY = 25;

	/** What one player has done so far. Saved with the player, and kept when they die. */
	public record Progress(Map<String, Integer> counts, List<String> done) {
		public static final Codec<Progress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("counts").forGetter(Progress::counts),
				Codec.STRING.listOf().fieldOf("done").forGetter(Progress::done)
		).apply(instance, Progress::new));

		public static final Progress EMPTY = new Progress(Map.of(), List.of());

		int count(String stat) {
			return counts.getOrDefault(stat, 0);
		}
	}

	private static final AttachmentType<Progress> PROGRESS = AttachmentRegistry.create(ModTemplate.id("quests"),
			builder -> builder.persistent(Progress.CODEC).initializer(() -> Progress.EMPTY).copyOnDeath());

	/** Bits of a block walked as Confetti Guy that haven't added up to a whole block yet. */
	private static final Map<UUID, Double> WALKED = new HashMap<>();

	private Quests() {
	}

	/** Touching this class registers the saved progress. */
	public static void init() {
	}

	private static Progress progress(ServerPlayer player) {
		return player.getAttachedOrElse(PROGRESS, Progress.EMPTY);
	}

	/** Something a mission counts just happened. Adds to it and hands out any prizes. */
	public static void add(ServerPlayer player, String stat, int amount) {
		Progress before = progress(player);
		Map<String, Integer> counts = new HashMap<>(before.counts());
		int now = before.count(stat) + amount;
		counts.put(stat, now);

		List<String> done = new ArrayList<>(before.done());
		List<Quest> finished = new ArrayList<>();
		for (Quest quest : QUESTS) {
			if (quest.stat().equals(stat) && !done.contains(quest.id()) && now >= quest.goal()) {
				done.add(quest.id());
				finished.add(quest);
			}
		}
		player.setAttached(PROGRESS, new Progress(Map.copyOf(counts), List.copyOf(done)));

		for (Quest quest : finished) {
			complete(player, quest);
		}
		if (finished.isEmpty()) {
			showStep(player, stat, now, amount);
		}
	}

	/** Confetti Guy walked a bit. Counts whole blocks toward Confetti Trail. */
	public static void walked(ServerPlayer player, double distance) {
		double total = WALKED.getOrDefault(player.getUUID(), 0.0) + distance;
		int blocks = (int) total;
		WALKED.put(player.getUUID(), total - blocks);
		if (blocks > 0) {
			add(player, CONFETTI_WALK, blocks);
		}
	}

	/** Shows the next mission that counts this, like "Party Popper: 3/5", above the hotbar. */
	private static void showStep(ServerPlayer player, String stat, int now, int amount) {
		Progress progress = progress(player);
		for (Quest quest : QUESTS) {
			if (!quest.stat().equals(stat) || progress.done().contains(quest.id())) {
				continue;
			}
			// Walking counts one block at a time, so only say something every few blocks.
			if (stat.equals(CONFETTI_WALK) && now / WALK_UPDATE_EVERY == (now - amount) / WALK_UPDATE_EVERY) {
				return;
			}
			player.sendSystemMessage(Component.literal(quest.name() + ": " + now + "/" + quest.goal())
					.withStyle(ChatFormatting.YELLOW), true);
			return;
		}
	}

	/** Mission done! Party time, and here's your prize. */
	private static void complete(ServerPlayer player, Quest quest) {
		player.sendSystemMessage(Component.literal("MISSION COMPLETE: " + quest.name() + "!")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		player.sendSystemMessage(Component.literal("Your prize: " + quest.rewardName())
				.withStyle(ChatFormatting.AQUA));

		ItemStack prize = new ItemStack(quest.reward(), quest.rewardCount());
		if (!player.getInventory().add(prize)) {
			player.drop(prize, false); // inventory full, so it lands at your feet
		}

		ServerLevel level = (ServerLevel) player.level();
		Confetti.burst(level, player.getX(), player.getY(), player.getZ(), 120, 3.0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0f, 1.0f);

		if (progress(player).done().size() == QUESTS.size()) {
			player.sendSystemMessage(Component.literal("You finished EVERY Confetti Mission! You are the ultimate Confetti Guy!")
					.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
		}
	}

	/** Prints the mission list in chat. */
	public static void show(ServerPlayer player) {
		Progress progress = progress(player);
		player.sendSystemMessage(Component.literal("=== Confetti Missions ===")
				.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
		for (Quest quest : QUESTS) {
			boolean done = progress.done().contains(quest.id());
			int count = Math.min(progress.count(quest.stat()), quest.goal());
			MutableComponent line = Component.literal(done ? "[DONE] " : "[" + count + "/" + quest.goal() + "] ")
					.withStyle(done ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
			line.append(Component.literal(quest.name()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
			line.append(Component.literal(" - " + quest.task()).withStyle(ChatFormatting.GRAY));
			if (!done) {
				line.append(Component.literal("  Prize: " + quest.rewardName()).withStyle(ChatFormatting.AQUA));
			}
			player.sendSystemMessage(line);
		}
	}

	/** When you join: a reminder that missions exist, unless you've done them all. */
	public static void welcome(ServerPlayer player) {
		if (progress(player).done().size() < QUESTS.size()) {
			player.sendSystemMessage(Component.literal("Type /quests to see your Confetti Missions!")
					.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}

	/** Forget the bits of a block someone walked when they leave. */
	public static void forget(ServerPlayer player) {
		WALKED.remove(player.getUUID());
	}
}
