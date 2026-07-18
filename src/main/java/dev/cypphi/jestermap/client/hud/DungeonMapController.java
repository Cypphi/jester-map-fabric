package dev.cypphi.jestermap.client.hud;

import dev.cypphi.jestermap.dungeon.map.DungeonMapParser;
import dev.cypphi.jestermap.dungeon.map.DungeonMapSnapshot;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Owns the client-thread boundary between Minecraft map data and the pure dungeon parser. */
public final class DungeonMapController {
	private static final int MAP_SLOT = 8;
	private static final int RETRY_TICKS = 10;
	private static final DungeonMapParser PARSER = new DungeonMapParser();

	private static volatile DungeonMapSnapshot snapshot = DungeonMapSnapshot.EMPTY;
	private static volatile boolean dirty = true;
	private static int ticksUntilRetry;

	private DungeonMapController() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(DungeonMapController::tick);
		ClientPlayConnectionEvents.JOIN.register((ignoredHandler, ignoredSender, ignoredClient) -> reset());
		ClientPlayConnectionEvents.DISCONNECT.register((ignoredHandler, ignoredClient) -> reset());
	}

	public static DungeonMapSnapshot snapshot() {
		return snapshot;
	}

	public static void onMapDataUpdated() {
		dirty = true;
	}

	private static void tick(Minecraft client) {
		if (!dirty) {
			if (--ticksUntilRetry > 0) {
				return;
			}
			if (snapshot.available() && hasMagicalMap(client)) {
				ticksUntilRetry = RETRY_TICKS;
				return;
			}
		}

		dirty = false;
		ticksUntilRetry = RETRY_TICKS;
		snapshot = readSnapshot(client);
	}

	private static DungeonMapSnapshot readSnapshot(Minecraft client) {
		if (client.player == null || client.level == null || !hasMagicalMap(client)) {
			return DungeonMapSnapshot.EMPTY;
		}

		List<ItemStack> inventory = client.player.getInventory().getNonEquipmentItems();
		ItemStack stack = inventory.get(MAP_SLOT);
		MapItemSavedData mapData = MapItem.getSavedData(stack, client.level);
		if (mapData == null) {
			return DungeonMapSnapshot.EMPTY;
		}

		DungeonMapSnapshot parsed = PARSER.parse(Arrays.copyOf(mapData.colors, mapData.colors.length));
		if (!parsed.available()) {
			return DungeonMapSnapshot.EMPTY;
		}
		return parsed.withMarkers(readMarkers(mapData));
	}

	private static boolean hasMagicalMap(Minecraft client) {
		if (client.player == null || client.level == null) {
			return false;
		}
		List<ItemStack> inventory = client.player.getInventory().getNonEquipmentItems();
		if (inventory.size() <= MAP_SLOT) {
			return false;
		}
		ItemStack stack = inventory.get(MAP_SLOT);
		return stack.is(Items.FILLED_MAP)
				&& stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains("magical map");
	}

	private static List<DungeonMapSnapshot.Marker> readMarkers(MapItemSavedData mapData) {
		List<DungeonMapSnapshot.Marker> markers = new ArrayList<>();
		for (MapDecoration decoration : mapData.getDecorations()) {
			boolean localPlayer = decoration.type().value().equals(MapDecorationTypes.FRAME.value());
			boolean teammate = decoration.type().value().equals(MapDecorationTypes.BLUE_MARKER.value())
					|| decoration.type().value().equals(MapDecorationTypes.PLAYER.value());
			if (!localPlayer && !teammate) {
				continue;
			}
			markers.add(new DungeonMapSnapshot.Marker(
					(decoration.x() + 128) * 0.5F,
					(decoration.y() + 128) * 0.5F,
					decoration.rot() * 22.5F,
					localPlayer
			));
		}
		return markers;
	}

	private static void reset() {
		snapshot = DungeonMapSnapshot.EMPTY;
		dirty = true;
		ticksUntilRetry = 0;
	}
}
