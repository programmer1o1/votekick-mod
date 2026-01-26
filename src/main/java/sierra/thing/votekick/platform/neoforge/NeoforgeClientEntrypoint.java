package sierra.thing.votekick.platform.neoforge;

//? if neoforge {

/*//? if >=1.20.6 {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.client.config.VoteKickConfigScreen;

@Mod(value = VoteKickMod.MOD_ID, dist = Dist.CLIENT)
public class NeoforgeClientEntrypoint {
    public NeoforgeClientEntrypoint(ModContainer container) {
        IConfigScreenFactory factory = (modContainer, parent) -> new VoteKickConfigScreen(parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }
}
//?}
*///?}
