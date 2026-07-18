package dev.cypphi.jestermap.config;

public record JesterConfig(int schemaVersion, MapHudConfig mapHud, VisibilityConfig visibility) {
	public JesterConfig withMapHud(MapHudConfig value) {
		return new JesterConfig(schemaVersion, value, visibility);
	}

	public JesterConfig withVisibility(VisibilityConfig value) {
		return new JesterConfig(schemaVersion, mapHud, value);
	}
}
