package sierra.thing.votekick.platform.fabric;

//? if fabric {

import net.fabricmc.api.ModInitializer;
import sierra.thing.votekick.VoteKickMod;

public class FabricEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        VoteKickMod.init();
        FabricEventSubscriber.register();
        FabricNetworkHandler.register();
    }
}
//?}
