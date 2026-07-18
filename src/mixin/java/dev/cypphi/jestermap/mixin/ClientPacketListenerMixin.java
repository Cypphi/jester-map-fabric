package dev.cypphi.jestermap.mixin;

import dev.cypphi.jestermap.client.hud.DungeonMapController;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleMapItemData", at = @At("RETURN"))
	private void jesterMap$afterMapData(ClientboundMapItemDataPacket packet, CallbackInfo callbackInfo) {
		DungeonMapController.onMapDataUpdated();
	}
}
