package com.cobblemon.yajatkaul.mega_showdown.mixin.pokemon;

import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PokemonClientDelegate.class, remap = false)
public class PokemonClientDelegateMixin {
    @Shadow
    public PokemonEntity currentEntity;

    @Inject(
            method = "onSyncedDataUpdated",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    public void inject(TrackedData<?> data, CallbackInfo ci){
        var currentAspects = currentEntity.getAspects();
        var pokemon = currentEntity.getPokemon();
        pokemon.setForcedAspects(currentAspects);
        pokemon.updateForm();
        currentEntity.calculateDimensions();
    }
}
