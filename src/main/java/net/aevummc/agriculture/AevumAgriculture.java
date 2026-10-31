package net.aevummc.agriculture;

import net.aevummc.agriculture.command.AgricultureCommand;
import net.aevummc.agriculture.data.AgricultureDatabase;
import net.aevummc.agriculture.gui.AgricultureGui;
import net.aevummc.agriculture.hook.TownyHook;
import net.aevummc.agriculture.listener.AgricultureListener;
import net.aevummc.agriculture.model.AgricultureRegistry;
import net.aevummc.agriculture.resourcepack.ResourcePackManager;
import net.aevummc.agriculture.service.BusinessService;
import net.aevummc.agriculture.service.CropService;
import net.aevummc.agriculture.service.ItemFactory;
import net.aevummc.agriculture.service.MarketService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.logging.Level;

public final class AevumAgriculture extends JavaPlugin {
    private AgricultureDatabase database;
    private AgricultureRegistry registry;
    private ItemFactory items;
    private CropService crops;
    private MarketService market;
    private BusinessService businesses;
    private AgricultureGui gui;
    private ResourcePackManager packManager;
    private TownyHook towny;
    private Economy economy;
    private NamespacedKey stationKey;
    private NamespacedKey stationOwnerKey;

    @Override public void onEnable() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Could not create plugins/AevumAgriculture. Disabling.");
            getServer().getPluginManager().disablePlugin(this); return;
        }
        saveDefaultConfig();
        saveResource("crops.yml", false);
        saveResource("foods.yml", false);
        saveResource("recipes.yml", false);
        try {
            database = new AgricultureDatabase(new File(getDataFolder(), "agriculture.db"), getLogger());
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, "Could not initialise agriculture.db. Disabling without deleting any data.", ex);
            getServer().getPluginManager().disablePlugin(this); return;
        }

        registry = new AgricultureRegistry(this);
        registry.load();
        items = new ItemFactory(this);
        towny = new TownyHook();
        setupEconomy();
        crops = new CropService(this);
        market = new MarketService(this);
        businesses = new BusinessService(this);
        gui = new AgricultureGui(this);
        packManager = new ResourcePackManager(this);
        packManager.start();

        AgricultureListener listener = new AgricultureListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(gui, this);
        getServer().getPluginManager().registerEvents(packManager, this);
        AgricultureCommand command = new AgricultureCommand(this);
        if (getCommand("agriculture") == null) {
            getLogger().severe("agriculture command missing from plugin.yml. Disabling.");
            getServer().getPluginManager().disablePlugin(this); return;
        }
        getCommand("agriculture").setExecutor(command);
        getCommand("agriculture").setTabCompleter(command);

        for (var world : Bukkit.getWorlds()) for (var chunk : world.getLoadedChunks()) crops.loadChunk(chunk);
        long period = Math.max(10L, getConfig().getLong("settings.growth-update-ticks", 20L));
        Bukkit.getScheduler().runTaskTimer(this, crops::tickGrowth, period, period);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") && getConfig().getBoolean("integrations.placeholders.enabled", true)) {
            new AgricultureExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered: %aevumagriculture_...%");
        }
        getLogger().info("AevumAgriculture v" + getPluginMeta().getVersion() + " enabled — " + registry.crops().size() + " crops, " + registry.goods().size() + " goods, " + registry.recipes().size() + " recipes.");
        getLogger().info("Bone meal is the only growth-boosting item. No compost, manure or other fertiliser system is enabled.");
    }

    @Override public void onDisable() {
        if (crops != null) crops.shutdown();
        if (packManager != null) packManager.stop();
        if (database != null) database.close();
        getLogger().info("AevumAgriculture shut down safely.");
    }

    public void reloadAgriculture() {
        reloadConfig();
        registry.load();
        setupEconomy();
        if (packManager != null) packManager.start();
        getLogger().info("AevumAgriculture configuration and catalogues reloaded.");
    }

    private void setupEconomy() {
        economy = null;
        if (!getServer().getPluginManager().isPluginEnabled("Vault")) {
            getLogger().warning("Vault not found: seed shop, marketplace and player business payments are disabled until Vault + an economy provider are installed.");
            return;
        }
        RegisteredServiceProvider<Economy> registration = getServer().getServicesManager().getRegistration(Economy.class);
        if (registration != null) economy = registration.getProvider();
        if (economy == null) getLogger().warning("Vault is present but no economy provider is registered.");
        else getLogger().info("Vault economy connected: " + economy.getName());
    }

    public AgricultureDatabase database() { return database; }
    public AgricultureRegistry registry() { return registry; }
    public ItemFactory items() { return items; }
    public CropService crops() { return crops; }
    public MarketService market() { return market; }
    public BusinessService businesses() { return businesses; }
    public AgricultureGui gui() { return gui; }
    public ResourcePackManager packManager() { return packManager; }
    public TownyHook towny() { return towny; }
    public Economy economy() { return economy; }
    public NamespacedKey stationKey() { if (stationKey == null) stationKey = new NamespacedKey(this, "station_id"); return stationKey; }
    public NamespacedKey stationOwnerKey() { if (stationOwnerKey == null) stationOwnerKey = new NamespacedKey(this, "station_owner"); return stationOwnerKey; }
    public Component message(String text) { return Component.text("🌾 AevumAgriculture  ▸  ", TextColor.fromHexString("#D4972C")).append(Component.text(text, TextColor.fromHexString("#F3EBDD"))); }
}
