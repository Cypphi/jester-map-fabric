package dev.cypphi.jestermap;

import java.util.concurrent.atomic.AtomicBoolean;

public final class JesterMapClient {
	private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

	private JesterMapClient() {
	}

	public static void initialize(String targetMinecraftVersion) {
		if (INITIALIZED.compareAndSet(false, true)) {
			JesterMap.LOGGER.info("{} client initialized for Minecraft {}", JesterMap.MOD_NAME, targetMinecraftVersion);
		}
	}
}
