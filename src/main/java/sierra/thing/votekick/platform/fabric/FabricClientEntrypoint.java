package sierra.thing.votekick.platform.fabric;

//? if fabric {

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import sierra.thing.votekick.client.VoteKickClient;

public class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        VoteKickClient.initClient();

        KeyMapping yesKey = KeyBindingHelper.registerKeyBinding(VoteKickClient.createVoteYesKey());
        KeyMapping noKey = KeyBindingHelper.registerKeyBinding(VoteKickClient.createVoteNoKey());
        VoteKickClient.setKeyMappings(yesKey, noKey);

        FabricClientEventSubscriber.register();
        FabricNetworkHandler.registerClient();
    }
}
//?}
