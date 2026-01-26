package sierra.thing.votekick.platform.neoforge;

//? if neoforge {

/*import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
//? if >=1.20.6 {
/^import net.neoforged.fml.common.EventBusSubscriber;
^///?} else {
import net.neoforged.fml.common.Mod.EventBusSubscriber;
//?}
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.client.VoteKickHud;
import sierra.thing.votekick.commands.VoteKickCommand;
import sierra.thing.votekick.network.CastVotePayload;
import sierra.thing.votekick.network.HideVotePanelPayload;
import sierra.thing.votekick.network.PayloadIo;
import sierra.thing.votekick.network.ShowVotePanelPayload;
import sierra.thing.votekick.network.UpdateVotePanelPayload;
//? if >=1.20.6 {
/^import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
^///?} else {
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
//?}

//? if >=1.21.6 {
/^@EventBusSubscriber(modid = VoteKickMod.MOD_ID)
^///?} else {
@EventBusSubscriber(modid = VoteKickMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
//?}
public final class NeoforgeNetworkHandler {
    private NeoforgeNetworkHandler() {
    }

    //? if >=1.20.6 {
    /^@SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VoteKickMod.VERSION);
        registrar.playToServer(CastVotePayload.TYPE, CastVotePayload.STREAM_CODEC, NeoforgeNetworkHandler::handleCastVote);
        registrar.playToClient(ShowVotePanelPayload.TYPE, ShowVotePanelPayload.STREAM_CODEC, NeoforgeNetworkHandler::handleShowVotePanel);
        registrar.playToClient(UpdateVotePanelPayload.TYPE, UpdateVotePanelPayload.STREAM_CODEC, NeoforgeNetworkHandler::handleUpdateVotePanel);
        registrar.playToClient(HideVotePanelPayload.TYPE, HideVotePanelPayload.STREAM_CODEC, NeoforgeNetworkHandler::handleHideVotePanel);
    }

    private static void handleCastVote(CastVotePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                VoteKickCommand.castVote(player, payload.voteYes());
            }
        });
    }

    private static void handleShowVotePanel(ShowVotePanelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> VoteKickHud.showVotePanel(
                payload.title(),
                payload.subtitle(),
                payload.time(),
                payload.yesVotes(),
                payload.noVotes(),
                payload.votesNeeded(),
                payload.isTarget()
        ));
    }

    private static void handleUpdateVotePanel(UpdateVotePanelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> VoteKickHud.updateVotePanel(payload.time(), payload.yesVotes(), payload.noVotes()));
    }

    private static void handleHideVotePanel(HideVotePanelPayload payload, IPayloadContext context) {
        context.enqueueWork(VoteKickHud::hideVotePanel);
    }
    ^///?} else {
    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar(VoteKickMod.MOD_ID);
        registrar.play(CastVotePayload.ID, PayloadIo::readCastVote, NeoforgeNetworkHandler::handleCastVote);
        registrar.play(ShowVotePanelPayload.ID, PayloadIo::readShowVotePanel, NeoforgeNetworkHandler::handleShowVotePanel);
        registrar.play(UpdateVotePanelPayload.ID, PayloadIo::readUpdateVotePanel, NeoforgeNetworkHandler::handleUpdateVotePanel);
        registrar.play(HideVotePanelPayload.ID, PayloadIo::readHideVotePanel, NeoforgeNetworkHandler::handleHideVotePanel);
    }

    private static void handleCastVote(CastVotePayload payload, PlayPayloadContext context) {
        context.workHandler().execute(() -> context.player().ifPresent(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                VoteKickCommand.castVote(serverPlayer, payload.voteYes());
            }
        }));
    }

    private static void handleShowVotePanel(ShowVotePanelPayload payload, PlayPayloadContext context) {
        context.workHandler().execute(() -> VoteKickHud.showVotePanel(
                payload.title(),
                payload.subtitle(),
                payload.time(),
                payload.yesVotes(),
                payload.noVotes(),
                payload.votesNeeded(),
                payload.isTarget()
        ));
    }

    private static void handleUpdateVotePanel(UpdateVotePanelPayload payload, PlayPayloadContext context) {
        context.workHandler().execute(() -> VoteKickHud.updateVotePanel(payload.time(), payload.yesVotes(), payload.noVotes()));
    }

    private static void handleHideVotePanel(HideVotePanelPayload payload, PlayPayloadContext context) {
        context.workHandler().execute(VoteKickHud::hideVotePanel);
    }
    //?}
}
*///?}
