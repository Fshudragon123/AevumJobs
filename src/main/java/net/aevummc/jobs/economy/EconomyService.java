package net.aevummc.jobs.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyService {
    private final JavaPlugin plugin;
    private Economy economy;
    public EconomyService(JavaPlugin plugin) { this.plugin = plugin; }
    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }
    public boolean available() { return economy != null; }
    public boolean deposit(Player player, double amount) {
        if (!available() || amount <= 0) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
    public String format(double amount) { return available() ? economy.format(amount) : "$" + String.format("%,.2f", amount); }
}