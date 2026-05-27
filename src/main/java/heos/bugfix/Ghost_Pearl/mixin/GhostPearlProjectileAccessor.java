package heos.bugfix.Ghost_Pearl.mixin;

//? if >= 1.21.2 {
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Projectile.class)
public interface GhostPearlProjectileAccessor {
    @Accessor("owner")
    EntityReference<Entity> heos$getOwnerReference();
}
//?}
