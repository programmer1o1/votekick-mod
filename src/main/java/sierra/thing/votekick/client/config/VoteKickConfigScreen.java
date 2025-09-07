// VoteKickConfigScreen.java
package sierra.thing.votekick.client.config;

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
        this.renderBackground(guiGraphics);

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
        int previewX = this.width - 180;
        int previewY = 40;
        int previewWidth = 160;
        int previewHeight = 140;

        // preview label
        guiGraphics.drawString(this.font, "Preview:", previewX, previewY - 12, 0xFFFF55);

        // calculate preview panel dimensions
        float scale = Math.min(tempScale * 0.7f, 1.0f); // scale down for preview
        int baseWidth = 140;
        int baseHeight = tempCompact ? 70 : 90;
        int panelWidth = (int)(baseWidth * scale);
        int panelHeight = (int)(baseHeight * scale);
        int padding = Math.max(4, (int)(6 * scale));

        // center the preview panel
        int panelX = previewX + (previewWidth - panelWidth) / 2;
        int panelY = previewY + 10;

        // shadow
        guiGraphics.fill(panelX + 1, panelY + 1, panelX + panelWidth + 1, panelY + panelHeight + 1, COLOR_SHADOW);

        // main background
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_BACKGROUND);

        // header background
        int headerHeight = Math.max(12, (int)(16 * scale));
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + headerHeight, COLOR_HEADER_BG);

        // accent stripe
        int stripeWidth = Math.max(2, (int)(3 * scale));
        guiGraphics.fill(panelX, panelY, panelX + stripeWidth, panelY + panelHeight, COLOR_ACCENT);

        // content
        int contentY = panelY + padding;
        int lineHeight = Math.max(8, (int)(9 * scale));

        // title
        String title = "🗳 Kick TestUser?";
        if (scale < 0.8f) {
            title = "Kick TestUser?";
        }
        guiGraphics.drawString(this.font, title, panelX + padding, contentY, COLOR_TEXT);
        contentY += lineHeight + 2;

        // reason
        if (!tempCompact) {
            String reason = "Reason: Griefing";
            guiGraphics.drawString(this.font, reason, panelX + padding, contentY, COLOR_TEXT_DIM);
            contentY += lineHeight + 1;
        }

        // vote progress bar
        int barWidth = panelWidth - (padding * 2);
        int barHeight = Math.max(6, (int)(8 * scale));

        guiGraphics.fill(panelX + padding, contentY, panelX + padding + barWidth, contentY + barHeight, 0xFF2c2c2c);

        // simulate 4 yes, 1 no votes
        int yesWidth = (int)(barWidth * 0.8f);
        guiGraphics.fill(panelX + padding, contentY, panelX + padding + yesWidth, contentY + barHeight, COLOR_YES);
        guiGraphics.fill(panelX + padding + yesWidth, contentY, panelX + padding + barWidth, contentY + barHeight, COLOR_NO);

        contentY += barHeight + 4;

        // timer (with pulse if warnings enabled)
        int timerColor = COLOR_TEXT;
        if (tempWarnings) {
            float pulse = (float)Math.sin(previewPulse * 4) * 0.4f + 0.6f;
            timerColor = interpolateColor(COLOR_TEXT, COLOR_WARNING, 1.0f - pulse);
        }
        guiGraphics.drawString(this.font, "Time remaining: 12s", panelX + padding, contentY, timerColor);
        contentY += lineHeight + 2;

        // action prompt
        String prompt = "[F1] Yes • [F2] No";
        int promptWidth = this.font.width(prompt);
        int promptX = panelX + (panelWidth - promptWidth) / 2;
        guiGraphics.drawString(this.font, prompt, promptX, panelY + panelHeight - lineHeight - 2, COLOR_TEXT);

        // position indicator below preview
        String[] posNames = {"Top-Right", "Top-Left", "Bottom-Right", "Bottom-Left"};
        String posText = posNames[tempPosition];
        int posTextWidth = this.font.width(posText);
        guiGraphics.drawString(this.font, posText, previewX + (previewWidth - posTextWidth) / 2,
                panelY + panelHeight + 8, 0xAAAAAA);
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