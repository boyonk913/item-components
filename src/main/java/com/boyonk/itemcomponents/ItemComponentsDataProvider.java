package com.boyonk.itemcomponents;

import com.boyonk.itemcomponents.ItemComponentsManager.ItemComponentsEntry;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.data.DataOutput.OutputType;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public abstract class ItemComponentsDataProvider extends FabricCodecDataProvider<ItemComponentsEntry> {
    protected ItemComponentsDataProvider(FabricDataOutput dataOutput,
            CompletableFuture<WrapperLookup> registriesFuture) {
        super(dataOutput, registriesFuture, OutputType.DATA_PACK, "item_components", ItemComponentsEntry.CODEC);
    }

    // Modders should create a class that extends this one and implement configure()

    @Override
    public String getName() {
        return ItemComponents.NAMESPACE;
    }
}
