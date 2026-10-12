package net.aevummc.jobs.gui;

import net.aevummc.jobs.job.JobType;

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

    /**
     * Profession accents make the gallery memorable while shared typography and
     * obsidian framing retain Aevum's coherent imperial visual identity.
     */
    public static String accent(JobType job) {
        if (job == null) return GOLD;
        return switch (job) {
            case MINER -> "§e";
            case FARMER -> "§a";
            case LUMBERJACK -> "§2";
            case FISHERMAN -> "§b";
            case RANCHER -> "§6";
            case WARRIOR -> "§c";
            case HUNTER -> "§3";
            case BUILDER -> "§7";
            case ALCHEMIST -> "§5";
            case ENCHANTER -> "§d";
            case BLACKSMITH -> "§6";
            case SCHOLAR -> "§9";
        };
    }

    public static String professionTitle(JobType job) {
        if (job == null) return GOLD + "§lIMPERIAL PROFESSION";
        return accent(job) + "§l" + job.getDisplayName().toUpperCase(java.util.Locale.ROOT);
    }

    public static String progressBar(double progress, int width) {
        return progressBarColour(GOLD, progress, width);
    }

    public static String progressBar(JobType job, double progress, int width) {
        return progressBarColour(accent(job), progress, width);
    }

    private static String progressBarColour(String colour, double progress, int width) {
        int safeWidth = Math.max(1, Math.min(40, width));
        double ratio = Double.isFinite(progress) ? Math.max(0.0, Math.min(1.0, progress)) : 0.0;
        int filled = (int) Math.round(ratio * safeWidth);
        return colour + "█".repeat(filled) + "§8" + "░".repeat(safeWidth - filled);
    }

    public static String percent(double progress) {
        double ratio = Double.isFinite(progress) ? Math.max(0.0, Math.min(1.0, progress)) : 0.0;
        return String.format(java.util.Locale.US, "%.0f%%", ratio * 100.0);
    }

    public static String number(long value) {
        return String.format(java.util.Locale.US, "%,d", value);
    }
}
