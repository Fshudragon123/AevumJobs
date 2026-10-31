package net.aevummc.agriculture;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.aevummc.agriculture.data.AgricultureDatabase;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class AgricultureExpansion extends PlaceholderExpansion {
    private final AevumAgriculture plugin;
    public AgricultureExpansion(AevumAgriculture plugin) { this.plugin = plugin; }
    @Override public @NotNull String getIdentifier() { return "aevumagriculture"; }
    @Override public @NotNull String getAuthor() { return "AevumMC"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getUniqueId() == null) return "";
        AgricultureDatabase.Profile p = plugin.database().profile(player.getUniqueId(), player.getName() == null ? "Unknown" : player.getName());
        return switch (params.toLowerCase()) {
            case "level" -> String.valueOf(p.level());
            case "xp" -> String.valueOf(p.xp());
            case "harvests" -> String.valueOf(p.harvests());
            case "crafts" -> String.valueOf(p.crafts());
            case "sales" -> String.valueOf(p.sales());
            case "business" -> { var b = plugin.database().business(player.getUniqueId()); yield b == null ? "none" : b.name(); }
            case "business_type" -> { var b = plugin.database().business(player.getUniqueId()); yield b == null ? "none" : b.type(); }
            default -> null;
        };
    }
}
