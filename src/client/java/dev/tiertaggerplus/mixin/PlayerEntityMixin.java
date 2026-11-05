package dev.tiertaggerplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.tiertaggerplus.render.Tags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Adds tiers to the player's display name, which the nametag renderer uses. */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text tiertaggerplus$decorate(Text original) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        // only on the client world, so the integrated server's chat/death messages are untouched
        if (self.getEntityWorld() == null || !self.getEntityWorld().isClient()) return original;
        return Tags.decorate(self.getGameProfile().name(), original);
    }
}
