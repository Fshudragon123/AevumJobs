package net.aevummc.agriculture.service;

import net.aevummc.agriculture.AevumAgriculture;
import net.aevummc.agriculture.model.AgricultureRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Creates namespaced, resource-pack-backed items with persistent server-side identities. */
public final class ItemFactory {
    private static final TextColor GOLD = TextColor.fromHexString("#D4972C");
    private static final TextColor PALE = TextColor.fromHexString("#F3EBDD");
    private static final TextColor MUTED = TextColor.fromHexString("#A8A095");
    private final AevumAgriculture plugin;
    private final NamespacedKey kindKey;
    private final NamespacedKey idKey;
    private final NamespacedKey actionKey;

    public ItemFactory(AevumAgriculture plugin) {
        this.plugin = plugin;
        kindKey = new NamespacedKey(plugin, "item_kind");
        idKey = new NamespacedKey(plugin, "item_id");
        actionKey = new NamespacedKey(plugin, "gui_action");
    }

    public ItemStack seed(AgricultureRegistry.Crop crop, int amount) {
        return custom("seed", crop.id(), crop.name() + " Seeds", "seed/" + crop.id(), amount,
                List.of("CATEGORY  /  " + crop.category().replace('_',' ').toUpperCase(Locale.ROOT),
                        "REGION  /  " + crop.region(), "GROWTH  /  " + formatTime(crop.growthSeconds()),
                        "SEED PRICE  /  " + money(crop.seedPrice()), "Plant on hydrated or dry farmland."));
    }

    public ItemStack produce(AgricultureRegistry.Crop crop, int amount) {
        return custom("produce", "produce_" + crop.id(), crop.name(), "produce/" + crop.id(), amount,
                List.of("HARVESTED PRODUCE", "REGION  /  " + crop.region(), crop.description()));
    }

    public ItemStack good(AgricultureRegistry.Good good, int amount) {
        ItemStack stack = custom(good.edible() ? "food" : "ingredient", good.id(), good.name(), good.model(), amount,
                List.of(good.category().replace('_',' ').toUpperCase(Locale.ROOT), good.description(),
                        good.edible() ? "FOOD  /  " + good.nutrition() + " hunger  •  " + good.saturation() + " saturation" : "PROCESSED AGRICULTURAL GOOD"));
        if (good.edible()) {
            ItemMeta meta = stack.getItemMeta();
            FoodComponent food = meta.getFood();
            food.setNutrition(good.nutrition());
            food.setSaturation(good.saturation());
            food.setCanAlwaysEat(false);
            meta.setFood(food);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack station(String id, String name, String material, int amount) {
        Material base = Material.matchMaterial(material == null ? "BARREL" : material);
        if (base == null || !base.isItem()) base = Material.BARREL;
        ItemStack stack = new ItemStack(base, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = stack.getItemMeta();
        meta.setItemModel(NamespacedKey.fromString("aevumagriculture:station/" + id));
        meta.displayName(Component.text(name, PALE));
        meta.lore(List.of(Component.text("IMPERIAL PRODUCTION EQUIPMENT", GOLD),
                Component.text("Place this kit to establish a " + name + ".", MUTED),
                Component.text("Use the station to process ingredients.", MUTED)));
        meta.getPersistentDataContainer().set(kindKey, PersistentDataType.STRING, "station");
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack cropStage(AgricultureRegistry.Crop crop, int stage) {
        return custom("crop_visual", crop.id(), crop.name(), "crop/" + crop.id() + "_stage_" + Math.max(0, Math.min(5, stage)), 1,
                List.of("Aevum agricultural crop visual"));
    }

    public ItemStack gui(String icon, String name, String action, List<String> lore) {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemModel(NamespacedKey.fromString("aevumagriculture:gui/" + icon));
        meta.displayName(Component.text(name, GOLD));
        if (lore != null && !lore.isEmpty()) meta.lore(lore.stream().map(s -> Component.text(s, MUTED)).toList());
        if (action != null) meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack filler() { return new ItemStack(Material.AIR); }

    private ItemStack custom(String kind, String id, String name, String model, int amount, List<String> lore) {
        ItemStack stack = new ItemStack(Material.PAPER, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = stack.getItemMeta();
        meta.setItemModel(NamespacedKey.fromString("aevumagriculture:" + model));
        meta.displayName(Component.text(name, PALE));
        meta.lore(lore.stream().map(s -> Component.text(s, MUTED)).toList());
        meta.getPersistentDataContainer().set(kindKey, PersistentDataType.STRING, kind);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
        stack.setItemMeta(meta);
        return stack;
    }

    public String itemId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }
    public String itemKind(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(kindKey, PersistentDataType.STRING);
    }
    public String action(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }
    public boolean isSeed(ItemStack stack) { return "seed".equals(itemKind(stack)); }
    public String seedCropId(ItemStack stack) { return isSeed(stack) ? itemId(stack) : null; }
    public ItemStack createById(String id, int amount) {
        if (id == null) return null;
        if (id.startsWith("seed_")) {
            AgricultureRegistry.Crop crop = plugin.registry().crop(id.substring(5));
            return crop == null ? null : seed(crop, amount);
        }
        if (id.startsWith("produce_")) {
            AgricultureRegistry.Crop crop = plugin.registry().crop(id.substring("produce_".length()));
            return crop == null ? null : produce(crop, amount);
        }
        AgricultureRegistry.Good good = plugin.registry().good(id);
        return good == null ? null : good(good, amount);
    }
    public static String money(double amount) {
        return String.format(Locale.UK, "£%,.2f", amount);
    }
    public static String formatTime(int seconds) {
        if (seconds < 60) return seconds + "s";
        int min = seconds / 60;
        return min + " min";
    }
}
