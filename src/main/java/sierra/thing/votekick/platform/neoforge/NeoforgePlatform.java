package sierra.thing.votekick.platform.neoforge;

//? if neoforge {

/*import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import sierra.thing.votekick.platform.Platform;

import java.nio.file.Path;

public class NeoforgePlatform implements Platform {
    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public ModLoader loader() {
        return ModLoader.NEOFORGE;
    }

    @Override
    public String mcVersion() {
        return "";
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        //? if >=1.21.9 {
        /^return !FMLLoader.getCurrent().isProduction();
        ^///?} else {
        return !FMLLoader.isProduction();
        //?}
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
*///?}
