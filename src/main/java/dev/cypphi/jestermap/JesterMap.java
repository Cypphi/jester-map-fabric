package dev.cypphi.jestermap;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JesterMap {
	public static final String MOD_ID = "jester_map";
	public static final String MOD_NAME = "Jester Map";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

	private JesterMap() {
	}

	public static void initialize(String targetMinecraftVersion) {
		if (INITIALIZED.compareAndSet(false, true)) {
			LOGGER.info("{} initialized for Minecraft {}", MOD_NAME, targetMinecraftVersion);
		}
	}
}
