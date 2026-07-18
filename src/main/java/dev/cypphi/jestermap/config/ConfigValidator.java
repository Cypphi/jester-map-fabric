package dev.cypphi.jestermap.config;

public final class ConfigValidator {
	private ConfigValidator() {
	}

	public static JesterConfig validate(JesterConfig candidate) {
		JesterConfig defaults = ConfigDefaults.create();
		if (candidate == null) {
			return defaults;
		}

		MapHudConfig map = candidate.mapHud() == null ? defaults.mapHud() : candidate.mapHud();
		VisibilityConfig visibility = candidate.visibility() == null ? defaults.visibility() : candidate.visibility();
		double scale = Double.isFinite(map.scale()) ? map.scale() : defaults.mapHud().scale();
		MapHudConfig validatedMap = new MapHudConfig(
				map.enabled(),
				Math.clamp(map.x(), ConfigDefaults.MIN_POSITION, ConfigDefaults.MAX_POSITION),
				Math.clamp(map.y(), ConfigDefaults.MIN_POSITION, ConfigDefaults.MAX_POSITION),
				Math.clamp(scale, ConfigDefaults.MIN_SCALE, ConfigDefaults.MAX_SCALE),
				map.rotateWithPlayer(),
				map.backgroundBlur(),
				map.showRoomNames(),
				map.showSecretCounts(),
				map.showTeammates()
		);
		VisibilityConfig validatedVisibility = new VisibilityConfig(
				visibility.mode() == null ? defaults.visibility().mode() : visibility.mode(),
				visibility.darkenUndiscoveredDoors()
		);
		return new JesterConfig(ConfigDefaults.SCHEMA_VERSION, validatedMap, validatedVisibility);
	}
}
