package heos.integrations;

import heos.utils.HeosLogger;

//? if >= 1.21.11 {
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
//?}

/**
 * Enables Fabric recipe sync for clientside recipe viewers when Fabric exposes the recipe sync API.
 */
public final class RecipeSyncFeature {
    private RecipeSyncFeature() {
    }

    public static void initialize() {
        //? if >= 1.21.11 {
        synchronizeModRecipeSerializers();
        //?}
    }

    //? if >= 1.21.11 {
    private static void synchronizeModRecipeSerializers() {
        int synchronizedCount = 0;

        for (RecipeSerializer<?> serializer : BuiltInRegistries.RECIPE_SERIALIZER) {
            if ("minecraft".equals(BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer).getNamespace())) {
                continue;
            }

            try {
                RecipeSynchronization.synchronizeRecipeSerializer(serializer);
                synchronizedCount++;
            } catch (RuntimeException exception) {
                HeosLogger.warn("Failed to enable recipe sync for serializer "
                        + BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer) + ": " + exception.getMessage());
            }
        }

        HeosLogger.info("Enabled Fabric recipe sync for " + synchronizedCount + " mod recipe serializers");
    }
    //?}
}
