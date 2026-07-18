package dev.cypphi.jestermap.dungeon.presentation;

import dev.cypphi.jestermap.config.MapHudConfig;
import dev.cypphi.jestermap.config.VisibilityConfig;
import dev.cypphi.jestermap.config.VisibilityMode;
import dev.cypphi.jestermap.dungeon.map.DungeonMapSnapshot;
import dev.cypphi.jestermap.dungeon.map.DungeonRoomType;
import dev.cypphi.jestermap.render.Renderer2d;
import dev.cypphi.jestermap.render.minecraft.MinecraftRenderBackend2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class DungeonMapRenderer {
	public static final float WIDTH = MapHudLayout.BASE_WIDTH;
	public static final float HEIGHT = MapHudLayout.BASE_HEIGHT;

	private static final float MAP_OFFSET = 10.0F;
	private static final float MAP_SIZE = 128.0F;
	private static final float MAP_CENTER = MAP_SIZE * 0.5F;
	private static final int PANEL_COLOR = 0xE60A0F0C;
	private static final int PANEL_OUTLINE = 0xB24C6252;
	private static final int MAP_BACKGROUND = 0xE6121914;
	private static final int ROOM_OUTLINE = 0xA02A332C;
	private static final int TEXT_COLOR = 0xFFAAB5AD;

	private final MinecraftRenderBackend2d backend = new MinecraftRenderBackend2d();
	private final Renderer2d renderer = new Renderer2d(backend);

	public void render(
			GuiGraphicsExtractor graphics,
			float x,
			float y,
			float scale,
			MapHudConfig mapConfig,
			VisibilityConfig visibilityConfig,
			float playerYaw,
			DungeonMapSnapshot snapshot
	) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		backend.beginExtraction(graphics);
		try {
			renderPanel();
			graphics.pose().pushMatrix();
			graphics.pose().translate(MAP_OFFSET, MAP_OFFSET);
			renderMapBackground();
			graphics.pose().popMatrix();

			graphics.pose().pushMatrix();
			if (mapConfig.rotateWithPlayer() && snapshot.available()) {
				DungeonMapSnapshot.Marker localPlayer = localPlayerMarker(snapshot);
				float playerMapX = localPlayer == null ? MAP_CENTER : localPlayer.mapX();
				float playerMapY = localPlayer == null ? MAP_CENTER : localPlayer.mapY();
				graphics.pose().translate(MAP_OFFSET + MAP_CENTER, MAP_OFFSET + MAP_CENTER);
				graphics.pose().rotate(MapRotation.mapRadians(playerYaw));
				graphics.pose().translate(-playerMapX, -playerMapY);
			} else {
				graphics.pose().translate(MAP_OFFSET, MAP_OFFSET);
			}
			renderMapContent(
					snapshot,
					mapConfig,
					visibilityConfig,
					x + MAP_OFFSET * scale,
					y + MAP_OFFSET * scale,
					x + (MAP_OFFSET + MAP_SIZE) * scale,
					y + (MAP_OFFSET + MAP_SIZE) * scale
			);
			graphics.pose().popMatrix();

			graphics.pose().pushMatrix();
			graphics.pose().translate(MAP_OFFSET, MAP_OFFSET);
			renderMapOutline();
			graphics.pose().popMatrix();
		} finally {
			backend.endExtraction();
		}

		if (!snapshot.available()) {
			drawCenteredScaledText(graphics, "Waiting for Magical Map", WIDTH * 0.5F, HEIGHT * 0.5F - 3.0F, 0.65F, TEXT_COLOR);
		}
		graphics.pose().popMatrix();
	}

	private void renderPanel() {
		renderer.beginFrame();
		renderer.drawRoundedRectShadow(0.0F, 0.0F, WIDTH, HEIGHT, 10.0F, 5.0F, 0x72000000);
		renderer.drawRoundedRect(0.0F, 0.0F, WIDTH, HEIGHT, 10.0F, PANEL_COLOR);
		renderer.drawRoundedRectOutline(0.0F, 0.0F, WIDTH, HEIGHT, 10.0F, PANEL_OUTLINE, 1.0F);
		renderer.endFrame();
	}

	private void renderMapBackground() {
		renderer.beginFrame();
		renderer.drawRoundedRect(0.0F, 0.0F, MAP_SIZE, MAP_SIZE, 6.0F, MAP_BACKGROUND);
		renderer.endFrame();
	}

	private void renderMapOutline() {
		renderer.beginFrame();
		renderer.drawRoundedRectOutline(0.0F, 0.0F, MAP_SIZE, MAP_SIZE, 6.0F, 0x70384A3D, 1.0F);
		renderer.endFrame();
	}

	private void renderMapContent(
			DungeonMapSnapshot snapshot,
			MapHudConfig mapConfig,
			VisibilityConfig visibilityConfig,
			float clipMinX,
			float clipMinY,
			float clipMaxX,
			float clipMaxY
	) {
		renderer.beginFrame();
		if (!snapshot.available()) {
			renderer.endFrame();
			return;
		}
		renderer.enableClipRect(clipMinX, clipMinY, clipMaxX, clipMaxY);

		for (DungeonMapSnapshot.Door door : snapshot.doors()) {
			renderDoor(door, snapshot, visibilityConfig);
		}
		for (DungeonMapSnapshot.Room room : snapshot.rooms()) {
			if (room.type() != DungeonRoomType.UNKNOWN || visibilityConfig.mode() != VisibilityMode.DISCOVERED) {
				renderRoom(room, snapshot.calibration());
			}
		}
		for (DungeonMapSnapshot.Marker marker : snapshot.markers()) {
			if (marker.localPlayer() || mapConfig.showTeammates()) {
				renderMarker(marker);
			}
		}
		renderer.endFrame();
	}

	private static DungeonMapSnapshot.Marker localPlayerMarker(DungeonMapSnapshot snapshot) {
		return snapshot.markers().stream()
				.filter(DungeonMapSnapshot.Marker::localPlayer)
				.findFirst()
				.orElse(null);
	}

	private void renderRoom(DungeonMapSnapshot.Room room, DungeonMapSnapshot.Calibration calibration) {
		int color = roomColor(room.type());
		float size = calibration.roomSize();
		for (DungeonMapSnapshot.Cell cell : room.cells()) {
			renderer.drawRoundedRectShadow(cell.mapX(), cell.mapY(), size, size, 3.0F, 1.5F, 0x55000000);
			renderer.drawOutlinedRoundedRect(cell.mapX(), cell.mapY(), size, size, 3.0F, color, ROOM_OUTLINE, 0.8F);
		}

		for (DungeonMapSnapshot.Cell first : room.cells()) {
			for (DungeonMapSnapshot.Cell second : room.cells()) {
				if (second.mapX() == first.mapX() + calibration.step() && second.mapY() == first.mapY()) {
					renderer.drawRectangle(first.mapX() + size - 1.0F, first.mapY() + 2.0F,
							calibration.step() - size + 2.0F, size - 4.0F, color);
				}
				if (second.mapY() == first.mapY() + calibration.step() && second.mapX() == first.mapX()) {
					renderer.drawRectangle(first.mapX() + 2.0F, first.mapY() + size - 1.0F,
							size - 4.0F, calibration.step() - size + 2.0F, color);
				}
			}
		}

		if (room.clearState() != DungeonMapSnapshot.ClearState.UNCLEARED) {
			float centerX = (float) room.cells().stream().mapToDouble(cell -> cell.mapX() + size * 0.5F).average().orElse(MAP_CENTER);
			float centerY = (float) room.cells().stream().mapToDouble(cell -> cell.mapY() + size * 0.5F).average().orElse(MAP_CENTER);
			int checkColor = switch (room.clearState()) {
				case GREEN -> 0xFF78F08F;
				case FAILED -> 0xFFFF6B6B;
				case WHITE -> 0xFFF3F5F2;
				case UNCLEARED -> 0;
			};
			renderer.drawLine(centerX - 3.0F, centerY, centerX - 0.5F, centerY + 2.5F, 1.8F, checkColor);
			renderer.drawLine(centerX - 0.5F, centerY + 2.5F, centerX + 4.0F, centerY - 3.0F, 1.8F, checkColor);
		}
	}

	private void renderDoor(DungeonMapSnapshot.Door door, DungeonMapSnapshot snapshot, VisibilityConfig visibilityConfig) {
		int roomSize = snapshot.calibration().roomSize();
		boolean hiddenEndpoint = snapshot.rooms().stream()
				.filter(room -> room.cells().contains(door.first()) || room.cells().contains(door.second()))
				.anyMatch(room -> room.type() == DungeonRoomType.UNKNOWN);
		int color = hiddenEndpoint && visibilityConfig.darkenUndiscoveredDoors()
				? 0xFF111512
				: switch (door.type()) {
					case NORMAL -> 0xFF382B22;
					case WITHER -> 0xFF171B18;
					case BLOOD -> 0xFF7A2929;
				};
		if (door.first().mapX() != door.second().mapX()) {
			float x = Math.min(door.first().mapX(), door.second().mapX()) + roomSize;
			renderer.drawRoundedRect(x - 1.0F, door.first().mapY() + roomSize * 0.5F - 3.0F, 6.0F, 6.0F, 2.0F, color);
		} else {
			float y = Math.min(door.first().mapY(), door.second().mapY()) + roomSize;
			renderer.drawRoundedRect(door.first().mapX() + roomSize * 0.5F - 3.0F, y - 1.0F, 6.0F, 6.0F, 2.0F, color);
		}
	}

	private void renderMarker(DungeonMapSnapshot.Marker marker) {
		int color = marker.localPlayer() ? 0xFF82F19B : 0xFF63B7FF;
		renderer.drawCircle(marker.mapX(), marker.mapY(), marker.localPlayer() ? 4.8F : 4.0F, 0xB8000000);
		renderer.drawOutlinedRegularPolygon(
				marker.mapX(),
				marker.mapY(),
				marker.localPlayer() ? 3.8F : 3.2F,
				3,
				MapRotation.markerRadians(marker.worldYaw(), false),
				color,
				0xFFE7FFEC,
				0.7F
		);
	}

	private static int roomColor(DungeonRoomType type) {
		return switch (type) {
			case NORMAL -> 0xFF795A4D;
			case ENTRANCE -> 0xFF63C56C;
			case PUZZLE -> 0xFFA15FC0;
			case TRAP -> 0xFFD07B3E;
			case MINIBOSS -> 0xFF70905B;
			case FAIRY -> 0xFFD783AA;
			case BLOOD -> 0xFFC84949;
			case UNKNOWN -> 0xFF4C5550;
		};
	}

	private static void drawCenteredScaledText(GuiGraphicsExtractor graphics, String text, float centerX, float y, float scale, int color) {
		Minecraft client = Minecraft.getInstance();
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, y);
		graphics.pose().scale(scale, scale);
		graphics.text(client.font, text, -client.font.width(text) / 2, 0, color, true);
		graphics.pose().popMatrix();
	}
}
