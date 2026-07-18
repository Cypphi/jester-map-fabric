package dev.cypphi.jestermap.client.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cypphi.jestermap.JesterMap;
import dev.cypphi.jestermap.config.FontFamily;
import dev.cypphi.jestermap.config.UiConfig;
import dev.cypphi.jestermap.mixin.accessor.GuiGraphicsExtractorAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JesterTextRenderer implements AutoCloseable {
	private static final JesterTextRenderer INSTANCE = new JesterTextRenderer();
	private static final float SHADOW_OFFSET = 1.0F;

	private final Map<FontFamily, FontAtlas> atlases = new EnumMap<>(FontFamily.class);
	private final Set<FontFamily> failedFonts = EnumSet.noneOf(FontFamily.class);

	public static JesterTextRenderer getInstance() {
		return INSTANCE;
	}

	private JesterTextRenderer() {
	}

	public float width(UiConfig config, Component text) {
		return width(config, text.getString());
	}

	public float width(UiConfig config, String text) {
		FontAtlas atlas = customAtlas(config);
		if (atlas == null) {
			return Minecraft.getInstance().font.width(text);
		}
		float lineWidth = 0.0F;
		float maximumWidth = 0.0F;
		for (int offset = 0; offset < text.length();) {
			int codePoint = text.codePointAt(offset);
			offset += Character.charCount(codePoint);
			if (codePoint == '\n') {
				maximumWidth = Math.max(maximumWidth, lineWidth);
				lineWidth = 0.0F;
				continue;
			}
			lineWidth += atlas.glyph(codePoint).advance();
		}
		float measuredWidth = Math.max(maximumWidth, lineWidth);
		return measuredWidth == 0.0F ? 0.0F : measuredWidth + atlas.syntheticBoldOffset();
	}

	public float lineHeight(UiConfig config) {
		FontAtlas atlas = customAtlas(config);
		return atlas == null ? Minecraft.getInstance().font.lineHeight : atlas.lineHeight();
	}

	public void draw(
			GuiGraphicsExtractor graphics,
			UiConfig config,
			Component text,
			float x,
			float y,
			int color,
			boolean shadow
	) {
		draw(graphics, config, text.getString(), x, y, color, shadow);
	}

	public void draw(
			GuiGraphicsExtractor graphics,
			UiConfig config,
			String text,
			float x,
			float y,
			int color,
			boolean shadow
	) {
		if (((color >>> 24) & 0xFF) == 0 || text.isEmpty()) {
			return;
		}
		FontAtlas atlas = customAtlas(config);
		if (atlas == null) {
			graphics.text(Minecraft.getInstance().font, text, Math.round(x), Math.round(y), color, shadow);
			return;
		}

		List<GlyphQuad> quads = new ArrayList<>(text.length() * (shadow ? 2 : 1));
		if (shadow) {
			int shadowAlpha = Math.round(((color >>> 24) & 0xFF) * 0.7F);
			appendQuads(atlas, text, x + SHADOW_OFFSET, y + SHADOW_OFFSET, shadowAlpha << 24 | 0x003C3C3C, quads);
		}
		appendQuads(atlas, text, x, y, color, quads);
		if (quads.isEmpty()) {
			return;
		}

		Matrix3x2f pose = new Matrix3x2f(graphics.pose());
		ScreenRectangle bounds = bounds(x, y, width(config, text), lineCount(text) * atlas.lineHeight(), shadow, pose);
		ScreenRectangle scissor = graphics.scissorStack.peek();
		if (scissor != null) {
			bounds = scissor.intersection(bounds);
		}
		((GuiGraphicsExtractorAccessor) graphics).jesterMap$getGuiRenderState().addGuiElement(
				new TextRenderState(pose, List.copyOf(quads), atlas.textureSetup(), scissor, bounds)
		);
	}

	private static void appendQuads(FontAtlas atlas, String text, float x, float y, int color, List<GlyphQuad> target) {
		float cursorX = x;
		float cursorY = y;
		for (int offset = 0; offset < text.length();) {
			int codePoint = text.codePointAt(offset);
			offset += Character.charCount(codePoint);
			if (codePoint == '\n') {
				cursorX = x;
				cursorY += atlas.lineHeight();
				continue;
			}

			FontAtlas.Glyph glyph = atlas.glyph(codePoint);
			if (glyph.x1() > glyph.x0() && glyph.y1() > glyph.y0()) {
				float baselineY = cursorY + atlas.baseline();
				addGlyphQuad(target, glyph, cursorX, baselineY, color);
				if (atlas.syntheticBoldOffset() > 0.0F) {
					addGlyphQuad(target, glyph, cursorX + atlas.syntheticBoldOffset(), baselineY, color);
				}
			}
			cursorX += glyph.advance();
		}
	}

	private static void addGlyphQuad(
			List<GlyphQuad> target,
			FontAtlas.Glyph glyph,
			float x,
			float baselineY,
			int color
	) {
		target.add(new GlyphQuad(
				x + glyph.x0(),
				baselineY + glyph.y0(),
				x + glyph.x1(),
				baselineY + glyph.y1(),
				glyph.u0(), glyph.v0(), glyph.u1(), glyph.v1(), color
		));
	}

	private FontAtlas customAtlas(UiConfig config) {
		if (config == null || !config.customFont()) {
			return null;
		}
		FontFamily family = config.fontFamily() == null ? FontFamily.ROBOTO : config.fontFamily();
		if (failedFonts.contains(family)) {
			return null;
		}
		FontAtlas existing = atlases.get(family);
		if (existing != null) {
			return existing;
		}
		try {
			FontAtlas loaded = FontAtlas.load(family);
			atlases.put(family, loaded);
			return loaded;
		} catch (IOException | RuntimeException exception) {
			failedFonts.add(family);
			JesterMap.LOGGER.error("Could not load the {} font; using Minecraft's font.", family.displayName(), exception);
			return null;
		}
	}

	private static int lineCount(String text) {
		int lines = 1;
		for (int index = 0; index < text.length(); index++) {
			if (text.charAt(index) == '\n') {
				lines++;
			}
		}
		return lines;
	}

	private static ScreenRectangle bounds(float x, float y, float width, float height, boolean shadow, Matrix3x2fc pose) {
		int padding = shadow ? 2 : 1;
		int left = (int) Math.floor(x) - 1;
		int top = (int) Math.floor(y) - 1;
		int right = (int) Math.ceil(x + width) + padding;
		int bottom = (int) Math.ceil(y + height) + padding;
		return new ScreenRectangle(left, top, Math.max(0, right - left), Math.max(0, bottom - top)).transformMaxBounds(pose);
	}

	@Override
	public void close() {
		atlases.values().forEach(FontAtlas::close);
		atlases.clear();
		failedFonts.clear();
	}

	private record GlyphQuad(
			float x0,
			float y0,
			float x1,
			float y1,
			float u0,
			float v0,
			float u1,
			float v1,
			int color
	) {
	}

	private record TextRenderState(
			Matrix3x2fc pose,
			List<GlyphQuad> quads,
			TextureSetup textureSetup,
			ScreenRectangle scissorArea,
			ScreenRectangle bounds
	) implements GuiElementRenderState {
		@Override
		public void buildVertices(VertexConsumer vertices) {
			for (GlyphQuad quad : quads) {
				vertex(vertices, quad.x0(), quad.y0(), quad.u0(), quad.v0(), quad.color());
				vertex(vertices, quad.x0(), quad.y1(), quad.u0(), quad.v1(), quad.color());
				vertex(vertices, quad.x1(), quad.y1(), quad.u1(), quad.v1(), quad.color());
				vertex(vertices, quad.x1(), quad.y0(), quad.u1(), quad.v0(), quad.color());
			}
		}

		private void vertex(VertexConsumer vertices, float x, float y, float u, float v, int color) {
			vertices.addVertexWith2DPose(pose, x, y).setUv(u, v).setColor(color);
		}

		@Override
		public RenderPipeline pipeline() {
			return RenderPipelines.GUI_TEXTURED;
		}
	}
}
