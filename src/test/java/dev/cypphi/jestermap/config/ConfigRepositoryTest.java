package dev.cypphi.jestermap.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigRepositoryTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void createsExpectedConfigPathAndPersistsAtomically() throws Exception {
		Path fabricConfig = temporaryDirectory.resolve("config");
		Path expectedPath = fabricConfig.resolve("jester-map/config.json");
		ConfigRepository repository = new ConfigRepository(ConfigRepository.configPath(fabricConfig));

		JesterConfig defaults = repository.load();
		JesterConfig changed = defaults.withMapHud(defaults.mapHud().withScale(1.4D));
		repository.save(changed);

		assertEquals(expectedPath, repository.path());
		assertEquals(changed, repository.load());
		assertTrue(Files.readString(expectedPath).contains("\n  \"mapHud\""));
		assertTrue(Files.exists(expectedPath.resolveSibling("config.json.bak")));
		try (var files = Files.list(expectedPath.getParent())) {
			assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
		}
	}

	@Test
	void preservesMalformedFileAndRecoversLastValidBackup() throws Exception {
		Path path = temporaryDirectory.resolve("config/jester-map/config.json");
		ConfigRepository repository = new ConfigRepository(path);
		JesterConfig defaults = repository.load();
		repository.save(defaults.withMapHud(defaults.mapHud().withEnabled(false)));
		Files.writeString(path, "{broken", StandardCharsets.UTF_8);

		JesterConfig recovered = repository.load();

		assertEquals(defaults, recovered);
		assertEquals("{broken", Files.readString(path.resolveSibling("config.json.broken")));
		assertEquals(defaults, repository.load());
	}
}
