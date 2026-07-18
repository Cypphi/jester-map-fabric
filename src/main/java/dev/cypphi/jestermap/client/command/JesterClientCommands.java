package dev.cypphi.jestermap.client.command;

import dev.cypphi.jestermap.client.screen.JesterConfigScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicBoolean;

public final class JesterClientCommands {
	private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

	private JesterClientCommands() {
	}

	public static void initialize() {
		if (!INITIALIZED.compareAndSet(false, true)) {
			return;
		}
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommands.literal("jester").executes(context -> {
					Minecraft.getInstance().execute(() -> JesterConfigScreen.open(null));
					return 1;
				})
		));
	}
}
