package com.boyonk.itemcomponents.mixin;

import com.boyonk.itemcomponents.BaseComponentSetter;
import com.boyonk.itemcomponents.ItemComponents;
import com.boyonk.itemcomponents.OwoHack;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements BaseComponentSetter {

	@Shadow
	@Final
	MergedComponentMap components;

	@Inject(method = "<init>(Lnet/minecraft/item/ItemConvertible;ILnet/minecraft/component/MergedComponentMap;)V", at = @At("RETURN"))
	void itemcomponents$storeStack(ItemConvertible item, int count, MergedComponentMap components, CallbackInfo ci) {
		ItemComponents.store((ItemStack) (Object) this);
	}

	@Override
	public void itemcomponents$setBaseComponents(ComponentMap baseComponents) {
		if (ItemComponents.applyOwoHack()) baseComponents = this.itemcomponents$owoHack(baseComponents);

		((BaseComponentSetter) (Object) this.components).itemcomponents$setBaseComponents(baseComponents);
	}

	@Unique
	private ComponentMap itemcomponents$owoHack(ComponentMap base) {
		try {
			Field field = ItemStack.class.getDeclaredField("owo$derivedMap");
			ComponentMap derived = OwoHack.apply((ItemStack) (Object) this, base);
			field.set(this, derived);
			return derived;
		} catch (Exception e) {
			ItemComponents.LOGGER.error("Failed to wrap owo: ", e);
			throw new RuntimeException(e);
		}
	}

}
