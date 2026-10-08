package net.aevummc.jobs.config;

import net.aevummc.jobs.job.JobType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration jobs;
    public ConfigManager(JavaPlugin plugin) { this.plugin = plugin; reload(); }
    public void reload() { plugin.reloadConfig(); jobs = plugin.getConfig(); }
    public FileConfiguration config() { return jobs; }
    public int activeLimit() { return Math.max(1, jobs.getInt("professions.active-limit", 3)); }
    public boolean preserveOnLeave() { return jobs.getBoolean("professions.preserve-progression-on-leave", true); }
    public double requiredXp(int level) {
        if (level >= 50) return Double.POSITIVE_INFINITY;
        String exact = "xp-curve.required." + (level + 1);
        if (jobs.contains(exact)) return Math.max(1, jobs.getDouble(exact));
        double base = jobs.getDouble("xp-curve.base", 500);
        double growth = jobs.getDouble("xp-curve.growth", 250);
        double exponent = jobs.getDouble("xp-curve.exponent", 1.18);
        return Math.max(1, base + growth * Math.pow(level, exponent));
    }
    public double xp(JobType job, String action, Object key) {
        String path = "jobs." + job.key() + ".actions." + action;
        double def = jobs.getDouble(path + ".default-xp", 0);
        if (key != null) def = jobs.getDouble(path + ".values." + String.valueOf(key), def);
        return Math.max(0, def);
    }
    public Set<String> actionValues(JobType job, String action) {
        ConfigurationSection sec = jobs.getConfigurationSection("jobs." + job.key() + ".actions." + action + ".values");
        return sec == null ? Collections.emptySet() : sec.getKeys(false);
    }
    public double cashReward(int level) { return Math.max(0, jobs.getDouble("cash-rewards." + level, 0)); }
    public String message(String key, String fallback) { return jobs.getString("messages." + key, fallback); }
}