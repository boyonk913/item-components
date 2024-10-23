package com.boyonk.itemcomponents;

import io.wispforest.owo.ext.DerivedComponentMap;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.ItemStack;

public class OwoHack {

	public static ComponentMap apply(ItemStack stack, ComponentMap map) {
		DerivedComponentMap derived = new DerivedComponentMap(map);
		derived.derive(stack);
		return derived;
	}
}
