package com.cobblemon.yajatkaul.mega_showdown.mixin.battle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.storage.player.GeneralPlayerData;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobblemon.yajatkaul.mega_showdown.mixin.accessors.ShowdownActionRequestAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ShowdownActionRequest.class, remap = false)
public class ShowdownActionRequestMixin {
    @Inject(method = "sanitize", at = @At("TAIL"), remap = false)
    private void afterSanitize(PokemonBattle battle, BattleActor battleActor, CallbackInfo ci) {
        ResourceLocation dynamaxBandId = ResourceLocation.fromNamespaceAndPath("cobblemon", "dynamax_band");

        for (ServerPlayer player : battle.getPlayers()) {
            GeneralPlayerData data = Cobblemon.INSTANCE.getPlayerDataManager().getGenericData(player);
            boolean hasBand = data.getKeyItems().contains(dynamaxBandId);

            if (player.getUUID().equals(battleActor.getUuid())) {
                List<ShowdownMoveset> activeMovesets = ((ShowdownActionRequestAccessor) this).getActive();
                if (!hasBand && activeMovesets != null) {
                    for (ShowdownMoveset moveset : activeMovesets) {
                        moveset.blockGimmick(ShowdownMoveset.Gimmick.DYNAMAX);
                        moveset.setMaxMoves(null);
                    }
                }
            }
        }
    }
}