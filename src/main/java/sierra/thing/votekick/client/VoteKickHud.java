package sierra.thing.votekick.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import sierra.thing.votekick.VoteKickMod;

/**
 * Handles the UI for vote kicks.
 * Draws a panel in the top-right corner of the screen.
 * Tried to make it look like TF2's vote system.
 */
public class VoteKickHud {

    // For cleanup when panel closes
    @FunctionalInterface
    public interface HideCallback {
        void onHide();
    }

    // Texture for the panel background - not actually using this right now
    // Ended up just drawing rectangles because it was easier
    private static final ResourceLocation VOTE_PANEL_TEXTURE = new ResourceLocation(VoteKickMod.MOD_ID, "textures/gui/vote_panel.png");

    // State tracking
    private static boolean showVotePanel = false;
    private static boolean isVoteTarget = false;
    private static boolean hasVoted = false;
    private static String voteTitle = "";
    private static String voteSubtitle = "";
    private static int timeRemaining = 0;
    private static int yesVotes = 0;
    private static int noVotes = 0;
    private static int votesNeeded = 0;

    // Animation stuff - makes the panel slide in/out
    private static float animationProgress = 0f;
    private static boolean isAnimating = false;
    private static boolean isShowing = false;
    private static final float ANIMATION_SPEED = 0.075f; // adjust if too fast/slow

    // Optional callback for when panel is hidden
    private static HideCallback onHideListener = null;

    public static void init() {
        // Hook into the render system
        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            updateAnimation();

            // Only draw if needed
            if (showVotePanel || isAnimating) {
                render(matrixStack);
            }
        });
    }

    private static void updateAnimation() {
        if (!isAnimating) return;

        if (isShowing) {
            // Slide in from top
            animationProgress += ANIMATION_SPEED;
            if (animationProgress >= 1.0f) {
                animationProgress = 1.0f;
                isAnimating = false;
            }
        } else {
            // Slide out to top
            animationProgress -= ANIMATION_SPEED;
            if (animationProgress <= 0f) {
                animationProgress = 0f;
                isAnimating = false;
                showVotePanel = false;

                // Let any listeners know we're done
                if (onHideListener != null) {
                    onHideListener.onHide();
                }
            }
        }
    }

    private static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        // Fixed size panel - might want to make this scale better
        int panelWidth = 210;
        int panelHeight = 100;
        int x = width - panelWidth - 10; // 10px padding from right
        int baseY = 10; // 10px from top

        // Calculate Y position based on animation
        int animatedY = baseY;
        if (isAnimating) {
            // Slide in from above screen
            animatedY = baseY - (int)((1.0f - animationProgress) * (panelHeight + 20));
        }

        // Draw the actual panel
        renderVotePanel(guiGraphics, x, animatedY, panelWidth, panelHeight);
    }

    private static void renderVotePanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getInstance();

        // Semi-transparent background
        guiGraphics.fill(x, y, x + width, y + height, 0xC0000000);

        // White border - makes it look nicer
        drawBorder(guiGraphics, x, y, x + width, y + height, 0xFFFFFFFF);

        // Title (player being voted on)
        int textColor = 0xFFFFFF;
        guiGraphics.drawString(mc.font, voteTitle, x + 10, y + 10, textColor);

        // Reason with highlighted box
        int reasonY = y + 25;
        if (voteSubtitle != null && voteSubtitle.startsWith("Reason:")) {
            // Make the reason stand out more
            guiGraphics.fill(x + 5, reasonY - 2, x + width - 5, reasonY + 12, 0x40FFAA00);
            // Gold text for reason
            guiGraphics.drawString(mc.font, voteSubtitle, x + 10, reasonY, 0xFFAA00);
        } else {
            // Normal text for other stuff
            guiGraphics.drawString(mc.font, voteSubtitle, x + 10, reasonY, 0xAAAAAA);
        }

        // Timer
        String timeText = "Time left: " + timeRemaining + "s";
        guiGraphics.drawString(mc.font, timeText, x + 10, y + 45, textColor);

        // Vote counts - green for yes, red for no
        guiGraphics.drawString(mc.font, "YES: " + yesVotes, x + 30, y + 65, 0x55FF55);
        guiGraphics.drawString(mc.font, "NO: " + noVotes, x + width - 70, y + 65, 0xFF5555);

        // Progress bar showing yes vs no votes
        int barWidth = width - 20;
        int totalVotes = yesVotes + noVotes;
        int filledWidth = totalVotes > 0 ? Mth.clamp((yesVotes * barWidth) / totalVotes, 0, barWidth) : 0;

        // Bar background
        guiGraphics.fill(x + 10, y + 80, x + 10 + barWidth, y + 88, 0x80000000);

        // Green filled portion (yes votes)
        if (filledWidth > 0) {
            guiGraphics.fill(x + 10, y + 80, x + 10 + filledWidth, y + 88, 0x8055FF55);
        }

        // White marker showing threshold needed to pass
        // This math is a bit wonky but it works - shows roughly where the pass threshold is
        int thresholdX = x + 10 + (int)(barWidth * ((float)votesNeeded / Math.max(totalVotes + 1, votesNeeded * 2)));
        guiGraphics.fill(thresholdX - 1, y + 78, thresholdX + 1, y + 90, 0xFFFFFFFF);

        // Different message based on state
        if (!isVoteTarget) {
            if (hasVoted) {
                guiGraphics.drawString(mc.font, "Vote cast!", x + 10, y + height - 15, 0xFFAA00);
            } else {
                // Prompt to vote - used to be J/K but changed to F1/F2
                guiGraphics.drawString(mc.font, "Press F1 - Yes, F2 - No", x + 10, y + height - 15, 0xCCCCCC);
            }
        } else {
            // Warning if you're the one being voted on
            guiGraphics.drawString(mc.font, "You are being voted on", x + 10, y + height - 15, 0xFF5555);
        }
    }

    // Quick helper to draw borders - Minecraft doesn't have this built-in
    private static void drawBorder(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        // Draw 1px lines on all sides
        guiGraphics.fill(x1, y1, x2, y1 + 1, color);      // Top
        guiGraphics.fill(x1, y2 - 1, x2, y2, color);      // Bottom
        guiGraphics.fill(x1, y1, x1 + 1, y2, color);      // Left
        guiGraphics.fill(x2 - 1, y1, x2, y2, color);      // Right
    }

    // Called when a vote starts
    public static void showVotePanel(String title, String subtitle, int time, int yes, int no, int needed, boolean isTarget) {
        voteTitle = title;
        voteSubtitle = subtitle;
        timeRemaining = time;
        yesVotes = yes;
        noVotes = no;
        votesNeeded = needed;
        isVoteTarget = isTarget;
        hasVoted = false;
        VoteKickClient.resetVoteState();

        // Start animation
        animationProgress = 0f;
        isShowing = true;
        isAnimating = true;
        showVotePanel = true;
    }

    // Updates vote counts and timer
    public static void updateVotePanel(int time, int yes, int no) {
        timeRemaining = time;
        yesVotes = yes;
        noVotes = no;
    }

    /**
     * Clean up UI when player disconnects
     * Had a bug where panel would stay visible in main menu - not good!
     */
    public static void onClientDisconnect() {
        // Force immediate hide
        showVotePanel = false;
        isAnimating = false;
        animationProgress = 0f;
        isShowing = false;

        hasVoted = false;
        isVoteTarget = false;

        // Reset vote state
        VoteKickClient.resetVoteState();

        // Call listener if needed
        if (onHideListener != null) {
            onHideListener.onHide();
        }
    }

    // Start slide-out animation when vote ends
    public static void hideVotePanel() {
        isShowing = false;
        isAnimating = true;
    }

    // Called when player casts a vote
    public static void markPlayerVoted() {
        hasVoted = true;
    }

    // Reset everything
    public static void resetVoteState() {
        hasVoted = false;
        VoteKickClient.resetVoteState();
    }

    /**
     * Check if vote UI is currently visible
     */
    public static boolean isVotePanelShowing() {
        return showVotePanel;
    }

    /**
     * Register callback for when panel is hidden
     * Used for cleanup
     */
    public static void setOnHideListener(HideCallback listener) {
        onHideListener = listener;
    }

    /**
     * Has player already voted in this session?
     */
    public static boolean hasPlayerVoted() { return hasVoted; }
}