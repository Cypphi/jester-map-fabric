package dev.cypphi.jestermap.render.minecraft;

import dev.cypphi.jestermap.render.ClipRect;
import dev.cypphi.jestermap.render.RenderBackend2d;
import dev.cypphi.jestermap.render.Renderer2d;
import dev.cypphi.jestermap.render.ShapeCommand;
import dev.cypphi.jestermap.render.ShapeType;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cypphi.jestermap.mixin.accessor.GuiGraphicsExtractorAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

import java.util.ArrayList;
import java.util.List;

/** Extraction-safe 26x backend that tessellates the shared 2D shape command surface. */
public final class MinecraftRenderBackend2d implements RenderBackend2d {
    /** Half-width of the centered edge transition used by the 1.21.11 SDF renderer. */
    private static final float ANTIALIAS_WIDTH = 0.5F;
    private static final int CURVE_SEGMENTS = 8;
    private static final int ELLIPSE_SEGMENTS = 48;

    private GuiGraphicsExtractor graphics;

    public void beginExtraction(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    public void endExtraction() {
        graphics = null;
    }

    @Override
    public void renderShapes(List<ShapeCommand> commands) {
        GuiGraphicsExtractor current = graphics;
        if (current == null || commands == null || commands.isEmpty()) {
            return;
        }

        GuiGraphicsExtractorAccessor accessor = (GuiGraphicsExtractorAccessor) current;
        Matrix3x2f pose = new Matrix3x2f(current.pose());
        for (ShapeCommand command : commands) {
            accessor.jesterMap$getGuiRenderState().addGuiElement(new ShapeRenderState(
                    pose,
                    command,
                    scissor(command.clipRect()),
                    bounds(command, pose)
            ));
        }
    }

    /** Retained for extraction adapters that only need one rounded primitive. */
    public static void fillRoundedRect(
            GuiGraphicsExtractor graphics,
            float x,
            float y,
            float width,
            float height,
            float radius,
            int color
    ) {
        MinecraftRenderBackend2d backend = new MinecraftRenderBackend2d();
        Renderer2d renderer = new Renderer2d(backend);
        backend.beginExtraction(graphics);
        renderer.beginFrame();
        renderer.drawRoundedRect(x, y, width, height, radius, color);
        renderer.endFrame();
        backend.endExtraction();
    }

    /** Retained for extraction adapters that need independently rounded corners. */
    public static void fillRoundedRect(
            GuiGraphicsExtractor graphics,
            float x,
            float y,
            float width,
            float height,
            float topLeftRadius,
            float topRightRadius,
            float bottomLeftRadius,
            float bottomRightRadius,
            int color
    ) {
        MinecraftRenderBackend2d backend = new MinecraftRenderBackend2d();
        Renderer2d renderer = new Renderer2d(backend);
        backend.beginExtraction(graphics);
        renderer.beginFrame();
        renderer.drawRoundedRect(
                x,
                y,
                width,
                height,
                topLeftRadius,
                topRightRadius,
                bottomLeftRadius,
                bottomRightRadius,
                color
        );
        renderer.endFrame();
        backend.endExtraction();
    }

    private static ScreenRectangle scissor(ClipRect clip) {
        if (clip == null) {
            return null;
        }
        int left = floor(clip.minX());
        int top = floor(clip.minY());
        return new ScreenRectangle(left, top, Math.max(0, ceil(clip.maxX()) - left), Math.max(0, ceil(clip.maxY()) - top));
    }

    private static ScreenRectangle bounds(ShapeCommand command, Matrix3x2fc pose) {
        int left = floor(command.x() - ANTIALIAS_WIDTH);
        int top = floor(command.y() - ANTIALIAS_WIDTH);
        int right = ceil(command.x() + command.width() + ANTIALIAS_WIDTH);
        int bottom = ceil(command.y() + command.height() + ANTIALIAS_WIDTH);
        ScreenRectangle bounds = new ScreenRectangle(left, top, right - left, bottom - top).transformMaxBounds(pose);
        ScreenRectangle scissor = scissor(command.clipRect());
        return scissor == null ? bounds : scissor.intersection(bounds);
    }

    private static int floor(float value) {
        return (int) Math.floor(value);
    }

    private static int ceil(float value) {
        return (int) Math.ceil(value);
    }

    private record ShapeRenderState(
            Matrix3x2fc pose,
            ShapeCommand command,
            ScreenRectangle scissorArea,
            ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer vertices) {
            ShapeMesh mesh = new ShapeMesh(vertices, pose);
            switch (command.type()) {
                case ROUNDED_RECT -> mesh.roundedRect(command);
                case SHADOW -> mesh.shadow(command);
                case ELLIPSE -> mesh.ellipse(command);
                case REGULAR_POLYGON -> mesh.regularPolygon(command);
                case LINE -> mesh.line(command);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }

    private static final class ShapeMesh {
        private final VertexConsumer vertices;
        private final Matrix3x2fc pose;

        private ShapeMesh(VertexConsumer vertices, Matrix3x2fc pose) {
            this.vertices = vertices;
            this.pose = pose;
        }

        private void roundedRect(ShapeCommand command) {
            float outlineWidth = command.outlineWidth();
            float edgeWidth = edgeWidth(command.width(), command.height());
            List<Point> outer = roundedBoundary(command, -edgeWidth);
            List<Point> innerEdge = roundedBoundary(command, edgeWidth);

            if (outlineWidth > 0.0F && alpha(command.outlineColor()) > 0) {
                float transitionWidth = Math.min(edgeWidth, outlineWidth * 0.5F);
                List<Point> outlineInnerEdge = roundedBoundary(command, outlineWidth - transitionWidth);
                List<Point> fillEdge = roundedBoundary(command, outlineWidth + transitionWidth);
                antialias(innerEdge, outer, command.outlineColor());
                ring(innerEdge, outlineInnerEdge, command.outlineColor(), command.outlineColor());
                if (alpha(command.fillColor()) > 0) {
                    ring(outlineInnerEdge, fillEdge, command.outlineColor(), command.fillColor());
                    fan(fillEdge, command.fillColor());
                }
                return;
            }

            if (alpha(command.fillColor()) > 0) {
                fan(innerEdge, command.fillColor());
                antialias(innerEdge, outer, command.fillColor());
            }
        }

        private void shadow(ShapeCommand command) {
            float spread = command.outlineWidth();
            if (spread <= 0.0F || alpha(command.fillColor()) == 0) {
                return;
            }

            int bands = Math.max(1, ceil(spread * 2.0F));
            for (int band = 0; band < bands; band++) {
                float outerInset = spread * band / bands;
                float innerInset = spread * (band + 1) / bands;
                float outerProgress = band / (float) bands;
                float innerProgress = (band + 1.0F) / bands;
                int outerColor = withAlpha(command.fillColor(), Math.round(alpha(command.fillColor()) * smoothstep(outerProgress)));
                int innerColor = withAlpha(command.fillColor(), Math.round(alpha(command.fillColor()) * smoothstep(innerProgress)));
                List<Point> outer = roundedBoundary(
                        command.x() + outerInset,
                        command.y() + outerInset,
                        command.width() - outerInset * 2.0F,
                        command.height() - outerInset * 2.0F,
                        command.topLeftRadius() + spread - outerInset,
                        command.topRightRadius() + spread - outerInset,
                        command.bottomRightRadius() + spread - outerInset,
                        command.bottomLeftRadius() + spread - outerInset
                );
                List<Point> inner = roundedBoundary(
                        command.x() + innerInset,
                        command.y() + innerInset,
                        command.width() - innerInset * 2.0F,
                        command.height() - innerInset * 2.0F,
                        command.topLeftRadius() + spread - innerInset,
                        command.topRightRadius() + spread - innerInset,
                        command.bottomRightRadius() + spread - innerInset,
                        command.bottomLeftRadius() + spread - innerInset
                );
                ring(outer, inner, outerColor, innerColor);
            }
        }

        private void ellipse(ShapeCommand command) {
            float centerX = command.x() + command.width() * 0.5F;
            float centerY = command.y() + command.height() * 0.5F;
            float radiusX = command.width() * 0.5F;
            float radiusY = command.height() * 0.5F;
            float edgeWidth = edgeWidth(command.width(), command.height());
            List<Point> outer = ellipseBoundary(centerX, centerY, radiusX + edgeWidth, radiusY + edgeWidth);
            List<Point> innerEdge = ellipseBoundary(
                    centerX,
                    centerY,
                    Math.max(0.0F, radiusX - edgeWidth),
                    Math.max(0.0F, radiusY - edgeWidth)
            );
            if (command.outlineWidth() > 0.0F && alpha(command.outlineColor()) > 0) {
                float transitionWidth = Math.min(edgeWidth, command.outlineWidth() * 0.5F);
                List<Point> outlineInnerEdge = ellipseBoundary(
                        centerX,
                        centerY,
                        Math.max(0.0F, radiusX - command.outlineWidth() + transitionWidth),
                        Math.max(0.0F, radiusY - command.outlineWidth() + transitionWidth)
                );
                List<Point> fillEdge = ellipseBoundary(
                        centerX,
                        centerY,
                        Math.max(0.0F, radiusX - command.outlineWidth() - transitionWidth),
                        Math.max(0.0F, radiusY - command.outlineWidth() - transitionWidth)
                );
                antialias(innerEdge, outer, command.outlineColor());
                ring(innerEdge, outlineInnerEdge, command.outlineColor(), command.outlineColor());
                if (alpha(command.fillColor()) > 0) {
                    ring(outlineInnerEdge, fillEdge, command.outlineColor(), command.fillColor());
                    fan(fillEdge, command.fillColor());
                }
            } else if (alpha(command.fillColor()) > 0) {
                fan(innerEdge, command.fillColor());
                antialias(innerEdge, outer, command.fillColor());
            }
        }

        private void regularPolygon(ShapeCommand command) {
            float centerX = command.x() + command.width() * 0.5F;
            float centerY = command.y() + command.height() * 0.5F;
            float radius = Math.min(command.width(), command.height()) * 0.5F;
            float edgeWidth = edgeWidth(command.width(), command.height());
            List<Point> outer = polygonBoundary(centerX, centerY, radius + edgeWidth, command.sideCount(), command.rotationRadians());
            List<Point> innerEdge = polygonBoundary(centerX, centerY, Math.max(0.0F, radius - edgeWidth), command.sideCount(), command.rotationRadians());
            if (command.outlineWidth() > 0.0F && alpha(command.outlineColor()) > 0) {
                float transitionWidth = Math.min(edgeWidth, command.outlineWidth() * 0.5F);
                List<Point> outlineInnerEdge = polygonBoundary(
                        centerX,
                        centerY,
                        Math.max(0.0F, radius - command.outlineWidth() + transitionWidth),
                        command.sideCount(),
                        command.rotationRadians()
                );
                List<Point> fillEdge = polygonBoundary(
                        centerX,
                        centerY,
                        Math.max(0.0F, radius - command.outlineWidth() - transitionWidth),
                        command.sideCount(),
                        command.rotationRadians()
                );
                antialias(innerEdge, outer, command.outlineColor());
                ring(innerEdge, outlineInnerEdge, command.outlineColor(), command.outlineColor());
                if (alpha(command.fillColor()) > 0) {
                    ring(outlineInnerEdge, fillEdge, command.outlineColor(), command.fillColor());
                    fan(fillEdge, command.fillColor());
                }
            } else if (alpha(command.fillColor()) > 0) {
                fan(innerEdge, command.fillColor());
                antialias(innerEdge, outer, command.fillColor());
            }
        }

        private void line(ShapeCommand command) {
            float centerX = command.x() + command.width() * 0.5F;
            float centerY = command.y() + command.height() * 0.5F;
            float startX = centerX + command.topLeftRadius();
            float startY = centerY + command.topRightRadius();
            float endX = centerX + command.bottomLeftRadius();
            float endY = centerY + command.bottomRightRadius();
            float radius = command.outlineWidth() * 0.5F;
            float edgeWidth = Math.min(ANTIALIAS_WIDTH, radius);
            List<Point> innerEdge = capsuleBoundary(startX, startY, endX, endY, Math.max(0.0F, radius - edgeWidth));
            List<Point> outer = capsuleBoundary(startX, startY, endX, endY, radius + edgeWidth);
            fan(innerEdge, command.fillColor());
            antialias(innerEdge, outer, command.fillColor());
        }

        private List<Point> roundedBoundary(ShapeCommand command, float inset) {
            return roundedBoundary(
                    command.x() + inset,
                    command.y() + inset,
                    command.width() - inset * 2.0F,
                    command.height() - inset * 2.0F,
                    Math.max(0.0F, command.topLeftRadius() - inset),
                    Math.max(0.0F, command.topRightRadius() - inset),
                    Math.max(0.0F, command.bottomRightRadius() - inset),
                    Math.max(0.0F, command.bottomLeftRadius() - inset)
            );
        }

        private void fan(List<Point> boundary, int color) {
            if (boundary.size() < 3 || alpha(color) == 0) {
                return;
            }

            Point anchor = boundary.getFirst();
            int index = 1;
            for (; index + 2 < boundary.size(); index += 2) {
                quad(anchor, boundary.get(index + 2), boundary.get(index + 1), boundary.get(index),
                        color, color, color, color);
            }
            if (index + 1 < boundary.size()) {
                quad(anchor, boundary.get(index + 1), boundary.get(index), anchor,
                        color, color, color, color);
            }
        }

        private void ring(List<Point> outer, List<Point> inner, int outerColor, int innerColor) {
            int count = Math.min(outer.size(), inner.size());
            for (int index = 0; index < count; index++) {
                int next = (index + 1) % count;
                quad(outer.get(index), inner.get(index), inner.get(next), outer.get(next),
                        outerColor, innerColor, innerColor, outerColor);
            }
        }

        private void antialias(List<Point> inner, List<Point> outer, int color) {
            ring(outer, inner, withAlpha(color, 0), color);
        }

        private void quad(Point first, Point second, Point third, Point fourth, int firstColor, int secondColor, int thirdColor, int fourthColor) {
            vertex(first, firstColor);
            vertex(second, secondColor);
            vertex(third, thirdColor);
            vertex(fourth, fourthColor);
        }

        private void vertex(Point point, int color) {
            vertices.addVertexWith2DPose(pose, point.x(), point.y()).setColor(color);
        }

        private static float edgeWidth(float width, float height) {
            return Math.min(ANTIALIAS_WIDTH, Math.min(width, height) * 0.5F);
        }

        private static List<Point> roundedBoundary(
                float x,
                float y,
                float width,
                float height,
                float topLeft,
                float topRight,
                float bottomRight,
                float bottomLeft
        ) {
            float safeWidth = Math.max(0.0F, width);
            float safeHeight = Math.max(0.0F, height);
            float maxRadius = Math.min(safeWidth, safeHeight) * 0.5F;
            topLeft = Math.clamp(topLeft, 0.0F, maxRadius);
            topRight = Math.clamp(topRight, 0.0F, maxRadius);
            bottomRight = Math.clamp(bottomRight, 0.0F, maxRadius);
            bottomLeft = Math.clamp(bottomLeft, 0.0F, maxRadius);

            List<Point> points = new ArrayList<>((CURVE_SEGMENTS + 1) * 4);
            addArc(points, x + topLeft, y + topLeft, topLeft, Math.PI, Math.PI * 1.5D);
            addArc(points, x + safeWidth - topRight, y + topRight, topRight, -Math.PI * 0.5D, 0.0D);
            addArc(points, x + safeWidth - bottomRight, y + safeHeight - bottomRight, bottomRight, 0.0D, Math.PI * 0.5D);
            addArc(points, x + bottomLeft, y + safeHeight - bottomLeft, bottomLeft, Math.PI * 0.5D, Math.PI);
            return points;
        }

        private static List<Point> ellipseBoundary(float centerX, float centerY, float radiusX, float radiusY) {
            List<Point> points = new ArrayList<>(ELLIPSE_SEGMENTS);
            for (int index = 0; index < ELLIPSE_SEGMENTS; index++) {
                double angle = Math.PI * 2.0D * index / ELLIPSE_SEGMENTS;
                points.add(new Point(
                        centerX + (float) Math.cos(angle) * radiusX,
                        centerY + (float) Math.sin(angle) * radiusY
                ));
            }
            return points;
        }

        private static List<Point> polygonBoundary(float centerX, float centerY, float radius, int sides, float rotation) {
            List<Point> points = new ArrayList<>(sides);
            for (int index = 0; index < sides; index++) {
                double angle = rotation + Math.PI * 2.0D * index / sides;
                points.add(new Point(
                        centerX + (float) Math.cos(angle) * radius,
                        centerY + (float) Math.sin(angle) * radius
                ));
            }
            return points;
        }

        private static List<Point> capsuleBoundary(float startX, float startY, float endX, float endY, float radius) {
            float angle = (float) Math.atan2(endY - startY, endX - startX);
            List<Point> points = new ArrayList<>((CURVE_SEGMENTS + 1) * 2);
            addArc(points, endX, endY, radius, angle - Math.PI * 0.5D, angle + Math.PI * 0.5D);
            addArc(points, startX, startY, radius, angle + Math.PI * 0.5D, angle + Math.PI * 1.5D);
            return points;
        }

        private static void addArc(List<Point> points, float centerX, float centerY, float radius, double start, double end) {
            if (radius <= 0.0F) {
                for (int index = 0; index <= CURVE_SEGMENTS; index++) {
                    points.add(new Point(centerX, centerY));
                }
                return;
            }
            for (int index = 0; index <= CURVE_SEGMENTS; index++) {
                double angle = start + (end - start) * index / CURVE_SEGMENTS;
                points.add(new Point(
                        centerX + (float) Math.cos(angle) * radius,
                        centerY + (float) Math.sin(angle) * radius
                ));
            }
        }
    }

    private static float smoothstep(float value) {
        float clamped = Math.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.clamp(alpha, 0, 255) << 24);
    }

    private record Point(float x, float y) {
    }
}
