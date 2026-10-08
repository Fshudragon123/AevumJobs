package net.aevummc.jobs;

import net.aevummc.jobs.antiabuse.AntiAbuseService;
import net.aevummc.jobs.command.JobsCommand;
import net.aevummc.jobs.command.JobsTabCompleter;
import net.aevummc.jobs.config.ConfigManager;
import net.aevummc.jobs.database.Database;
import net.aevummc.jobs.economy.EconomyService;
import net.aevummc.jobs.gui.GuiManager;
import net.aevummc.jobs.listener.JobListener;
import net.aevummc.jobs.placeholder.AevumJobsExpansion;
import net.aevummc.jobs.reward.RewardService;
import net.aevummc.jobs.service.JobService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;

import java.sql.SQLException;

public final class AevumJobs extends JavaPlugin {
    private ConfigManager config;
    private Database database;
    private EconomyService economy;
    private RewardService rewards;
    private JobService jobs;
    private GuiManager gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config=new ConfigManager(this);
        try { database=new Database(this); }
        catch(SQLException ex) {
            getLogger().severe("Unable to open SQLite database: "+ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        economy=new EconomyService(this);
        if(!economy.setup()) getLogger().warning("Vault/economy not available. XP remains usable, cash payouts will wait for an economy provider.");
        AntiAbuseService anti=new AntiAbuseService(config,database);
        rewards=new RewardService(this,config,economy);
        jobs=new JobService(this,config,rewards,anti);
        gui=new GuiManager(this,config,jobs,rewards);
        getServer().getPluginManager().registerEvents(new JobListener(this,jobs,config,gui),this);

        PluginCommand command=getCommand("jobs");
        if(command==null) {
            getLogger().severe("The jobs command is missing from plugin.yml. Disabling AevumJobs.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(new JobsCommand(this,jobs,config,gui,rewards));
        command.setTabCompleter(new JobsTabCompleter());

        if(getServer().getPluginManager().getPlugin("PlaceholderAPI")!=null) {
            new AevumJobsExpansion(this,jobs).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }
        getLogger().info("AevumJobs 1.0.0 enabled — 12 professions, 3 active slots, SQLite persistence.");
    }

    @Override
    public void onDisable() {
        if(jobs!=null) for(org.bukkit.entity.Player p:getServer().getOnlinePlayers()) jobs.unload(p.getUniqueId());
        if(database!=null) database.close();
    }

    public ConfigManager getConfigManager(){return config;}
    public Database getDatabase(){return database;}
    public EconomyService getEconomy(){return economy;}
    public RewardService getRewards(){return rewards;}
    public JobService getJobs(){return jobs;}
    public GuiManager getGui(){return gui;}
}