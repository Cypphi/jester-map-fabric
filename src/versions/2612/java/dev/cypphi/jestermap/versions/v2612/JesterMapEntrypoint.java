package dev.cypphi.jestermap.versions.v2612;

import dev.cypphi.jestermap.JesterMap;
import dev.cypphi.jestermap.JesterMapClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public final class JesterMapEntrypoint implements ModInitializer, ClientModInitializer {
	private static final String TARGET_MINECRAFT_VERSION = "26.1.2";

	@Override
	public void onInitialize() {
		JesterMap.initialize(TARGET_MINECRAFT_VERSION);
	}

	@Override
	public void onInitializeClient() {
		JesterMapClient.initialize(TARGET_MINECRAFT_VERSION);
	}
}
