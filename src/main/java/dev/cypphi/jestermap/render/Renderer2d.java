package dev.cypphi.jestermap.render;

import java.util.ArrayList;
import java.util.List;

public final class Renderer2d {
    private static final float DEFAULT_SOFTNESS = 1.0f;
    private static final int TRANSPARENT = 0x00000000;
    private static volatile RenderBackend2d backend = RenderBackend2d.NO_OP;

    private final List<ShapeCommand> commands = new ArrayList<>();
    private final RenderBackend2d instanceBackend;
    private ClipRect clipRect;

    public Renderer2d() {
        this.instanceBackend = null;
    }

    public Renderer2d(RenderBackend2d backend) {
        this.instanceBackend = backend == null ? RenderBackend2d.NO_OP : backend;
    }

    public static void setBackend(RenderBackend2d backend) {
        Renderer2d.backend = backend == null ? RenderBackend2d.NO_OP : backend;
    }

    public void beginFrame() {
        commands.clear();
        clipRect = null;
        activeBackend().beginFrame();
    }

    public void endFrame() {
        if (!commands.isEmpty()) {
            activeBackend().renderShapes(commands);
            commands.clear();
        }
    }

    private RenderBackend2d activeBackend() {
        return instanceBackend == null ? backend : instanceBackend;
    }

    public void enableClipRect(float minX, float minY, float maxX, float maxY) {
        clipRect = new ClipRect(
                Math.min(minX, maxX),
                Math.min(minY, maxY),
                Math.max(minX, maxX),
                Math.max(minY, maxY)
        );
    }

    public void disableClipRect() {
        clipRect = null;
    }

    public void drawRectangle(float x, float y, float width, float height, int argbColor) {
        drawRoundedRect(x, y, width, height, 0.0f, argbColor);
    }

    public void drawRoundedRect(float x, float y, float width, float height, float radius, int argbColor) {
        drawRoundedRect(x, y, width, height, radius, radius, radius, radius, argbColor);
    }

    public void drawRoundedRect(
            float x,
            float y,
            float width,
            float height,
            float topLeftRadius,
            float topRightRadius,
            float bottomLeftRadius,
            float bottomRightRadius,
            int argbColor
    ) {
        addShape(
                ShapeType.ROUNDED_RECT,
                x,
                y,
                width,
                height,
                topLeftRadius,
                topRightRadius,
                bottomLeftRadius,
                bottomRightRadius,
                argbColor,
                TRANSPARENT,
                0.0f,
                0,
                0.0f
        );
    }

    public void drawOutlinedRoundedRect(float x, float y, float width, float height, float radius, int fillColor, int outlineColor, float outlineWidth) {
        addShape(
                ShapeType.ROUNDED_RECT,
                x,
                y,
                width,
                height,
                radius,
                radius,
                radius,
                radius,
                fillColor,
                outlineColor,
                outlineWidth,
                0,
                0.0f
        );
    }

    public void drawRoundedRectOutline(float x, float y, float width, float height, float radius, int outlineColor, float outlineWidth) {
        drawOutlinedRoundedRect(x, y, width, height, radius, TRANSPARENT, outlineColor, outlineWidth);
    }

    public void drawRoundedRectShadow(float x, float y, float width, float height, float radius, float spread, int argbColor) {
        float safeSpread = Math.max(0.0f, spread);
        if (safeSpread <= 0.0f) {
            return;
        }

        addShape(
                ShapeType.SHADOW,
                x - safeSpread,
                y - safeSpread,
                width + safeSpread * 2.0f,
                height + safeSpread * 2.0f,
                radius,
                radius,
                radius,
                radius,
                argbColor,
                TRANSPARENT,
                safeSpread,
                0,
                0.0f
        );
    }

    public void drawLine(float startX, float startY, float endX, float endY, float thickness, int argbColor) {
        float safeThickness = Math.max(0.0f, thickness);
        if (safeThickness <= 0.0f || alpha(argbColor) == 0) {
            return;
        }

        float padding = safeThickness * 0.5f + DEFAULT_SOFTNESS;
        float x = Math.min(startX, endX) - padding;
        float y = Math.min(startY, endY) - padding;
        float width = Math.abs(endX - startX) + padding * 2.0f;
        float height = Math.abs(endY - startY) + padding * 2.0f;
        float centerX = x + width * 0.5f;
        float centerY = y + height * 0.5f;

        commands.add(new ShapeCommand(
                ShapeType.LINE,
                x,
                y,
                width,
                height,
                startX - centerX,
                startY - centerY,
                endX - centerX,
                endY - centerY,
                argbColor,
                TRANSPARENT,
                safeThickness,
                0,
                0.0f,
                DEFAULT_SOFTNESS,
                clipRect
        ));
    }

    public void drawCircle(float centerX, float centerY, float radius, int argbColor) {
        drawEllipse(centerX, centerY, radius, radius, argbColor);
    }

    public void drawCircleOutline(float centerX, float centerY, float radius, int outlineColor, float outlineWidth) {
        drawOutlinedEllipse(centerX, centerY, radius, radius, TRANSPARENT, outlineColor, outlineWidth);
    }

    public void drawEllipse(float centerX, float centerY, float radiusX, float radiusY, int argbColor) {
        drawOutlinedEllipse(centerX, centerY, radiusX, radiusY, argbColor, TRANSPARENT, 0.0f);
    }

    public void drawOutlinedEllipse(
            float centerX,
            float centerY,
            float radiusX,
            float radiusY,
            int fillColor,
            int outlineColor,
            float outlineWidth
    ) {
        addShape(
                ShapeType.ELLIPSE,
                centerX - radiusX,
                centerY - radiusY,
                radiusX * 2.0f,
                radiusY * 2.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                fillColor,
                outlineColor,
                outlineWidth,
                0,
                0.0f
        );
    }

    public void drawRegularPolygon(float centerX, float centerY, float radius, int sideCount, int argbColor) {
        drawOutlinedRegularPolygon(centerX, centerY, radius, sideCount, -((float) Math.PI * 0.5f), argbColor, TRANSPARENT, 0.0f);
    }

    public void drawRegularPolygonOutline(float centerX, float centerY, float radius, int sideCount, int outlineColor, float outlineWidth) {
        drawOutlinedRegularPolygon(centerX, centerY, radius, sideCount, -((float) Math.PI * 0.5f), TRANSPARENT, outlineColor, outlineWidth);
    }

    public void drawOutlinedRegularPolygon(
            float centerX,
            float centerY,
            float radius,
            int sideCount,
            float rotationRadians,
            int fillColor,
            int outlineColor,
            float outlineWidth
    ) {
        if (sideCount < 3) {
            return;
        }

        addShape(
                ShapeType.REGULAR_POLYGON,
                centerX - radius,
                centerY - radius,
                radius * 2.0f,
                radius * 2.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                fillColor,
                outlineColor,
                outlineWidth,
                sideCount,
                rotationRadians
        );
    }

    private void addShape(
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
            float rotationRadians
    ) {
        if (width <= 0.0f || height <= 0.0f || shouldSkip(fillColor, outlineColor, outlineWidth)) {
            return;
        }

        float maxOutlineWidth = Math.min(width, height) * 0.5f;
        commands.add(new ShapeCommand(
                type,
                x,
                y,
                width,
                height,
                Math.max(0.0f, topLeftRadius),
                Math.max(0.0f, topRightRadius),
                Math.max(0.0f, bottomLeftRadius),
                Math.max(0.0f, bottomRightRadius),
                fillColor,
                outlineColor,
                Math.clamp(outlineWidth, 0.0f, maxOutlineWidth),
                sideCount,
                rotationRadians,
                DEFAULT_SOFTNESS,
                clipRect
        ));
    }

    private boolean shouldSkip(int fillColor, int outlineColor, float outlineWidth) {
        return alpha(fillColor) == 0 && (outlineWidth <= 0.0f || alpha(outlineColor) == 0);
    }

    private int alpha(int argbColor) {
        return (argbColor >>> 24) & 0xFF;
    }
}
