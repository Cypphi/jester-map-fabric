package dev.cypphi.jestermap.render;

import java.util.List;

public interface RenderBackend2d {
    RenderBackend2d NO_OP = new RenderBackend2d() {
    };

    default void beginFrame() {
    }

    default void renderShapes(List<ShapeCommand> commands) {
    }
}
