package dev.cypphi.jestermap.config;

import dev.cypphi.jestermap.JesterMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class ConfigService {
	private static final ConfigService INSTANCE = new ConfigService();

	private final AtomicReference<JesterConfig> current = new AtomicReference<>(ConfigDefaults.create());
	private final List<Consumer<JesterConfig>> listeners = new CopyOnWriteArrayList<>();
	private ConfigRepository repository;

	public static ConfigService getInstance() {
		return INSTANCE;
	}

	private ConfigService() {
	}

	public synchronized void initialize(Path fabricConfigDirectory) {
		if (repository != null) {
			return;
		}
		repository = new ConfigRepository(ConfigRepository.configPath(fabricConfigDirectory));
		try {
			current.set(repository.load());
			JesterMap.LOGGER.info("Loaded Jester Map config from {}", repository.path());
		} catch (IOException exception) {
			JesterMap.LOGGER.error("Could not load Jester Map config from {}; using defaults for this session.", repository.path(), exception);
		}
	}

	public JesterConfig current() {
		return current.get();
	}

	public synchronized void save(JesterConfig replacement) throws IOException {
		requireRepository();
		JesterConfig validated = ConfigValidator.validate(replacement);
		repository.save(validated);
		setCurrent(validated);
	}

	public synchronized void updateMapHud(UnaryOperator<MapHudConfig> update) {
		JesterConfig existing = current.get();
		MapHudConfig updatedMap = update.apply(existing.mapHud());
		if (updatedMap == null) {
			throw new IllegalArgumentException("Map HUD update cannot return null");
		}
		setCurrent(ConfigValidator.validate(existing.withMapHud(updatedMap)));
	}

	public synchronized void saveCurrent() throws IOException {
		requireRepository();
		repository.save(current.get());
	}

	public void addListener(Consumer<JesterConfig> listener) {
		listeners.add(listener);
	}

	private void requireRepository() {
		if (repository == null) {
			throw new IllegalStateException("Config service has not been initialized");
		}
	}

	private void setCurrent(JesterConfig replacement) {
		current.set(replacement);
		listeners.forEach(listener -> listener.accept(replacement));
	}
}
