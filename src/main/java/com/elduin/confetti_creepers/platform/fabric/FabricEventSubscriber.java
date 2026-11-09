package com.elduin.confetti_creepers.platform.fabric;

//? fabric {

import com.elduin.confetti_creepers.Confetti;
import com.elduin.confetti_creepers.ModItems;
import com.elduin.confetti_creepers.NachoChipRocket;
import com.elduin.confetti_creepers.PieRocket;
import com.elduin.confetti_creepers.Quests;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
//? if <26.2 {
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
//?} else {
/*import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
*///?}
import net.minecraft.world.item.CreativeModeTabs;

public class FabricEventSubscriber {

	public static void registerEvents() {
		ServerTickEvents.END_SERVER_TICK.register(Confetti::tick);
		ServerTickEvents.END_SERVER_TICK.register(PieRocket::tick);
		ServerTickEvents.END_SERVER_TICK.register(NachoChipRocket::tick);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("quests").executes(context -> {
					Quests.show(context.getSource().getPlayerOrException());
					return 1;
				})));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Quests.welcome(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Quests.forget(handler.getPlayer()));
		//? if <26.2 {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(entries -> {
					entries.accept(ModItems.PHONE);
					entries.accept(ModItems.PIE_ROCKET);
					entries.accept(ModItems.NACHO_CHIP_ROCKET);
				});
		//?} else {
		/*CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> {
					output.accept(ModItems.PHONE);
					output.accept(ModItems.PIE_ROCKET);
					output.accept(ModItems.NACHO_CHIP_ROCKET);
				});
		*///?}
	}
}
//?}
