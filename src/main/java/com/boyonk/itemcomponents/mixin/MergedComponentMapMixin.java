package com.boyonk.itemcomponents.mixin;

import com.boyonk.itemcomponents.BaseComponentSetter;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.MergedComponentMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MergedComponentMap.class)
public abstract class MergedComponentMapMixin implements BaseComponentSetter {
	@Mutable
	@Shadow
	@Final
	private ComponentMap baseComponents;

	@Override
	public void itemcomponents$setBaseComponents(ComponentMap baseComponents) {
		this.baseComponents = baseComponents;
	}
}
