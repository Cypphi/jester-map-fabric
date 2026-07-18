package dev.cypphi.jestermap.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigValidatorTest {
	@Test
	void clampsPersistedGeometryAndRepairsNulls() {
		JesterConfig validated = ConfigValidator.validate(new JesterConfig(
				42,
				new MapHudConfig(true, Integer.MIN_VALUE, Integer.MAX_VALUE, Double.NaN, true, true, true, true, true),
				new VisibilityConfig(null, false)
		));

		assertEquals(ConfigDefaults.SCHEMA_VERSION, validated.schemaVersion());
		assertEquals(ConfigDefaults.MIN_POSITION, validated.mapHud().x());
		assertEquals(ConfigDefaults.MAX_POSITION, validated.mapHud().y());
		assertEquals(ConfigDefaults.create().mapHud().scale(), validated.mapHud().scale());
		assertEquals(ConfigDefaults.create().visibility().mode(), validated.visibility().mode());
	}
}
