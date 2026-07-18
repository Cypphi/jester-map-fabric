package dev.cypphi.jestermap.dungeon.presentation;

import dev.cypphi.jestermap.config.MapHudConfig;
import dev.cypphi.jestermap.config.VisibilityConfig;
import dev.cypphi.jestermap.render.Renderer2d;
import dev.cypphi.jestermap.render.minecraft.MinecraftRenderBackend2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/** Temporary synthetic Catacombs snapshot used to exercise the HUD renderer before live detection lands. */
public final class MockDungeonMapRenderer {
	public static final float WIDTH = 148.0F;
	public static final float HEIGHT = 164.0F;

	private static final float MAP_OFFSET = 10.0F;
	private static final float MAP_SIZE = 128.0F;
	private static final float MAP_CENTER = MAP_OFFSET + MAP_SIZE * 0.5F;
	private static final float CELL_SIZE = 16.0F;
	private static final float CELL_STEP = 20.0F;
	private static final float GRID_OFFSET = 6.0F;
	private static final int PANEL_COLOR = 0xE60A0F0C;
	private static final int PANEL_OUTLINE = 0xB24C6252;
	private static final int MAP_BACKGROUND = 0xE6121914;
	private static final int ROOM_OUTLINE = 0xA02A332C;
	private static final int DOOR_COLOR = 0xFF241D18;
	private static final int HIDDEN_DOOR_COLOR = 0xFF111512;
	private static final int TEXT_COLOR = 0xFFF2F5F1;
	private static final int MUTED_TEXT_COLOR = 0xFFAAB5AD;

	private static final List<Room> ROOMS = List.of(
			room("ENT", "0/0", 0xFF63C56C, Check.GREEN, cell(3, 5)),
			room("A", "3/5", 0xFF76584A, Check.GREEN, cell(3, 4)),
			room("L", "5/7", 0xFF795A4D, Check.WHITE, cell(2, 3), cell(3, 3), cell(3, 2)),
			room("PUZ", "1/1", 0xFFA15FC0, Check.GREEN, cell(1, 3), cell(1, 2)),
			room("?", "?/6", 0xFF4C5550, Check.NONE, cell(0, 3)),
			room("TRAP", "2/4", 0xFFD07B3E, Check.WHITE, cell(4, 2)),
			room("B", "4/6", 0xFF795A4D, Check.NONE, cell(4, 3), cell(4, 4), cell(5, 4)),
			room("FAIRY", "0/0", 0xFFD783AA, Check.NONE, cell(2, 1)),
			room("BLOOD", "0/0", 0xFFC84949, Check.NONE, cell(4, 1)),
			room("BOSS", "0/0", 0xFF9E3438, Check.NONE, cell(4, 0), cell(5, 0))
	);

	private static final List<Door> DOORS = List.of(
			door(3, 5, 3, 4, false),
			door(3, 4, 3, 3, false),
			door(2, 3, 1, 3, false),
			door(1, 3, 0, 3, true),
			door(3, 2, 4, 2, false),
			door(4, 2, 4, 1, true),
			door(4, 1, 4, 0, true),
			door(4, 3, 4, 2, false),
			door(4, 4, 3, 4, false),
			door(2, 1, 2, 2, false)
	);

	private final MinecraftRenderBackend2d backend = new MinecraftRenderBackend2d();
	private final Renderer2d renderer = new Renderer2d(backend);

	public void render(
			GuiGraphicsExtractor graphics,
			float x,
			float y,
			float scale,
			MapHudConfig mapConfig,
			VisibilityConfig visibilityConfig,
			float playerYaw
	) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		backend.beginExtraction(graphics);
		try {
			renderPanel();
			graphics.pose().pushMatrix();
			graphics.pose().translate(MAP_CENTER, MAP_CENTER);
			if (mapConfig.rotateWithPlayer()) {
				graphics.pose().rotate(MapRotation.mapRadians(playerYaw));
			}
			graphics.pose().translate(-MAP_SIZE * 0.5F, -MAP_SIZE * 0.5F);
			renderMap(graphics, mapConfig, visibilityConfig);
			graphics.pose().popMatrix();

			renderLocalMarker(playerYaw, mapConfig.rotateWithPlayer());
			renderStatus(graphics);
		} finally {
			backend.endExtraction();
			graphics.pose().popMatrix();
		}
	}

	private void renderPanel() {
		renderer.beginFrame();
		renderer.drawRoundedRectShadow(0.0F, 0.0F, WIDTH, HEIGHT, 10.0F, 5.0F, 0x72000000);
		renderer.drawRoundedRect(0.0F, 0.0F, WIDTH, HEIGHT, 10.0F, PANEL_COLOR);
		renderer.drawRoundedRectOutline(0.0F, 0.0F, WIDTH, HEIGHT, 10.0F, PANEL_OUTLINE, 1.0F);
		renderer.endFrame();
	}

	private void renderMap(GuiGraphicsExtractor graphics, MapHudConfig mapConfig, VisibilityConfig visibilityConfig) {
		renderer.beginFrame();
		renderer.drawRoundedRect(0.0F, 0.0F, MAP_SIZE, MAP_SIZE, 6.0F, MAP_BACKGROUND);
		renderer.drawRoundedRectOutline(0.0F, 0.0F, MAP_SIZE, MAP_SIZE, 6.0F, 0x70384A3D, 1.0F);

		for (Door door : DOORS) {
			renderDoor(door, visibilityConfig.darkenUndiscoveredDoors());
		}
		for (Room room : ROOMS) {
			renderRoom(room);
		}
		if (mapConfig.showTeammates()) {
			renderTeammate(2, 3, 0xFF63B7FF);
			renderTeammate(4, 4, 0xFFFFC857);
			renderTeammate(4, 1, 0xFFE777D2);
		}
		renderer.endFrame();

		if (mapConfig.showRoomNames() || mapConfig.showSecretCounts()) {
			for (Room room : ROOMS) {
				renderRoomText(graphics, room, mapConfig);
			}
		}
	}

	private void renderRoom(Room room) {
		for (Cell cell : room.cells()) {
			float x = cellX(cell.x());
			float y = cellY(cell.y());
			renderer.drawRoundedRectShadow(x, y, CELL_SIZE, CELL_SIZE, 3.0F, 1.5F, 0x55000000);
			renderer.drawOutlinedRoundedRect(x, y, CELL_SIZE, CELL_SIZE, 3.0F, room.color(), ROOM_OUTLINE, 0.8F);
		}

		for (Cell first : room.cells()) {
			for (Cell second : room.cells()) {
				if (second.x() == first.x() + 1 && second.y() == first.y()) {
					renderer.drawRectangle(cellX(first.x()) + CELL_SIZE - 1.0F, cellY(first.y()) + 2.0F, CELL_STEP - CELL_SIZE + 2.0F, CELL_SIZE - 4.0F, room.color());
				}
				if (second.y() == first.y() + 1 && second.x() == first.x()) {
					renderer.drawRectangle(cellX(first.x()) + 2.0F, cellY(first.y()) + CELL_SIZE - 1.0F, CELL_SIZE - 4.0F, CELL_STEP - CELL_SIZE + 2.0F, room.color());
				}
			}
		}

		if (room.check() != Check.NONE) {
			float centerX = roomCenterX(room);
			float centerY = roomCenterY(room);
			int color = room.check() == Check.GREEN ? 0xFF78F08F : 0xFFF3F5F2;
			renderer.drawLine(centerX - 3.0F, centerY, centerX - 0.5F, centerY + 2.5F, 1.8F, color);
			renderer.drawLine(centerX - 0.5F, centerY + 2.5F, centerX + 4.0F, centerY - 3.0F, 1.8F, color);
		}
	}

	private void renderDoor(Door door, boolean darkenHidden) {
		int color = door.hidden() && darkenHidden ? HIDDEN_DOOR_COLOR : DOOR_COLOR;
		float firstX = cellX(door.first().x());
		float firstY = cellY(door.first().y());
		if (door.first().x() != door.second().x()) {
			float x = Math.min(firstX, cellX(door.second().x())) + CELL_SIZE;
			renderer.drawRoundedRect(x - 1.0F, firstY + 5.0F, CELL_STEP - CELL_SIZE + 2.0F, 6.0F, 2.0F, color);
		} else {
			float y = Math.min(firstY, cellY(door.second().y())) + CELL_SIZE;
			renderer.drawRoundedRect(firstX + 5.0F, y - 1.0F, 6.0F, CELL_STEP - CELL_SIZE + 2.0F, 2.0F, color);
		}
	}

	private void renderTeammate(int gridX, int gridY, int color) {
		float x = cellX(gridX) + CELL_SIZE * 0.5F;
		float y = cellY(gridY) + CELL_SIZE * 0.5F;
		renderer.drawCircle(x, y, 3.5F, 0xD9000000);
		renderer.drawCircle(x, y, 2.6F, color);
		renderer.drawLine(x, y - 2.0F, x, y - 5.0F, 1.2F, color);
	}

	private void renderLocalMarker(float playerYaw, boolean rotatingMap) {
		renderer.beginFrame();
		renderer.drawCircle(MAP_CENTER, MAP_CENTER, 5.0F, 0xB8000000);
		renderer.drawOutlinedRegularPolygon(
				MAP_CENTER,
				MAP_CENTER,
				4.0F,
				3,
				MapRotation.markerRadians(playerYaw, rotatingMap),
				0xFF82F19B,
				0xFFE7FFEC,
				0.8F
		);
		renderer.endFrame();
	}

	private void renderRoomText(GuiGraphicsExtractor graphics, Room room, MapHudConfig config) {
		String label = config.showRoomNames() ? room.label() : "";
		String secrets = config.showSecretCounts() ? room.secrets() : "";
		if (!label.isEmpty()) {
			drawCenteredScaledText(graphics, label, roomCenterX(room), roomCenterY(room) - (secrets.isEmpty() ? 1.5F : 4.0F), 0.42F, TEXT_COLOR);
		}
		if (!secrets.isEmpty()) {
			drawCenteredScaledText(graphics, secrets, roomCenterX(room), roomCenterY(room) + 2.0F, 0.38F, MUTED_TEXT_COLOR);
		}
	}

	private void renderStatus(GuiGraphicsExtractor graphics) {
		renderer.beginFrame();
		renderer.drawRoundedRect(8.0F, 143.0F, WIDTH - 16.0F, 14.0F, 5.0F, 0xB51A241D);
		renderer.endFrame();
		drawCenteredScaledText(graphics, "Secrets 13/29   •   Score 287", WIDTH * 0.5F, 147.0F, 0.62F, MUTED_TEXT_COLOR);
	}

	private static void drawCenteredScaledText(GuiGraphicsExtractor graphics, String text, float centerX, float y, float scale, int color) {
		Minecraft client = Minecraft.getInstance();
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, y);
		graphics.pose().scale(scale, scale);
		graphics.text(client.font, text, -client.font.width(text) / 2, 0, color, true);
		graphics.pose().popMatrix();
	}

	private static float roomCenterX(Room room) {
		return (float) room.cells().stream().mapToDouble(cell -> cellX(cell.x()) + CELL_SIZE * 0.5F).average().orElse(MAP_SIZE * 0.5F);
	}

	private static float roomCenterY(Room room) {
		return (float) room.cells().stream().mapToDouble(cell -> cellY(cell.y()) + CELL_SIZE * 0.5F).average().orElse(MAP_SIZE * 0.5F);
	}

	private static float cellX(int gridX) {
		return GRID_OFFSET + gridX * CELL_STEP;
	}

	private static float cellY(int gridY) {
		return GRID_OFFSET + gridY * CELL_STEP;
	}

	private static Cell cell(int x, int y) {
		return new Cell(x, y);
	}

	private static Room room(String label, String secrets, int color, Check check, Cell... cells) {
		return new Room(label, secrets, color, check, List.of(cells));
	}

	private static Door door(int firstX, int firstY, int secondX, int secondY, boolean hidden) {
		return new Door(cell(firstX, firstY), cell(secondX, secondY), hidden);
	}

	private enum Check {
		NONE,
		WHITE,
		GREEN
	}

	private record Cell(int x, int y) {
	}

	private record Room(String label, String secrets, int color, Check check, List<Cell> cells) {
	}

	private record Door(Cell first, Cell second, boolean hidden) {
	}
}
