package dev.cypphi.jestermap.dungeon.map;

import dev.cypphi.jestermap.dungeon.map.DungeonMapSnapshot.ClearState;
import dev.cypphi.jestermap.dungeon.map.DungeonMapSnapshot.DoorType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonMapParserTest {
	private static final int SIZE = DungeonMapParser.MAP_SIZE;
	private final DungeonMapParser parser = new DungeonMapParser();

	@Test
	void calibratesFromEntranceAndBuildsLogicalRooms() {
		byte[] colors = new byte[SIZE * SIZE];
		fill(colors, 5, 5, 16, 16, 30);
		fill(colors, 25, 25, 16, 16, 63);
		fill(colors, 45, 25, 16, 16, 63);
		fill(colors, 41, 25, 4, 16, 63);

		DungeonMapSnapshot snapshot = parser.parse(colors);

		assertTrue(snapshot.available());
		assertEquals(new DungeonMapSnapshot.Calibration(16, 20, 5, 5), snapshot.calibration());
		assertEquals(2, snapshot.rooms().size());
		assertEquals(2, snapshot.rooms().stream()
				.filter(room -> room.type() == DungeonRoomType.NORMAL)
				.findFirst()
				.orElseThrow()
				.cells().size());
	}

	@Test
	void identifiesDoorTypeAndClearStateFromContextualColors() {
		byte[] colors = new byte[SIZE * SIZE];
		fill(colors, 5, 5, 16, 16, 30);
		fill(colors, 25, 5, 16, 16, 63);
		fill(colors, 21, 11, 4, 4, 119);
		set(colors, 31, 11, 34);
		set(colors, 32, 12, 34);
		set(colors, 33, 13, 34);

		DungeonMapSnapshot snapshot = parser.parse(colors);

		assertEquals(1, snapshot.doors().size());
		assertEquals(DoorType.WITHER, snapshot.doors().getFirst().type());
		assertFalse(snapshot.doors().getFirst().open());
		assertEquals(ClearState.WHITE, snapshot.rooms().stream()
				.filter(room -> room.type() == DungeonRoomType.NORMAL)
				.findFirst()
				.orElseThrow()
				.clearState());
	}

	@Test
	void ignoresOrdinaryMapsWithoutAnEntranceRoom() {
		byte[] colors = new byte[SIZE * SIZE];
		fill(colors, 20, 20, 30, 30, 63);

		assertFalse(parser.parse(colors).available());
	}

	private static void fill(byte[] colors, int x, int y, int width, int height, int color) {
		for (int row = y; row < y + height; row++) {
			for (int column = x; column < x + width; column++) {
				set(colors, column, row, color);
			}
		}
	}

	private static void set(byte[] colors, int x, int y, int color) {
		colors[x + y * SIZE] = (byte) color;
	}
}
