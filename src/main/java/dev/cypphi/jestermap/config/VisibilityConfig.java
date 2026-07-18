package dev.cypphi.jestermap.config;

public record VisibilityConfig(VisibilityMode mode, boolean darkenUndiscoveredDoors) {
	public VisibilityConfig withMode(VisibilityMode value) {
		return new VisibilityConfig(value, darkenUndiscoveredDoors);
	}

	public VisibilityConfig withDarkenUndiscoveredDoors(boolean value) {
		return new VisibilityConfig(mode, value);
	}
}
