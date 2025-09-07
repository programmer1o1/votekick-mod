// VoteKickHud.java
package sierra.thing.votekick.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import sierra.thing.votekick.VoteKickMod;
import sierra.thing.votekick.client.config.ClientConfig;

import java.util.List;

public class VoteKickHud {

    @FunctionalInterface
    public interface HideCallback {
        void onHide();
    }

    // colors
    private static final int COLOR_BACKGROUND = 0xE0000000;
    private static final int COLOR_HEADER_BG = 0xFF1a1a1a;
    private static final int COLOR_ACCENT = 0xFF3498db;
    private static final int COLOR_YES = 0xFF27ae60;
    private static final int COLOR_NO = 0xFFe74c3c;
    private static final int COLOR_WARNING = 0xFFf39c12;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM = 0xFFAAAAAA;
    private static final int COLOR_SHADOW = 0x40000000;

    // base dimensions (will be scaled)
    private static final int BASE_PANEL_WIDTH = 250;
    private static final int BASE_MIN_PANEL_HEIGHT = 140;
    private static final int BASE_PADDING = 12;
    private static final int BASE_LINE_HEIGHT = 12;
    private static final int BASE_MARGIN = 10;

    // animation
    private static final float ANIMATION_SPEED = 0.12f;
    private static final float EASE_FACTOR = 0.5f;
    private static float animationProgress = 0f;
    private static boolean isAnimating = false;
    private static boolean isShowing = false;
    private static float pulseAnimation = 0f;

    // vote state
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

    // cached wrapped text
    private static List<FormattedCharSequence> wrappedReasonText = null;
    private static int cachedPanelHeight = BASE_MIN_PANEL_HEIGHT;

    public static void init() {
        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            updateAnimation(deltaTracker);

            if (showVotePanel || isAnimating) {
                render(guiGraphics, deltaTracker);
            }
        });
    }

    private static void updateAnimation(float tickDelta) {
        ClientConfig config = VoteKickClient.getClientConfig();

        // pulse animation for time warning
        if (config.isShowTimeWarnings()) {
            pulseAnimation += tickDelta * 0.1f;
        }

        if (!isAnimating || !config.isAnimationsEnabled()) {
            if (!config.isAnimationsEnabled() && isShowing) {
                animationProgress = 1.0f;
            }
            return;
        }

        float targetProgress = isShowing ? 1.0f : 0.0f;
        float diff = targetProgress - animationProgress;

        // eased animation
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

    private static void render(GuiGraphics guiGraphics, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        ClientConfig config = VoteKickClient.getClientConfig();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // apply ui scaling
        float scale = config.getUiScale();
        int panelWidth = (int)(BASE_PANEL_WIDTH * scale);
        int padding = (int)(BASE_PADDING * scale);
        int margin = (int)(BASE_MARGIN * scale);

        // calculate dynamic panel height based on wrapped text
        int panelHeight = calculatePanelHeight(font, scale);

        // calculate position based on config
        int x, y;
        switch (config.getPanelPosition()) {
            case 1: // top-left
                x = margin;
                y = margin;
                break;
            case 2: // bottom-right
                x = screenWidth - panelWidth - margin;
                y = screenHeight - panelHeight - margin;
                break;
            case 3: // bottom-left
                x = margin;
                y = screenHeight - panelHeight - margin;
                break;
            default: // top-right (0)
                x = screenWidth - panelWidth - margin;
                y = margin;
                break;
        }

        // apply slide animation
        if (config.isAnimationsEnabled()) {
            float easedProgress = easeInOutCubic(animationProgress);

            switch (config.getPanelPosition()) {
                case 1: // top-left - slide from left
                    x -= (int)((1.0f - easedProgress) * (panelWidth + 20));
                    break;
                case 2: // bottom-right - slide from bottom
                    y += (int)((1.0f - easedProgress) * (panelHeight + 20));
                    break;
                case 3: // bottom-left - slide from left
                    x -= (int)((1.0f - easedProgress) * (panelWidth + 20));
                    break;
                default: // top-right - slide from right
                    x += (int)((1.0f - easedProgress) * (panelWidth + 20));
                    break;
            }

            // bounce effect
            if (animationProgress > 0.8f && isShowing) {
                float bounce = (float)Math.sin((animationProgress - 0.8f) * 15.0f) * 2.0f * scale;
                y += (int)(bounce * (1.0f - animationProgress));
            }
        }

        // shadow
        guiGraphics.fill(x + 2, y + 2, x + panelWidth + 2, y + panelHeight + 2, COLOR_SHADOW);

        // main background
        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, COLOR_BACKGROUND);

        // header background
        int headerHeight = (int)(30 * scale);
        guiGraphics.fill(x, y, x + panelWidth, y + headerHeight, COLOR_HEADER_BG);

        // accent stripe
        int stripeWidth = (int)(3 * scale);
        guiGraphics.fill(x, y, x + stripeWidth, y + panelHeight, COLOR_ACCENT);

        renderContent(guiGraphics, font, x, y, panelWidth, panelHeight, scale);
    }

    private static void renderContent(GuiGraphics guiGraphics, Font font, int x, int y, int panelWidth, int panelHeight, float scale) {
        ClientConfig config = VoteKickClient.getClientConfig();
        int padding = (int)(BASE_PADDING * scale);
        int lineHeight = (int)(BASE_LINE_HEIGHT * scale);
        int currentY = y + padding;

        // title with icon indicator
        String icon = isVoteTarget ? "⚠ " : "🗳 ";
        Component titleComponent = Component.literal(icon + voteTitle);

        drawScaledText(guiGraphics, font, titleComponent, x + padding, currentY, COLOR_TEXT, scale);
        currentY += lineHeight + (int)(8 * scale);

        // separator line
        guiGraphics.fill(x + padding, currentY, x + panelWidth - padding, currentY + 1, 0x30FFFFFF);
        currentY += (int)(6 * scale);

        // vote progress section (moved up)
        renderVoteProgress(guiGraphics, font, x, currentY, panelWidth, scale);
        currentY += (int)(50 * scale);

        // wrapped reason text (moved after vote progress)
        if (!config.isCompactMode() && wrappedReasonText != null && !wrappedReasonText.isEmpty()) {
            // reason label
            drawScaledText(guiGraphics, font, Component.literal("REASON:"), x + padding, currentY, COLOR_WARNING, scale);
            currentY += lineHeight + (int)(2 * scale);

            // reason content with background
            int reasonStartY = currentY;
            for (FormattedCharSequence line : wrappedReasonText) {
                drawScaledText(guiGraphics, font, line, x + padding + (int)(4 * scale), currentY, COLOR_TEXT_DIM, scale);
                currentY += lineHeight;
            }

            // subtle background for reason
            guiGraphics.fill(x + padding, reasonStartY - 2,
                    x + panelWidth - padding, currentY, 0x20FFFFFF);
            currentY += (int)(8 * scale);
        }

        // timer section
        if (config.isShowTimeWarnings()) {
            renderTimer(guiGraphics, font, x, currentY, panelWidth, scale);
        } else {
            // simple timer without effects
            Component timerText = Component.literal("Time remaining: " + timeRemaining + "s");
            drawScaledText(guiGraphics, font, timerText, x + padding, currentY, COLOR_TEXT, scale);
        }
        currentY += lineHeight + (int)(8 * scale);

        // vote target warning (moved down, more prominent)
        if (isVoteTarget) {
            renderVoteTargetWarning(guiGraphics, font, x, currentY, panelWidth, scale);
            currentY += (int)(30 * scale);
        }

        // action prompt (moved to bottom)
        renderActionPrompt(guiGraphics, font, x, y + panelHeight - (int)(25 * scale), panelWidth, scale);
    }

    private static void drawScaledText(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int color, float scale) {
        if (scale != 1.0f) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.drawString(font, text, 0, 0, color);
            guiGraphics.pose().popPose();
        } else {
            guiGraphics.drawString(font, text, x, y, color);
        }
    }

    private static void drawScaledText(GuiGraphics guiGraphics, Font font, FormattedCharSequence text, int x, int y, int color, float scale) {
        if (scale != 1.0f) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.drawString(font, text, 0, 0, color);
            guiGraphics.pose().popPose();
        } else {
            guiGraphics.drawString(font, text, x, y, color);
        }
    }

    private static void renderVoteTargetWarning(GuiGraphics guiGraphics, Font font, int x, int y, int panelWidth, float scale) {
        int padding = (int)(BASE_PADDING * scale);
        int lineHeight = (int)(BASE_LINE_HEIGHT * scale);

        // pulsing background for warning
        float pulse = (float)Math.sin(pulseAnimation * 3) * 0.4f + 0.4f;
        int bgColor = (int)(pulse * 180) << 24 | 0xFF4444;
        guiGraphics.fill(x + padding - 4, y - 4, x + panelWidth - padding + 4, y + lineHeight + 4, bgColor);

        // border
        guiGraphics.fill(x + padding - 5, y - 5, x + panelWidth - padding + 5, y - 4, COLOR_NO);
        guiGraphics.fill(x + padding - 5, y + lineHeight + 4, x + panelWidth - padding + 5, y + lineHeight + 5, COLOR_NO);
        guiGraphics.fill(x + padding - 5, y - 4, x + padding - 4, y + lineHeight + 4, COLOR_NO);
        guiGraphics.fill(x + panelWidth - padding + 4, y - 4, x + panelWidth - padding + 5, y + lineHeight + 4, COLOR_NO);

        Component warningText = Component.literal("⚠ YOU ARE THE VOTE TARGET! ⚠");
        int textWidth = (int)(font.width(warningText) * scale);
        int textX = x + (panelWidth / 2) - (textWidth / 2);
        drawScaledText(guiGraphics, font, warningText, textX, y, COLOR_TEXT, scale);
    }

    private static void renderVoteProgress(GuiGraphics guiGraphics, Font font, int x, int y, int panelWidth, float scale) {
        int padding = (int)(BASE_PADDING * scale);
        int barX = x + padding;
        int barY = y + (int)(BASE_LINE_HEIGHT * scale + 4 * scale); // moved down to make room for labels
        int barWidth = panelWidth - (padding * 2);
        int barHeight = (int)(20 * scale);

        // vote counts (positioned above the bar)
        String yesText = "✓ " + yesVotes;
        String noText = "✗ " + noVotes;

        drawScaledText(guiGraphics, font, Component.literal(yesText), barX, y, COLOR_YES, scale);

        // calculate position for "No" votes to be right-aligned
        int noTextWidth = (int)(font.width(noText) * scale);
        drawScaledText(guiGraphics, font, Component.literal(noText),
                barX + barWidth - noTextWidth, y, COLOR_NO, scale);

        // progress bar background
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF2c2c2c);

        // calculate fill
        int totalVotes = yesVotes + noVotes;
        if (totalVotes > 0) {
            float yesRatio = (float)yesVotes / totalVotes;
            int yesWidth = (int)(barWidth * yesRatio);

            // yes votes (green)
            if (yesWidth > 0) {
                drawGradientRect(guiGraphics, barX, barY, barX + yesWidth, barY + barHeight,
                        COLOR_YES, darken(COLOR_YES, 0.7f));
            }

            // no votes (red)
            if (yesWidth < barWidth) {
                drawGradientRect(guiGraphics, barX + yesWidth, barY, barX + barWidth, barY + barHeight,
                        COLOR_NO, darken(COLOR_NO, 0.7f));
            }
        }

        // threshold marker
        if (votesNeeded > 0) {
            float threshold = (float)votesNeeded / Math.max(totalVotes + 2, votesNeeded + 1);
            int markerX = barX + (int)(barWidth * threshold);

            // white dashed line
            for (int i = 0; i < barHeight; i += 3) {
                guiGraphics.fill(markerX - 1, barY + i, markerX + 1, barY + Math.min(i + 2, barHeight), 0xFFFFFFFF);
            }

            // threshold label (positioned below the bar)
            String thresholdText = votesNeeded + " needed";
            int textWidth = (int)(font.width(thresholdText) * scale);
            int textX = Math.min(markerX - textWidth / 2, barX + barWidth - textWidth);
            textX = Math.max(barX, textX);

            drawScaledText(guiGraphics, font, Component.literal(thresholdText),
                    textX, barY + barHeight + (int)(4 * scale), COLOR_TEXT_DIM, scale);
        }
    }

    private static void renderTimer(GuiGraphics guiGraphics, Font font, int x, int y, int panelWidth, float scale) {
        ClientConfig config = VoteKickClient.getClientConfig();
        int padding = (int)(BASE_PADDING * scale);
        int lineHeight = (int)(BASE_LINE_HEIGHT * scale);
        boolean isUrgent = timeRemaining <= 5;

        // pulse effect for urgent timer
        int timerColor = COLOR_TEXT;
        if (isUrgent && config.isShowTimeWarnings()) {
            float pulse = (float)Math.sin(pulseAnimation * 5) * 0.5f + 0.5f;
            timerColor = interpolateColor(COLOR_WARNING, COLOR_NO, pulse);
        }

        String timeIcon = isUrgent ? "⏰ " : "⏱ ";
        Component timerText = Component.literal(timeIcon + "Time remaining: " + timeRemaining + "s");

        if (isUrgent && config.isShowTimeWarnings()) {
            // background flash for urgency
            float pulse = (float)Math.sin(pulseAnimation * 5) * 0.3f + 0.3f;
            int bgColor = (int)(pulse * 255) << 24 | 0xFF0000;
            guiGraphics.fill(x + padding - 2, y - 2, x + panelWidth - padding + 2, y + lineHeight + 2, bgColor);
        }

        drawScaledText(guiGraphics, font, timerText, x + padding, y, timerColor, scale);
    }

    private static void renderActionPrompt(GuiGraphics guiGraphics, Font font, int x, int y, int panelWidth, float scale) {
        Component prompt;
        int color;
        int padding = (int)(BASE_PADDING * scale);
        int lineHeight = (int)(BASE_LINE_HEIGHT * scale);

        if (isVoteTarget) {
            return;
        } else if (hasVoted) {
            prompt = Component.literal("✓ Vote submitted");
            color = COLOR_YES;
        } else {
            String yesKey = VoteKickClient.voteYesKey.getTranslatedKeyMessage().getString();
            String noKey = VoteKickClient.voteNoKey.getTranslatedKeyMessage().getString();
            prompt = Component.literal("Press [" + yesKey + "] Yes • [" + noKey + "] No");
            color = COLOR_TEXT;

            // subtle highlight
            guiGraphics.fill(x + padding - 2, y - 2, x + panelWidth - padding + 2, y + lineHeight + 2, 0x20FFFFFF);
        }

        int textWidth = (int)(font.width(prompt) * scale);
        int textX = x + (panelWidth / 2) - (textWidth / 2);
        drawScaledText(guiGraphics, font, prompt, textX, y, color, scale);
    }

    private static int calculatePanelHeight(Font font, float scale) {
        ClientConfig config = VoteKickClient.getClientConfig();

        if (config.isCompactMode() || wrappedReasonText == null) {
            return (int)(BASE_MIN_PANEL_HEIGHT * scale);
        }

        int baseHeight = BASE_MIN_PANEL_HEIGHT;
        int reasonHeight = wrappedReasonText.size() * (int)(BASE_LINE_HEIGHT * scale);
        return (int)((baseHeight + reasonHeight + 20) * scale);
    }

    // Updated wrapReasonText method to account for proper scaling
    private static void wrapReasonText(Font font, float scale) {
        if (voteReason == null || voteReason.isEmpty()) {
            wrappedReasonText = null;
            return;
        }

        String cleanReason = voteReason.replace("Reason: ", "").trim();
        // Account for scaling in text wrapping calculation
        int maxWidth = BASE_PANEL_WIDTH - (BASE_PADDING * 2) - 8;

        wrappedReasonText = font.split(Component.literal(cleanReason), maxWidth);
        cachedPanelHeight = calculatePanelHeight(font, scale);
    }

    // utility methods
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

    // public api methods
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

        // wrap text for display
        Minecraft mc = Minecraft.getInstance();
        wrapReasonText(mc.font, VoteKickClient.getClientConfig().getUiScale());

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

        // reset pulse on time change for emphasis
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
        isAnimating = VoteKickClient.getClientConfig().isAnimationsEnabled();

        if (!VoteKickClient.getClientConfig().isAnimationsEnabled()) {
            // instant hide
            showVotePanel = false;
            if (onHideListener != null) {
                onHideListener.onHide();
            }
        }
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