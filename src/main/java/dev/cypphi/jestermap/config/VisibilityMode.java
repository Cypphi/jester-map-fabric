package dev.cypphi.jestermap.config;

import com.google.gson.annotations.SerializedName;

public enum VisibilityMode {
	@SerializedName("discovered")
	DISCOVERED,
	@SerializedName("hinted")
	HINTED,
	@SerializedName("all_known")
	ALL_KNOWN;

	public VisibilityMode next() {
		VisibilityMode[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public String displayName() {
		return switch (this) {
			case DISCOVERED -> "Discovered only";
			case HINTED -> "Include hinted";
			case ALL_KNOWN -> "All known rooms";
		};
	}
}
