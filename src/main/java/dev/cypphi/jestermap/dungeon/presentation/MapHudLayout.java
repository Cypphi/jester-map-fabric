package dev.cypphi.jestermap.dungeon.presentation;

import dev.cypphi.jestermap.config.MapHudConfig;

public final class MapHudLayout {
	public static final float BASE_WIDTH = 148.0F;
	public static final float BASE_HEIGHT = 164.0F;

	private MapHudLayout() {
	}

	public static Bounds bounds(MapHudConfig config) {
		return new Bounds(
				config.x(),
				config.y(),
				BASE_WIDTH * config.scale(),
				BASE_HEIGHT * config.scale()
		);
	}

	public static Position draggedPosition(
			double desiredX,
			double desiredY,
			double scale,
			int screenWidth,
			int screenHeight,
			boolean alignToClosestCorner
	) {
		double width = BASE_WIDTH * scale;
		double height = BASE_HEIGHT * scale;
		double maxX = Math.max(0.0D, screenWidth - width);
		double maxY = Math.max(0.0D, screenHeight - height);
		double x = Math.clamp(desiredX, 0.0D, maxX);
		double y = Math.clamp(desiredY, 0.0D, maxY);
		if (!alignToClosestCorner) {
			return roundedPosition(x, y, maxX, maxY);
		}

		boolean nearestRight = x + width * 0.5D > screenWidth * 0.5D;
		boolean nearestBottom = y + height * 0.5D > screenHeight * 0.5D;
		double horizontalDistance = nearestRight ? screenWidth - x - width : x;
		double verticalDistance = nearestBottom ? screenHeight - y - height : y;
		double sharedDistance = Math.clamp(
				(horizontalDistance + verticalDistance) * 0.5D,
				0.0D,
				Math.min(maxX, maxY)
		);
		x = nearestRight ? maxX - sharedDistance : sharedDistance;
		y = nearestBottom ? maxY - sharedDistance : sharedDistance;
		return roundedPosition(x, y, maxX, maxY);
	}

	public static Position resizedAroundPointer(
			MapHudConfig config,
			double newScale,
			double pointerX,
			double pointerY,
			int screenWidth,
			int screenHeight
	) {
		Bounds oldBounds = bounds(config);
		double relativeX = Math.clamp((pointerX - oldBounds.x()) / oldBounds.width(), 0.0D, 1.0D);
		double relativeY = Math.clamp((pointerY - oldBounds.y()) / oldBounds.height(), 0.0D, 1.0D);
		double desiredX = pointerX - relativeX * BASE_WIDTH * newScale;
		double desiredY = pointerY - relativeY * BASE_HEIGHT * newScale;
		return draggedPosition(desiredX, desiredY, newScale, screenWidth, screenHeight, false);
	}

	private static Position roundedPosition(double x, double y, double maxX, double maxY) {
		return new Position(
				(int) Math.round(Math.clamp(x, 0.0D, maxX)),
				(int) Math.round(Math.clamp(y, 0.0D, maxY))
		);
	}

	public record Position(int x, int y) {
	}

	public record Bounds(double x, double y, double width, double height) {
		public boolean contains(double pointX, double pointY) {
			return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
		}
	}
}
