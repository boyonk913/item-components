package com.boyonk.itemcomponents.mixin;

import com.boyonk.itemcomponents.ItemComponents;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.item.ItemStack$1")
public class ItemStack_PacketCodecMixin {

	@ModifyExpressionValue(method = "encode(Lnet/minecraft/network/RegistryByteBuf;Lnet/minecraft/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/component/MergedComponentMap;getChanges()Lnet/minecraft/component/ComponentChanges;"))
	ComponentChanges defaultitemcomponents$sendExtraChanges(ComponentChanges original, @Local(argsOnly = true) ItemStack stack) {
		ComponentChanges base = ItemComponents.MANAGER.getChanges(stack);
		if (base.isEmpty()) return original;
		if (original.isEmpty()) return base;

		ComponentChanges.Builder builder = ComponentChanges.builder();
		ComponentChanges.AddedRemovedPair pair;

		pair = base.toAddedRemovedPair();
		pair.added().forEach(builder::add);
		pair.removed().forEach(builder::remove);

		pair = original.toAddedRemovedPair();
		pair.added().forEach(builder::add);
		pair.removed().forEach(builder::remove);

		return builder.build();
	}


}
