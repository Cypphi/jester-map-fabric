package dev.cypphi.jestermap.dungeon.map;

public enum DungeonRoomType {
	NORMAL(63),
	ENTRANCE(30),
	PUZZLE(66),
	TRAP(62),
	MINIBOSS(74),
	FAIRY(82),
	BLOOD(18),
	UNKNOWN(85);

	private final int packedColor;

	DungeonRoomType(int packedColor) {
		this.packedColor = packedColor;
	}

	public int packedColor() {
		return packedColor;
	}

	static DungeonRoomType fromPackedColor(byte color) {
		int unsignedColor = Byte.toUnsignedInt(color);
		for (DungeonRoomType type : values()) {
			if (type.packedColor == unsignedColor) {
				return type;
			}
		}
		return null;
	}
}
