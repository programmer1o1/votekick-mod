// VoteKickModMenuIntegration.java
package sierra.thing.votekick.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import sierra.thing.votekick.client.config.VoteKickConfigScreen;

public class VoteKickModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return VoteKickConfigScreen::new;
    }
}