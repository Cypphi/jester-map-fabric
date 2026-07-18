package dev.cypphi.jestermap.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public final class ConfigCodec {
	private final Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	public String encode(JesterConfig config) {
		JesterConfig validated = ConfigValidator.validate(config);
		PersistedConfig persisted = new PersistedConfig();
		persisted.schemaVersion = validated.schemaVersion();
		persisted.mapHud = PersistedMapHud.from(validated.mapHud());
		persisted.visibility = PersistedVisibility.from(validated.visibility());
		persisted.ui = PersistedUi.from(validated.ui());
		return gson.toJson(persisted) + System.lineSeparator();
	}

	public JesterConfig decode(String json) {
		if (json == null || json.isBlank()) {
			throw new JsonParseException("Config file is empty");
		}
		PersistedConfig persisted = gson.fromJson(json, PersistedConfig.class);
		if (persisted == null) {
			throw new JsonParseException("Config root must be an object");
		}

		int schemaVersion = ConfigMigrator.migrateSchemaVersion(persisted.schemaVersion);
		JesterConfig defaults = ConfigDefaults.create();
		return ConfigValidator.validate(new JesterConfig(
				schemaVersion,
				persisted.mapHud == null ? defaults.mapHud() : persisted.mapHud.toConfig(defaults.mapHud()),
				persisted.visibility == null ? defaults.visibility() : persisted.visibility.toConfig(defaults.visibility()),
				persisted.ui == null ? defaults.ui() : persisted.ui.toConfig(defaults.ui())
		));
	}

	private static final class PersistedConfig {
		private Integer schemaVersion;
		private PersistedMapHud mapHud;
		private PersistedVisibility visibility;
		private PersistedUi ui;
	}

	private static final class PersistedMapHud {
		private Boolean enabled;
		private Integer x;
		private Integer y;
		private Double scale;
		private Boolean rotateWithPlayer;
		private Boolean backgroundBlur;
		private Boolean showRoomNames;
		private Boolean showSecretCounts;
		private Boolean showTeammates;

		private static PersistedMapHud from(MapHudConfig config) {
			PersistedMapHud persisted = new PersistedMapHud();
			persisted.enabled = config.enabled();
			persisted.x = config.x();
			persisted.y = config.y();
			persisted.scale = config.scale();
			persisted.rotateWithPlayer = config.rotateWithPlayer();
			persisted.backgroundBlur = config.backgroundBlur();
			persisted.showRoomNames = config.showRoomNames();
			persisted.showSecretCounts = config.showSecretCounts();
			persisted.showTeammates = config.showTeammates();
			return persisted;
		}

		private MapHudConfig toConfig(MapHudConfig defaults) {
			return new MapHudConfig(
					enabled == null ? defaults.enabled() : enabled,
					x == null ? defaults.x() : x,
					y == null ? defaults.y() : y,
					scale == null ? defaults.scale() : scale,
					rotateWithPlayer == null ? defaults.rotateWithPlayer() : rotateWithPlayer,
					backgroundBlur == null ? defaults.backgroundBlur() : backgroundBlur,
					showRoomNames == null ? defaults.showRoomNames() : showRoomNames,
					showSecretCounts == null ? defaults.showSecretCounts() : showSecretCounts,
					showTeammates == null ? defaults.showTeammates() : showTeammates
			);
		}
	}

	private static final class PersistedVisibility {
		private VisibilityMode mode;
		private Boolean darkenUndiscoveredDoors;

		private static PersistedVisibility from(VisibilityConfig config) {
			PersistedVisibility persisted = new PersistedVisibility();
			persisted.mode = config.mode();
			persisted.darkenUndiscoveredDoors = config.darkenUndiscoveredDoors();
			return persisted;
		}

		private VisibilityConfig toConfig(VisibilityConfig defaults) {
			return new VisibilityConfig(
					mode == null ? defaults.mode() : mode,
					darkenUndiscoveredDoors == null ? defaults.darkenUndiscoveredDoors() : darkenUndiscoveredDoors
			);
		}
	}

	private static final class PersistedUi {
		private Boolean customFont;
		private FontFamily fontFamily;

		private static PersistedUi from(UiConfig config) {
			PersistedUi persisted = new PersistedUi();
			persisted.customFont = config.customFont();
			persisted.fontFamily = config.fontFamily();
			return persisted;
		}

		private UiConfig toConfig(UiConfig defaults) {
			return new UiConfig(
					customFont == null ? defaults.customFont() : customFont,
					fontFamily == null ? defaults.fontFamily() : fontFamily
			);
		}
	}
}
