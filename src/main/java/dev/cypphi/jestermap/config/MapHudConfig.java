package dev.cypphi.jestermap.config;

public record MapHudConfig(
		boolean enabled,
		int x,
		int y,
		double scale,
		boolean rotateWithPlayer,
		boolean backgroundBlur,
		boolean showRoomNames,
		boolean showSecretCounts,
		boolean showTeammates
) {
	public MapHudConfig withEnabled(boolean value) {
		return new MapHudConfig(value, x, y, scale, rotateWithPlayer, backgroundBlur, showRoomNames, showSecretCounts, showTeammates);
	}

	public MapHudConfig withPosition(int newX, int newY) {
		return new MapHudConfig(enabled, newX, newY, scale, rotateWithPlayer, backgroundBlur, showRoomNames, showSecretCounts, showTeammates);
	}

	public MapHudConfig withScale(double value) {
		return new MapHudConfig(enabled, x, y, value, rotateWithPlayer, backgroundBlur, showRoomNames, showSecretCounts, showTeammates);
	}

	public MapHudConfig withRotateWithPlayer(boolean value) {
		return new MapHudConfig(enabled, x, y, scale, value, backgroundBlur, showRoomNames, showSecretCounts, showTeammates);
	}

	public MapHudConfig withBackgroundBlur(boolean value) {
		return new MapHudConfig(enabled, x, y, scale, rotateWithPlayer, value, showRoomNames, showSecretCounts, showTeammates);
	}

	public MapHudConfig withShowRoomNames(boolean value) {
		return new MapHudConfig(enabled, x, y, scale, rotateWithPlayer, backgroundBlur, value, showSecretCounts, showTeammates);
	}

	public MapHudConfig withShowSecretCounts(boolean value) {
		return new MapHudConfig(enabled, x, y, scale, rotateWithPlayer, backgroundBlur, showRoomNames, value, showTeammates);
	}

	public MapHudConfig withShowTeammates(boolean value) {
		return new MapHudConfig(enabled, x, y, scale, rotateWithPlayer, backgroundBlur, showRoomNames, showSecretCounts, value);
	}
}
