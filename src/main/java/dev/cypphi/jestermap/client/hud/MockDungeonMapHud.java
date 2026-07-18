package dev.cypphi.jestermap.client.hud;

import dev.cypphi.jestermap.JesterMap;
import dev.cypphi.jestermap.config.ConfigService;
import dev.cypphi.jestermap.config.JesterConfig;
import dev.cypphi.jestermap.dungeon.presentation.MockDungeonMapRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class MockDungeonMapHud {
	private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(JesterMap.MOD_ID, "mock_dungeon_map");
	private static final MockDungeonMapRenderer RENDERER = new MockDungeonMapRenderer();

	private MockDungeonMapHud() {
	}

	public static void initialize() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.MOB_EFFECTS, HUD_ID, (graphics, ignored) -> {
			Minecraft client = Minecraft.getInstance();
			JesterConfig config = ConfigService.getInstance().current();
			if (client.player == null || !config.mapHud().enabled()) {
				return;
			}
			RENDERER.render(
					graphics,
					config.mapHud().x(),
					config.mapHud().y(),
					(float) config.mapHud().scale(),
					config.mapHud(),
					config.visibility(),
					client.player.getYRot()
			);
		});
	}
}
