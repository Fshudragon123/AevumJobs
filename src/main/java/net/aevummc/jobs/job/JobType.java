package net.aevummc.jobs.job;

import org.bukkit.Material;

import java.util.Locale;

public enum JobType {
    MINER("Miner", "⛏", "Master of the earth.", Material.DIAMOND_PICKAXE),
    FARMER("Farmer", "🌾", "Cultivate fields and feed the realm.", Material.DIAMOND_HOE),
    LUMBERJACK("Lumberjack", "🪓", "Guard the forests and harvest timber.", Material.DIAMOND_AXE),
    FISHERMAN("Fisherman", "🎣", "Bring the riches of the waters home.", Material.FISHING_ROD),
    RANCHER("Rancher", "🐄", "Raise livestock and master husbandry.", Material.WHEAT),
    WARRIOR("Warrior", "⚔", "Earn glory through genuine player combat.", Material.DIAMOND_SWORD),
    HUNTER("Hunter", "🏹", "Track and defeat hostile and passive creatures.", Material.BOW),
    BUILDER("Builder", "🧱", "Raise structures worthy of Aevum.", Material.BRICKS),
    ALCHEMIST("Alchemist", "🧪", "Brew successful potions and mixtures.", Material.BREWING_STAND),
    ENCHANTER("Enchanter", "✨", "Imbue equipment with powerful enchantments.", Material.ENCHANTING_TABLE),
    BLACKSMITH("Blacksmith", "💎", "Craft, repair, smith and upgrade equipment.", Material.ANVIL),
    SCHOLAR("Scholar", "📚", "Record knowledge through books and maps.", Material.BOOK);

    private final String displayName;
    private final String icon;
    private final String description;
    private final Material material;

    JobType(String displayName, String icon, String description, Material material) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.material = material;
    }
    public String key() { return name().toLowerCase(Locale.ROOT); }
    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }
    public Material getMaterial() { return material; }
    public static JobType fromString(String raw) {
        if (raw == null) return null;
        for (JobType type : values()) if (type.name().equalsIgnoreCase(raw) || type.key().equalsIgnoreCase(raw)) return type;
        return null;
    }
}