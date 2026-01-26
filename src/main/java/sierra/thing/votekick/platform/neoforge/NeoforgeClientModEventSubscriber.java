package sierra.thing.votekick.platform.neoforge;

//? if neoforge {

/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
//? if >=1.20.6 {
/^import net.neoforged.fml.common.EventBusSubscriber;
^///?} else {
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import sierra.thing.votekick.client.config.VoteKickConfigScreen;
//?}
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.client.VoteKickClient;

//? if >=1.21.6 {
/^@EventBusSubscriber(modid = VoteKickMod.MOD_ID, value = Dist.CLIENT)
^///?} else {
@EventBusSubscriber(modid = VoteKickMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
//?}
public final class NeoforgeClientModEventSubscriber {
    private NeoforgeClientModEventSubscriber() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        VoteKickClient.initClient();
        //? if >=1.20.6 {
        //?} else {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new VoteKickConfigScreen(parent))
        );
        //?}
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        KeyMapping yesKey = VoteKickClient.createVoteYesKey();
        KeyMapping noKey = VoteKickClient.createVoteNoKey();
        event.register(yesKey);
        event.register(noKey);
        VoteKickClient.setKeyMappings(yesKey, noKey);
    }
}
*///?}
