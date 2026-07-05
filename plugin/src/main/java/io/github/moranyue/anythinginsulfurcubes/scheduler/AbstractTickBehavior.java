package io.github.moranyue.anythinginsulfurcubes.scheduler;

import io.github.moranyue.anythinginsulfurcubes.AnythingInSulfurCubesPlugin;
import io.github.moranyue.anythinginsulfurcubes.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Abstract base class for all tick-based behaviors.
 * <p>
 * Schedules a repeating task per-world using {@link org.bukkit.scheduler.BukkitRegionScheduler#runAtFixedRate},
 * which is compatible with both Paper (single-threaded) and Folia (per-region threading).
 * Entity state access is safe because each task runs on its world's region thread.
 */
public abstract class AbstractTickBehavior {

    protected final AnythingInSulfurCubesPlugin plugin;
    protected final PluginConfig config;

    private final List<Runnable> cancelTokens = new ArrayList<>();

    public AbstractTickBehavior(AnythingInSulfurCubesPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Starts per-world repeating tasks, running every tick (1 tick = 50ms).
     * Uses the world's spawn location as the region anchor for Folia scheduling.
     */
    public void start() {
        for (World world : plugin.getServer().getWorlds()) {
            var task = Bukkit.getRegionScheduler().runAtFixedRate(
                plugin,
                world.getSpawnLocation(),
                scheduledTask -> {
                    try {
                        tick(world);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE,
                            "Error in " + getBehaviorName() + " behavior for world " + world.getName(), e);
                    }
                },
                1,  // delay ticks
                1   // period ticks
            );
            cancelTokens.add(task::cancel);
        }
    }

    /**
     * Cancels all per-world tasks.
     */
    public void cancel() {
        for (Runnable cancel : cancelTokens) {
            cancel.run();
        }
        cancelTokens.clear();
    }

    /**
     * Called every tick for each world, on that world's region thread.
     *
     * @param world the world to process
     */
    protected abstract void tick(World world);

    /**
     * @return a human-readable name for this behavior (for logging)
     */
    public abstract String getBehaviorName();
}
