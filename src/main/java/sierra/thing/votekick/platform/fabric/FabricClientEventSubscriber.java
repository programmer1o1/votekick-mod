package sierra.thing.votekick.platform.fabric;

//? if fabric {

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import sierra.thing.votekick.client.VoteKickHud;

public final class FabricClientEventSubscriber {
    private FabricClientEventSubscriber() {
    }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            VoteKickHud.onClientDisconnect();
            VoteKickHud.resetVoteState();
        });

        //? if >=1.21 {
        /*HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) ->
                VoteKickHud.onHudRender(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false))
        );
        *///?} else {
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) ->
                VoteKickHud.onHudRender(guiGraphics, tickDelta)
        );
        //?}
    }
}
//?}
