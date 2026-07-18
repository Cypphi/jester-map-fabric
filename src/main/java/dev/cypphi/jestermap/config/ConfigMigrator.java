package dev.cypphi.jestermap.config;

final class ConfigMigrator {
	private ConfigMigrator() {
	}

	static int migrateSchemaVersion(Integer schemaVersion) {
		int sourceVersion = schemaVersion == null ? 0 : schemaVersion;
		if (sourceVersion < 0 || sourceVersion > ConfigDefaults.SCHEMA_VERSION) {
			throw new IllegalArgumentException("Unsupported config schema version " + sourceVersion);
		}
		return ConfigDefaults.SCHEMA_VERSION;
	}
}
