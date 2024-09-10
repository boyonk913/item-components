package com.boyonk.itemcomponents.mixin;

import com.boyonk.itemcomponents.ItemComponents;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Item.class)
public class ItemMixin {

	@ModifyReturnValue(method = "getComponents", at = @At("RETURN"))
	public ComponentMap itemcomponents$getComponents(ComponentMap original) {
		return ItemComponents.MANAGER.getMap((Item) (Object) this, original);
	}

	@ModifyExpressionValue(method = "getMaxCount", at = @At(value = "FIELD", target = "Lnet/minecraft/item/Item;components:Lnet/minecraft/component/ComponentMap;"))
	public ComponentMap itemcomponents$getComponentsForMaxCount(ComponentMap original) {
		return ItemComponents.MANAGER.getMap((Item) (Object) this, original);
	}
}
