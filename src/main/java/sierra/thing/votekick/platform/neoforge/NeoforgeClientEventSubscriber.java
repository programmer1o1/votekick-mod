package sierra.thing.votekick.platform.neoforge;

//? if neoforge {

/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
//? if >=1.20.6 {
/^import net.neoforged.fml.common.EventBusSubscriber;
^///?} else {
import net.neoforged.fml.common.Mod.EventBusSubscriber;
//?}
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.client.VoteKickHud;

//? if >=1.21.6 {
/^@EventBusSubscriber(modid = VoteKickMod.MOD_ID, value = Dist.CLIENT)
^///?} else {
//? if >=1.20.6 {
/^@EventBusSubscriber(modid = VoteKickMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
^///?} else {
@EventBusSubscriber(modid = VoteKickMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
//?}
//?}
public final class NeoforgeClientEventSubscriber {
    private NeoforgeClientEventSubscriber() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        //? if >=1.21 {
        /^float tickDelta = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        ^///?} else {
        float tickDelta = event.getPartialTick();
        //?}
        VoteKickHud.onHudRender(event.getGuiGraphics(), tickDelta);
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        VoteKickHud.onClientDisconnect();
        VoteKickHud.resetVoteState();
    }
}
*///?}
