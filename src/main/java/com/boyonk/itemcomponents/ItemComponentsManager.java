package com.boyonk.itemcomponents;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.dynamic.Codecs;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class ItemComponentsManager implements SimpleSynchronousResourceReloadListener {

	public static final Logger LOGGER = ItemComponents.LOGGER;
	public static final Identifier ID = Identifier.of(ItemComponents.NAMESPACE, "item_components");

	public static final String DIRECTORY = "item_components";

	private static final Codec<List<Codecs.TagEntryId>> TARGETS_CODEC = Codec.either(
					Codecs.TAG_ENTRY_ID,
					Codecs.TAG_ENTRY_ID.listOf()
			)
			.xmap(
					either -> either.map(List::of, Function.identity()),
					entries -> entries.size() == 1 ? Either.left(entries.getFirst()) : Either.right(entries)
			);
	private static final Codec<List<Identifier>> PARENTS_CODEC = Identifier.CODEC.listOf();

	private final Map<RegistryEntry<Item>, List<UnmergedComponents>> itemComponents = new HashMap<>();
	private final Map<TagKey<Item>, List<UnmergedComponents>> tagComponents = new HashMap<>();
	private final Map<Item, ComponentMap> itemMapCache = new HashMap<>();
	private final Map<Item, ComponentChanges> itemChangesCache = new HashMap<>();
	private boolean populated = false;

	protected ItemComponentsManager() {
	}

	@Override
	public void reload(ResourceManager manager) {
		this.clear();

		Map<Identifier, UnresolvedComponents> map = new HashMap<>();
		this.loadIntoMap(manager, map);
		new Resolver(map).resolve(this.itemComponents::put, this.tagComponents::put);
		this.markPopulated();

		ItemComponents.forEachStack(stack -> ((BaseComponentSetter) (Object) stack).itemcomponents$setBaseComponents(stack.getItem().getComponents()));

		LOGGER.info("Loaded {} component changes", map.size());
	}

	private void loadIntoMap(ResourceManager manager, Map<Identifier, UnresolvedComponents> map) {
		ResourceFinder finder = ResourceFinder.json(ItemComponentsManager.DIRECTORY);

		for (Map.Entry<Identifier, Resource> entry : finder.findResources(manager).entrySet()) {
			Identifier resourcePath = entry.getKey();
			Resource resource = entry.getValue();

			Identifier resourceId = finder.toResourceId(resourcePath);


			try (BufferedReader reader = resource.getReader()) {
				int priority;
				List<Identifier> parents = List.of();
				List<Codecs.TagEntryId> targets = List.of();
				ComponentChanges changes = ComponentChanges.EMPTY;

				JsonElement json = JsonParser.parseReader(reader);

				JsonObject object = JsonHelper.asObject(json, "item_components");

				priority = JsonHelper.getInt(object, "priority", 0);
				if (object.has("parents")) {
					parents = PARENTS_CODEC.decode(JsonOps.INSTANCE, JsonHelper.getElement(object, "parents")).getOrThrow(JsonSyntaxException::new).getFirst();
				}
				if (object.has("targets")) {
					targets = TARGETS_CODEC.decode(JsonOps.INSTANCE, JsonHelper.getElement(object, "targets")).getOrThrow(JsonSyntaxException::new).getFirst();
				}
				if (object.has("components")) {
					changes = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, JsonHelper.getObject(object, "components")).getOrThrow(JsonSyntaxException::new).getFirst();
				}

				map.put(resourceId, new UnresolvedComponents(resourceId, priority, targets, parents, changes));
			} catch (Exception exception) {
				LOGGER.error("Couldn't read components {} from {} in data pack {}", resourceId, resourcePath, resource.getPackId(), exception);
			}
		}
	}


	protected void clear() {
		this.itemComponents.clear();
		this.tagComponents.clear();
		this.itemMapCache.clear();
		this.itemChangesCache.clear();
		this.populated = false;
	}

	protected void markPopulated() {
		this.populated = true;
	}

	public void close() {
		this.clear();

		ItemComponents.forEachStack(stack -> ((BaseComponentSetter) (Object) stack).itemcomponents$setBaseComponents(stack.getItem().getComponents()));
	}


	public final ComponentMap getMap(Item item, ComponentMap base) {
		if (!this.populated) return base;

		ComponentMap cached = this.itemMapCache.get(item);
		if (cached != null) return cached;

		ComponentChanges changes = this.getChanges(item);

		ComponentMap result = MergedComponentMap.create(base, changes);
		this.itemMapCache.put(item, result);
		return result;
	}

	public final ComponentChanges getChanges(Item item) {
		if (!this.populated) return ComponentChanges.EMPTY;

		ComponentChanges cached = this.itemChangesCache.get(item);
		if (cached != null) return cached;

		RegistryEntry<Item> entry = Registries.ITEM.getEntry(item);

		ComponentChanges.Builder builder = ComponentChanges.builder();

		Stream.concat(
						this.itemComponents.getOrDefault(entry, List.of()).stream(),
						entry.streamTags().map(tag -> this.tagComponents.getOrDefault(tag, List.of())).flatMap(Collection::stream)
				).sorted(Comparator.comparingInt(UnmergedComponents::priority))
				.map(UnmergedComponents::components)
				.flatMap(Collection::stream)
				.forEachOrdered(changes -> {
					ComponentChanges.AddedRemovedPair pair = changes.toAddedRemovedPair();
					pair.added().forEach(builder::add);
					pair.removed().forEach(builder::remove);

				});

		ComponentChanges result = builder.build();
		this.itemChangesCache.put(item, result);
		return result;
	}


	@Override
	public Identifier getFabricId() {
		return ID;
	}

	private static class Resolver {
		private final Map<Identifier, UnresolvedComponents> unresolved;
		private final Map<Identifier, UnmergedComponents> resolved = new HashMap<>();
		private final Set<Identifier> toResolve = new HashSet<>();

		Resolver(Map<Identifier, UnresolvedComponents> unresolved) {
			this.unresolved = unresolved;
		}

		public void resolve(BiConsumer<RegistryEntry<Item>, List<UnmergedComponents>> itemAdder, BiConsumer<TagKey<Item>, List<UnmergedComponents>> tagAdder) {
			Map<Codecs.TagEntryId, List<UnmergedComponents>> unmerged = new HashMap<>();

			for (Map.Entry<Identifier, UnresolvedComponents> entry : this.unresolved.entrySet()) {
				try {
					Identifier id = entry.getKey();
					UnresolvedComponents unresolved = entry.getValue();
					if (unresolved.targets().isEmpty()) continue;

					UnmergedComponents resolved = this.getOrResolve(id);
					unresolved.targets().forEach(target -> unmerged.computeIfAbsent(target, (k) -> new ArrayList<>(1)).add(resolved));
				} catch (Exception e) {
					LOGGER.error("Failed to load {}", entry.getKey(), e);
				}
			}

			for (Map.Entry<Codecs.TagEntryId, List<UnmergedComponents>> entry : unmerged.entrySet()) {
				Codecs.TagEntryId id = entry.getKey();
				List<UnmergedComponents> list = entry.getValue();
				if (id.tag()) {
					tagAdder.accept(TagKey.of(RegistryKeys.ITEM, id.id()), list);
				} else {
					Registries.ITEM.getEntry(id.id()).ifPresent(item -> itemAdder.accept(item, list));
				}
			}
		}


		UnmergedComponents getOrResolve(Identifier id) throws Exception {
			if (this.resolved.containsKey(id)) return this.resolved.get(id);

			if (this.toResolve.contains(id)) {
				throw new IllegalStateException("Circular reference while loading " + id);
			}
			this.toResolve.add(id);
			UnresolvedComponents unresolved = this.unresolved.get(id);
			if (unresolved == null) throw new FileNotFoundException(id.toString());

			List<ComponentChanges> components = new ArrayList<>();
			for (Identifier parentId : unresolved.parents()) {
				try {
					components.addAll(this.getOrResolve(parentId).components());
				} catch (Exception e) {
					LOGGER.error("Unable to resolve parent {} referenced from {}", parentId, id, e);
				}
			}
			components.add(unresolved.components());
			UnmergedComponents resolved = new UnmergedComponents(unresolved.resourceId(), unresolved.priority(), components);

			this.resolved.put(id, resolved);
			this.toResolve.remove(id);

			return resolved;
		}

	}

	record UnresolvedComponents(Identifier resourceId, int priority, List<Codecs.TagEntryId> targets, List<Identifier> parents, ComponentChanges components) {
	}

	record UnmergedComponents(Identifier resourceId, int priority, List<ComponentChanges> components) {

	}


}

