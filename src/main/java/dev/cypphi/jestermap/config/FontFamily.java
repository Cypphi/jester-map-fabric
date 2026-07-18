package dev.cypphi.jestermap.config;

import com.google.gson.annotations.SerializedName;

public enum FontFamily {
	@SerializedName("roboto")
	ROBOTO("Roboto", "roboto.ttf", 1.0F, 0.0F),
	@SerializedName("comic_sans")
	COMIC_SANS("Comic Sans", "comic_sans.ttf", 1.0F, 0.0F),
	@SerializedName("papyrus")
	PAPYRUS("Papyrus", "papyrus.ttf", 1.65F, 0.55F);

	private final String displayName;
	private final String resourceName;
	private final float displayScale;
	private final float syntheticBoldOffset;

	FontFamily(String displayName, String resourceName, float displayScale, float syntheticBoldOffset) {
		this.displayName = displayName;
		this.resourceName = resourceName;
		this.displayScale = displayScale;
		this.syntheticBoldOffset = syntheticBoldOffset;
	}

	public String displayName() {
		return displayName;
	}

	public String resourcePath() {
		return "/assets/jester_map/fonts/" + resourceName;
	}

	public float displayScale() {
		return displayScale;
	}

	public float syntheticBoldOffset() {
		return syntheticBoldOffset;
	}

	public FontFamily next() {
		FontFamily[] families = values();
		return families[(ordinal() + 1) % families.length];
	}
}
