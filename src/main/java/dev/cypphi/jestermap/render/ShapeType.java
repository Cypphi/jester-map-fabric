package dev.cypphi.jestermap.render;

public enum ShapeType {
    ROUNDED_RECT(0.0f),
    ELLIPSE(1.0f),
    REGULAR_POLYGON(2.0f),
    SHADOW(3.0f),
    LINE(4.0f);

    private final float shaderId;

    ShapeType(float shaderId) {
        this.shaderId = shaderId;
    }

    float shaderId() {
        return shaderId;
    }
}
