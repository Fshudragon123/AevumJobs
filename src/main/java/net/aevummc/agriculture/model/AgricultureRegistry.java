package net.aevummc.agriculture.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import net.aevummc.agriculture.AevumAgriculture;

import java.io.File;
import java.util.*;

/** Loads every crop, item and production recipe from editable YAML catalogues. */
public final class AgricultureRegistry {
    public record Crop(String id, String name, String category, String region, String visualStyle,
                       int growthSeconds, double seedPrice, double marketPrice, int minYield, int maxYield,
                       double seedReturnChance, String description) {}
    public record Good(String id, String name, String category, String model, boolean edible,
                       int nutrition, float saturation, double marketPrice, String description) {}
    public record Recipe(String id, String name, String station, Map<String, Integer> ingredients,
                         String result, int amount, int xp, String description) {}

    private final AevumAgriculture plugin;
    private final Map<String, Crop> crops = new LinkedHashMap<>();
    private final Map<String, Good> goods = new LinkedHashMap<>();
    private final Map<String, Recipe> recipes = new LinkedHashMap<>();

    public AgricultureRegistry(AevumAgriculture plugin) { this.plugin = plugin; }

    public void load() {
        crops.clear(); goods.clear(); recipes.clear();
        loadCrops(); loadGoods(); loadRecipes();
        plugin.getLogger().info("Loaded " + crops.size() + " crops, " + goods.size() + " custom goods and " + recipes.size() + " recipes.");
    }

    private void loadCrops() {
        File file = new File(plugin.getDataFolder(), "crops.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("crops");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection c = section.getConfigurationSection(id);
            if (c == null || !c.getBoolean("enabled", true)) continue;
            String safe = id.toLowerCase(Locale.ROOT);
            crops.put(safe, new Crop(safe, c.getString("name", title(safe)),
                    c.getString("category", "vegetables"), c.getString("region", "Aevum"),
                    c.getString("visual-style", "leaf"), Math.max(60, c.getInt("growth-seconds", 600)),
                    Math.max(0.01, c.getDouble("seed-price", 2.0)), Math.max(0.0, c.getDouble("market-price", 2.5)),
                    Math.max(1, c.getInt("yield-min", 1)), Math.max(c.getInt("yield-min", 1), c.getInt("yield-max", 3)),
                    Math.max(0.0, Math.min(1.0, c.getDouble("seed-return-chance", 0.28))),
                    c.getString("description", "A cultivated Aevum crop.")));
        }
    }

    private void loadGoods() {
        File file = new File(plugin.getDataFolder(), "foods.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("items");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection g = section.getConfigurationSection(id);
            if (g == null || !g.getBoolean("enabled", true)) continue;
            String safe = id.toLowerCase(Locale.ROOT);
            goods.put(safe, new Good(safe, g.getString("name", title(safe)),
                    g.getString("category", "ingredient"), g.getString("model", "ingredient/" + safe),
                    g.getBoolean("edible", false), Math.max(0, g.getInt("nutrition", 0)),
                    Math.max(0f, (float) g.getDouble("saturation", 0.0)), Math.max(0.0, g.getDouble("market-price", 0.0)),
                    g.getString("description", "An agricultural product.")));
        }
    }

    private void loadRecipes() {
        File file = new File(plugin.getDataFolder(), "recipes.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("recipes");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection r = section.getConfigurationSection(id);
            if (r == null || !r.getBoolean("enabled", true)) continue;
            Map<String, Integer> ingredients = new LinkedHashMap<>();
            ConfigurationSection input = r.getConfigurationSection("ingredients");
            if (input != null) for (String key : input.getKeys(false)) ingredients.put(key, Math.max(1, input.getInt(key, 1)));
            if (ingredients.isEmpty()) continue;
            String safe = id.toLowerCase(Locale.ROOT);
            recipes.put(safe, new Recipe(safe, r.getString("name", title(safe)),
                    r.getString("station", "kitchen"), Collections.unmodifiableMap(ingredients),
                    r.getString("result", ""), Math.max(1, r.getInt("amount", 1)),
                    Math.max(1, r.getInt("xp", 5)), r.getString("description", "A prepared agricultural recipe.")));
        }
    }

    public Collection<Crop> crops() { return Collections.unmodifiableCollection(crops.values()); }
    public Collection<Good> goods() { return Collections.unmodifiableCollection(goods.values()); }
    public Collection<Recipe> recipes() { return Collections.unmodifiableCollection(recipes.values()); }
    public Crop crop(String id) { return id == null ? null : crops.get(id.toLowerCase(Locale.ROOT)); }
    public Good good(String id) { return id == null ? null : goods.get(id.toLowerCase(Locale.ROOT)); }
    public Recipe recipe(String id) { return id == null ? null : recipes.get(id.toLowerCase(Locale.ROOT)); }
    public List<Crop> cropsByCategory(String category) {
        return crops.values().stream().filter(c -> category == null || category.equals("all") || c.category().equalsIgnoreCase(category))
                .sorted(Comparator.comparing(Crop::name)).toList();
    }
    public List<Recipe> recipesByStation(String station) {
        return recipes.values().stream().filter(r -> r.station().equalsIgnoreCase(station)).sorted(Comparator.comparing(Recipe::name)).toList();
    }
    public Set<String> categories() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        crops.values().forEach(c -> result.add(c.category()));
        return result;
    }
    public double basePrice(String itemId) {
        if (itemId == null) return 0;
        if (itemId.startsWith("produce_")) {
            Crop c = crop(itemId.substring("produce_".length()));
            return c == null ? 0 : c.marketPrice();
        }
        Good g = good(itemId);
        return g == null ? 0 : g.marketPrice();
    }
    public String itemName(String itemId) {
        if (itemId == null) return "Unknown Item";
        if (itemId.startsWith("produce_")) {
            Crop c = crop(itemId.substring("produce_".length()));
            if (c != null) return c.name();
        }
        Good g = good(itemId);
        if (g != null) return g.name();
        if (itemId.startsWith("seed_")) {
            Crop c = crop(itemId.substring("seed_".length()));
            if (c != null) return c.name() + " Seeds";
        }
        return title(itemId);
    }
    public static String title(String id) {
        String[] parts = id.replace('_', ' ').split(" ");
        StringJoiner join = new StringJoiner(" ");
        for (String p : parts) if (!p.isBlank()) join.add(p.substring(0, 1).toUpperCase(Locale.ROOT) + p.substring(1));
        return join.toString();
    }
}
