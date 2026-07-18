package dev.cypphi.jestermap.config;

import dev.cypphi.jestermap.JesterMap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public final class ConfigRepository {
	public static final String CONFIG_DIRECTORY = "jester-map";
	public static final String CONFIG_FILE = "config.json";

	private final Path configPath;
	private final Path backupPath;
	private final Path brokenPath;
	private final ConfigCodec codec;

	public ConfigRepository(Path configPath) {
		this(configPath, new ConfigCodec());
	}

	ConfigRepository(Path configPath, ConfigCodec codec) {
		this.configPath = configPath;
		this.backupPath = configPath.resolveSibling(CONFIG_FILE + ".bak");
		this.brokenPath = configPath.resolveSibling(CONFIG_FILE + ".broken");
		this.codec = codec;
	}

	public static Path configPath(Path fabricConfigDirectory) {
		return fabricConfigDirectory.resolve(CONFIG_DIRECTORY).resolve(CONFIG_FILE);
	}

	public JesterConfig load() throws IOException {
		if (Files.notExists(configPath)) {
			JesterConfig defaults = ConfigDefaults.create();
			writeAtomic(defaults, false);
			return defaults;
		}

		try {
			return codec.decode(Files.readString(configPath, StandardCharsets.UTF_8));
		} catch (RuntimeException exception) {
			return recoverFromInvalidConfig(exception);
		}
	}

	public void save(JesterConfig config) throws IOException {
		writeAtomic(ConfigValidator.validate(config), true);
	}

	public Path path() {
		return configPath;
	}

	private JesterConfig recoverFromInvalidConfig(RuntimeException cause) throws IOException {
		Files.move(configPath, brokenPath, StandardCopyOption.REPLACE_EXISTING);
		Optional<JesterConfig> backup = loadBackup();
		JesterConfig recovered = backup.orElseGet(ConfigDefaults::create);
		writeAtomic(recovered, false);
		JesterMap.LOGGER.warn("Invalid Jester Map config moved to {}. Recovered {}.",
				brokenPath, backup.isPresent() ? "the last valid backup" : "defaults", cause);
		return recovered;
	}

	private Optional<JesterConfig> loadBackup() {
		if (Files.notExists(backupPath)) {
			return Optional.empty();
		}
		try {
			return Optional.of(codec.decode(Files.readString(backupPath, StandardCharsets.UTF_8)));
		} catch (IOException | RuntimeException exception) {
			JesterMap.LOGGER.warn("Could not recover Jester Map config backup at {}.", backupPath, exception);
			return Optional.empty();
		}
	}

	private void writeAtomic(JesterConfig config, boolean preserveCurrent) throws IOException {
		Path directory = configPath.getParent();
		Files.createDirectories(directory);
		Path temporaryPath = Files.createTempFile(directory, "config-", ".tmp");
		try {
			byte[] bytes = codec.encode(config).getBytes(StandardCharsets.UTF_8);
			try (FileChannel channel = FileChannel.open(temporaryPath, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
				channel.write(ByteBuffer.wrap(bytes));
				channel.force(true);
			}
			if (preserveCurrent && Files.exists(configPath)) {
				Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
			}
			moveIntoPlace(temporaryPath);
		} finally {
			Files.deleteIfExists(temporaryPath);
		}
	}

	private void moveIntoPlace(Path temporaryPath) throws IOException {
		try {
			Files.move(temporaryPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(temporaryPath, configPath, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
