package dev.cypphi.jestermap.dungeon.presentation;

final class MapRotation {
	private MapRotation() {
	}

	static float mapRadians(float playerYaw) {
		return (float) Math.toRadians(-(playerYaw + 180.0F));
	}

	static float markerRadians(float playerYaw, boolean rotatingMap) {
		float relativeDegrees = rotatingMap ? 0.0F : playerYaw + 180.0F;
		return (float) (-Math.PI * 0.5D + Math.toRadians(relativeDegrees));
	}
}
