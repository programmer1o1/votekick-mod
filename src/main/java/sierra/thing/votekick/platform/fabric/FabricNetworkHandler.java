package sierra.thing.votekick.platform.fabric;

//? if fabric {

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import sierra.thing.votekick.client.VoteKickHud;
import sierra.thing.votekick.commands.VoteKickCommand;
import sierra.thing.votekick.network.CastVotePayload;
import sierra.thing.votekick.network.HideVotePanelPayload;
import sierra.thing.votekick.network.PayloadIo;
import sierra.thing.votekick.network.PayloadRegistry;
import sierra.thing.votekick.network.ShowVotePanelPayload;
import sierra.thing.votekick.network.UpdateVotePanelPayload;

public final class FabricNetworkHandler {
    private FabricNetworkHandler() {
    }

    public static void register() {
        PayloadRegistry.register();

        //? if >=1.20.6 {
        /*ServerPlayNetworking.registerGlobalReceiver(CastVotePayload.TYPE, (payload, context) -> {
            VoteKickCommand.castVote(context.player(), payload.voteYes());
        });
        *///?} else {
        ServerPlayNetworking.registerGlobalReceiver(CastVotePayload.ID, (server, player, handler, buf, responseSender) -> {
            CastVotePayload payload = PayloadIo.readCastVote(buf);
            server.execute(() -> VoteKickCommand.castVote(player, payload.voteYes()));
        });
        //?}
    }

    public static void registerClient() {
        //? if >=1.20.6 {
        /*ClientPlayNetworking.registerGlobalReceiver(ShowVotePanelPayload.TYPE, (payload, context) -> {
            VoteKickHud.showVotePanel(
                    payload.title(),
                    payload.subtitle(),
                    payload.time(),
                    payload.yesVotes(),
                    payload.noVotes(),
                    payload.votesNeeded(),
                    payload.isTarget()
            );
        });
        ClientPlayNetworking.registerGlobalReceiver(UpdateVotePanelPayload.TYPE, (payload, context) -> {
            VoteKickHud.updateVotePanel(payload.time(), payload.yesVotes(), payload.noVotes());
        });
        ClientPlayNetworking.registerGlobalReceiver(HideVotePanelPayload.TYPE, (payload, context) -> {
            VoteKickHud.hideVotePanel();
        });
        *///?} else {
        ClientPlayNetworking.registerGlobalReceiver(ShowVotePanelPayload.ID, (client, handler, buf, responseSender) -> {
            ShowVotePanelPayload payload = PayloadIo.readShowVotePanel(buf);
            client.execute(() -> VoteKickHud.showVotePanel(
                    payload.title(),
                    payload.subtitle(),
                    payload.time(),
                    payload.yesVotes(),
                    payload.noVotes(),
                    payload.votesNeeded(),
                    payload.isTarget()
            ));
        });
        ClientPlayNetworking.registerGlobalReceiver(UpdateVotePanelPayload.ID, (client, handler, buf, responseSender) -> {
            UpdateVotePanelPayload payload = PayloadIo.readUpdateVotePanel(buf);
            client.execute(() -> VoteKickHud.updateVotePanel(payload.time(), payload.yesVotes(), payload.noVotes()));
        });
        ClientPlayNetworking.registerGlobalReceiver(HideVotePanelPayload.ID, (client, handler, buf, responseSender) -> {
            client.execute(VoteKickHud::hideVotePanel);
        });
        //?}
    }
}
//?}
