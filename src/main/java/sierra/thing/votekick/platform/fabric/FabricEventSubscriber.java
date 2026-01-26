package sierra.thing.votekick.platform.fabric;

//? if fabric {

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import sierra.thing.votekick.VoteKickMod;

public final class FabricEventSubscriber {
    private FabricEventSubscriber() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                VoteKickMod.registerCommands(dispatcher)
        );

        ServerLifecycleEvents.SERVER_STARTING.register(VoteKickMod::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(VoteKickMod::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(VoteKickMod::onServerTick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                VoteKickMod.onPlayerJoin(handler.getPlayer())
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                VoteKickMod.onPlayerDisconnect(handler.getPlayer(), server)
        );
    }
}
//?}
