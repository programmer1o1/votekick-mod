package sierra.thing.votekick.platform.neoforge;

//? if neoforge {

/*import net.neoforged.bus.api.SubscribeEvent;
//? if >=1.20.6 {
/^import net.neoforged.fml.common.EventBusSubscriber;
^///?} else {
import net.neoforged.fml.common.Mod.EventBusSubscriber;
//?}
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
//? if >=1.20.6 {
/^import net.neoforged.neoforge.event.tick.ServerTickEvent;
^///?} else {
import net.neoforged.neoforge.event.TickEvent;
//?}
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
//? if >=1.21.9 {
/^import net.minecraft.server.MinecraftServer;
^///?}
import net.minecraft.server.level.ServerPlayer;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.permissions.VoteKickPermissions;

@EventBusSubscriber(modid = VoteKickMod.MOD_ID)
public final class NeoforgeEventSubscriber {
    private NeoforgeEventSubscriber() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        VoteKickMod.registerCommands(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPermissionNodes(PermissionGatherEvent.Nodes event) {
        VoteKickPermissions.registerNeoForgeNodes(event);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        VoteKickMod.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        VoteKickMod.onServerStopping(event.getServer());
    }

    @SubscribeEvent
    //? if >=1.20.6 {
    /^public static void onServerTick(ServerTickEvent.Post event) {
        VoteKickMod.onServerTick(event.getServer());
    }
    ^///?} else {
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            VoteKickMod.onServerTick(event.getServer());
        }
    }
    //?}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VoteKickMod.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            //? if >=1.21.9 {
            /^MinecraftServer server = player.level().getServer();
            if (server != null) {
                VoteKickMod.onPlayerDisconnect(player, server);
            }
            ^///?} else {
            if (player.getServer() != null) {
                VoteKickMod.onPlayerDisconnect(player, player.getServer());
            }
            //?}
        }
    }
}
*///?}
