package io.github.moranyue.anythinginsulfurcubes.config;

import io.github.moranyue.anythinginsulfurcubes.AnythingInSulfurCubesPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Level;

/**
 * Manages the plugin behavior configuration (config.yml).
 * <p>
 * Reads custom behavior settings for cactus damage, creaking heart sounds,
 * and potent sulfur effects. Block-to-archetype mappings are now handled
 * by {@link io.github.moranyue.anythinginsulfurcubes.AnythingInSulfurCubesBootstrap}
 * via the registry compose event system, so they no longer reside here.
 */
public class PluginConfig {

    private static final String CONFIG_FILE_NAME = "config.yml";

    private final AnythingInSulfurCubesPlugin plugin;
    private FileConfiguration config;

    // Behavior settings
    private boolean cactusEnabled = true;
    private double cactusDamage = 1.0;
    private double cactusRange = 1.35;

    private boolean creakingHeartEnabled = true;
    private int creakingHeartTimerMin = 40;
    private int creakingHeartTimerMax = 100;

    private boolean potentSulfurEnabled = true;
    private int potentSulfurDuration = 5; // seconds
    private int potentSulfurAmplifier = 1;
    private double potentSulfurRange = 1.35;

    public PluginConfig(AnythingInSulfurCubesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads (or reloads) the configuration from config.yml.
     * Falls back to defaults embedded in the plugin JAR.
     */
    public void loadConfig() {
        // Save default config if not present
        plugin.saveDefaultConfig();
        // Reload from file
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Ensure all default behavior keys exist
        mergeDefaults();

        // Read behavior settings
        cactusEnabled = config.getBoolean("behaviors.cactus.enabled", true);
        cactusDamage = config.getDouble("behaviors.cactus.damage", 1.0);
        cactusRange = config.getDouble("behaviors.cactus.range", 1.35);

        creakingHeartEnabled = config.getBoolean("behaviors.creaking-heart.enabled", true);
        creakingHeartTimerMin = config.getInt("behaviors.creaking-heart.timer-min", 40);
        creakingHeartTimerMax = config.getInt("behaviors.creaking-heart.timer-max", 100);

        potentSulfurEnabled = config.getBoolean("behaviors.potent-sulfur.enabled", true);
        potentSulfurDuration = config.getInt("behaviors.potent-sulfur.duration-seconds", 5);
        potentSulfurAmplifier = config.getInt("behaviors.potent-sulfur.amplifier", 1);
        potentSulfurRange = config.getDouble("behaviors.potent-sulfur.range", 1.35);
    }

    /**
     * Merges defaults from the bundled config.yml into the user's config.
     */
    private void mergeDefaults() {
        // Ensure all default behavior keys exist
        if (!config.contains("behaviors.cactus.enabled")) config.set("behaviors.cactus.enabled", true);
        if (!config.contains("behaviors.cactus.damage")) config.set("behaviors.cactus.damage", 1.0);
        if (!config.contains("behaviors.cactus.range")) config.set("behaviors.cactus.range", 1.35);
        if (!config.contains("behaviors.creaking-heart.enabled")) config.set("behaviors.creaking-heart.enabled", true);
        if (!config.contains("behaviors.creaking-heart.timer-min")) config.set("behaviors.creaking-heart.timer-min", 40);
        if (!config.contains("behaviors.creaking-heart.timer-max")) config.set("behaviors.creaking-heart.timer-max", 100);
        if (!config.contains("behaviors.potent-sulfur.enabled")) config.set("behaviors.potent-sulfur.enabled", true);
        if (!config.contains("behaviors.potent-sulfur.duration-seconds")) config.set("behaviors.potent-sulfur.duration-seconds", 5);
        if (!config.contains("behaviors.potent-sulfur.amplifier")) config.set("behaviors.potent-sulfur.amplifier", 1);
        if (!config.contains("behaviors.potent-sulfur.range")) config.set("behaviors.potent-sulfur.range", 1.35);

        // Save changes back
        plugin.saveConfig();
    }

    // --- Getters ---

    public boolean isCactusEnabled() { return cactusEnabled; }
    public double getCactusDamage() { return cactusDamage; }
    public double getCactusRange() { return cactusRange; }

    public boolean isCreakingHeartEnabled() { return creakingHeartEnabled; }
    public int getCreakingHeartTimerMin() { return creakingHeartTimerMin; }
    public int getCreakingHeartTimerMax() { return creakingHeartTimerMax; }

    public boolean isPotentSulfurEnabled() { return potentSulfurEnabled; }
    public int getPotentSulfurDuration() { return potentSulfurDuration; }
    public int getPotentSulfurAmplifier() { return potentSulfurAmplifier; }
    public double getPotentSulfurRange() { return potentSulfurRange; }
}
