package dev.cypphi.jestermap.config;

import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigCodecTest {
	private final ConfigCodec codec = new ConfigCodec();

	@Test
	void showsAllKnownRoomsByDefault() {
		assertEquals(VisibilityMode.ALL_KNOWN, ConfigDefaults.create().visibility().mode());
	}

	@Test
	void roundTripsPrettyPrintedConfig() {
		JesterConfig config = new JesterConfig(
				ConfigDefaults.SCHEMA_VERSION,
				new MapHudConfig(false, 140, -22, 1.35D, false, false, true, false, true),
				new VisibilityConfig(VisibilityMode.ALL_KNOWN, false)
		);

		String json = codec.encode(config);

		assertTrue(json.contains("\n  \"schemaVersion\""));
		assertTrue(json.contains("\"all_known\""));
		assertEquals(config, codec.decode(json));
	}

	@Test
	void suppliesDefaultsForMissingSettings() {
		JesterConfig config = codec.decode("{\"schemaVersion\":1,\"mapHud\":{\"scale\":1.5}}");

		assertEquals(1.5D, config.mapHud().scale());
		assertEquals(ConfigDefaults.create().mapHud().rotateWithPlayer(), config.mapHud().rotateWithPlayer());
		assertEquals(ConfigDefaults.create().mapHud().showTeammates(), config.mapHud().showTeammates());
		assertEquals(ConfigDefaults.create().visibility(), config.visibility());
	}

	@Test
	void rejectsFutureSchemaVersions() {
		assertThrows(IllegalArgumentException.class, () -> codec.decode("{\"schemaVersion\":999}"));
	}

	@Test
	void rejectsMalformedJson() {
		assertThrows(JsonParseException.class, () -> codec.decode("{not json"));
	}
}
