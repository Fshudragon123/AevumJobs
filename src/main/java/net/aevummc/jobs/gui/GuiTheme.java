package net.aevummc.jobs.gui;

/**
 * Shared visual language for the Aevum Imperium inventory interfaces.
 * Uses legacy formatting codes and vanilla materials so no resource pack is required.
 */
public final class GuiTheme {
    private GuiTheme() {}

    public static final String OBSIDIAN = "§8";
    public static final String GOLD = "§6";
    public static final String BRIGHT_GOLD = "§e";
    public static final String MARBLE = "§f";
    public static final String MUTED = "§7";
    public static final String SUCCESS = "§a";
    public static final String WARNING = "§6";
    public static final String DANGER = "§c";
    public static final String LOCKED = "§8";

    public static String divider() {
        return "§8━━━━━━━━━━━━━━━━━━━━";
    }

    public static String progressBar(double progress, int width) {
        int safeWidth = Math.max(1, Math.min(40, width));
        double ratio = Double.isFinite(progress) ? Math.max(0.0, Math.min(1.0, progress)) : 0.0;
        int filled = (int) Math.round(ratio * safeWidth);
        return "§6" + "█".repeat(filled) + "§8" + "░".repeat(safeWidth - filled);
    }

    public static String percent(double progress) {
        double ratio = Double.isFinite(progress) ? Math.max(0.0, Math.min(1.0, progress)) : 0.0;
        return String.format(java.util.Locale.US, "%.0f%%", ratio * 100.0);
    }

    public static String number(long value) {
        return String.format(java.util.Locale.US, "%,d", value);
    }
}
