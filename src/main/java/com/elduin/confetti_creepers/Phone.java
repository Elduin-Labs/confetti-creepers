package com.elduin.confetti_creepers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The phone. Right-click it to call a creeper. It also rings when a creeper calls you.
 * Sneak and right-click to check your Confetti Missions.
 */
public class Phone extends Item {

	public Phone(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()) {
			Quests.show(serverPlayer);
		} else if (player instanceof ServerPlayer serverPlayer) {
			Confetti.ring(serverPlayer);
			serverPlayer.sendSystemMessage(Component.literal("[Phone] Calling: Creeper...")
					.withStyle(ChatFormatting.GREEN));
			serverPlayer.sendSystemMessage(Component.literal("Creeper: Ssssss... hello? Is this the confetti guy?")
					.withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
			serverPlayer.getCooldowns().addCooldown(serverPlayer.getItemInHand(hand), 40);
			Quests.add(serverPlayer, Quests.PRANK_CALLS, 1);
		}
		return InteractionResult.SUCCESS;
	}
}
