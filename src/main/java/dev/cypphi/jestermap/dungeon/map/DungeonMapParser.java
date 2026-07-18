package dev.cypphi.jestermap.dungeon.map;

import dev.cypphi.jestermap.dungeon.map.DungeonMapSnapshot.Calibration;
import dev.cypphi.jestermap.dungeon.map.DungeonMapSnapshot.Cell;
import dev.cypphi.jestermap.dungeon.map.DungeonMapSnapshot.ClearState;
import dev.cypphi.jestermap.dungeon.map.DungeonMapSnapshot.Door;
import dev.cypphi.jestermap.dungeon.map.DungeonMapSnapshot.DoorType;
import dev.cypphi.jestermap.dungeon.map.DungeonMapSnapshot.Room;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts Hypixel's 128 by 128 map colors into immutable room and door topology. */
public final class DungeonMapParser {
	public static final int MAP_SIZE = 128;
	private static final int PIXEL_COUNT = MAP_SIZE * MAP_SIZE;
	private static final int CONNECTION_SIZE = 4;
	private static final int MIN_CHECKMARK_PIXELS = 3;

	public DungeonMapSnapshot parse(byte[] colors) {
		if (colors == null || colors.length < PIXEL_COUNT) {
			return DungeonMapSnapshot.EMPTY;
		}

		Calibration calibration = findCalibration(colors);
		if (calibration == null) {
			return DungeonMapSnapshot.EMPTY;
		}

		List<ObservedCell> cells = observeCells(colors, calibration);
		if (cells.isEmpty()) {
			return DungeonMapSnapshot.EMPTY;
		}

		DisjointSet roomGroups = new DisjointSet(cells.size());
		Map<Long, Integer> cellsByPosition = indexCells(cells);
		for (int index = 0; index < cells.size(); index++) {
			ObservedCell cell = cells.get(index);
			mergeIfConnected(colors, calibration, cells, cellsByPosition, roomGroups, index, cell, calibration.step(), 0);
			mergeIfConnected(colors, calibration, cells, cellsByPosition, roomGroups, index, cell, 0, calibration.step());
		}

		List<Room> rooms = buildRooms(cells, roomGroups);
		List<Door> doors = observeDoors(colors, calibration, cells, cellsByPosition, roomGroups);
		return new DungeonMapSnapshot(calibration, rooms, doors, List.of());
	}

	private static Calibration findCalibration(byte[] colors) {
		for (int y = 0; y < MAP_SIZE; y++) {
			for (int x = 0; x < MAP_SIZE; x++) {
				if (colorAt(colors, x, y) != 30 || colorAt(colors, x - 1, y) == 30 || colorAt(colors, x, y - 1) == 30) {
					continue;
				}

				int width = coloredRun(colors, x, y, 1, 0, 30);
				if (width != 16 && width != 18) {
					continue;
				}
				int height = coloredRun(colors, x, y, 0, 1, 30);
				if (height < width || colorAt(colors, x + width - 1, y + width - 1) != 30) {
					continue;
				}

				int step = width + CONNECTION_SIZE;
				return new Calibration(width, step, Math.floorMod(x, step), Math.floorMod(y, step));
			}
		}
		return null;
	}

	private static int coloredRun(byte[] colors, int x, int y, int deltaX, int deltaY, int expectedColor) {
		int length = 0;
		while (colorAt(colors, x + deltaX * length, y + deltaY * length) == expectedColor) {
			length++;
		}
		return length;
	}

	private static List<ObservedCell> observeCells(byte[] colors, Calibration calibration) {
		List<ObservedCell> cells = new ArrayList<>(36);
		for (int y = calibration.originY(); y + calibration.roomSize() <= MAP_SIZE; y += calibration.step()) {
			for (int x = calibration.originX(); x + calibration.roomSize() <= MAP_SIZE; x += calibration.step()) {
				DungeonRoomType type = observeRoomType(colors, x, y, calibration.roomSize());
				if (type != null) {
					cells.add(new ObservedCell(new Cell(x, y), type, observeClearState(colors, x, y, calibration.roomSize(), type)));
				}
			}
		}
		return cells;
	}

	private static DungeonRoomType observeRoomType(byte[] colors, int x, int y, int roomSize) {
		int inset = Math.max(2, roomSize / 5);
		int[][] samples = {
				{x + inset, y + inset},
				{x + roomSize - inset - 1, y + inset},
				{x + inset, y + roomSize - inset - 1},
				{x + roomSize - inset - 1, y + roomSize - inset - 1}
		};
		Map<DungeonRoomType, Integer> counts = new HashMap<>();
		for (int[] sample : samples) {
			DungeonRoomType type = DungeonRoomType.fromPackedColor(colors[sample[0] + sample[1] * MAP_SIZE]);
			if (type != null) {
				counts.merge(type, 1, Integer::sum);
			}
		}
		return counts.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse(null);
	}

	private static ClearState observeClearState(byte[] colors, int x, int y, int roomSize, DungeonRoomType type) {
		int centerX = x + roomSize / 2;
		int centerY = y + roomSize / 2;
		int green = countColor(colors, centerX, centerY, 5, 30);
		int white = countColor(colors, centerX, centerY, 5, 34);
		int red = countColor(colors, centerX, centerY, 5, 18);
		if (type != DungeonRoomType.ENTRANCE && green >= MIN_CHECKMARK_PIXELS) {
			return ClearState.GREEN;
		}
		if (white >= MIN_CHECKMARK_PIXELS) {
			return ClearState.WHITE;
		}
		if (type != DungeonRoomType.BLOOD && red >= MIN_CHECKMARK_PIXELS) {
			return ClearState.FAILED;
		}
		return ClearState.UNCLEARED;
	}

	private static int countColor(byte[] colors, int centerX, int centerY, int radius, int expectedColor) {
		int count = 0;
		for (int y = centerY - radius; y <= centerY + radius; y++) {
			for (int x = centerX - radius; x <= centerX + radius; x++) {
				if (colorAt(colors, x, y) == expectedColor) {
					count++;
				}
			}
		}
		return count;
	}

	private static Map<Long, Integer> indexCells(List<ObservedCell> cells) {
		Map<Long, Integer> result = new HashMap<>();
		for (int index = 0; index < cells.size(); index++) {
			Cell cell = cells.get(index).cell();
			result.put(positionKey(cell.mapX(), cell.mapY()), index);
		}
		return result;
	}

	private static void mergeIfConnected(
			byte[] colors,
			Calibration calibration,
			List<ObservedCell> cells,
			Map<Long, Integer> cellsByPosition,
			DisjointSet groups,
			int index,
			ObservedCell cell,
			int deltaX,
			int deltaY
	) {
		Integer neighborIndex = cellsByPosition.get(positionKey(cell.cell().mapX() + deltaX, cell.cell().mapY() + deltaY));
		if (neighborIndex == null || cells.get(neighborIndex).type() != cell.type()) {
			return;
		}
		if (hasBroadConnection(colors, calibration, cell.cell(), deltaX != 0)) {
			groups.union(index, neighborIndex);
		}
	}

	private static boolean hasBroadConnection(byte[] colors, Calibration calibration, Cell first, boolean horizontal) {
		int sideInset = Math.max(2, calibration.roomSize() / 5);
		if (horizontal) {
			int gapX = first.mapX() + calibration.roomSize() + CONNECTION_SIZE / 2;
			return colorAt(colors, gapX, first.mapY() + sideInset) != 0
					&& colorAt(colors, gapX, first.mapY() + calibration.roomSize() - sideInset - 1) != 0;
		}
		int gapY = first.mapY() + calibration.roomSize() + CONNECTION_SIZE / 2;
		return colorAt(colors, first.mapX() + sideInset, gapY) != 0
				&& colorAt(colors, first.mapX() + calibration.roomSize() - sideInset - 1, gapY) != 0;
	}

	private static List<Room> buildRooms(List<ObservedCell> cells, DisjointSet groups) {
		Map<Integer, List<ObservedCell>> groupedCells = new LinkedHashMap<>();
		for (int index = 0; index < cells.size(); index++) {
			groupedCells.computeIfAbsent(groups.find(index), ignored -> new ArrayList<>()).add(cells.get(index));
		}

		List<Room> rooms = new ArrayList<>(groupedCells.size());
		for (List<ObservedCell> group : groupedCells.values()) {
			ClearState clearState = group.stream()
					.map(ObservedCell::clearState)
					.max(Enum::compareTo)
					.orElse(ClearState.UNCLEARED);
			rooms.add(new Room(group.getFirst().type(), clearState, group.stream().map(ObservedCell::cell).toList()));
		}
		return rooms;
	}

	private static List<Door> observeDoors(
			byte[] colors,
			Calibration calibration,
			List<ObservedCell> cells,
			Map<Long, Integer> cellsByPosition,
			DisjointSet groups
	) {
		List<Door> doors = new ArrayList<>();
		for (int index = 0; index < cells.size(); index++) {
			ObservedCell first = cells.get(index);
			observeDoor(colors, calibration, cells, cellsByPosition, groups, doors, index, first, calibration.step(), 0);
			observeDoor(colors, calibration, cells, cellsByPosition, groups, doors, index, first, 0, calibration.step());
		}
		return doors;
	}

	private static void observeDoor(
			byte[] colors,
			Calibration calibration,
			List<ObservedCell> cells,
			Map<Long, Integer> cellsByPosition,
			DisjointSet groups,
			List<Door> doors,
			int firstIndex,
			ObservedCell first,
			int deltaX,
			int deltaY
	) {
		Integer secondIndex = cellsByPosition.get(positionKey(first.cell().mapX() + deltaX, first.cell().mapY() + deltaY));
		if (secondIndex == null || groups.find(firstIndex) == groups.find(secondIndex)) {
			return;
		}

		boolean horizontal = deltaX != 0;
		int centerX = first.cell().mapX() + (horizontal ? calibration.roomSize() + CONNECTION_SIZE / 2 : calibration.roomSize() / 2);
		int centerY = first.cell().mapY() + (horizontal ? calibration.roomSize() / 2 : calibration.roomSize() + CONNECTION_SIZE / 2);
		int packedColor = colorAt(colors, centerX, centerY);
		if (packedColor == 0 || hasBroadConnection(colors, calibration, first.cell(), horizontal)) {
			return;
		}

		DoorType type = switch (packedColor) {
			case 119 -> DoorType.WITHER;
			case 18 -> DoorType.BLOOD;
			default -> DoorType.NORMAL;
		};
		doors.add(new Door(first.cell(), cells.get(secondIndex).cell(), type, packedColor != 119 && packedColor != 18));
	}

	private static int colorAt(byte[] colors, int x, int y) {
		if (x < 0 || y < 0 || x >= MAP_SIZE || y >= MAP_SIZE) {
			return -1;
		}
		return Byte.toUnsignedInt(colors[x + y * MAP_SIZE]);
	}

	private static long positionKey(int x, int y) {
		return ((long) x << 32) ^ Integer.toUnsignedLong(y);
	}

	private record ObservedCell(Cell cell, DungeonRoomType type, ClearState clearState) {
	}

	private static final class DisjointSet {
		private final int[] parents;

		private DisjointSet(int size) {
			parents = new int[size];
			for (int index = 0; index < size; index++) {
				parents[index] = index;
			}
		}

		private int find(int value) {
			int parent = parents[value];
			if (parent != value) {
				parents[value] = find(parent);
			}
			return parents[value];
		}

		private void union(int first, int second) {
			parents[find(second)] = find(first);
		}
	}
}
