// VoteKickConfigScreen.java
package sierra.thing.votekick.client.config;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import sierra.thing.votekick.client.VoteKickClient;

public class VoteKickConfigScreen extends Screen {
    private final Screen parent;
    private final ClientConfig config;

    // colors matching the actual vote panel
    private static final int COLOR_BACKGROUND = 0xE0000000;
    private static final int COLOR_HEADER_BG = 0xFF1a1a1a;
    private static final int COLOR_ACCENT = 0xFF3498db;
    private static final int COLOR_YES = 0xFF27ae60;
    private static final int COLOR_NO = 0xFFe74c3c;
    private static final int COLOR_WARNING = 0xFFf39c12;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM = 0xFFAAAAAA;
    private static final int COLOR_SHADOW = 0x40000000;

    // buttons and controls
    private Button scaleDownButton;
    private Button scaleUpButton;
    private Button resetButton;
    private CycleButton<Boolean> soundButton;
    private CycleButton<Boolean> compactButton;
    private CycleButton<Boolean> animationsButton;
    private CycleButton<Boolean> warningsButton;
    private CycleButton<String> positionButton;

    // temp values for preview
    private float tempScale;
    private boolean tempSound;
    private boolean tempCompact;
    private boolean tempAnimations;
    private boolean tempWarnings;
    private int tempPosition;

    // animation for preview
    private float previewPulse = 0f;

    public VoteKickConfigScreen(Screen parent) {
        super(Component.literal("VoteKick Config"));
        this.parent = parent;
        this.config = VoteKickClient.getClientConfig();

        // load current values
        this.tempScale = config.getUiScale();
        this.tempSound = config.isSoundEnabled();
        this.tempCompact = config.isCompactMode();
        this.tempAnimations = config.isAnimationsEnabled();
        this.tempWarnings = config.isShowTimeWarnings();
        this.tempPosition = config.getPanelPosition();
    }

    @Override
    protected void init() {
        // layout constants for smaller screens
        int leftColumnX = 20;
        int rightColumnX = this.width - 200;
        int buttonWidth = 120;
        int buttonHeight = 18;
        int spacing = 22;
        int startY = 40;

        // left column - controls
        int currentY = startY;

        // ui scale controls
        this.scaleDownButton = Button.builder(Component.literal("-"), button -> {
            tempScale = Math.max(0.5f, tempScale - 0.1f);
            updateScaleButtons();
        }).bounds(leftColumnX, currentY, 25, buttonHeight).build();

        this.scaleUpButton = Button.builder(Component.literal("+"), button -> {
            tempScale = Math.min(3.0f, tempScale + 0.1f);
            updateScaleButtons();
        }).bounds(leftColumnX + 95, currentY, 25, buttonHeight).build();

        currentY += spacing;

        // sound toggle
        this.soundButton = CycleButton.onOffBuilder(tempSound)
                .create(leftColumnX, currentY, buttonWidth, buttonHeight,
                        Component.literal("Sounds"), (button, value) -> {
                            tempSound = value;
                            // only play test sound when enabling, not when disabling
                            if (value && !config.isSoundEnabled()) {
                                // temporarily enable sounds to play test sound
                                VoteKickClient.playLocalSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 0.5f, 1.0f);
                            }
                        });
        currentY += spacing;

        // compact mode toggle
        this.compactButton = CycleButton.onOffBuilder(tempCompact)
                .create(leftColumnX, currentY, buttonWidth, buttonHeight,
                        Component.literal("Compact"), (button, value) -> {
                            tempCompact = value;
                        });
        currentY += spacing;

        // animations toggle
        this.animationsButton = CycleButton.onOffBuilder(tempAnimations)
                .create(leftColumnX, currentY, buttonWidth, buttonHeight,
                        Component.literal("Animations"), (button, value) -> {
                            tempAnimations = value;
                        });
        currentY += spacing;

        // time warnings toggle
        this.warningsButton = CycleButton.onOffBuilder(tempWarnings)
                .create(leftColumnX, currentY, buttonWidth, buttonHeight,
                        Component.literal("Warnings"), (button, value) -> {
                            tempWarnings = value;
                        });
        currentY += spacing;

        // position cycle button
        String[] positions = {"Top-Right", "Top-Left", "Bottom-Right", "Bottom-Left"};
        this.positionButton = CycleButton.<String>builder(Component::literal)
                .withValues(positions)
                .withInitialValue(positions[tempPosition])
                .create(leftColumnX, currentY, buttonWidth, buttonHeight,
                        Component.literal("Position"), (button, value) -> {
                            for (int i = 0; i < positions.length; i++) {
                                if (positions[i].equals(value)) {
                                    tempPosition = i;
                                    break;
                                }
                            }
                        });
        currentY += spacing * 2;

        // reset button
        this.resetButton = Button.builder(Component.literal("Reset"), button -> {
            tempScale = 1.0f;
            tempSound = true;
            tempCompact = false;
            tempAnimations = true;
            tempWarnings = true;
            tempPosition = 0;

            updateAllButtons();
        }).bounds(leftColumnX, currentY, buttonWidth, buttonHeight).build();

        // done button
        Button doneButton = Button.builder(CommonComponents.GUI_DONE, button -> {
            saveSettings();
            minecraft.setScreen(parent);
        }).bounds(this.width / 2 - 50, this.height - 25, 100, buttonHeight).build();

        // add all widgets
        addRenderableWidget(scaleDownButton);
        addRenderableWidget(scaleUpButton);
        addRenderableWidget(soundButton);
        addRenderableWidget(compactButton);
        addRenderableWidget(animationsButton);
        addRenderableWidget(warningsButton);
        addRenderableWidget(positionButton);
        addRenderableWidget(resetButton);
        addRenderableWidget(doneButton);

        updateScaleButtons();
    }

    private void updateScaleButtons() {
        scaleDownButton.active = tempScale > 0.5f;
        scaleUpButton.active = tempScale < 3.0f;
    }

    private void updateAllButtons() {
        soundButton.setValue(tempSound);
        compactButton.setValue(tempCompact);
        animationsButton.setValue(tempAnimations);
        warningsButton.setValue(tempWarnings);

        String[] positions = {"Top-Right", "Top-Left", "Bottom-Right", "Bottom-Left"};
        positionButton.setValue(positions[tempPosition]);

        updateScaleButtons();
    }

    private void saveSettings() {
        config.setUiScale(tempScale);
        config.setSoundEnabled(tempSound);
        config.setCompactMode(tempCompact);
        config.setAnimationsEnabled(tempAnimations);
        config.setShowTimeWarnings(tempWarnings);
        config.setPanelPosition(tempPosition);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // update animation
        previewPulse += partialTick * 0.1f;

        // title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        // left column labels
        int leftColumnX = 20;
        int labelY = 25;
        int spacing = 22;

        // scale label with value
        String scaleText = "Scale: " + String.format("%.1f", tempScale) + "x";
        guiGraphics.drawString(this.font, scaleText, leftColumnX + 30, labelY + 18, 0xAAAAAA);

        // preview on the right side
        renderCompactPreview(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderCompactPreview(GuiGraphics guiGraphics) {
        // calculate preview area
        int previewX = this.width - 220;
        int previewY = 40;
        int previewWidth = 200;
        int previewHeight = 180;

        // preview label
        guiGraphics.drawString(this.font, "Preview:", previewX, previewY - 12, 0xFFFF55);

        // calculate preview panel dimensions
        float scale = tempScale * 0.7f; // scale down for preview
        int panelWidth = (int)(250 * scale);
        int panelHeight = calculatePreviewPanelHeight(scale);
        int padding = (int)(12 * scale);

        // center the preview panel
        int panelX = previewX + (previewWidth - panelWidth) / 2;
        int panelY = previewY + 10;

        // shadow
        guiGraphics.fill(panelX + 2, panelY + 2, panelX + panelWidth + 2, panelY + panelHeight + 2, COLOR_SHADOW);

        // main background
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_BACKGROUND);

        // header background
        int headerHeight = (int)(30 * scale);
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + headerHeight, COLOR_HEADER_BG);

        // accent stripe
        int stripeWidth = (int)(3 * scale);
        guiGraphics.fill(panelX, panelY, panelX + stripeWidth, panelY + panelHeight, COLOR_ACCENT);

        // render content matching VoteKickHud
        renderPreviewContent(guiGraphics, panelX, panelY, panelWidth, panelHeight, scale);

        // position indicator below preview
        String[] posNames = {"Top-Right", "Top-Left", "Bottom-Right", "Bottom-Left"};
        String posText = posNames[tempPosition];
        int posTextWidth = this.font.width(posText);
        guiGraphics.drawString(this.font, posText, previewX + (previewWidth - posTextWidth) / 2,
                panelY + panelHeight + 8, 0xAAAAAA);
    }

    private void renderPreviewContent(GuiGraphics guiGraphics, int x, int y, int panelWidth, int panelHeight, float scale) {
        int padding = (int)(12 * scale);
        int lineHeight = (int)(12 * scale);
        int currentY = y + padding;

        // title with icon
        String icon = "🗳 ";
        Component titleComponent = Component.literal(icon + "Kick TestUser?");
        drawScaledText(guiGraphics, this.font, titleComponent, x + padding, currentY, COLOR_TEXT, scale);
        currentY += lineHeight + (int)(8 * scale);

        // separator line
        guiGraphics.fill(x + padding, currentY, x + panelWidth - padding, currentY + 1, 0x30FFFFFF);
        currentY += (int)(6 * scale);

        // vote progress section
        renderPreviewVoteProgress(guiGraphics, x, currentY, panelWidth, scale);
        currentY += (int)(50 * scale);

        // reason text
        if (!tempCompact) {
            // reason label
            drawScaledText(guiGraphics, this.font, Component.literal("REASON:"), x + padding, currentY, COLOR_WARNING, scale);
            currentY += lineHeight + (int)(2 * scale);

            // reason content with background
            int reasonStartY = currentY;
            String reasonText = "Griefing spawn area";
            drawScaledText(guiGraphics, this.font, Component.literal(reasonText),
                    x + padding + (int)(4 * scale), currentY, COLOR_TEXT_DIM, scale);
            currentY += lineHeight;

            // subtle background for reason
            guiGraphics.fill(x + padding, reasonStartY - 2,
                    x + panelWidth - padding, currentY, 0x20FFFFFF);
            currentY += (int)(8 * scale);
        }

        // timer section
        if (tempWarnings) {
            renderPreviewTimer(guiGraphics, x, currentY, panelWidth, scale);
        } else {
            Component timerText = Component.literal("⏱ Time remaining: 12s");
            drawScaledText(guiGraphics, this.font, timerText, x + padding, currentY, COLOR_TEXT, scale);
        }

        // action prompt at bottom
        renderPreviewActionPrompt(guiGraphics, x, y + panelHeight - (int)(25 * scale), panelWidth, scale);
    }

    private void renderPreviewVoteProgress(GuiGraphics guiGraphics, int x, int y, int panelWidth, float scale) {
        int padding = (int)(12 * scale);
        int barX = x + padding;
        int barY = y + (int)(12 * scale + 4 * scale);
        int barWidth = panelWidth - (padding * 2);
        int barHeight = (int)(20 * scale);

        // vote counts above bar
        String yesText = "✓ 4";
        String noText = "✗ 1";

        drawScaledText(guiGraphics, this.font, Component.literal(yesText), barX, y, COLOR_YES, scale);

        // right-align no votes
        int noTextWidth = (int)(this.font.width(noText) * scale);
        drawScaledText(guiGraphics, this.font, Component.literal(noText),
                barX + barWidth - noTextWidth, y, COLOR_NO, scale);

        // progress bar background
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF2c2c2c);

        // simulate 80% yes votes
        int yesWidth = (int)(barWidth * 0.8f);

        // yes votes gradient
        guiGraphics.fillGradient(barX, barY, barX + yesWidth, barY + barHeight,
                COLOR_YES, darken(COLOR_YES, 0.7f));

        // no votes gradient
        guiGraphics.fillGradient(barX + yesWidth, barY, barX + barWidth, barY + barHeight,
                COLOR_NO, darken(COLOR_NO, 0.7f));

        // threshold marker at 66%
        int markerX = barX + (int)(barWidth * 0.66f);

        // white dashed line
        for (int i = 0; i < barHeight; i += 3) {
            guiGraphics.fill(markerX - 1, barY + i, markerX + 1, barY + Math.min(i + 2, barHeight), 0xFFFFFFFF);
        }

        // threshold label below bar
        String thresholdText = "3 needed";
        int textWidth = (int)(this.font.width(thresholdText) * scale);
        int textX = Math.min(markerX - textWidth / 2, barX + barWidth - textWidth);
        textX = Math.max(barX, textX);

        drawScaledText(guiGraphics, this.font, Component.literal(thresholdText),
                textX, barY + barHeight + (int)(4 * scale), COLOR_TEXT_DIM, scale);
    }

    private void renderPreviewTimer(GuiGraphics guiGraphics, int x, int y, int panelWidth, float scale) {
        int padding = (int)(12 * scale);
        int lineHeight = (int)(12 * scale);

        // simulate urgent timer
        float pulse = (float)Math.sin(previewPulse * 5) * 0.5f + 0.5f;
        int timerColor = interpolateColor(COLOR_WARNING, COLOR_NO, pulse);

        // background flash
        float bgPulse = (float)Math.sin(previewPulse * 5) * 0.3f + 0.3f;
        int bgColor = (int)(bgPulse * 255) << 24 | 0xFF0000;
        guiGraphics.fill(x + padding - 2, y - 2, x + panelWidth - padding + 2, y + lineHeight + 2, bgColor);

        Component timerText = Component.literal("⏰ Time remaining: 4s");
        drawScaledText(guiGraphics, this.font, timerText, x + padding, y, timerColor, scale);
    }

    private void renderPreviewActionPrompt(GuiGraphics guiGraphics, int x, int y, int panelWidth, float scale) {
        int padding = (int)(12 * scale);
        int lineHeight = (int)(12 * scale);

        // subtle highlight
        guiGraphics.fill(x + padding - 2, y - 2, x + panelWidth - padding + 2, y + lineHeight + 2, 0x20FFFFFF);

        Component prompt = Component.literal("Press [F1] Yes • [F2] No");
        int textWidth = (int)(this.font.width(prompt) * scale);
        int textX = x + (panelWidth / 2) - (textWidth / 2);
        drawScaledText(guiGraphics, this.font, prompt, textX, y, COLOR_TEXT, scale);
    }

    private int calculatePreviewPanelHeight(float scale) {
        if (tempCompact) {
            return (int)(140 * scale);
        }
        return (int)(160 * scale);
    }

    private void drawScaledText(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int color, float scale) {
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

    private int darken(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * factor);
        int g = (int)(((color >> 8) & 0xFF) * factor);
        int b = (int)((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int interpolateColor(int color1, int color2, float ratio) {
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

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}