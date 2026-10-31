package net.aevummc.agriculture.service;

import net.aevummc.agriculture.AevumAgriculture;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public final class MarketService {
    private final AevumAgriculture plugin;
    public MarketService(AevumAgriculture plugin) { this.plugin = plugin; }

    public double price(String itemId) {
        double base = plugin.registry().basePrice(itemId);
        if (base <= 0) return 0;
        int target = Math.max(1, plugin.getConfig().getInt("economy.market-daily-cap-per-item", 256));
        int sold = plugin.database().soldLast24h(itemId);
        double impact = plugin.getConfig().getDouble("economy.market-price-impact", 0.55);
        double multiplier = 1.35 - Math.min(1.5, sold / (double) target) * impact;
        multiplier = Math.max(plugin.getConfig().getDouble("economy.market-min-price-multiplier", 0.55),
                Math.min(plugin.getConfig().getDouble("economy.market-max-price-multiplier", 1.35), multiplier));
        return base * multiplier * plugin.getConfig().getDouble("economy.market-base-price-multiplier", 1.0);
    }

    public boolean sellAll(Player player, String itemId) {
        Economy economy = plugin.economy();
        if (economy == null || !plugin.getConfig().getBoolean("economy.market-enabled", true)) {
            player.sendMessage(plugin.message("The Imperial Commodity Exchange is currently unavailable.")); return false;
        }
        double unit = price(itemId);
        if (unit <= 0) { player.sendMessage(plugin.message("That item is not accepted by the Imperial Commodity Exchange.")); return false; }
        int owned = count(player, itemId);
        int dailyCap = Math.max(1, plugin.getConfig().getInt("economy.market-daily-cap-per-item", 256));
        int remaining = Math.max(0, dailyCap - plugin.database().soldLast24h(itemId));
        int amount = Math.min(owned, remaining);
        if (amount <= 0) { player.sendMessage(plugin.message("The market has reached today's purchase limit for that item.")); return false; }
        double total = Math.round(unit * amount * 100.0) / 100.0;
        remove(player, itemId, amount);
        economy.depositPlayer(player, total);
        plugin.database().recordSale(itemId, amount, unit, player.getUniqueId(), "npc");
        plugin.database().addActivity(player.getUniqueId(), player.getName(), plugin.getConfig().getLong("progression.sale-xp", 2), 0, 0, 1);
        player.sendMessage(plugin.message("Sold " + amount + " × " + plugin.registry().itemName(itemId) + " for " + ItemFactory.money(total) + "."));
        return true;
    }

    private int count(Player player, String id) {
        int total=0; for(ItemStack item:player.getInventory().getContents()) if(id.equals(plugin.items().itemId(item))) total+=item.getAmount(); return total;
    }
    private void remove(Player player,String id,int amount) {
        int left=amount; ItemStack[] contents=player.getInventory().getContents();
        for(int i=0;i<contents.length&&left>0;i++){ItemStack item=contents[i];if(!id.equals(plugin.items().itemId(item)))continue;int take=Math.min(left,item.getAmount());item.setAmount(item.getAmount()-take);if(item.getAmount()<=0)contents[i]=null;left-=take;}
        player.getInventory().setContents(contents);
    }
}
