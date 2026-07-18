package dev.cypphi.jestermap.config;

public final class ConfigDefaults {
	public static final int SCHEMA_VERSION = 2;
	public static final double MIN_SCALE = 0.5D;
	public static final double MAX_SCALE = 2.0D;
	public static final int MIN_POSITION = -4096;
	public static final int MAX_POSITION = 4096;

	private ConfigDefaults() {
	}

	public static JesterConfig create() {
		return new JesterConfig(
				SCHEMA_VERSION,
				new MapHudConfig(true, 12, 12, 1.0D, true, true, true, true, true),
				new VisibilityConfig(VisibilityMode.ALL_KNOWN, true)
		);
	}
}
