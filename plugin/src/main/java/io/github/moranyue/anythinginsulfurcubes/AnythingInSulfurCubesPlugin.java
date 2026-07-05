package io.github.moranyue.anythinginsulfurcubes;

import io.github.moranyue.anythinginsulfurcubes.config.PluginConfig;
import io.github.moranyue.anythinginsulfurcubes.listener.SulfurCubeListener;
import io.github.moranyue.anythinginsulfurcubes.scheduler.CactusBehavior;
import io.github.moranyue.anythinginsulfurcubes.scheduler.CreakingHeartBehavior;
import io.github.moranyue.anythinginsulfurcubes.scheduler.PotentSulfurBehavior;
import io.github.moranyue.anythinginsulfurcubes.scheduler.TransparencyNotifyBehavior;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Anything in Sulfur Cubes - Paper/Folia plugin port.
 * <p>
 * Allows players to put nearly any block in Sulfur Cubes,
 * with each block having fitting properties and custom behaviors.
 * <p>
 * Uses a PlayerInteractEntityEvent listener to bypass vanilla's
 * sulfur_cube_swallowable item tag check, allowing any block
 * with an archetype mapping in config.yml to be placed in a sulfur cube.
 */
public final class AnythingInSulfurCubesPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private CactusBehavior cactusBehavior;
    private CreakingHeartBehavior creakingHeartBehavior;
    private PotentSulfurBehavior potentSulfurBehavior;
    private TransparencyNotifyBehavior transparencyNotifyBehavior;

    @Override
    public void onEnable() {
        // Load configuration
        this.pluginConfig = new PluginConfig(this);
        this.pluginConfig.loadConfig();

        // Register event listeners (bypasses vanilla tag check)
        registerListeners();

        // Start scheduled behaviors
        startBehaviors();

        getLogger().info("Anything in Sulfur Cubes v" + getPluginMeta().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        // Cancel all scheduled tasks
        if (cactusBehavior != null) cactusBehavior.cancel();
        if (creakingHeartBehavior != null) creakingHeartBehavior.cancel();
        if (potentSulfurBehavior != null) potentSulfurBehavior.cancel();
        if (transparencyNotifyBehavior != null) transparencyNotifyBehavior.cancel();

        getLogger().info("Anything in Sulfur Cubes disabled.");
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(
            new SulfurCubeListener(this, pluginConfig), this);
    }

    private void startBehaviors() {
        if (pluginConfig.isCactusEnabled()) {
            cactusBehavior = new CactusBehavior(this, pluginConfig);
            cactusBehavior.start();
        }

        if (pluginConfig.isCreakingHeartEnabled()) {
            creakingHeartBehavior = new CreakingHeartBehavior(this, pluginConfig);
            creakingHeartBehavior.start();
        }

        if (pluginConfig.isPotentSulfurEnabled()) {
            potentSulfurBehavior = new PotentSulfurBehavior(this, pluginConfig);
            potentSulfurBehavior.start();
        }

        if (pluginConfig.isTransparencyNotifyEnabled()) {
            transparencyNotifyBehavior = new TransparencyNotifyBehavior(this, pluginConfig);
            transparencyNotifyBehavior.start();
        }
    }

    /**
     * Reloads the plugin configuration and restarts behaviors.
     */
    public void reload() {
        pluginConfig.loadConfig();
        // Cancel existing tasks
        if (cactusBehavior != null) cactusBehavior.cancel();
        if (creakingHeartBehavior != null) creakingHeartBehavior.cancel();
        if (potentSulfurBehavior != null) potentSulfurBehavior.cancel();
        if (transparencyNotifyBehavior != null) transparencyNotifyBehavior.cancel();
        // Restart with new config
        startBehaviors();
        getLogger().info("Configuration reloaded.");
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }
}
