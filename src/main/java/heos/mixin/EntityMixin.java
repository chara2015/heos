package heos.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Base entity mixin for protection features
 */
@Mixin(Entity.class)
public class EntityMixin {
    
    /**
     * Makes unauthenticated players invisible to mobs
     */
    @ModifyReturnValue(method = "isInvisible()Z", at = @At("RETURN"))
    public boolean heos$isInvisible(boolean original) {
        return original;
    }
    
    /**
     * Makes unauthenticated players invulnerable
     */
    @ModifyReturnValue(method = "isInvulnerable()Z", at = @At("RETURN"))
    public boolean heos$isInvulnerable(boolean original) {
        return original;
    }
}
