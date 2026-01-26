package sierra.thing.votekick.permissions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21.11 {
/*import net.minecraft.server.permissions.Permissions;
*///?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.platform.Platform;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

//? if neoforge {
import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
//?}

public final class VoteKickPermissions {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    public static final String NODE_START = VoteKickMod.MOD_ID + ".start";
    public static final String NODE_VOTE = VoteKickMod.MOD_ID + ".vote";
    public static final String NODE_ADMIN = VoteKickMod.MOD_ID + ".admin";
    public static final String NODE_EXEMPT = VoteKickMod.MOD_ID + ".exempt";

    private static final String FABRIC_PERMISSIONS_MOD = "fabric-permissions-api-v0";
    private static final String FABRIC_PERMISSIONS_CLASS = "me.lucko.fabric.api.permissions.v0.Permissions";
    private static final String LUCKPERMS_MOD_ID = "luckperms";
    private static final String LUCKPERMS_PROVIDER_CLASS = "net.luckperms.api.LuckPermsProvider";
    private static final String LUCKPERMS_USER_CLASS = "net.luckperms.api.model.user.User";
    private static final String LUCKPERMS_CONTEXT_MANAGER_CLASS = "net.luckperms.api.context.ContextManager";
    private static final String LUCKPERMS_QUERY_OPTIONS_CLASS = "net.luckperms.api.query.QueryOptions";

    private static Method fabricCheckPlayer;
    private static Method fabricCheckSource;
    private static boolean fabricCheckPlayerHasDefault;
    private static boolean fabricCheckSourceHasDefault;
    private static boolean fabricAvailable;
    private static boolean loggedFabricError;
    private static boolean luckPermsChecked;
    private static boolean luckPermsAvailable;
    private static boolean loggedLuckPermsError;

    //? if neoforge {
    private static final PermissionNode<Boolean> START_NODE = createBooleanNode("start",
            (player, uuid, ctx) -> defaultHasPermission(player, VoteKickMod.getConfig().getPermissionStartDefaultLevel()),
            "Start vote kicks");
    private static final PermissionNode<Boolean> VOTE_NODE = createBooleanNode("vote",
            (player, uuid, ctx) -> defaultHasPermission(player, VoteKickMod.getConfig().getPermissionVoteDefaultLevel()),
            "Cast votes on active kick votes");
    private static final PermissionNode<Boolean> ADMIN_NODE = createBooleanNode("admin",
            (player, uuid, ctx) -> defaultHasPermission(player, VoteKickMod.getConfig().getPermissionAdminDefaultLevel()),
            "Manage vote kick admin actions");
    private static final PermissionNode<Boolean> EXEMPT_NODE = createBooleanNode("exempt",
            (player, uuid, ctx) -> defaultHasPermission(player, VoteKickMod.getConfig().getPermissionExemptDefaultLevel()),
            "Exempt a player from being vote-kicked");
    //?}

    static {
        initFabricPermissions();
    }

    private VoteKickPermissions() {
    }

    public static boolean canStartVote(ServerPlayer player) {
        int defaultLevel = VoteKickMod.getConfig().getPermissionStartDefaultLevel();
        return checkPlayerPermission(player, NODE_START, defaultHasPermission(player, defaultLevel), defaultLevel);
    }

    public static boolean canVote(ServerPlayer player) {
        int defaultLevel = VoteKickMod.getConfig().getPermissionVoteDefaultLevel();
        return checkPlayerPermission(player, NODE_VOTE, defaultHasPermission(player, defaultLevel), defaultLevel);
    }

    public static boolean canAdmin(CommandSourceStack source) {
        if (source == null) {
            return false;
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            return canAdmin(player);
        }

        return true;
    }

    public static boolean canAdmin(ServerPlayer player) {
        int defaultLevel = VoteKickMod.getConfig().getPermissionAdminDefaultLevel();
        return checkPlayerPermission(player, NODE_ADMIN, defaultHasPermission(player, defaultLevel), defaultLevel);
    }

    public static boolean isExempt(ServerPlayer player) {
        int defaultLevel = VoteKickMod.getConfig().getPermissionExemptDefaultLevel();
        return checkPlayerPermission(player, NODE_EXEMPT, defaultHasPermission(player, defaultLevel), defaultLevel);
    }

    //? if neoforge {
    public static void registerNeoForgeNodes(PermissionGatherEvent.Nodes event) {
        if (event == null) {
            return;
        }
        event.addNodes(START_NODE, VOTE_NODE, ADMIN_NODE, EXEMPT_NODE);
    }
    //?}

    private static boolean checkPlayerPermission(ServerPlayer player, String node, boolean defaultValue, int defaultLevel) {
        if (player == null) {
            return false;
        }

        if (!VoteKickMod.getConfig().isPermissionsEnabled()) {
            return defaultValue;
        }

        Boolean fabricResult = checkFabricPermission(player, node, defaultLevel);
        if (fabricResult != null) {
            return fabricResult;
        }

        Boolean luckPermsResult = checkLuckPermsPermission(player, node);
        if (luckPermsResult != null) {
            return luckPermsResult;
        }

        //? if neoforge {
        Boolean neoResult = checkNeoForgePermission(player, node);
        if (neoResult != null) {
            return neoResult;
        }
        //?}

        return defaultValue;
    }

    private static boolean defaultHasPermission(ServerPlayer player, int defaultLevel) {
        if (player == null) {
            return false;
        }
        //? if >=1.21.11 {
        /*if (defaultLevel <= 0) {
            return true;
        }
        net.minecraft.server.permissions.Permission permission = switch (defaultLevel) {
            case 1 -> Permissions.COMMANDS_MODERATOR;
            case 2 -> Permissions.COMMANDS_GAMEMASTER;
            case 3 -> Permissions.COMMANDS_ADMIN;
            default -> Permissions.COMMANDS_OWNER;
        };
        return player.permissions().hasPermission(permission);
        *///?} else {
        return player.hasPermissions(defaultLevel);
        //?}
    }

    private static Boolean checkFabricPermission(ServerPlayer player, String node, int defaultLevel) {
        if (!fabricAvailable || player == null) {
            return null;
        }

        try {
            if (fabricCheckPlayer != null) {
                return (Boolean) invokeFabricCheck(fabricCheckPlayer, fabricCheckPlayerHasDefault, player, node, defaultLevel);
            }

            if (fabricCheckSource != null) {
                CommandSourceStack source = player.createCommandSourceStack();
                return (Boolean) invokeFabricCheck(fabricCheckSource, fabricCheckSourceHasDefault, source, node, defaultLevel);
            }
        } catch (Exception e) {
            logFabricErrorOnce(e);
        }

        return null;
    }

    private static Boolean checkLuckPermsPermission(ServerPlayer player, String node) {
        if (player == null || !isLuckPermsAvailable()) {
            return null;
        }

        try {
            Object api = Class.forName(LUCKPERMS_PROVIDER_CLASS).getMethod("get").invoke(null);
            if (api == null) {
                return null;
            }

            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            if (userManager == null) {
                return null;
            }

            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class)
                    .invoke(userManager, player.getUUID());
            if (user == null) {
                return null;
            }

            Object contextManager = api.getClass().getMethod("getContextManager").invoke(api);
            Object queryOptions = resolveLuckPermsQueryOptions(contextManager, user);

            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            if (cachedData == null) {
                return null;
            }

            Object permissionData = resolveLuckPermsPermissionData(cachedData, queryOptions);
            if (permissionData == null) {
                return null;
            }

            Method checkPermission = findMethod(permissionData.getClass(), "checkPermission", 1, String.class);
            if (checkPermission == null) {
                checkPermission = findMethod(permissionData.getClass(), "checkPermission", 1);
            }
            if (checkPermission == null) {
                return null;
            }

            Object result = checkPermission.invoke(permissionData, node);
            return coerceLuckPermsResult(result);
        } catch (Throwable t) {
            logLuckPermsErrorOnce(t);
            return null;
        }
    }

    private static Object resolveLuckPermsQueryOptions(Object contextManager, Object user) throws Exception {
        if (contextManager == null) {
            return null;
        }

        Method queryOptionsMethod = findMethod(contextManager.getClass(), "getQueryOptions", 1, user.getClass());
        if (queryOptionsMethod != null) {
            return queryOptionsMethod.invoke(contextManager, user);
        }

        Method noArgQueryOptions = findMethod(contextManager.getClass(), "getQueryOptions", 0);
        if (noArgQueryOptions != null) {
            return noArgQueryOptions.invoke(contextManager);
        }

        return null;
    }

    private static Object resolveLuckPermsPermissionData(Object cachedData, Object queryOptions) throws Exception {
        Method permissionDataMethod = null;
        if (queryOptions != null) {
            permissionDataMethod = findMethod(cachedData.getClass(), "getPermissionData", 1, queryOptions.getClass());
        }

        if (permissionDataMethod != null) {
            return permissionDataMethod.invoke(cachedData, queryOptions);
        }

        Method noArgPermissionData = findMethod(cachedData.getClass(), "getPermissionData", 0);
        if (noArgPermissionData != null) {
            return noArgPermissionData.invoke(cachedData);
        }

        return null;
    }

    private static Boolean coerceLuckPermsResult(Object result) throws Exception {
        if (result == null) {
            return null;
        }

        if (result instanceof Boolean booleanResult) {
            return booleanResult;
        }

        Method asBoolean = findMethod(result.getClass(), "asBoolean", 0);
        if (asBoolean != null) {
            Object value = asBoolean.invoke(result);
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
        }

        Method getValue = findMethod(result.getClass(), "getValue", 0);
        if (getValue != null) {
            Object value = getValue.invoke(result);
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
        }

        if (result.getClass().isEnum()) {
            String name = ((Enum<?>) result).name();
            if ("TRUE".equalsIgnoreCase(name)) {
                return true;
            }
            if ("FALSE".equalsIgnoreCase(name)) {
                return false;
            }
        }

        return null;
    }

    private static Object invokeFabricCheck(Method method, boolean hasDefault, Object target, String node, int defaultLevel) throws Exception {
        if (hasDefault) {
            return method.invoke(null, target, node, defaultLevel);
        }

        return method.invoke(null, target, node);
    }

    private static void initFabricPermissions() {
        if (VoteKickMod.platform().loader() != Platform.ModLoader.FABRIC) {
            return;
        }

        if (!VoteKickMod.platform().isModLoaded(FABRIC_PERMISSIONS_MOD)) {
            return;
        }

        try {
            Class<?> permissionsClass = Class.forName(FABRIC_PERMISSIONS_CLASS);
            for (Method method : permissionsClass.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (!"check".equals(method.getName())) {
                    continue;
                }
                Class<?> returnType = method.getReturnType();
                if (!(returnType.equals(boolean.class) || returnType.equals(Boolean.class))) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 3 && params[1].equals(String.class) && params[2].equals(int.class)) {
                    if (ServerPlayer.class.isAssignableFrom(params[0])) {
                        fabricCheckPlayer = method;
                        fabricCheckPlayerHasDefault = true;
                    } else if (CommandSourceStack.class.isAssignableFrom(params[0])) {
                        fabricCheckSource = method;
                        fabricCheckSourceHasDefault = true;
                    }
                } else if (params.length == 2 && params[1].equals(String.class)) {
                    if (ServerPlayer.class.isAssignableFrom(params[0])) {
                        fabricCheckPlayer = method;
                        fabricCheckPlayerHasDefault = false;
                    } else if (CommandSourceStack.class.isAssignableFrom(params[0])) {
                        fabricCheckSource = method;
                        fabricCheckSourceHasDefault = false;
                    }
                }
            }

            fabricAvailable = fabricCheckPlayer != null || fabricCheckSource != null;
            if (!fabricAvailable) {
                LOGGER.warn("Fabric permissions API found but no compatible check method was detected.");
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to initialize Fabric permissions integration", t);
        }
    }

    private static boolean isLuckPermsAvailable() {
        if (luckPermsChecked) {
            return luckPermsAvailable;
        }

        luckPermsChecked = true;
        if (!VoteKickMod.platform().isModLoaded(LUCKPERMS_MOD_ID)) {
            luckPermsAvailable = false;
            return false;
        }

        try {
            Class.forName(LUCKPERMS_PROVIDER_CLASS);
            luckPermsAvailable = true;
        } catch (Throwable t) {
            luckPermsAvailable = false;
            logLuckPermsErrorOnce(t);
        }

        return luckPermsAvailable;
    }

    private static Method findMethod(Class<?> type, String name, int paramCount) {
        return findMethod(type, name, paramCount, null);
    }

    private static Method findMethod(Class<?> type, String name, int paramCount, Class<?> paramType) {
        if (type == null) {
            return null;
        }

        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name)) {
                continue;
            }
            if (method.getParameterCount() != paramCount) {
                continue;
            }
            if (paramType != null && paramCount > 0) {
                Class<?> methodParam = method.getParameterTypes()[0];
                if (!methodParam.isAssignableFrom(paramType)) {
                    continue;
                }
            }
            return method;
        }

        return null;
    }

    private static void logFabricErrorOnce(Exception e) {
        if (loggedFabricError) {
            return;
        }

        loggedFabricError = true;
        LOGGER.warn("Fabric permissions check failed, falling back to defaults", e);
    }

    private static void logLuckPermsErrorOnce(Throwable e) {
        if (loggedLuckPermsError) {
            return;
        }

        loggedLuckPermsError = true;
        LOGGER.warn("LuckPerms permissions check failed, falling back to defaults", e);
    }

    //? if neoforge {
    private static PermissionNode<Boolean> createBooleanNode(String nodeName,
            PermissionNode.PermissionResolver<Boolean> resolver,
            String description) {
        PermissionNode<Boolean> node = new PermissionNode<>(
                //? if >=1.21.11 {
                Identifier.fromNamespaceAndPath(VoteKickMod.MOD_ID, nodeName),
                //?} else if >=1.21 {
                ResourceLocation.fromNamespaceAndPath(VoteKickMod.MOD_ID, nodeName),
                //?} else {
                new ResourceLocation(VoteKickMod.MOD_ID, nodeName),
                //?}
                PermissionTypes.BOOLEAN,
                resolver);
        node.setInformation(Component.literal("votekick." + nodeName), Component.literal(description));
        return node;
    }

    private static Boolean checkNeoForgePermission(ServerPlayer player, String nodeKey) {
        try {
            PermissionNode<Boolean> node = switch (nodeKey) {
                case NODE_START -> START_NODE;
                case NODE_VOTE -> VOTE_NODE;
                case NODE_ADMIN -> ADMIN_NODE;
                case NODE_EXEMPT -> EXEMPT_NODE;
                default -> null;
            };

            if (node == null) {
                return null;
            }

            return PermissionAPI.getPermission(player, node);
        } catch (Exception e) {
            return null;
        }
    }
    //?}
}
