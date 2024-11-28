package com.boyonk.itemcomponents;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public class ItemComponents implements ModInitializer {

	public static final String NAMESPACE = "item_components";
	public static final Logger LOGGER = LoggerFactory.getLogger("Item Components");

	public static final ItemComponentsManager MANAGER = new ItemComponentsManager();

	private static final Set<ItemStack> WEAK_STACKS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	private static boolean owoHack = false;

	@Override
	public void onInitialize() {
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(MANAGER.getFabricId(), (registries) -> {
			MANAGER.setRegistries(registries);
			return MANAGER;
		});
		CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
			if (!client) MANAGER.resolve();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> MANAGER.close());

		if (FabricLoader.getInstance().isModLoaded("owo")) owoHack = true;
	}

	public static void store(ItemStack stack) {
		WEAK_STACKS.add(stack);
	}

	public static void forEachStack(Consumer<ItemStack> action) {
		synchronized (WEAK_STACKS) {
			WEAK_STACKS.forEach(action);
		}
	}

	public static boolean applyOwoHack() {
		return owoHack;
	}


}
