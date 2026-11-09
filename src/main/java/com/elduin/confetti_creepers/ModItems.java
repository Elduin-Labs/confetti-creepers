package com.elduin.confetti_creepers;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {

	public static final Item PHONE = register("phone", Phone::new, new Item.Properties().stacksTo(1));
	public static final Item PIE_ROCKET = register("pie_rocket", PieRocket::new, new Item.Properties().stacksTo(16));
	public static final Item NACHO_CHIP_ROCKET = register("nacho_chip_rocket", NachoChipRocket::new,
			new Item.Properties().stacksTo(16));

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ModTemplate.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	/** Touching this class registers everything in it. */
	public static void init() {
	}
}
