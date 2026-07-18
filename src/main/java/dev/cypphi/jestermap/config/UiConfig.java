package dev.cypphi.jestermap.config;

public record UiConfig(boolean customFont, FontFamily fontFamily) {
	public UiConfig withCustomFont(boolean value) {
		return new UiConfig(value, fontFamily);
	}

	public UiConfig withFontFamily(FontFamily value) {
		return new UiConfig(customFont, value);
	}
}
