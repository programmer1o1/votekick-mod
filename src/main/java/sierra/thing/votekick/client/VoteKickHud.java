package sierra.thing.votekick.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import sierra.thing.votekick.VoteKickMod;

import java.util.List;

/**
 * Enhanced vote kick HUD with proper text wrapping and improved visuals
 */
public class VoteKickHud {

    @FunctionalInterface
    public interface HideCallback {
        void onHide();
    }

    // Colors
    private static final int COLOR_BACKGROUND = 0xE0000000;
    private static final int COLOR_HEADER_BG = 0xFF1a1a1a;
    private static final int COLOR_ACCENT = 0xFF3498db;
    private static final int COLOR_YES = 0xFF27ae60;
    private static final int COLOR_NO = 0xFFe74c3c;
    private static final int COLOR_WARNING = 0xFFf39c12;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM = 0xFFAAAAAA;
    private static final int COLOR_SHADOW = 0x40000000;

    // Dimensions
    private static final int PANEL_WIDTH = 250;
    private static final int MIN_PANEL_HEIGHT = 120;
    private static final int PADDING = 12;
    private static final int LINE_HEIGHT = 12;
    private static final int MARGIN = 10;

    // Animation
    private static final float ANIMATION_SPEED = 0.12f;
    private static final float EASE_FACTOR = 0.5f;
    private static float animationProgress = 0f;
    private static boolean isAnimating = false;
    private static boolean isShowing = false;
    private static float pulseAnimation = 0f;

    // Vote state
    private static boolean showVotePanel = false;
    private static boolean isVoteTarget = false;
    private static boolean hasVoted = false;
    private static String voteTitle = "";
    private static String voteReason = "";
    private static int timeRemaining = 0;
    private static int yesVotes = 0;
    private static int noVotes = 0;
    private static int votesNeeded = 0;
    private static int lastTimeRemaining = -1;

    private static HideCallback onHideListener = null;

    // Cached wrapped text
    private static List<FormattedCharSequence> wrappedReasonText = null;
    private static int cachedPanelHeight = MIN_PANEL_HEIGHT;

    // Texture resource - using parse() for 1.21.4
    private static final ResourceLocation VOTE_PANEL_TEXTURE = ResourceLocation.parse(VoteKickMod.MOD_ID + ":textures/gui/vote_panel.png");

    public static void init() {
        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            updateAnimation(deltaTracker);

            if (showVotePanel || isAnimating) {
                render(guiGraphics, deltaTracker);
            }
        });
    }

    private static void updateAnimation(DeltaTracker deltaTracker) {
        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);

        // Pulse animation for time warning
        pulseAnimation += tickDelta * 0.1f;

        if (!isAnimating) return;

        float targetProgress = isShowing ? 1.0f : 0.0f;
        float diff = targetProgress - animationProgress;

        // Eased animation
        animationProgress += diff * ANIMATION_SPEED;

        if (Math.abs(diff) < 0.01f) {
            animationProgress = targetProgress;
            isAnimating = false;

            if (!isShowing) {
                showVotePanel = false;
                if (onHideListener != null) {
                    onHideListener.onHide();
                }
            }
        }
    }

    private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Calculate dynamic panel height based on wrapped text
        int panelHeight = calculatePanelHeight(font);

        // Position with slide animation
        int x = screenWidth - PANEL_WIDTH - MARGIN;
        float easedProgress = easeInOutCubic(animationProgress);
        int y = MARGIN - (int)((1.0f - easedProgress) * (panelHeight + 20));

        // Add subtle bounce at the end
        if (animationProgress > 0.8f && isShowing) {
            float bounce = (float)Math.sin((animationProgress - 0.8f) * 15.0f) * 2.0f;
            y += (int)(bounce * (1.0f - animationProgress));
        }

        // Shadow
        guiGraphics.fill(x + 2, y + 2, x + PANEL_WIDTH + 2, y + panelHeight + 2, COLOR_SHADOW);

        // Main background
        guiGraphics.fill(x, y, x + PANEL_WIDTH, y + panelHeight, COLOR_BACKGROUND);

        // Header background
        guiGraphics.fill(x, y, x + PANEL_WIDTH, y + 30, COLOR_HEADER_BG);

        // Accent stripe
        guiGraphics.fill(x, y, x + 3, y + panelHeight, COLOR_ACCENT);

        renderContent(guiGraphics, font, x, y, panelHeight);
    }

    private static void renderContent(GuiGraphics guiGraphics, Font font, int x, int y, int panelHeight) {
        int currentY = y + PADDING;

        // Title with icon indicator
        String icon = isVoteTarget ? "⚠ " : "🗳 ";
        Component titleComponent = Component.literal(icon + voteTitle);
        guiGraphics.drawString(font, titleComponent, x + PADDING, currentY, COLOR_TEXT);
        currentY += LINE_HEIGHT + 8;

        // Separator line
        guiGraphics.fill(x + PADDING, currentY, x + PANEL_WIDTH - PADDING, currentY + 1, 0x30FFFFFF);
        currentY += 6;

        // Wrapped reason text
        if (wrappedReasonText != null && !wrappedReasonText.isEmpty()) {
            // Reason label
            guiGraphics.drawString(font, "REASON:", x + PADDING, currentY, COLOR_WARNING);
            currentY += LINE_HEIGHT + 2;

            // Reason content with background
            int reasonStartY = currentY;
            for (FormattedCharSequence line : wrappedReasonText) {
                guiGraphics.drawString(font, line, x + PADDING + 4, currentY, COLOR_TEXT_DIM);
                currentY += LINE_HEIGHT;
            }

            // Subtle background for reason
            guiGraphics.fill(x + PADDING, reasonStartY - 2,
                    x + PANEL_WIDTH - PADDING, currentY, 0x20FFFFFF);
            currentY += 8;
        }

        // Vote progress section
        renderVoteProgress(guiGraphics, font, x, currentY);
        currentY += 35;

        // Timer with warning pulse
        renderTimer(guiGraphics, font, x, currentY);
        currentY += LINE_HEIGHT + 8;

        // Action prompt
        renderActionPrompt(guiGraphics, font, x, y + panelHeight - 25);
    }

    private static void renderVoteProgress(GuiGraphics guiGraphics, Font font, int x, int y) {
        int barX = x + PADDING;
        int barY = y;
        int barWidth = PANEL_WIDTH - (PADDING * 2);
        int barHeight = 20;

        // Vote counts
        String yesText = String.valueOf(yesVotes);
        String noText = String.valueOf(noVotes);

        guiGraphics.drawString(font, Component.literal("✓ " + yesText).withStyle(s -> s.withColor(COLOR_YES)),
                barX, barY - LINE_HEIGHT, COLOR_YES);
        guiGraphics.drawString(font, Component.literal("✗ " + noText).withStyle(s -> s.withColor(COLOR_NO)),
                barX + barWidth - font.width("✗ " + noText), barY - LINE_HEIGHT, COLOR_NO);

        // Progress bar background
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF2c2c2c);

        // Calculate fill
        int totalVotes = yesVotes + noVotes;
        if (totalVotes > 0) {
            float yesRatio = (float)yesVotes / totalVotes;
            int yesWidth = (int)(barWidth * yesRatio);

            // Yes votes (green)
            if (yesWidth > 0) {
                drawGradientRect(guiGraphics, barX, barY, barX + yesWidth, barY + barHeight,
                        COLOR_YES, darken(COLOR_YES, 0.7f));
            }

            // No votes (red)
            if (yesWidth < barWidth) {
                drawGradientRect(guiGraphics, barX + yesWidth, barY, barX + barWidth, barY + barHeight,
                        COLOR_NO, darken(COLOR_NO, 0.7f));
            }
        }

        // Threshold marker
        if (votesNeeded > 0) {
            float threshold = (float)votesNeeded / Math.max(totalVotes + 2, votesNeeded + 1);
            int markerX = barX + (int)(barWidth * threshold);

            // White dashed line
            for (int i = 0; i < barHeight; i += 3) {
                guiGraphics.fill(markerX - 1, barY + i, markerX + 1, barY + Math.min(i + 2, barHeight), 0xFFFFFFFF);
            }

            // Threshold label
            String thresholdText = votesNeeded + " needed";
            int textX = Math.min(markerX - font.width(thresholdText) / 2, barX + barWidth - font.width(thresholdText));
            guiGraphics.drawString(font, thresholdText, Math.max(barX, textX), barY + barHeight + 2, COLOR_TEXT_DIM);
        }
    }

    private static void renderTimer(GuiGraphics guiGraphics, Font font, int x, int y) {
        boolean isUrgent = timeRemaining <= 5;

        // Pulse effect for urgent timer
        int timerColor = COLOR_TEXT;
        if (isUrgent) {
            float pulse = (float)Math.sin(pulseAnimation * 5) * 0.5f + 0.5f;
            timerColor = interpolateColor(COLOR_WARNING, COLOR_NO, pulse);
        }

        String timeIcon = isUrgent ? "⏰ " : "⏱ ";
        Component timerText = Component.literal(timeIcon + "Time remaining: " + timeRemaining + "s");

        if (isUrgent) {
            // Background flash for urgency
            float pulse = (float)Math.sin(pulseAnimation * 5) * 0.3f + 0.3f;
            int bgColor = (int)(pulse * 255) << 24 | 0xFF0000;
            guiGraphics.fill(x + PADDING - 2, y - 2, x + PANEL_WIDTH - PADDING + 2, y + LINE_HEIGHT + 2, bgColor);
        }

        guiGraphics.drawString(font, timerText, x + PADDING, y, timerColor);
    }

    private static void renderActionPrompt(GuiGraphics guiGraphics, Font font, int x, int y) {
        Component prompt;
        int color;

        if (isVoteTarget) {
            prompt = Component.literal("⚠ You are the vote target!");
            color = COLOR_NO;

            // Pulsing background for warning
            float pulse = (float)Math.sin(pulseAnimation * 3) * 0.3f + 0.3f;
            int bgColor = (int)(pulse * 255) << 24 | 0xFF0000;
            guiGraphics.fill(x + PADDING - 4, y - 2, x + PANEL_WIDTH - PADDING + 4, y + LINE_HEIGHT + 2, bgColor);
        } else if (hasVoted) {
            prompt = Component.literal("✓ Vote submitted");
            color = COLOR_YES;
        } else {
            prompt = Component.literal("Press [F1] Yes • [F2] No");
            color = COLOR_TEXT;

            // Subtle highlight
            guiGraphics.fill(x + PADDING - 2, y - 2, x + PANEL_WIDTH - PADDING + 2, y + LINE_HEIGHT + 2, 0x20FFFFFF);
        }

        int textX = x + (PANEL_WIDTH / 2) - (font.width(prompt) / 2);
        guiGraphics.drawString(font, prompt, textX, y, color);
    }

    private static int calculatePanelHeight(Font font) {
        if (wrappedReasonText == null) {
            return MIN_PANEL_HEIGHT;
        }

        int baseHeight = 120; // Base UI elements
        int reasonHeight = wrappedReasonText.size() * LINE_HEIGHT;
        return baseHeight + reasonHeight + 20; // Extra padding
    }

    private static void wrapReasonText(Font font) {
        if (voteReason == null || voteReason.isEmpty()) {
            wrappedReasonText = null;
            return;
        }

        String cleanReason = voteReason.replace("Reason: ", "").trim();
        int maxWidth = PANEL_WIDTH - (PADDING * 2) - 8;

        wrappedReasonText = font.split(Component.literal(cleanReason), maxWidth);
        cachedPanelHeight = calculatePanelHeight(font);
    }

    // Utility methods
    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float)Math.pow(-2 * t + 2, 3) / 2;
    }

    private static void drawGradientRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
        graphics.fillGradient(x1, y1, x2, y2, colorTop, colorBottom);
    }

    private static int darken(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * factor);
        int g = (int)(((color >> 8) & 0xFF) * factor);
        int b = (int)((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int interpolateColor(int color1, int color2, float ratio) {
        ratio = Mth.clamp(ratio, 0, 1);
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int)(a1 + (a2 - a1) * ratio);
        int r = (int)(r1 + (r2 - r1) * ratio);
        int g = (int)(g1 + (g2 - g1) * ratio);
        int b = (int)(b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // Public API methods
    public static void showVotePanel(String title, String subtitle, int time, int yes, int no, int needed, boolean isTarget) {
        voteTitle = title;
        voteReason = subtitle;
        timeRemaining = time;
        lastTimeRemaining = time;
        yesVotes = yes;
        noVotes = no;
        votesNeeded = needed;
        isVoteTarget = isTarget;
        hasVoted = false;

        // Wrap text for display
        Minecraft mc = Minecraft.getInstance();
        wrapReasonText(mc.font);

        VoteKickClient.resetVoteState();

        animationProgress = 0f;
        isShowing = true;
        isAnimating = true;
        showVotePanel = true;
    }

    public static void updateVotePanel(int time, int yes, int no) {
        boolean timeChanged = (time != lastTimeRemaining);
        lastTimeRemaining = timeRemaining;

        timeRemaining = time;
        yesVotes = yes;
        noVotes = no;

        // Reset pulse on time change for emphasis
        if (timeChanged && time <= 5) {
            pulseAnimation = 0;
        }
    }

    public static void onClientDisconnect() {
        showVotePanel = false;
        isAnimating = false;
        animationProgress = 0f;
        isShowing = false;
        hasVoted = false;
        isVoteTarget = false;
        wrappedReasonText = null;

        VoteKickClient.resetVoteState();

        if (onHideListener != null) {
            onHideListener.onHide();
        }
    }

    public static void hideVotePanel() {
        isShowing = false;
        isAnimating = true;
    }

    public static void markPlayerVoted() {
        hasVoted = true;
    }

    public static void resetVoteState() {
        hasVoted = false;
        VoteKickClient.resetVoteState();
    }

    public static boolean isVotePanelShowing() {
        return showVotePanel;
    }

    public static void setOnHideListener(HideCallback listener) {
        onHideListener = listener;
    }

    public static boolean hasPlayerVoted() {
        return hasVoted;
    }
}