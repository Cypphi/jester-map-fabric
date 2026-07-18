package dev.cypphi.jestermap.dungeon.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapRotationTest {
	private static final float EPSILON = 0.0001F;

	@Test
	void keepsNorthUpWhenLookingNorth() {
		assertEquivalentAngle(0.0F, MapRotation.mapRadians(180.0F));
	}

	@Test
	void rotatesEastToTheTopWhenLookingEast() {
		assertEquivalentAngle((float) (-Math.PI * 0.5D), MapRotation.mapRadians(-90.0F));
	}

	@Test
	void keepsPlayerMarkerPointingUpOnRotatingMap() {
		assertEquals((float) (-Math.PI * 0.5D), MapRotation.markerRadians(37.0F, true), EPSILON);
	}

	@Test
	void pointsMarkerEastOnNorthUpMap() {
		assertEquals(0.0F, MapRotation.markerRadians(-90.0F, false), EPSILON);
	}

	@Test
	void pointsMarkerSouthOnNorthUpMap() {
		assertEquals((float) (Math.PI * 0.5D), MapRotation.markerRadians(0.0F, false), EPSILON);
	}

	private static void assertEquivalentAngle(float expected, float actual) {
		float turn = (float) (Math.PI * 2.0D);
		float difference = (actual - expected) % turn;
		if (difference > Math.PI) {
			difference -= turn;
		} else if (difference < -Math.PI) {
			difference += turn;
		}
		assertEquals(0.0F, difference, EPSILON);
	}
}
