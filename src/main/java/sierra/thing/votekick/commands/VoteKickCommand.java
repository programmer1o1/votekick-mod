// VoteKickCommand.java
package sierra.thing.votekick.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.protection.PlayerProtectionManager;
import sierra.thing.votekick.vote.VoteSession;

import java.util.Map;
import java.util.UUID;

public class VoteKickCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    private static final TextColor ERROR_COLOR = TextColor.fromRgb(0xFF5555);
    private static final TextColor SUCCESS_COLOR = TextColor.fromRgb(0x55FF55);
    private static final TextColor HIGHLIGHT_COLOR = TextColor.fromRgb(0xFFFF55);
    private static final TextColor INFO_COLOR = TextColor.fromRgb(0xAAAAAA);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> voteKickCommand = Commands.literal("votekick");

        voteKickCommand.then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(context -> startVoteKick(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "target"),
                                StringArgumentType.getString(context, "reason")
                        ))
                )
        );

        if (!VoteKickMod.getConfig().isRequireKickReason()) {
            voteKickCommand.then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> startVoteKick(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "target"),
                            "No reason provided"
                    ))
            );
        }

        voteKickCommand.executes(context -> showVoteKickHelp(context.getSource()));

        dispatcher.register(voteKickCommand);
        dispatcher.register(Commands.literal("vk").redirect(dispatcher.getRoot().getChild("votekick")));

        dispatcher.register(
                Commands.literal("vote")
                        .then(Commands.literal("yes")
                                .executes(ctx -> castVote(ctx.getSource(), true))
                        )
                        .then(Commands.literal("no")
                                .executes(ctx -> castVote(ctx.getSource(), false))
                        )
                        .then(Commands.literal("status")
                                .executes(ctx -> showVoteStatus(ctx.getSource()))
                        )
                        .executes(ctx -> showVoteHelp(ctx.getSource()))
        );

        dispatcher.register(Commands.literal("v")
                .redirect(dispatcher.getRoot().getChild("vote")));
    }

    private static int startVoteKick(CommandSourceStack source, ServerPlayer target, String reason) throws CommandSyntaxException {
        try {
            ServerPlayer player = source.getPlayerOrException();

            if (VoteKickMod.getConfig().isRequireKickReason() && (reason == null || reason.trim().isEmpty())) {
                sendError(player, "You must provide a reason for the vote kick");
                return 0;
            }

            int maxLength = VoteKickMod.getConfig().getMaxReasonLength();
            if (reason != null && reason.length() > maxLength) {
                reason = reason.substring(0, maxLength) + "...";
            }

            if (player.getUUID().equals(target.getUUID()) && !VoteKickMod.getConfig().isAllowSelfVoting()) {
                sendError(player, "You cannot start a vote against yourself");
                return 0;
            }

            if (source.getServer().getPlayerList().isOp(target.getGameProfile())) {
                sendError(player, "You cannot vote to kick server operators");
                return 0;
            }

            if (target.hasPermissions(2)) {
                sendError(player, "This player cannot be vote-kicked");
                return 0;
            }

            // check if target is protected
            PlayerProtectionManager protectionManager = VoteKickMod.getProtectionManager();
            if (protectionManager.isProtected(target.getUUID())) {
                int remaining = protectionManager.getRemainingProtectionTime(target.getUUID());
                sendError(player, "This player has immunity for " + remaining + " more seconds");
                return 0;
            }

            // check cooldowns
            if (VoteSession.isOnCooldown(player.getUUID())) {
                int remainingSeconds = VoteSession.getRemainingCooldown(player.getUUID());
                sendError(player, "You must wait " + remainingSeconds + " seconds before starting another vote");
                return 0;
            }

            // check target-specific cooldown to prevent harassment
            if (VoteSession.isTargetOnCooldown(player.getUUID(), target.getUUID())) {
                int remainingSeconds = VoteSession.getRemainingTargetCooldown(player.getUUID(), target.getUUID());
                sendError(player, "You must wait " + remainingSeconds + " seconds before voting this player again");
                return 0;
            }

            if (!VoteKickMod.getActiveVotes().isEmpty()) {
                sendError(player, "A vote is already in progress");
                return 0;
            }

            int playerCount = source.getServer().getPlayerList().getPlayerCount();
            if (playerCount < VoteKickMod.getConfig().getMinimumPlayers()) {
                sendError(player, "At least " + VoteKickMod.getConfig().getMinimumPlayers() +
                        " players must be online for voting");
                return 0;
            }

            // check if player has been kicked too many times recently
            int recentKicks = protectionManager.getRecentKickCount(target.getUUID(), 3600_000); // last hour
            if (recentKicks >= 3) {
                sendError(player, "This player has been kicked " + recentKicks + " times recently. Please wait before starting another vote.");
                return 0;
            }

            VoteSession session = new VoteSession(
                    player.getUUID(),
                    target.getUUID(),
                    player.getGameProfile().getName(),
                    target.getGameProfile().getName(),
                    reason,
                    playerCount,
                    VoteKickMod.getConfig().getVoteDurationSeconds()
            );

            VoteKickMod.addVote(target.getUUID(), session);

            Component announcement = Component.literal(
                            player.getGameProfile().getName() + " started a vote to kick " +
                                    target.getGameProfile().getName())
                    .setStyle(Style.EMPTY.withColor(HIGHLIGHT_COLOR).withBold(true));

            Component reasonText = Component.literal(
                            "Reason: " + reason + " (" +
                                    VoteKickMod.getConfig().getVoteDurationSeconds() + "s to vote)")
                    .setStyle(Style.EMPTY.withColor(HIGHLIGHT_COLOR));

            source.getServer().getPlayerList().broadcastSystemMessage(announcement, false);
            source.getServer().getPlayerList().broadcastSystemMessage(reasonText, false);

            source.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("Type /vote yes or /vote no to cast your vote")
                            .setStyle(Style.EMPTY.withColor(INFO_COLOR)),
                    false
            );

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error starting vote", e);
            sendError(source, "An error occurred while starting the vote");
            return 0;
        }
    }

    private static int castVote(CommandSourceStack source, boolean inFavor) throws CommandSyntaxException {
        try {
            ServerPlayer player = source.getPlayerOrException();
            Map<UUID, VoteSession> activeVotes = VoteKickMod.getActiveVotes();

            if (activeVotes.isEmpty()) {
                sendError(player, "There is no vote in progress");
                return 0;
            }

            VoteSession session = activeVotes.values().iterator().next();

            if (player.getUUID().equals(session.getTargetUUID())) {
                sendError(player, "You cannot vote on your own kick");
                return 0;
            }

            if (player.getUUID().equals(session.getInitiatorUUID())) {
                sendSuccess(player, "Your vote has been counted (you started this vote)");
                return 1;
            }

            if (session.castVote(player, inFavor)) {
                String voteText = inFavor ? "YES" : "NO";
                player.sendSystemMessage(Component.literal("You voted " + voteText)
                        .setStyle(Style.EMPTY.withColor(inFavor ? SUCCESS_COLOR : ERROR_COLOR)));
                return 1;
            } else {
                sendError(player, "You have already voted in this session");
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Error casting vote", e);
            sendError(source, "An error occurred while casting your vote");
            return 0;
        }
    }

    private static int showVoteStatus(CommandSourceStack source) {
        try {
            if (VoteKickMod.getActiveVotes().isEmpty()) {
                sendInfo(source, "There is no vote in progress");
                return 0;
            }

            VoteSession session = VoteKickMod.getActiveVotes().values().iterator().next();

            source.sendSuccess(() -> Component.literal("=== Current Vote Status ===")
                    .setStyle(Style.EMPTY.withColor(HIGHLIGHT_COLOR)), false);

            source.sendSuccess(() -> Component.literal("Target: " + session.getTargetName()), false);
            source.sendSuccess(() -> Component.literal("Initiated by: " + session.getInitiatorName()), false);
            source.sendSuccess(() -> Component.literal("Reason: " + session.getKickReason()), false);
            source.sendSuccess(() -> Component.literal("Time remaining: " + session.getSecondsRemaining() + "s"), false);
            source.sendSuccess(() -> Component.literal("Votes: " + session.getYesVotes() + " yes, " +
                    session.getNoVotes() + " no"), false);
            source.sendSuccess(() -> Component.literal("Votes needed: " + session.getTotalVotesNeeded()), false);

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing vote status", e);
            sendError(source, "An error occurred while retrieving vote status");
            return 0;
        }
    }

    private static int showVoteKickHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("===== VoteKick Help =====")
                .setStyle(Style.EMPTY.withColor(HIGHLIGHT_COLOR)), false);

        if (VoteKickMod.getConfig().isRequireKickReason()) {
            source.sendSuccess(() -> Component.literal("/votekick <player> <reason> - Start a vote to kick a player"), false);
            source.sendSuccess(() -> Component.literal("/vk <player> <reason> - Shorthand for /votekick"), false);
        } else {
            source.sendSuccess(() -> Component.literal("/votekick <player> [reason] - Start a vote to kick a player"), false);
            source.sendSuccess(() -> Component.literal("/vk <player> [reason] - Shorthand for /votekick"), false);
        }

        source.sendSuccess(() -> Component.literal("Use /vote yes or /vote no to vote on active kicks"), false);
        source.sendSuccess(() -> Component.literal("Use /vote status to check the current vote"), false);
        return 1;
    }

    private static int showVoteHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("===== Vote Commands =====")
                .setStyle(Style.EMPTY.withColor(HIGHLIGHT_COLOR)), false);
        source.sendSuccess(() -> Component.literal("/vote yes - Vote YES to current vote"), false);
        source.sendSuccess(() -> Component.literal("/vote no - Vote NO to current vote"), false);
        source.sendSuccess(() -> Component.literal("/vote status - Show current vote status"), false);
        source.sendSuccess(() -> Component.literal("/v yes, /v no - Shorthand commands"), false);
        return 1;
    }

    private static void sendError(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).setStyle(Style.EMPTY.withColor(ERROR_COLOR)));
    }

    private static void sendError(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message).setStyle(Style.EMPTY.withColor(ERROR_COLOR)));
    }

    private static void sendSuccess(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message).setStyle(Style.EMPTY.withColor(SUCCESS_COLOR)));
    }

    private static void sendInfo(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message).setStyle(Style.EMPTY.withColor(INFO_COLOR)), false);
    }
}