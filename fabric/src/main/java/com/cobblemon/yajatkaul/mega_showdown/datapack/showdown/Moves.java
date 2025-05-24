package com.cobblemon.yajatkaul.mega_showdown.datapack.showdown;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.data.DataRegistry;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.reactive.SimpleObservable;
import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import com.cobblemon.mod.relocations.graalvm.polyglot.Value;
import com.cobblemon.yajatkaul.mega_showdown.MegaShowdown;
import com.cobblemon.yajatkaul.mega_showdown.utility.datapack.NewMove;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import kotlin.Unit;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Moves implements DataRegistry {
    private static final Identifier ID = Identifier.of(MegaShowdown.MOD_ID, "showdown/moves");
    private static final SimpleObservable<Moves> OBSERVABLE = new SimpleObservable<>();

    private final Map<String, String> moveScripts = new HashMap<>();

    public static final Moves INSTANCE = new Moves();

    Gson gson = new Gson();

    private Moves() {
        OBSERVABLE.subscribe(Priority.NORMAL , this::movesLoad);
    }

    private Unit movesLoad(Moves move) {
        Cobblemon.INSTANCE.getShowdownThread().queue(showdownService -> {
            if(showdownService instanceof GraalShowdownService service){
                Value receiveMoveDataFn = service.context.getBindings("js").getMember("receiveMoveData");
                //TODO FIX THIS
                if(receiveMoveDataFn == null){
                    return Unit.INSTANCE;
                }
                for (Map.Entry<String, String> entry : Moves.INSTANCE.getMoveScripts().entrySet()) {
                    String moveId = entry.getKey();
                    String js = entry.getValue().replace("\n", " ");
                    JsonObject moveData = gson.fromJson(receiveMoveDataFn.execute(moveId, js).asString(), JsonObject.class);
                    MoveTemplate newMove = NewMove.INSTANCE.createMoveTemplate(moveData, moveId);
                    NewMove.INSTANCE.register(newMove);
                }
            }
            return Unit.INSTANCE;
        });
        return Unit.INSTANCE;
    }

    public Map<String, String> getMoveScripts() {
        return moveScripts;
    }

    @NotNull
    @Override
    public Identifier getId() {
        return ID;
    }


    @NotNull
    @Override
    public SimpleObservable<? extends DataRegistry> getObservable() {
        return OBSERVABLE;
    }

    @Override
    public void reload(@NotNull ResourceManager resourceManager) {
        moveScripts.clear();
        resourceManager.findAllResources("showdown/moves", path -> path.getPath().endsWith(".js")).forEach((id, resource) -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getFirst().getInputStream(), StandardCharsets.UTF_8))) {
                String js = reader.lines().collect(Collectors.joining("\n"));
                String moveId = new File(id.getPath()).getName().replace(".js", "");
                moveScripts.put(moveId, js);
            } catch (IOException e) {
                MegaShowdown.LOGGER.error("Failed to load move script: {} {}", id, e);
            }
        });
        OBSERVABLE.emit(this);
    }

    @NotNull
    @Override
    public ResourceType getType() {
        return ResourceType.SERVER_DATA;
    }

    @Override
    public void sync(@NotNull ServerPlayerEntity serverPlayerEntity) {

    }
}
