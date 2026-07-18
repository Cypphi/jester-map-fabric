package dev.cypphi.jestermap;

import dev.cypphi.jestermap.client.command.JesterClientCommands;
import dev.cypphi.jestermap.client.hud.MockDungeonMapHud;
import dev.cypphi.jestermap.config.ConfigService;
import net.fabricmc.loader.api.FabricLoader;

import java.util.concurrent.atomic.AtomicBoolean;

public final class JesterMapClient {
	private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

	private JesterMapClient() {
	}

	public static void initialize(String targetMinecraftVersion) {
		if (INITIALIZED.compareAndSet(false, true)) {
			ConfigService.getInstance().initialize(FabricLoader.getInstance().getConfigDir());
			JesterClientCommands.initialize();
			MockDungeonMapHud.initialize();
			JesterMap.LOGGER.info("{} client initialized for Minecraft {}", JesterMap.MOD_NAME, targetMinecraftVersion);
		}
	}
}
