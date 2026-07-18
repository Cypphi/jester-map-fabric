package dev.cypphi.jestermap.dungeon.presentation;

import dev.cypphi.jestermap.config.ConfigDefaults;
import dev.cypphi.jestermap.config.MapHudConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapHudLayoutTest {
	@Test
	void detectsPointsInsideScaledHud() {
		MapHudConfig config = ConfigDefaults.create().mapHud().withPosition(20, 30).withScale(1.5D);
		MapHudLayout.Bounds bounds = MapHudLayout.bounds(config);

		assertTrue(bounds.contains(20, 30));
		assertTrue(bounds.contains(20 + MapHudLayout.BASE_WIDTH * 1.5D, 30 + MapHudLayout.BASE_HEIGHT * 1.5D));
		assertFalse(bounds.contains(19.9D, 30));
	}

	@Test
	void clampsFreeDraggingToTheScreen() {
		assertEquals(
				new MapHudLayout.Position(352, 252),
				MapHudLayout.draggedPosition(900, 900, 1.0D, 500, 400, false)
		);
	}

	@Test
	void alignsEqualDistancesFromTopLeftCorner() {
		assertEquals(
				new MapHudLayout.Position(50, 50),
				MapHudLayout.draggedPosition(38, 62, 1.0D, 500, 400, true)
		);
	}

	@Test
	void alignsEqualDistancesFromBottomRightCorner() {
		assertEquals(
				new MapHudLayout.Position(295, 195),
				MapHudLayout.draggedPosition(300, 190, 1.0D, 500, 400, true)
		);
	}

	@Test
	void resizingKeepsThePointUnderThePointer() {
		MapHudConfig config = ConfigDefaults.create().mapHud().withPosition(100, 80);

		assertEquals(
				new MapHudLayout.Position(63, 39),
				MapHudLayout.resizedAroundPointer(config, 1.5D, 174, 162, 500, 400)
		);
	}
}
