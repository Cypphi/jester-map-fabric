package dev.cypphi.jestermap.client.hud;

import dev.cypphi.jestermap.JesterMap;
import dev.cypphi.jestermap.config.ConfigService;
import dev.cypphi.jestermap.config.JesterConfig;
import dev.cypphi.jestermap.dungeon.map.DungeonMapSnapshot;
import dev.cypphi.jestermap.dungeon.presentation.DungeonMapRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class DungeonMapHud {
	private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(JesterMap.MOD_ID, "dungeon_map");
	private static final DungeonMapRenderer RENDERER = new DungeonMapRenderer();

	private DungeonMapHud() {
	}

	public static void initialize() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.MOB_EFFECTS, HUD_ID, (graphics, ignored) -> {
			Minecraft client = Minecraft.getInstance();
			JesterConfig config = ConfigService.getInstance().current();
			DungeonMapSnapshot snapshot = DungeonMapController.snapshot();
			if (client.player == null || !config.mapHud().enabled() || !snapshot.available()) {
				return;
			}
			RENDERER.render(
					graphics,
					config.mapHud().x(),
					config.mapHud().y(),
					(float) config.mapHud().scale(),
					config.mapHud(),
					config.visibility(),
					client.player.getYRot(),
					snapshot
			);
		});
	}
}
