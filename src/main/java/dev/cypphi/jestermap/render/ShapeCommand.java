package dev.cypphi.jestermap.render;

public record ShapeCommand(
        ShapeType type,
        float x,
        float y,
        float width,
        float height,
        float topLeftRadius,
        float topRightRadius,
        float bottomLeftRadius,
        float bottomRightRadius,
        int fillColor,
        int outlineColor,
        float outlineWidth,
        int sideCount,
        float rotationRadians,
        float softness,
        ClipRect clipRect
) {
}
