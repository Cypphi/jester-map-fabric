package dev.cypphi.jestermap.client.screen;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.cypphi.jestermap.JesterMap;
import dev.cypphi.jestermap.config.ConfigDefaults;
import dev.cypphi.jestermap.config.ConfigService;
import dev.cypphi.jestermap.config.JesterConfig;
import dev.cypphi.jestermap.config.MapHudConfig;
import dev.cypphi.jestermap.config.VisibilityConfig;
import dev.cypphi.jestermap.dungeon.presentation.MockDungeonMapRenderer;
import dev.cypphi.jestermap.render.Renderer2d;
import dev.cypphi.jestermap.render.minecraft.MinecraftRenderBackend2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.util.List;

public final class JesterConfigScreen extends Screen {
	private static final int PANEL_WIDTH = 660;
	private static final int PANEL_HEIGHT = 420;
	private static final int PANEL_COLOR = 0xF20B100D;
	private static final int CARD_COLOR = 0xC9141C16;
	private static final int HEADER_COLOR = 0xF2152018;
	private static final int OUTLINE_COLOR = 0xB2506656;
	private static final int INNER_OUTLINE_COLOR = 0x60394B3E;
	private static final int ACCENT_COLOR = 0xFF75E58D;
	private static final int ACCENT_DIM_COLOR = 0xFF436D4C;
	private static final int TEXT_COLOR = 0xFFF1F5F0;
	private static final int MUTED_TEXT_COLOR = 0xFF91A097;
	private static final int HOVER_COLOR = 0x70344939;
	private static final int TRACK_COLOR = 0xFF263229;
	private static final int TOGGLE_OFF_COLOR = 0xFF303B33;
	private static final int ERROR_COLOR = 0xFFFF7A7A;
	private static final int ROW_HEIGHT = 40;
	private static final Identifier ICON = Identifier.fromNamespaceAndPath(JesterMap.MOD_ID, "icon.png");

	private final Screen parent;
	private final MinecraftRenderBackend2d backend = new MinecraftRenderBackend2d();
	private final Renderer2d renderer = new Renderer2d(backend);
	private final MockDungeonMapRenderer previewRenderer = new MockDungeonMapRenderer();
	private JesterConfig draft;
	private Tab activeTab = Tab.MAP;
	private boolean draggingScale;
	private String status = "";
	private boolean statusError;

	public JesterConfigScreen(Screen parent) {
		super(Component.translatable("jester_map.config.title"));
		this.parent = parent;
		this.draft = ConfigService.getInstance().current();
	}

	public static void open(Screen parent) {
		Minecraft client = Minecraft.getInstance();
		//? if jester_mc_262_or_newer {
		/*client.gui.setScreen(new JesterConfigScreen(parent));
		*///?} else {
		client.setScreen(new JesterConfigScreen(parent));
		//?}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
		if (minecraft != null && minecraft.level == null) {
			extractPanorama(graphics, deltaTicks);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
		super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
		graphics.blurBeforeThisStratum();
		graphics.nextStratum();

		ScreenLayout layout = layout();
		backend.beginExtraction(graphics);
		try {
			renderer.beginFrame();
			renderChrome(layout, mouseX, mouseY);
			renderer.endFrame();
		} finally {
			backend.endExtraction();
		}
		renderIcon(graphics, layout.icon());
		renderPreview(graphics, layout.previewCard());
		renderText(graphics, layout);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
		if (click.button() != 0) {
			return super.mouseClicked(click, doubleClick);
		}

		ScreenLayout layout = layout();
		for (TabButton tabButton : layout.tabs()) {
			if (tabButton.bounds().contains(click.x(), click.y())) {
				activeTab = tabButton.tab();
				draggingScale = false;
				status = "";
				return true;
			}
		}
		if (layout.saveButton().contains(click.x(), click.y())) {
			saveDraft();
			return true;
		}
		if (layout.resetButton().contains(click.x(), click.y())) {
			draft = ConfigDefaults.create();
			status = "Defaults restored in this draft";
			statusError = false;
			return true;
		}
		if (layout.closeButton().contains(click.x(), click.y())) {
			onClose();
			return true;
		}

		for (SettingRow row : layout.rows()) {
			if (!row.bounds().contains(click.x(), click.y())) {
				continue;
			}
			if (row.control() == Control.SCALE) {
				draggingScale = true;
				updateScale(click.x(), row);
			} else {
				activate(row.control());
			}
			return true;
		}
		return super.mouseClicked(click, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if (click.button() == 0 && draggingScale) {
			layout().rows().stream()
					.filter(row -> row.control() == Control.SCALE)
					.findFirst()
					.ifPresent(row -> updateScale(click.x(), row));
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (click.button() == 0 && draggingScale) {
			draggingScale = false;
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public void onClose() {
		//? if jester_mc_262_or_newer {
		/*minecraft.gui.setScreen(parent);
		*///?} else {
		minecraft.setScreen(parent);
		//?}
	}

	private void renderChrome(ScreenLayout layout, int mouseX, int mouseY) {
		Rect panel = layout.panel();
		renderer.drawRoundedRectShadow(panel.x(), panel.y(), panel.width(), panel.height(), 14.0F, 8.0F, 0x80000000);
		renderer.drawRoundedRect(panel.x(), panel.y(), panel.width(), panel.height(), 14.0F, PANEL_COLOR);
		renderer.drawRoundedRect(panel.x(), panel.y(), panel.width(), 68.0F, 14.0F, 14.0F, 0.0F, 0.0F, HEADER_COLOR);
		renderer.drawRoundedRectOutline(panel.x(), panel.y(), panel.width(), panel.height(), 14.0F, OUTLINE_COLOR, 1.0F);
		renderer.drawRectangle(panel.x() + 18.0F, panel.y() + 67.0F, panel.width() - 36.0F, 1.0F, INNER_OUTLINE_COLOR);

		renderCard(layout.previewCard());
		renderCard(layout.settingsCard());
		for (TabButton tabButton : layout.tabs()) {
			renderTab(tabButton, tabButton.bounds().contains(mouseX, mouseY));
		}
		for (SettingRow row : layout.rows()) {
			if (row.bounds().contains(mouseX, mouseY)) {
				renderer.drawRoundedRect(row.bounds().x(), row.bounds().y(), row.bounds().width(), row.bounds().height(), 5.0F, HOVER_COLOR);
			}
			renderControl(row);
		}

		renderButton(layout.resetButton(), layout.resetButton().contains(mouseX, mouseY), false);
		renderButton(layout.closeButton(), layout.closeButton().contains(mouseX, mouseY), false);
		renderButton(layout.saveButton(), layout.saveButton().contains(mouseX, mouseY), true);
	}

	private void renderCard(Rect card) {
		renderer.drawRoundedRect(card.x(), card.y(), card.width(), card.height(), 9.0F, CARD_COLOR);
		renderer.drawRoundedRectOutline(card.x(), card.y(), card.width(), card.height(), 9.0F, INNER_OUTLINE_COLOR, 1.0F);
	}

	private void renderTab(TabButton button, boolean hovered) {
		boolean selected = button.tab() == activeTab;
		int color = selected ? ACCENT_DIM_COLOR : hovered ? HOVER_COLOR : TRACK_COLOR;
		renderer.drawRoundedRect(button.bounds().x(), button.bounds().y(), button.bounds().width(), button.bounds().height(), 7.0F, color);
		if (selected) {
			renderer.drawRoundedRectOutline(button.bounds().x(), button.bounds().y(), button.bounds().width(), button.bounds().height(), 7.0F, ACCENT_COLOR, 1.0F);
		}
	}

	private void renderControl(SettingRow row) {
		if (row.control() == Control.SCALE) {
			float trackWidth = 90.0F;
			float trackX = row.bounds().maxX() - trackWidth - 12.0F;
			float trackY = row.bounds().y() + row.bounds().height() * 0.5F - 2.0F;
			double normalized = (draft.mapHud().scale() - ConfigDefaults.MIN_SCALE)
					/ (ConfigDefaults.MAX_SCALE - ConfigDefaults.MIN_SCALE);
			renderer.drawRoundedRect(trackX, trackY, trackWidth, 4.0F, 2.0F, TRACK_COLOR);
			renderer.drawRoundedRect(trackX, trackY, (float) (trackWidth * normalized), 4.0F, 2.0F, ACCENT_DIM_COLOR);
			renderer.drawCircle((float) (trackX + trackWidth * normalized), trackY + 2.0F, 4.5F, ACCENT_COLOR);
			return;
		}
		if (row.control() == Control.VISIBILITY) {
			Rect chip = new Rect(row.bounds().maxX() - 116.0F, row.bounds().y() + 9.0F, 104.0F, 20.0F);
			renderer.drawRoundedRect(chip.x(), chip.y(), chip.width(), chip.height(), 6.0F, TRACK_COLOR);
			renderer.drawRoundedRectOutline(chip.x(), chip.y(), chip.width(), chip.height(), 6.0F, ACCENT_DIM_COLOR, 1.0F);
			return;
		}

		boolean enabled = value(row.control());
		float toggleWidth = 30.0F;
		float toggleHeight = 14.0F;
		float toggleX = row.bounds().maxX() - toggleWidth - 12.0F;
		float toggleY = row.bounds().y() + (row.bounds().height() - toggleHeight) * 0.5F;
		renderer.drawRoundedRect(toggleX, toggleY, toggleWidth, toggleHeight, 7.0F, enabled ? ACCENT_DIM_COLOR : TOGGLE_OFF_COLOR);
		float knobX = enabled ? toggleX + toggleWidth - 7.0F : toggleX + 7.0F;
		renderer.drawCircle(knobX, toggleY + 7.0F, 5.0F, enabled ? ACCENT_COLOR : 0xFF7C8980);
	}

	private void renderButton(Rect bounds, boolean hovered, boolean primary) {
		int fill = primary ? (hovered ? 0xFF5BAE6B : ACCENT_DIM_COLOR) : (hovered ? 0xFF27352B : TRACK_COLOR);
		renderer.drawRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 7.0F, fill);
		renderer.drawRoundedRectOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 7.0F,
				primary ? ACCENT_COLOR : INNER_OUTLINE_COLOR, 1.0F);
	}

	private void renderIcon(GuiGraphicsExtractor graphics, Rect bounds) {
		RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
		graphics.blit(pipeline, ICON, (int) bounds.x(), (int) bounds.y(), 0.0F, 0.0F,
				(int) bounds.width(), (int) bounds.height(), (int) bounds.width(), (int) bounds.height());
	}

	private void renderPreview(GuiGraphicsExtractor graphics, Rect card) {
		float availableWidth = card.width() - 20.0F;
		float availableHeight = card.height() - 44.0F;
		float scale = Math.min(availableWidth / MockDungeonMapRenderer.WIDTH, availableHeight / MockDungeonMapRenderer.HEIGHT);
		float previewX = card.x() + (card.width() - MockDungeonMapRenderer.WIDTH * scale) * 0.5F;
		float previewY = card.y() + 32.0F + (availableHeight - MockDungeonMapRenderer.HEIGHT * scale) * 0.5F;
		float yaw = minecraft != null && minecraft.player != null ? minecraft.player.getYRot() : -55.0F;
		previewRenderer.render(graphics, previewX, previewY, scale, draft.mapHud(), draft.visibility(), yaw);
	}

	private void renderText(GuiGraphicsExtractor graphics, ScreenLayout layout) {
		Rect panel = layout.panel();
		graphics.text(font, Component.translatable("jester_map.config.title"), (int) panel.x() + 76, (int) panel.y() + 18, TEXT_COLOR, true);
		graphics.text(font, Component.translatable("jester_map.config.subtitle"), (int) panel.x() + 76, (int) panel.y() + 37, MUTED_TEXT_COLOR, false);
		graphics.text(font, "LIVE MOCK MAP", (int) layout.previewCard().x() + 14, (int) layout.previewCard().y() + 12, ACCENT_COLOR, false);
		graphics.text(font, activeTab.heading, (int) layout.settingsCard().x() + 14, (int) layout.settingsCard().y() + 12, ACCENT_COLOR, false);

		for (TabButton tabButton : layout.tabs()) {
			centeredText(graphics, Component.literal(tabButton.tab().label), tabButton.bounds(),
					tabButton.tab() == activeTab ? TEXT_COLOR : MUTED_TEXT_COLOR);
		}
		for (SettingRow row : layout.rows()) {
			graphics.text(font, row.label(), (int) row.bounds().x() + 12, (int) row.bounds().y() + 15, TEXT_COLOR, false);
			if (row.control() == Control.SCALE) {
				String value = Math.round(draft.mapHud().scale() * 100.0D) + "%";
				graphics.text(font, value, (int) row.bounds().maxX() - 142, (int) row.bounds().y() + 15, MUTED_TEXT_COLOR, false);
			} else if (row.control() == Control.VISIBILITY) {
				String value = draft.visibility().mode().displayName();
				int valueX = (int) row.bounds().maxX() - 64 - font.width(value) / 2;
				graphics.text(font, value, valueX, (int) row.bounds().y() + 15, TEXT_COLOR, false);
			}
		}

		centeredText(graphics, Component.translatable("jester_map.config.reset"), layout.resetButton(), MUTED_TEXT_COLOR);
		centeredText(graphics, Component.translatable("jester_map.config.close"), layout.closeButton(), TEXT_COLOR);
		centeredText(graphics, Component.translatable("jester_map.config.save"), layout.saveButton(), TEXT_COLOR);
		if (!status.isBlank()) {
			graphics.text(font, status, (int) panel.x() + 76, (int) panel.y() + 51,
					statusError ? ERROR_COLOR : MUTED_TEXT_COLOR, false);
		}
	}

	private void centeredText(GuiGraphicsExtractor graphics, Component text, Rect bounds, int color) {
		int x = (int) (bounds.x() + (bounds.width() - font.width(text)) * 0.5F);
		int y = (int) (bounds.y() + (bounds.height() - font.lineHeight) * 0.5F + 1.0F);
		graphics.text(font, text, x, y, color, false);
	}

	private void activate(Control control) {
		MapHudConfig map = draft.mapHud();
		VisibilityConfig visibility = draft.visibility();
		draft = switch (control) {
			case ENABLED -> draft.withMapHud(map.withEnabled(!map.enabled()));
			case ROTATION -> draft.withMapHud(map.withRotateWithPlayer(!map.rotateWithPlayer()));
			case BACKGROUND_BLUR -> draft.withMapHud(map.withBackgroundBlur(!map.backgroundBlur()));
			case ROOM_NAMES -> draft.withMapHud(map.withShowRoomNames(!map.showRoomNames()));
			case SECRET_COUNTS -> draft.withMapHud(map.withShowSecretCounts(!map.showSecretCounts()));
			case TEAMMATES -> draft.withMapHud(map.withShowTeammates(!map.showTeammates()));
			case VISIBILITY -> draft.withVisibility(visibility.withMode(visibility.mode().next()));
			case DARKEN_DOORS -> draft.withVisibility(visibility.withDarkenUndiscoveredDoors(!visibility.darkenUndiscoveredDoors()));
			case SCALE -> draft;
		};
		status = "";
	}

	private boolean value(Control control) {
		return switch (control) {
			case ENABLED -> draft.mapHud().enabled();
			case ROTATION -> draft.mapHud().rotateWithPlayer();
			case BACKGROUND_BLUR -> draft.mapHud().backgroundBlur();
			case ROOM_NAMES -> draft.mapHud().showRoomNames();
			case SECRET_COUNTS -> draft.mapHud().showSecretCounts();
			case TEAMMATES -> draft.mapHud().showTeammates();
			case DARKEN_DOORS -> draft.visibility().darkenUndiscoveredDoors();
			case SCALE, VISIBILITY -> false;
		};
	}

	private void updateScale(double mouseX, SettingRow row) {
		float trackWidth = 90.0F;
		float trackX = row.bounds().maxX() - trackWidth - 12.0F;
		double normalized = Math.clamp((mouseX - trackX) / trackWidth, 0.0D, 1.0D);
		double scale = ConfigDefaults.MIN_SCALE + normalized * (ConfigDefaults.MAX_SCALE - ConfigDefaults.MIN_SCALE);
		scale = Math.round(scale * 20.0D) / 20.0D;
		draft = draft.withMapHud(draft.mapHud().withScale(scale));
		status = "";
	}

	private void saveDraft() {
		try {
			ConfigService.getInstance().save(draft);
			draft = ConfigService.getInstance().current();
			status = "Saved to config/jester-map/config.json";
			statusError = false;
		} catch (IOException | RuntimeException exception) {
			status = "Could not save config: " + exception.getMessage();
			statusError = true;
		}
	}

	private ScreenLayout layout() {
		float panelWidth = Math.min(PANEL_WIDTH, Math.max(420, width - 28));
		float panelHeight = Math.min(PANEL_HEIGHT, Math.max(320, height - 24));
		float panelX = (width - panelWidth) * 0.5F;
		float panelY = (height - panelHeight) * 0.5F;
		Rect panel = new Rect(panelX, panelY, panelWidth, panelHeight);
		float contentY = panelY + 80.0F;
		float contentHeight = panelHeight - 138.0F;
		float previewWidth = Math.clamp(panelWidth * 0.38F, 150.0F, 232.0F);
		Rect previewCard = new Rect(panelX + 18.0F, contentY, previewWidth, contentHeight);
		float gap = 12.0F;
		float settingsX = previewCard.maxX() + gap;
		float settingsWidth = panel.maxX() - 18.0F - settingsX;
		float tabHeight = 27.0F;
		float tabGap = 6.0F;
		float tabWidth = (settingsWidth - tabGap * 2.0F) / 3.0F;
		List<TabButton> tabs = List.of(
				new TabButton(new Rect(settingsX, contentY, tabWidth, tabHeight), Tab.MAP),
				new TabButton(new Rect(settingsX + tabWidth + tabGap, contentY, tabWidth, tabHeight), Tab.DETAILS),
				new TabButton(new Rect(settingsX + (tabWidth + tabGap) * 2.0F, contentY, tabWidth, tabHeight), Tab.VISIBILITY)
		);
		Rect settingsCard = new Rect(settingsX, contentY + tabHeight + 8.0F, settingsWidth, contentHeight - tabHeight - 8.0F);
		List<SettingRow> rows = rowsFor(settingsCard);

		float buttonY = panel.maxY() - 45.0F;
		Rect reset = new Rect(panel.x() + 18.0F, buttonY, 112.0F, 28.0F);
		Rect save = new Rect(panel.maxX() - 132.0F, buttonY, 114.0F, 28.0F);
		Rect close = new Rect(save.x() - 86.0F, buttonY, 74.0F, 28.0F);
		Rect icon = new Rect(panel.x() + 24.0F, panel.y() + 14.0F, 42.0F, 42.0F);
		return new ScreenLayout(panel, previewCard, settingsCard, tabs, rows, reset, close, save, icon);
	}

	private List<SettingRow> rowsFor(Rect card) {
		return switch (activeTab) {
			case MAP -> List.of(
					row(card, 36, Control.ENABLED, "Map enabled"),
					row(card, 36 + ROW_HEIGHT, Control.SCALE, "Map scale"),
					row(card, 36 + ROW_HEIGHT * 2, Control.ROTATION, "Rotate with view"),
					row(card, 36 + ROW_HEIGHT * 3, Control.BACKGROUND_BLUR, "Backdrop blur")
			);
			case DETAILS -> List.of(
					row(card, 36, Control.ROOM_NAMES, "Room names"),
					row(card, 36 + ROW_HEIGHT, Control.SECRET_COUNTS, "Secret counts"),
					row(card, 36 + ROW_HEIGHT * 2, Control.TEAMMATES, "Teammate markers")
			);
			case VISIBILITY -> List.of(
					row(card, 36, Control.VISIBILITY, "Room visibility"),
					row(card, 36 + ROW_HEIGHT, Control.DARKEN_DOORS, "Darken hidden doors")
			);
		};
	}

	private SettingRow row(Rect card, float offsetY, Control control, String label) {
		return new SettingRow(new Rect(card.x() + 7.0F, card.y() + offsetY, card.width() - 14.0F, ROW_HEIGHT - 2.0F), control, label);
	}

	private enum Tab {
		MAP("Map", "MAP HUD"),
		DETAILS("Details", "MAP DETAILS"),
		VISIBILITY("Visibility", "VISIBILITY");

		private final String label;
		private final String heading;

		Tab(String label, String heading) {
			this.label = label;
			this.heading = heading;
		}
	}

	private enum Control {
		ENABLED,
		SCALE,
		ROTATION,
		BACKGROUND_BLUR,
		VISIBILITY,
		ROOM_NAMES,
		SECRET_COUNTS,
		TEAMMATES,
		DARKEN_DOORS
	}

	private record SettingRow(Rect bounds, Control control, String label) {
	}

	private record TabButton(Rect bounds, Tab tab) {
	}

	private record ScreenLayout(
			Rect panel,
			Rect previewCard,
			Rect settingsCard,
			List<TabButton> tabs,
			List<SettingRow> rows,
			Rect resetButton,
			Rect closeButton,
			Rect saveButton,
			Rect icon
	) {
	}

	private record Rect(float x, float y, float width, float height) {
		private float maxX() {
			return x + width;
		}

		private float maxY() {
			return y + height;
		}

		private boolean contains(double pointX, double pointY) {
			return pointX >= x && pointX <= maxX() && pointY >= y && pointY <= maxY();
		}
	}
}
