package sierra.thing.votekick.platform.fabric;

//? if fabric {

import net.fabricmc.loader.api.FabricLoader;
import sierra.thing.votekick.platform.Platform;

import java.nio.file.Path;

public class FabricPlatform implements Platform {
    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public ModLoader loader() {
        return ModLoader.FABRIC;
    }

    @Override
    public String mcVersion() {
        return FabricLoader.getInstance().getRawGameVersion();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
//?}
