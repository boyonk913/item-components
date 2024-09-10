package com.boyonk.itemcomponents.mixin;

import com.boyonk.itemcomponents.BaseComponentSetter;
import com.boyonk.itemcomponents.ItemComponents;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentMapImpl;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements BaseComponentSetter {

	@Shadow
	@Final
	ComponentMapImpl components;

	@Inject(method = "<init>(Lnet/minecraft/item/ItemConvertible;ILnet/minecraft/component/ComponentMapImpl;)V", at = @At("RETURN"))
	void itemcomponents$storeStack(ItemConvertible item, int count, ComponentMapImpl components, CallbackInfo ci) {
		ItemComponents.store((ItemStack) (Object) this);
	}

	@Override
	public void itemcomponents$setBaseComponents(ComponentMap baseComponents) {
		((BaseComponentSetter) (Object) this.components).itemcomponents$setBaseComponents(baseComponents);
	}

}
