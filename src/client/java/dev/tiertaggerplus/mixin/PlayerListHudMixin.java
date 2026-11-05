package dev.tiertaggerplus.mixin;

import dev.tiertaggerplus.config.TTPConfig;
import dev.tiertaggerplus.render.Tags;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds tiers in the tab list. */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void tiertaggerplus$decorate(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (!TTPConfig.get().showInTab) return;
        cir.setReturnValue(Tags.decorate(entry.getProfile().name(), cir.getReturnValue()));
    }
}
