package dev.cypphi.jestermap.dungeon.map;

import java.util.List;

public record DungeonMapSnapshot(
		Calibration calibration,
		List<Room> rooms,
		List<Door> doors,
		List<Marker> markers
) {
	public static final DungeonMapSnapshot EMPTY = new DungeonMapSnapshot(null, List.of(), List.of(), List.of());

	public DungeonMapSnapshot {
		rooms = List.copyOf(rooms);
		doors = List.copyOf(doors);
		markers = List.copyOf(markers);
	}

	public boolean available() {
		return calibration != null;
	}

	public DungeonMapSnapshot withMarkers(List<Marker> replacement) {
		return new DungeonMapSnapshot(calibration, rooms, doors, replacement);
	}

	public record Calibration(int roomSize, int step, int originX, int originY) {
	}

	public record Cell(int mapX, int mapY) {
	}

	public record Room(DungeonRoomType type, ClearState clearState, List<Cell> cells) {
		public Room {
			cells = List.copyOf(cells);
		}
	}

	public record Door(Cell first, Cell second, DoorType type, boolean open) {
	}

	public record Marker(float mapX, float mapY, float worldYaw, boolean localPlayer) {
	}

	public enum ClearState {
		UNCLEARED,
		FAILED,
		WHITE,
		GREEN
	}

	public enum DoorType {
		NORMAL,
		WITHER,
		BLOOD
	}
}
