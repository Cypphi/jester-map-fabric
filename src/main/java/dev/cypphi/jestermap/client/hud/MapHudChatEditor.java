package dev.cypphi.jestermap.client.hud;

import dev.cypphi.jestermap.JesterMap;
import dev.cypphi.jestermap.config.ConfigDefaults;
import dev.cypphi.jestermap.config.ConfigService;
import dev.cypphi.jestermap.config.MapHudConfig;
import dev.cypphi.jestermap.dungeon.presentation.MapHudLayout;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

import java.io.IOException;

public final class MapHudChatEditor {
	private static final double SCALE_STEP = 0.05D;
	private static MapHudChatEditor activeEditor;

	private final Minecraft client;
	private final Screen screen;
	private boolean dragging;
	private boolean dirty;
	private double dragOffsetX;
	private double dragOffsetY;

	private MapHudChatEditor(Minecraft client, Screen screen) {
		this.client = client;
		this.screen = screen;
	}

	public static void initialize() {
		ScreenEvents.BEFORE_INIT.register((client, screen, ignoredWidth, ignoredHeight) -> {
			if (!(screen instanceof ChatScreen)) {
				return;
			}

			MapHudChatEditor editor = editorFor(client, screen);
			ScreenMouseEvents.allowMouseClick(screen).register((currentScreen, event) ->
					!editor.mouseClicked(event));
			ScreenMouseEvents.allowMouseDrag(screen).register((currentScreen, event, deltaX, deltaY) ->
					!editor.mouseDragged(currentScreen, event));
			ScreenMouseEvents.allowMouseRelease(screen).register((currentScreen, event) ->
					!editor.mouseReleased(event));
			ScreenMouseEvents.allowMouseScroll(screen).register((currentScreen, mouseX, mouseY, horizontal, vertical) ->
					!editor.mouseScrolled(currentScreen, mouseX, mouseY, vertical));
			ScreenEvents.remove(screen).register(ignored -> closeEditor(editor));
		});
	}

	private static MapHudChatEditor editorFor(Minecraft client, Screen screen) {
		if (activeEditor == null || activeEditor.screen != screen) {
			if (activeEditor != null) {
				activeEditor.close();
			}
			activeEditor = new MapHudChatEditor(client, screen);
		}
		return activeEditor;
	}

	private static void closeEditor(MapHudChatEditor editor) {
		if (activeEditor == editor) {
			editor.close();
			activeEditor = null;
		}
	}

	private boolean mouseClicked(MouseButtonEvent event) {
		if (event.button() != 0 || !isEditable()) {
			return false;
		}

		MapHudConfig config = currentMapConfig();
		if (!MapHudLayout.bounds(config).contains(event.x(), event.y())) {
			return false;
		}
		dragging = true;
		dragOffsetX = event.x() - config.x();
		dragOffsetY = event.y() - config.y();
		return true;
	}

	private boolean mouseDragged(Screen screen, MouseButtonEvent event) {
		if (event.button() != 0 || !dragging) {
			return false;
		}

		MapHudConfig config = currentMapConfig();
		MapHudLayout.Position position = MapHudLayout.draggedPosition(
				event.x() - dragOffsetX,
				event.y() - dragOffsetY,
				config.scale(),
				screen.width,
				screen.height,
				client.hasShiftDown()
		);
		updateMapConfig(config.withPosition(position.x(), position.y()));
		dirty = true;
		return true;
	}

	private boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() != 0 || !dragging) {
			return false;
		}
		dragging = false;
		saveIfDirty();
		return true;
	}

	private boolean mouseScrolled(Screen screen, double mouseX, double mouseY, double verticalAmount) {
		if (verticalAmount == 0.0D || !isEditable()) {
			return false;
		}

		MapHudConfig config = currentMapConfig();
		if (!MapHudLayout.bounds(config).contains(mouseX, mouseY)) {
			return false;
		}
		double scale = Math.clamp(
				config.scale() + Math.copySign(SCALE_STEP, verticalAmount),
				ConfigDefaults.MIN_SCALE,
				ConfigDefaults.MAX_SCALE
		);
		scale = Math.round(scale / SCALE_STEP) * SCALE_STEP;
		if (scale != config.scale()) {
			MapHudLayout.Position position = MapHudLayout.resizedAroundPointer(
					config,
					scale,
					mouseX,
					mouseY,
					screen.width,
					screen.height
			);
			updateMapConfig(config.withScale(scale).withPosition(position.x(), position.y()));
			dirty = true;
			saveIfDirty();
		}
		return true;
	}

	private boolean isEditable() {
		return client.player != null && currentMapConfig().enabled();
	}

	private MapHudConfig currentMapConfig() {
		return ConfigService.getInstance().current().mapHud();
	}

	private void updateMapConfig(MapHudConfig replacement) {
		ConfigService.getInstance().updateMapHud(ignored -> replacement);
	}

	private void close() {
		dragging = false;
		saveIfDirty();
	}

	private void saveIfDirty() {
		if (!dirty) {
			return;
		}
		try {
			ConfigService.getInstance().saveCurrent();
			dirty = false;
		} catch (IOException | RuntimeException exception) {
			JesterMap.LOGGER.error("Could not save the edited map HUD position and scale.", exception);
		}
	}
}
