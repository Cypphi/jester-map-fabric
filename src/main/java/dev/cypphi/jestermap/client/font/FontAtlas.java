package dev.cypphi.jestermap.client.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.cypphi.jestermap.config.FontFamily;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

final class FontAtlas implements AutoCloseable {
	private static final float BASE_LINE_HEIGHT = 10.0F;

	private static final int FIRST_CODE_POINT = 32;
	private static final int LAST_CODE_POINT = 255;
	private static final int GLYPH_COUNT = LAST_CODE_POINT - FIRST_CODE_POINT + 1;
	private static final int ATLAS_SIZE = 1024;
	private static final float PACKED_HEIGHT = 32.0F;
	private static final float LOGICAL_SCALE = BASE_LINE_HEIGHT / PACKED_HEIGHT;

	private final Map<Integer, Glyph> glyphs;
	private final Glyph fallback;
	private final float baseline;
	private final float lineHeight;
	private final float syntheticBoldOffset;
	private final LinearDynamicTexture texture;
	private final TextureSetup textureSetup;

	private FontAtlas(
			Map<Integer, Glyph> glyphs,
			float baseline,
			float lineHeight,
			float syntheticBoldOffset,
			NativeImage image,
			String label
	) {
		this.glyphs = glyphs;
		this.fallback = glyphs.get((int) '?');
		this.baseline = baseline;
		this.lineHeight = lineHeight;
		this.syntheticBoldOffset = syntheticBoldOffset;
		this.texture = new LinearDynamicTexture(label, image);
		this.textureSetup = TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler());
	}

	static FontAtlas load(FontFamily family) throws IOException {
		ByteBuffer fontData = readFont(family);
		ByteBuffer bitmap = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE);
		Map<Integer, Glyph> glyphs = new HashMap<>(GLYPH_COUNT);
		float baseline;
		float packedHeight = PACKED_HEIGHT * family.displayScale();

		try (MemoryStack stack = MemoryStack.stackPush()) {
			STBTTFontinfo fontInfo = STBTTFontinfo.malloc(stack);
			if (!STBTruetype.stbtt_InitFont(fontInfo, fontData)) {
				throw new IOException("Invalid font data for " + family.displayName());
			}

			IntBuffer ascent = stack.mallocInt(1);
			STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
			baseline = ascent.get(0) * STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, packedHeight) * LOGICAL_SCALE;

			STBTTPackedchar.Buffer packedCharacters = STBTTPackedchar.malloc(GLYPH_COUNT, stack);
			STBTTPackContext context = STBTTPackContext.malloc(stack);
			if (!STBTruetype.stbtt_PackBegin(context, bitmap, ATLAS_SIZE, ATLAS_SIZE, 0, 1)) {
				throw new IOException("Could not create glyph atlas for " + family.displayName());
			}
			try {
				if (!STBTruetype.stbtt_PackFontRange(
						context,
						fontData,
						0,
						packedHeight,
						FIRST_CODE_POINT,
						packedCharacters
				)) {
					throw new IOException("Font atlas is too small for " + family.displayName());
				}
			} finally {
				STBTruetype.stbtt_PackEnd(context);
			}

			for (int index = 0; index < GLYPH_COUNT; index++) {
				STBTTPackedchar packed = packedCharacters.get(index);
				glyphs.put(FIRST_CODE_POINT + index, new Glyph(
						packed.xoff() * LOGICAL_SCALE,
						packed.yoff() * LOGICAL_SCALE,
						packed.xoff2() * LOGICAL_SCALE,
						packed.yoff2() * LOGICAL_SCALE,
						packed.x0() / (float) ATLAS_SIZE,
						packed.y0() / (float) ATLAS_SIZE,
						packed.x1() / (float) ATLAS_SIZE,
						packed.y1() / (float) ATLAS_SIZE,
						packed.xadvance() * LOGICAL_SCALE
				));
			}
		}

		NativeImage image = new NativeImage(ATLAS_SIZE, ATLAS_SIZE, true);
		for (int y = 0; y < ATLAS_SIZE; y++) {
			for (int x = 0; x < ATLAS_SIZE; x++) {
				int alpha = Byte.toUnsignedInt(bitmap.get(y * ATLAS_SIZE + x));
				if (alpha != 0) {
					image.setPixel(x, y, alpha << 24 | 0x00FFFFFF);
				}
			}
		}
		return new FontAtlas(
				glyphs,
				baseline,
				BASE_LINE_HEIGHT * family.displayScale(),
				family.syntheticBoldOffset(),
				image,
				"Jester Map " + family.displayName()
		);
	}

	private static ByteBuffer readFont(FontFamily family) throws IOException {
		try (InputStream stream = FontAtlas.class.getResourceAsStream(family.resourcePath())) {
			if (stream == null) {
				throw new IOException("Missing bundled font " + family.resourcePath());
			}
			byte[] bytes = stream.readAllBytes();
			return BufferUtils.createByteBuffer(bytes.length).put(bytes).flip();
		}
	}

	Glyph glyph(int codePoint) {
		return glyphs.getOrDefault(codePoint, fallback);
	}

	float baseline() {
		return baseline;
	}

	float lineHeight() {
		return lineHeight;
	}

	float syntheticBoldOffset() {
		return syntheticBoldOffset;
	}

	TextureSetup textureSetup() {
		return textureSetup;
	}

	@Override
	public void close() {
		texture.close();
	}

	record Glyph(
			float x0,
			float y0,
			float x1,
			float y1,
			float u0,
			float v0,
			float u1,
			float v1,
			float advance
	) {
	}

	private static final class LinearDynamicTexture extends DynamicTexture {
		private LinearDynamicTexture(String label, NativeImage image) {
			super(() -> label, image);
			sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
		}
	}
}
