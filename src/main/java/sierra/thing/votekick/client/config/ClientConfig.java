// ClientConfig.java (Properties version)
package sierra.thing.votekick.client.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sierra.thing.votekick.VoteKickMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * client-side configuration for ui scaling and other preferences
 * uses properties file for easy manual editing
 */
public class ClientConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteKickMod.MOD_ID);

    // config values
    private float uiScale = 1.0f;
    private boolean soundEnabled = true;
    private boolean compactMode = false;
    private int panelPosition = 0; // 0=top-right, 1=top-left, 2=bottom-right, 3=bottom-left
    private boolean showTimeWarnings = true;
    private boolean animationsEnabled = true;

    private final File configFile;

    public ClientConfig() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        this.configFile = new File(configDir, "votekick-client.properties");
    }

    public void load() {
        Properties props = new Properties();

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                props.load(reader);
                LOGGER.info("Loaded client config from {}", configFile.getName());
            } catch (IOException e) {
                LOGGER.error("Failed to load client config", e);
            }
        } else {
            LOGGER.info("Creating new client config file");
        }

        // load values with validation
        try {
            float scale = Float.parseFloat(props.getProperty("ui_scale", "1.0"));
            this.uiScale = Math.max(0.5f, Math.min(3.0f, scale));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid ui_scale value, using default");
            this.uiScale = 1.0f;
        }

        this.soundEnabled = Boolean.parseBoolean(props.getProperty("sound_enabled", "true"));
        this.compactMode = Boolean.parseBoolean(props.getProperty("compact_mode", "false"));
        this.showTimeWarnings = Boolean.parseBoolean(props.getProperty("show_time_warnings", "true"));
        this.animationsEnabled = Boolean.parseBoolean(props.getProperty("animations_enabled", "true"));

        try {
            int pos = Integer.parseInt(props.getProperty("panel_position", "0"));
            this.panelPosition = Math.max(0, Math.min(3, pos));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid panel_position value, using default");
            this.panelPosition = 0;
        }

        // save to create/update config file with comments
        save();
    }

    public void save() {
        Properties props = new Properties();

        props.setProperty("ui_scale", Float.toString(uiScale));
        props.setProperty("sound_enabled", Boolean.toString(soundEnabled));
        props.setProperty("compact_mode", Boolean.toString(compactMode));
        props.setProperty("panel_position", Integer.toString(panelPosition));
        props.setProperty("show_time_warnings", Boolean.toString(showTimeWarnings));
        props.setProperty("animations_enabled", Boolean.toString(animationsEnabled));

        try (FileWriter writer = new FileWriter(configFile)) {
            props.store(writer, createConfigHeader());
        } catch (IOException e) {
            LOGGER.error("Failed to save client config", e);
        }
    }

    private String createConfigHeader() {
        return "VoteKick Client Configuration\n" +
                "# \n" +
                "# ui_scale: Scale factor for vote panel (0.5 to 3.0, default: 1.0)\n" +
                "#   - 0.5 = Half size (compact)\n" +
                "#   - 1.0 = Normal size\n" +
                "#   - 1.5 = 50% larger\n" +
                "#   - 2.0 = Double size\n" +
                "# \n" +
                "# sound_enabled: Enable vote notification sounds (true/false, default: true)\n" +
                "# \n" +
                "# compact_mode: Hide reason text for smaller UI (true/false, default: false)\n" +
                "# \n" +
                "# panel_position: Panel corner position (default: 0)\n" +
                "#   - 0 = Top-right corner\n" +
                "#   - 1 = Top-left corner\n" +
                "#   - 2 = Bottom-right corner\n" +
                "#   - 3 = Bottom-left corner\n" +
                "# \n" +
                "# show_time_warnings: Show pulsing timer warnings when time is low (true/false, default: true)\n" +
                "# \n" +
                "# animations_enabled: Enable panel slide animations (true/false, default: true)\n" +
                "#   - Set to false for better performance on slower machines\n" +
                "# ";
    }

    // getters
    public float getUiScale() {
        return uiScale;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public boolean isCompactMode() {
        return compactMode;
    }

    public int getPanelPosition() {
        return panelPosition;
    }

    public boolean isShowTimeWarnings() {
        return showTimeWarnings;
    }

    public boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    // setters with validation and auto-save
    public void setUiScale(float scale) {
        float oldScale = this.uiScale;
        this.uiScale = Math.max(0.5f, Math.min(3.0f, scale));
        if (oldScale != this.uiScale) {
            save();
            LOGGER.debug("UI scale changed to {}", this.uiScale);
        }
    }

    public void setSoundEnabled(boolean enabled) {
        if (this.soundEnabled != enabled) {
            this.soundEnabled = enabled;
            save();
            LOGGER.debug("Sound enabled: {}", enabled);
        }
    }

    public void setCompactMode(boolean compact) {
        if (this.compactMode != compact) {
            this.compactMode = compact;
            save();
            LOGGER.debug("Compact mode: {}", compact);
        }
    }

    public void setPanelPosition(int position) {
        int newPos = Math.max(0, Math.min(3, position));
        if (this.panelPosition != newPos) {
            this.panelPosition = newPos;
            save();
            String[] positions = {"top-right", "top-left", "bottom-right", "bottom-left"};
            LOGGER.debug("Panel position: {}", positions[newPos]);
        }
    }

    public void setShowTimeWarnings(boolean show) {
        if (this.showTimeWarnings != show) {
            this.showTimeWarnings = show;
            save();
            LOGGER.debug("Time warnings: {}", show);
        }
    }

    public void setAnimationsEnabled(boolean enabled) {
        if (this.animationsEnabled != enabled) {
            this.animationsEnabled = enabled;
            save();
            LOGGER.debug("Animations enabled: {}", enabled);
        }
    }

    /**
     * get position name for display
     */
    public String getPositionName() {
        String[] positions = {"Top-Right", "Top-Left", "Bottom-Right", "Bottom-Left"};
        return positions[Math.max(0, Math.min(3, panelPosition))];
    }

    /**
     * reset all settings to defaults
     */
    public void resetToDefaults() {
        this.uiScale = 1.0f;
        this.soundEnabled = true;
        this.compactMode = false;
        this.panelPosition = 0;
        this.showTimeWarnings = true;
        this.animationsEnabled = true;
        save();
        LOGGER.info("Reset client config to defaults");
    }
}