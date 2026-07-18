package dev.cypphi.jestermap.config;

public record JesterConfig(int schemaVersion, MapHudConfig mapHud, VisibilityConfig visibility, UiConfig ui) {
	public JesterConfig withMapHud(MapHudConfig value) {
		return new JesterConfig(schemaVersion, value, visibility, ui);
	}

	public JesterConfig withVisibility(VisibilityConfig value) {
		return new JesterConfig(schemaVersion, mapHud, value, ui);
	}

	public JesterConfig withUi(UiConfig value) {
		return new JesterConfig(schemaVersion, mapHud, visibility, value);
	}
}
