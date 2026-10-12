package net.aevummc.jobs.gui;

import net.aevummc.jobs.AevumJobs;
import net.aevummc.jobs.achievement.AchievementService;
import net.aevummc.jobs.config.ConfigManager;
import net.aevummc.jobs.database.Database;
import net.aevummc.jobs.job.JobData;
import net.aevummc.jobs.job.JobType;
import net.aevummc.jobs.reward.RewardService;
import net.aevummc.jobs.service.JobService;
import net.aevummc.jobs.service.PlayerJobs;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class GuiManager {
    public enum Menu { MAIN, SELECT, MANAGE, CONFIRM, PROGRESSION, REWARDS, STATS, LEADERBOARD, ACHIEVEMENTS }
    public static final int[] JOB_SLOTS = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34};
    /** A spacious four-column gallery used only by the flagship hub. */
    public static final int[] MAIN_JOB_SLOTS = {19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43};
    public static final int[] STAT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23};
    public static final int[] ACHIEVEMENT_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    /** Four tidy rows of seven reward cards, leaving the frame and controls untouched. */
    public static final int[] REWARD_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};

    private final AevumJobs plugin;
    private final ConfigManager config;
    private final JobService jobs;
    private final RewardService rewards;

    public GuiManager(AevumJobs plugin, ConfigManager config, JobService jobs, RewardService rewards) {
        this.plugin = plugin;
        this.config = config;
        this.jobs = jobs;
        this.rewards = rewards;
    }

    /** Main hub: profession gallery, player overview and consistent utility footer. */
    public void openMain(Player p, PlayerJobs d) {
        jobs.refreshAchievements(d, p, false);
        Inventory inv = Bukkit.createInventory(new Holder(Menu.MAIN, null, null),
                54, title("gui.main-title", "§8⚜ §6Aevum Imperium §8• §fJobs"));
        decorate(inv);
        inv.setItem(4, playerHead(p, "§6§l" + p.getName(),
                List.of("§7THE IMPERIAL REGISTER", GuiTheme.divider(),
                        "§7Active professions: §f" + d.activeCount() + "§7 / §f" + config.activeLimit(),
                        "§7Combined job levels: §e" + combinedLevels(d),
                        "§7Lifetime earnings: §a" + money(totalEarned(d)),
                        "", "§eYour record across Aevum.")));
        addGalleryGutters(inv);
        addJobCards(inv, d, MAIN_JOB_SLOTS);
        addCareerRibbon(inv, d);
        addActive(inv, d);
        inv.setItem(48, item(Material.NETHER_STAR, "§6§lAchievement Codex",
                List.of("§7Your permanent career honours.", "§7Unlocked: §e" + d.completedAchievements().size()
                                + " §8/ §f" + jobs.achievementService().all().size(),
                        "", "§eClick to inspect achievements.")));
        inv.setItem(49, item(Material.KNOWLEDGE_BOOK, "§6§lPersonal Records",
                List.of("§7Your XP, earnings and profession levels.", "", "§eClick to inspect your record.")));
        inv.setItem(50, item(Material.LECTERN, "§6§lMy Professions",
                List.of("§7Manage your active employment.", "§7Leave a profession or browse others.", "", "§eClick to manage.")));
        inv.setItem(51, item(Material.CHEST, "§6§lReward Chronicle",
                List.of("§7Your next reward in every profession.", "§7Inspect each profession's reward track.", "", "§eClick to view all rewards.")));
        inv.setItem(52, item(Material.GOLD_INGOT, "§6§lImperial Rankings",
                List.of("§7The leading specialists of Aevum.", "", "§eClick to view the rankings.")));
        inv.setItem(53, item(Material.BARRIER, "§c§lClose",
                List.of("§7Close the jobs interface.")));
        p.openInventory(inv);
    }

    public void openSelection(Player p, PlayerJobs d) {
        Inventory inv = Bukkit.createInventory(new Holder(Menu.SELECT, null, null), 54,
                title("gui.selection-title", "§8⚜ §6Choose a Profession"));
        decorate(inv);
        inv.setItem(4, item(Material.COMPASS, "§6§lChoose Your Path",
                List.of("§7Every craft contributes to the realm.", "§7Select a profession to inspect or join.", "",
                        "§7Active slots: §e" + d.activeCount() + "§8/§e" + config.activeLimit())));
        addJobCards(inv, d);
        inv.setItem(45, item(Material.IRON_INGOT, "§6§lEmployment Capacity",
                List.of("§7Occupied slots: §f" + d.activeCount(),
                        "§7Available slots: §f" + Math.max(0, config.activeLimit() - d.activeCount()),
                        "§7Maximum: §f" + config.activeLimit())));
        inv.setItem(49, item(Material.ARROW, "§e§lBack to the Hub",
                List.of("§7Return to the main jobs screen.")));
        inv.setItem(53, item(Material.BARRIER, "§c§lClose", List.of("§7Close the jobs interface.")));
        p.openInventory(inv);
    }

    public void openManage(Player p, PlayerJobs d) {
        Inventory inv = Bukkit.createInventory(new Holder(Menu.MANAGE, null, null), 54,
                title("gui.manage-title", "§8⚜ §6My Professions"));
        decorate(inv);
        inv.setItem(4, playerHead(p, "§6§lEmployment Register",
                List.of("§7Your currently active professions.", GuiTheme.divider(),
                        "§7Slots used: §e" + d.activeCount() + "§8/§e" + config.activeLimit(),
                        config.preserveOnLeave() ? "§7Progress is preserved when leaving." : "§cProgress resets when leaving, by server configuration.")));

        int[] slots = {20, 21, 22};
        int i = 0;
        for (JobType j : d.active()) {
            if (i >= slots.length) break;
            JobData jd = d.get(j);
            inv.setItem(slots[i++], item(j.getMaterial(), GuiTheme.professionTitle(j),
                    List.of("§7" + j.getDescription(), "", "§7Status: §aACTIVE",
                            "§7Level: §e" + jd.level() + " §8/ §f50",
                            "§7XP: §f" + number(Math.round(jd.xp())) + " §8/ §f" + reqText(jd.level()),
                            GuiTheme.progressBar(j, progress(jd), 10) + " §e" + progressPct(jd),
                            "", "§eLeft-click §7to view this profession.",
                            "§cRight-click §7to leave safely.")));
        }
        if (d.active().isEmpty()) {
            inv.setItem(22, item(Material.BOOK, "§e§lYour story starts here",
                    List.of("§7You have no active professions yet.", "§7Progress is saved as you play.", "",
                            "§eClick here or browse professions to begin.")));
        } else if (i < slots.length) {
            for (int n = i; n < slots.length; n++) {
                inv.setItem(slots[n], item(Material.GRAY_STAINED_GLASS_PANE, "§8§lAvailable Slot",
                        List.of("§7You can activate another profession.", "", "§eClick to browse professions.")));
            }
        }

        inv.setItem(48, item(Material.COMPASS, "§e§lBrowse Professions",
                List.of("§7Explore the full profession gallery.", "", "§eClick to browse.")));
        inv.setItem(49, item(Material.ARROW, "§e§lBack to the Hub", List.of("§7Return to the main jobs screen.")));
        inv.setItem(53, item(Material.BARRIER, "§c§lClose", List.of("§7Close the jobs interface.")));
        p.openInventory(inv);
    }

    public void openConfirm(Player p, PlayerJobs d, JobType j) {
        if (j == null || !d.isActive(j)) {
            openManage(p, d);
            return;
        }
        JobData jd = d.get(j);
        Inventory inv = Bukkit.createInventory(new Holder(Menu.CONFIRM, j, null), 27,
                "§8Leave §c" + j.getDisplayName() + "§8?");
        decorate(inv);
        inv.setItem(4, item(j.getMaterial(), "§c§lLeave " + j.getDisplayName(),
                List.of("§7You are about to leave this profession.", "", config.preserveOnLeave() ? "§aProgression will be preserved." : "§cProgression will reset by server configuration.",
                        "§7Level: §e" + jd.level(), "§7XP: §f" + number(Math.round(jd.xp())),
                        "§7Lifetime earnings: §a" + money(jd.totalEarned()))));
        inv.setItem(11, item(Material.RED_CONCRETE, "§c§lConfirm Departure",
                List.of("§7Remove this profession from your active slots.", config.preserveOnLeave() ? "§7Saved levels and earnings remain." : "§cSaved progression will be reset.", "",
                        "§cThis action changes active employment only.")));
        inv.setItem(13, item(Material.GRAY_CONCRETE, "§7§lKeep Profession",
                List.of("§7Return without making changes.")));
        inv.setItem(15, item(Material.LIME_CONCRETE, "§a§lLeave Profession",
                List.of("§7Confirm leaving " + j.getDisplayName() + ".", "", "§eClick to confirm.")));
        p.openInventory(inv);
    }

    public void openProgression(Player p, PlayerJobs d, JobType j) {
        if (j == null) return;
        JobData jd = d.get(j);
        Inventory inv = Bukkit.createInventory(new Holder(Menu.PROGRESSION, j, null), 54,
                title("gui.progress-title", "§8⚜ §6Profession Dossier"));
        decorate(inv);

        inv.setItem(4, item(jd.level() >= 50 ? Material.NETHER_STAR : j.getMaterial(), GuiTheme.professionTitle(j),
                List.of("§8IMPERIAL DISCIPLINE FILE", "§7" + j.getDescription(), GuiTheme.divider(),
                        "§7Employment: " + (d.isActive(j) ? "§aACTIVE" : "§8INACTIVE"),
                        "§7Lifetime earnings: §a" + money(jd.totalEarned()),
                        "§7Actions completed: §f" + number(jd.actions()),
                        jd.level() >= 50 ? "§6✦ MASTERY CONFIRMED" : "§7Mastery threshold: §eLevel 50"), jd.level() >= 50));

        int[] milestoneSlots = {11, 15, 20, 24, 29, 33};
        int[] levels = {1, 10, 20, 30, 40, 50};
        int milestonesReached = 0;
        for (int i = 0; i < levels.length; i++) {
            int level = levels[i];
            boolean completed = jd.level() > level || (level == 50 && jd.level() == 50);
            boolean current = jd.level() == level && level != 50;
            if (completed || current) milestonesReached++;
            String status = completed ? "§a✓ MILESTONE REACHED" : current ? "§e★ CURRENT LEVEL" : "§8LOCKED • FUTURE HONOUR";
            Material icon = completed ? Material.GOLD_BLOCK : current ? Material.GOLD_INGOT : Material.IRON_BLOCK;
            List<String> lore = new ArrayList<>();
            lore.add(status);
            lore.add(GuiTheme.divider());
            lore.add("§7Level reward: §a" + money(config.cashReward(level)));
            if (level == 50) lore.add("§7Masterwork: §e" + masterName(j));
            else lore.add("§7Milestone level: §f" + level);
            lore.add("");
            lore.add(completed ? "§aRecorded in your imperial career." : current ? "§eYou are standing at this milestone." : "§7Continue earning XP to reach it.");
            inv.setItem(milestoneSlots[i], item(icon, (level == 50 ? "§6§l✦ " : "§6§l") + "Level " + level, lore, level == 50 && completed));
        }

        String nextMilestone = jd.level() >= 50 ? "MASTERY COMPLETE" : "LEVEL " + java.util.Arrays.stream(levels).filter(level -> level > jd.level()).findFirst().orElse(50);
        inv.setItem(13, item(Material.BOOK, GuiTheme.accent(j) + "§lTHE PATH TO MASTERY",
                List.of("§8PROGRESSION REGISTER", GuiTheme.divider(),
                        "§7Milestones secured §f" + milestonesReached + " §8/ §f" + levels.length,
                        "§7Next honour §e" + nextMilestone,
                        "§7Milestone path §f1 · 10 · 20 · 30 · 40 · 50",
                        "", jd.level() >= 50 ? "§6✦ Your discipline is fully mastered." : "§7Every milestone carries its configured cash reward.")));

        if (jd.level() >= 50) {
            inv.setItem(22, item(Material.NETHER_STAR, "§6§lMASTER OF THE DISCIPLINE",
                    List.of("§6✦ LEVEL 50 ACHIEVED", GuiTheme.divider(),
                            "§7Masterwork §e" + masterName(j),
                            jd.level50Rewarded() ? "§aMasterwork already claimed" : "§eMasterwork still available to claim",
                            "", "§7Your progression remains recorded permanently."), true));
        } else {
            double needed = Math.max(0.0, config.requiredXp(jd.level()) - jd.xp());
            int nextLevel = Math.min(50, jd.level() + 1);
            inv.setItem(22, item(Material.COMPASS, "§e§lNEXT IMPERIAL OBJECTIVE",
                    List.of("§7Next level §f" + nextLevel,
                            "§7Experience needed §e" + number(Math.round(needed)) + " XP",
                            GuiTheme.progressBar(j, progress(jd), 10) + " §e" + progressPct(jd),
                            "§7Next payout §a" + money(config.cashReward(nextLevel)),
                            "", "§7Progress is based on your actual saved XP.")));
        }

        double ratio = progress(jd);
        inv.setItem(31, item(jd.level() >= 50 ? Material.NETHER_STAR : Material.EXPERIENCE_BOTTLE,
                jd.level() >= 50 ? "§6§lPROFESSION MASTERED" : "§6§lPROGRESSION",
                List.of("§7Current level: §e" + jd.level() + " §8/ §f50",
                        "§7Current XP: §f" + number(Math.round(jd.xp())),
                        "§7Next level: §f" + reqText(jd.level()),
                        GuiTheme.progressBar(j, ratio, 18) + " §e" + progressPct(jd),
                        "", "§7Total XP earned: §f" + number(jd.totalXp()),
                        "§7Actions completed: §f" + number(jd.actions()),
                        "§7Lifetime earnings: §a" + money(jd.totalEarned()))));

        inv.setItem(39, item(Material.WRITABLE_BOOK, "§b§lFIELD RECORD",
                List.of("§8CAREER STATISTICS", GuiTheme.divider(),
                        "§7Lifetime XP §f" + number(jd.totalXp()),
                        "§7Eligible actions §f" + number(jd.actions()),
                        "§7Lifetime earnings §a" + money(jd.totalEarned()),
                        "", "§eClick to open your full career record.")));
        inv.setItem(41, item(Material.CHEST, "§6§lReward Chronicle",
                List.of("§7Review all 50 level rewards.", "§7See what awaits further progress.", "",
                        "§eClick to open rewards.")));
        if (d.isActive(j)) {
            inv.setItem(43, item(Material.RED_CONCRETE, "§c§lLeave Profession",
                    List.of("§7Leave this active profession.", config.preserveOnLeave() ? "§7Saved progression will remain." : "§cSaved progression will be reset.", "",
                            "§eClick to review departure.")));
        } else {
            boolean full = d.activeCount() >= config.activeLimit();
            inv.setItem(43, item(full ? Material.BARRIER : Material.LIME_CONCRETE,
                    full ? "§c§lEmployment Full" : "§a§lJoin Profession",
                    List.of(full ? "§cNo active slots remain." : "§7Add " + j.getDisplayName() + " to your active professions.",
                            "§7Slots used: §e" + d.activeCount() + "§8/§e" + config.activeLimit(),
                            "", full ? "§7Leave a profession before joining." : "§eClick to activate.")));
        }
        inv.setItem(49, item(Material.ARROW, "§e§lBack to the Hub", List.of("§7Return to the main jobs screen.")));
        inv.setItem(53, item(Material.BARRIER, "§c§lClose", List.of("§7Close the jobs interface.")));
        p.openInventory(inv);
    }

    /** Overview of each profession's next real reward milestone. */
    public void openRewardOverview(Player p, PlayerJobs d) {
        Holder holder = new Holder(Menu.REWARDS, null, null, 0);
        Inventory inv = Bukkit.createInventory(holder, 54,
                title("gui.rewards-title", "§8⚜ §6Reward Chronicle"));
        decorate(inv);
        inv.setItem(4, playerHead(p, "§6§lImperial Reward Chronicle",
                List.of("§7One record for every profession.", GuiTheme.divider(),
                        "§7Cash pays on level-up when Vault economy is installed.",
                        "§7Level 50 unlocks a unique profession masterwork.",
                        "", "§eSelect a profession to inspect all 50 levels.")));

        for (int i = 0; i < JobType.values().length && i < JOB_SLOTS.length; i++) {
            JobType job = JobType.values()[i];
            JobData jd = d.get(job);
            boolean mastered = jd.level() >= 50;
            int nextLevel = Math.min(50, jd.level() + 1);
            List<String> lore = new ArrayList<>();
            lore.add(d.isActive(job) ? "§a● ACTIVE PROFESSION" : "§8○ NOT ACTIVE");
            lore.add(GuiTheme.divider());
            lore.add("§7Current level: §e" + jd.level() + " §8/ §f50");
            if (mastered) {
                lore.add(jd.level50Rewarded() ? "§a✓ MASTERWORK CLAIMED" : "§e✦ MASTERWORK AVAILABLE");
                lore.add("§7Masterwork: §e" + masterName(job));
            } else {
                lore.add("§7Next milestone: §eLevel " + nextLevel);
                lore.add("§7Cash reward: §a" + money(config.cashReward(nextLevel)));
                lore.add(GuiTheme.progressBar(job, progress(jd), 8) + " §e" + progressPct(jd));
            }
            lore.add("");
            lore.add("§eClick to open this reward track.");
            inv.setItem(JOB_SLOTS[i], item(mastered ? Material.NETHER_STAR : job.getMaterial(),
                    GuiTheme.professionTitle(job), lore));
        }
        inv.setItem(49, item(Material.ARROW, "§e§lBack to the Hub", List.of("§7Return to the main jobs screen.")));
        inv.setItem(53, item(Material.BARRIER, "§c§lClose", List.of("§7Close the jobs interface.")));
        p.openInventory(inv);
    }

    public void openRewards(Player p, PlayerJobs d, JobType j) {
        openRewards(p, d, j, 0);
    }

    public void openRewards(Player p, PlayerJobs d, JobType j, int page) {
        if (j == null) return;
        int safePage = Math.max(0, Math.min(1, page));
        JobData jd = d.get(j);
        int start = safePage * REWARD_SLOTS.length + 1;
        int end = Math.min(50, start + REWARD_SLOTS.length - 1);
        String menuTitle = title("gui.rewards-title", "§8⚜ §6Reward Chronicle");
        Inventory inv = Bukkit.createInventory(new Holder(Menu.REWARDS, j, null, safePage), 54,
                menuTitle + " §8• §f" + start + "–" + end);
        decorate(inv);

        for (int level = start; level <= end; level++) {
            int slotIndex = level - start;
            // Page two contains 22 entries: centre its final card rather than leaving it stranded at the left edge.
            int slot = safePage == 1 && slotIndex == 21 ? 40 : REWARD_SLOTS[slotIndex];
            boolean completed = jd.level() > level || (level == 50 && jd.level() == 50);
            boolean current = jd.level() == level && level != 50;
            String state = completed ? "§a✓ LEVEL REACHED" : current ? "§e★ CURRENT LEVEL" : "§8🔒 LOCKED";
            Material material = level == 50 ? Material.NETHER_STAR
                    : completed ? Material.GOLD_INGOT : Material.GOLD_NUGGET;
            List<String> lore = new ArrayList<>();
            lore.add(state);
            lore.add(GuiTheme.divider());
            lore.add("§7Cash reward: §a" + money(config.cashReward(level)));
            lore.add(level == 50 ? "§7Master reward: §e" + masterName(j) : "§7Paid on level-up with Vault economy");
            if (!completed && !current && jd.level() < level) lore.add("§7Reach level §e" + level + " §7to progress.");
            inv.setItem(slot, item(material, (level == 50 ? "§6§l✦ " : "§6§l") + "Level " + level, lore));
        }

        if (safePage > 0) inv.setItem(48, item(Material.ARROW, "§e§lPrevious Page", List.of("§7Return to levels 1–28.")));
        if (end < 50) inv.setItem(50, item(Material.ARROW, "§e§lNext Page", List.of("§7Continue to levels 29–50.")));
        if (jd.level() >= 50 && !jd.level50Rewarded()) {
            inv.setItem(49, item(Material.NETHER_STAR, "§6§l✦ CLAIM MASTERWORK",
                    List.of("§6YOUR FINAL PROFESSION HONOUR", GuiTheme.divider(),
                            "§7Your level 50 reward is ready.", "§7Requires enough free inventory space.", "",
                            "§eClick once to claim your reward."), true));
        } else {
            inv.setItem(49, item(jd.level50Rewarded() ? Material.EMERALD_BLOCK : Material.GOLD_BLOCK,
                    jd.level50Rewarded() ? "§a§lMASTERWORK CLAIMED" : "§6§lMASTERWORK REWARD",
                    List.of(jd.level50Rewarded() ? "§a✓ Already claimed" : "§8Reach level 50 to unlock.",
                            "§7Reward: §e" + masterName(j)), jd.level50Rewarded()));
        }
        inv.setItem(53, item(Material.ARROW, "§e§lBack to Profession", List.of("§7Return to the profession dossier.")));
        p.openInventory(inv);
    }

    public void openStats(Player p, PlayerJobs d) {
        Inventory inv = Bukkit.createInventory(new Holder(Menu.STATS, null, null), 54,
                title("gui.stats-title", "§8⚜ §6Personal Records"));
        decorate(inv);
        inv.setItem(4, playerHead(p, "§6§l" + p.getName() + " — Personal Record",
                List.of("§7THE IMPERIAL ARCHIVE", GuiTheme.divider(),
                        "§7Every statistic below comes from your saved job data.",
                        "§8Need command help? Use §f/jobs help§8.")));

        int[] slots = STAT_SLOTS;
        JobType[] all = JobType.values();
        for (int i = 0; i < all.length && i < slots.length; i++) {
            JobType j = all[i];
            JobData jd = d.get(j);
            inv.setItem(slots[i], item(j.getMaterial(), GuiTheme.professionTitle(j),
                    List.of(d.isActive(j) ? "§a● ACTIVE" : "§8○ INACTIVE",
                            "§7Level: §e" + jd.level() + " §8/ §f50",
                            "§7XP: §f" + number(Math.round(jd.xp())) + " §8/ §f" + reqText(jd.level()),
                            GuiTheme.progressBar(j, progress(jd), 8) + " §e" + progressPct(jd),
                            "§7Lifetime XP: §f" + number(jd.totalXp()),
                            "§7Actions: §f" + number(jd.actions()),
                            "§7Earned: §a" + money(jd.totalEarned()),
                            "", "§eClick to inspect progression.")));
        }

        inv.setItem(31, item(Material.EXPERIENCE_BOTTLE, "§6§lCareer Summary",
                List.of(GuiTheme.divider(),
                        "§7Active professions: §e" + d.activeCount() + "§8/§e" + config.activeLimit(),
                        "§7Combined job levels: §e" + combinedLevels(d),
                        "§7Highest profession level: §e" + highestLevel(d),
                        "§7Total job XP: §f" + number(totalXp(d)),
                        "§7Total actions: §f" + number(totalActions(d)),
                        "§7Lifetime earnings: §a" + money(totalEarned(d)))));
        inv.setItem(49, item(Material.ARROW, "§e§lBack to the Hub", List.of("§7Return to the main jobs screen.")));
        inv.setItem(53, item(Material.BARRIER, "§c§lClose", List.of("§7Close the jobs interface.")));
        p.openInventory(inv);
    }

    /** Paginated, view-only achievements. Completion is permanent and persisted by the database. */
    public void openAchievements(Player p, PlayerJobs d, int page) {
        jobs.refreshAchievements(d, p, false);
        AchievementService service = jobs.achievementService();
        List<AchievementService.Achievement> all = service.all();
        int pages = Math.max(1, (all.size() + ACHIEVEMENT_SLOTS.length - 1) / ACHIEVEMENT_SLOTS.length);
        int safePage = Math.max(0, Math.min(pages - 1, page));
        int start = safePage * ACHIEVEMENT_SLOTS.length;
        int end = Math.min(all.size(), start + ACHIEVEMENT_SLOTS.length);
        Inventory inv = Bukkit.createInventory(new Holder(Menu.ACHIEVEMENTS, null, null, safePage), 54,
                title("gui.achievements-title", "§8⚜ §6Achievement Codex") + " §8• §f" + (safePage + 1) + "/" + pages);
        decorate(inv);
        inv.setItem(4, playerHead(p, "§6§lThe Imperial Achievement Codex",
                List.of("§7A permanent record of your service to Aevum.", GuiTheme.divider(),
                        "§7Unlocked: §e" + d.completedAchievements().size() + " §8/ §f" + all.size(),
                        "§7Achievement completions are prestige-only.",
                        "§7Progress is calculated from saved job statistics.")));

        if (!service.enabled()) {
            inv.setItem(22, item(Material.BARRIER, "§c§lAchievements Disabled",
                    List.of("§7The server owner has disabled achievements.", "§7Ask an administrator for details.")));
        } else if (all.isEmpty()) {
            inv.setItem(22, item(Material.BOOK, "§e§lNo Achievements Configured",
                    List.of("§7No achievement milestones are currently configured.")));
        } else {
            java.util.Set<String> completed = d.completedAchievements();
            for (int index = start; index < end; index++) {
                AchievementService.Achievement achievement = all.get(index);
                boolean unlocked = completed.contains(achievement.id());
                long current = service.progress(d, achievement);
                long displayed = unlocked ? achievement.target() : current;
                double ratio = achievement.target() <= 0 ? 0.0
                        : Math.max(0.0, Math.min(1.0, (double) displayed / (double) achievement.target()));
                Material material;
                if (unlocked) material = Material.GOLD_BLOCK;
                else if (achievement.job() != null) material = achievement.job().getMaterial();
                else material = switch (achievement.metric()) {
                    case ACTIVE_PROFESSIONS -> Material.LECTERN;
                    case TOTAL_ACTIONS -> Material.ANVIL;
                    case TOTAL_EARNINGS, JOB_EARNINGS -> Material.GOLD_INGOT;
                    case JOB_LEVEL -> Material.EXPERIENCE_BOTTLE;
                    case JOB_ACTIONS -> Material.WRITABLE_BOOK;
                };
                String status = unlocked ? "§a✓ UNLOCKED" : current > 0 ? "§e◈ IN PROGRESS" : "§8🔒 NOT YET EARNED";
                String currentValue = achievement.metric() == AchievementService.Metric.JOB_EARNINGS
                        || achievement.metric() == AchievementService.Metric.TOTAL_EARNINGS
                        ? money(displayed) : number(displayed);
                String targetValue = achievement.metric() == AchievementService.Metric.JOB_EARNINGS
                        || achievement.metric() == AchievementService.Metric.TOTAL_EARNINGS
                        ? money(achievement.target()) : number(achievement.target());
                List<String> lore = new ArrayList<>();
                lore.add("§7" + achievement.category());
                lore.add(GuiTheme.divider());
                lore.add("§7" + achievement.description());
                lore.add("");
                lore.add(status);
                lore.add("§7Progress: §f" + currentValue + " §8/ §f" + targetValue);
                lore.add(GuiTheme.progressBar(ratio, 9) + " §e" + GuiTheme.percent(ratio));
                lore.add("");
                lore.add(unlocked ? "§aRecorded in your imperial archive." : "§7Keep working to earn this honour.");
                inv.setItem(ACHIEVEMENT_SLOTS[index - start],
                        item(material, (unlocked ? "§6§l✦ " : "§f§l") + achievement.title(), lore, unlocked));
            }
        }
        if (safePage > 0) inv.setItem(48, item(Material.ARROW, "§e§lPrevious Page", List.of("§7Browse previous honours.")));
        inv.setItem(49, item(Material.ARROW, "§e§lBack to the Hub", List.of("§7Return to Aevum Imperium.")));
        if (safePage + 1 < pages) inv.setItem(50, item(Material.ARROW, "§e§lNext Page", List.of("§7Browse more honours.")));
        inv.setItem(53, item(Material.BARRIER, "§c§lClose", List.of("§7Close the jobs interface.")));
        p.openInventory(inv);
    }

    public void openLeaderboard(Player p, PlayerJobs d) {
        Holder holder = new Holder(Menu.LEADERBOARD, null, null);
        Inventory inv = Bukkit.createInventory(holder, 54,
                title("gui.leaderboard-title", "§8⚜ §6Imperial Rankings"));
        decorate(inv);
        inv.setItem(4, item(Material.GOLDEN_HELMET, "§6§lHall of Specialists",
                List.of("§7The leading six players in each profession.", "§7Rankings use persisted profession XP.", "",
                        "§eResults load without blocking the server thread.")));
        p.openInventory(inv);

        for (int i = 0; i < JobType.values().length; i++) {
            JobType job = JobType.values()[i];
            int slot = JOB_SLOTS[i];
            inv.setItem(slot, item(job.getMaterial(), "§6§l" + job.getDisplayName(),
                    List.of("§7Loading the imperial register...")));
            jobs.leaderboard(job, 6).thenAccept(list -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (!p.isOnline() || p.getOpenInventory().getTopInventory().getHolder() != holder) return;
                List<String> lore = new ArrayList<>();
                lore.add("§7TOP SPECIALISTS");
                lore.add(GuiTheme.divider());
                int rank = 1;
                if (list.isEmpty()) lore.add("§8No ranked players yet.");
                for (Database.LeaderboardEntry entry : list) {
                    Player online = Bukkit.getPlayer(entry.uuid());
                    String name = online != null ? online.getName()
                            : Optional.ofNullable(Bukkit.getOfflinePlayer(entry.uuid()).getName())
                                    .orElse(entry.uuid().toString().substring(0, 8));
                    String medal = switch (rank) {
                        case 1 -> "§e✦";
                        case 2 -> "§f◆";
                        case 3 -> "§6◆";
                        default -> "§8" + rank + ".";
                    };
                    lore.add(medal + " §f" + name);
                    lore.add("  §7Level §e" + entry.level() + " §8• §f" + number(entry.totalXp()) + " XP");
                    rank++;
                }
                if (list.stream().noneMatch(e -> e.uuid().equals(p.getUniqueId()))) {
                    lore.add("");
                    lore.add("§8Your position is not in this top six.");
                }
                inv.setItem(slot, item(job.getMaterial(), "§6§l" + job.getDisplayName(), lore));
            }));
        }
        inv.setItem(53, item(Material.ARROW, "§e§lBack to the Hub", List.of("§7Return to the main jobs screen.")));
    }

    /** Classic compact gallery remains available on the selection screen. */
    private void addJobCards(Inventory inv, PlayerJobs d) {
        addJobCards(inv, d, JOB_SLOTS);
    }

    /** Distinctive profession cards with a unique accent, live progression and state. */
    private void addJobCards(Inventory inv, PlayerJobs d, int[] slots) {
        JobType[] all = JobType.values();
        for (int i = 0; i < all.length && i < slots.length; i++) {
            JobType job = all[i];
            JobData data = d.get(job);
            boolean active = d.isActive(job);
            boolean mastered = data.level() >= 50;
            Material icon = mastered ? Material.NETHER_STAR : job.getMaterial();

            List<String> lore = new ArrayList<>();
            lore.add("§8THE IMPERIAL GUILD");
            lore.add("§7" + job.getDescription());
            lore.add(GuiTheme.divider());
            lore.add(active ? "§a● CURRENTLY EMPLOYED" : mastered ? "§6✦ PROFESSION MASTERED" : "§8○ AVAILABLE DISCIPLINE");
            lore.add("§7PROFESSION LEVEL §f" + data.level() + " §8/ §f50");
            if (mastered) {
                lore.add("§6✦ Masterwork threshold achieved");
                lore.add("§7Masterwork: §e" + masterName(job));
                lore.add(GuiTheme.progressBar(job, 1.0, 8) + " §6100%");
            } else {
                lore.add("§7XP: §f" + number(Math.round(data.xp())) + " §8/ §f" + reqText(data.level()));
                lore.add(GuiTheme.progressBar(job, progress(data), 8) + " §e" + progressPct(data));
                lore.add("§7Next level: §e" + (data.level() + 1) + " §8• §a" + money(config.cashReward(data.level() + 1)));
            }
            lore.add("§7Lifetime earnings: §a" + money(data.totalEarned()));
            lore.add("§7Recorded actions: §f" + number(data.actions()));
            lore.add("");
            lore.add(active ? "§e▶ Inspect your active dossier" : "§e▶ Inspect discipline");
            lore.add(active || mastered ? "§8Imperial record verified" : "§8Progress is tracked from real actions");

            String display = (active ? "§a● " : mastered ? "§6✦ " : "") + GuiTheme.professionTitle(job);
            inv.setItem(slots[i], item(icon, display, lore, active || mastered));
        }
    }

    /** Gutter panes separate the profession cards like a deliberately composed gallery. */
    private void addGalleryGutters(Inventory inv) {
        int[] gutters = {20, 22, 24, 29, 31, 33, 38, 40, 42};
        for (int slot : gutters) {
            int row = slot / 9;
            Material material = (slot % 9 == 4 || row == 2 && slot == 22 || row == 3 && slot == 31 || row == 4 && slot == 40)
                    ? Material.BROWN_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE;
            inv.setItem(slot, item(material, "§0", List.of()));
        }
    }

    /** The career dashboard is the visual centrepiece of the jobs hub. */
    private void addCareerRibbon(Inventory inv, PlayerJobs d) {
        int mastered = 0;
        for (JobType job : JobType.values()) if (d.get(job).level() >= 50) mastered++;

        inv.setItem(10, item(Material.LECTERN, "§6§lEMPLOYMENT",
                List.of("§7Active professions §f" + d.activeCount() + " §8/ §f" + config.activeLimit(),
                        "§7Available slots §a" + Math.max(0, config.activeLimit() - d.activeCount()),
                        "", "§eClick to manage employment.")));
        inv.setItem(11, item(Material.EXPERIENCE_BOTTLE, "§b§lCAREER LEVELS",
                List.of("§7Combined levels §f" + combinedLevels(d) + " §8/ §f600",
                        "§7Every profession contributes to this record.", "",
                        "§eClick to inspect Personal Records.")));
        inv.setItem(12, item(Material.AMETHYST_SHARD, "§d§lCAREER EXPERIENCE",
                List.of("§7Total profession XP §f" + number(totalXp(d)),
                        "§7Experience accumulated across your disciplines.", "",
                        "§eClick to inspect Personal Records.")));
        inv.setItem(13, item(Material.NETHER_STAR, "§6§lTHE IMPERIUM",
                List.of("§8A E V U M   •   CAREER DOSSIER", GuiTheme.divider(),
                        "§7Twelve disciplines. One lasting legacy.",
                        "§7Choose a profession below to open its dossier.",
                        "§7Master a craft. Build your name.", "",
                        "§eFORGE YOUR LEGACY."), true));
        inv.setItem(14, item(Material.GOLD_INGOT, "§6§lIMPERIAL TREASURY",
                List.of("§7Lifetime earnings §a" + money(totalEarned(d)),
                        "§7Actual job payouts recorded by AevumJobs.", "",
                        "§eClick to inspect Personal Records.")));
        inv.setItem(15, item(Material.DIAMOND, "§b§lMASTERED DISCIPLINES",
                List.of("§7Professions mastered §f" + mastered + " §8/ §f" + JobType.values().length,
                        mastered == JobType.values().length ? "§6✦ Every discipline mastered" : "§7Reach level 50 to master a profession.", "",
                        "§eClick to inspect your career."), mastered > 0));
        inv.setItem(16, item(Material.WRITABLE_BOOK, "§d§lHONOURS",
                List.of("§7Achievements unlocked §f" + d.completedAchievements().size() + " §8/ §f" + jobs.achievementService().all().size(),
                        "§7Permanent honours earned through real career progress.", "",
                        "§eClick to open the Achievement Codex."), !d.completedAchievements().isEmpty()));
    }

    private void addActive(Inventory inv, PlayerJobs d) {
        int[] slots = {45, 46, 47};
        int i = 0;
        for (JobType j : d.active()) {
            if (i >= slots.length) break;
            JobData jd = d.get(j);
            inv.setItem(slots[i++], item(j.getMaterial(), "§a● " + GuiTheme.professionTitle(j),
                    List.of("§aACTIVE EMPLOYMENT", "§7Level: §e" + jd.level() + " §8/ §f50",
                            GuiTheme.progressBar(j, progress(jd), 8) + " §e" + progressPct(jd),
                            "", "§eClick to open this profession dossier."), true));
        }
        while (i < slots.length) {
            inv.setItem(slots[i++], item(Material.GRAY_STAINED_GLASS_PANE, "§8§lEmpty Employment Slot",
                    List.of("§7There is room for another active profession.", "", "§eClick to browse professions.")));
        }
    }

    /** Draw a restrained obsidian frame; content slots remain open and readable. */
    private void decorate(Inventory inv) {
        int size = inv.getSize();
        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            boolean horizontal = row == 0 || row == (size / 9) - 1;
            boolean vertical = col == 0 || col == 8;
            if (!horizontal && !vertical) continue;

            // Blackstone-like edges with occasional bronze inlays create an imperial frame
            // without stealing space from actual content or interactive controls.
            Material pane = Material.BLACK_STAINED_GLASS_PANE;
            if (horizontal && (col == 2 || col == 6)) pane = Material.BROWN_STAINED_GLASS_PANE;
            if (vertical && (row == 2 || row == 4)) pane = Material.BROWN_STAINED_GLASS_PANE;
            inv.setItem(slot, item(pane, "§0", List.of()));
        }
    }

    private ItemStack playerHead(Player p, String name, List<String> lore) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta raw = stack.getItemMeta();
        if (raw instanceof SkullMeta meta) {
            meta.setOwningPlayer(p);
            meta.setDisplayName(name);
            meta.setLore(new ArrayList<>(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        return item(material, name, lore, false);
    }

    /** Adds a controlled vanilla enchantment glint for meaningful high-status items. */
    private ItemStack item(Material material, String name, List<String> lore, boolean glint) {
        ItemStack stack = new ItemStack(material == null ? Material.PAPER : material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        // Intentionally retain legacy formatting codes: stripping them removes
        // the dark-gold theme and makes every item appear unformatted.
        meta.setDisplayName(name);
        meta.setLore(new ArrayList<>(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (glint) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private String title(String path, String fallback) {
        String configured = config.config().getString(path);
        if (configured == null || configured.isBlank()) return fallback;

        // Migrate the exact built-in titles from earlier AevumJobs defaults to
        // the new theme, while preserving any server owner's custom title.
        String legacyDefault = switch (path) {
            case "gui.main-title" -> "🏛 A E V U M  •  J O B S";
            case "gui.progress-title" -> "✦ Profession Progression";
            case "gui.selection-title" -> "Choose Your Profession";
            case "gui.manage-title" -> "Manage Your Professions";
            case "gui.rewards-title" -> "Profession Rewards";
            case "gui.stats-title" -> "AevumJobs Statistics";
            case "gui.leaderboard-title" -> "AevumJobs Leaderboards";
            default -> null;
        };
        return legacyDefault != null && legacyDefault.equals(configured) ? fallback : configured;
    }

    private String reqText(int level) {
        return level >= 50 ? "MAX" : number(Math.round(config.requiredXp(level))) + " XP";
    }

    private String money(double value) {
        return plugin.getEconomy().format(Math.max(0, value));
    }

    private String number(long value) {
        return GuiTheme.number(value);
    }

    private double progress(JobData data) {
        if (data.level() >= 50) return 1.0;
        double required = config.requiredXp(data.level());
        if (!Double.isFinite(required) || required <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, data.xp() / required));
    }

    private String progressPct(JobData data) {
        return data.level() >= 50 ? "100%" : GuiTheme.percent(progress(data));
    }

    private String masterName(JobType job) {
        return config.config().getString("master-rewards." + job.key() + ".name",
                "Master " + job.getDisplayName());
    }

    private long combinedLevels(PlayerJobs data) {
        return data.all().stream().mapToLong(JobData::level).sum();
    }

    private int highestLevel(PlayerJobs data) {
        return data.all().stream().mapToInt(JobData::level).max().orElse(0);
    }

    private long totalXp(PlayerJobs data) {
        return data.all().stream().mapToLong(JobData::totalXp).sum();
    }

    private long totalActions(PlayerJobs data) {
        return data.all().stream().mapToLong(JobData::actions).sum();
    }

    private double totalEarned(PlayerJobs data) {
        return data.all().stream().mapToDouble(JobData::totalEarned).sum();
    }

    public static final class Holder implements InventoryHolder {
        private final Menu menu;
        private final JobType job;
        private final UUID target;
        private final int page;

        public Holder(Menu menu, JobType job, UUID target) {
            this(menu, job, target, 0);
        }

        public Holder(Menu menu, JobType job, UUID target, int page) {
            this.menu = menu;
            this.job = job;
            this.target = target;
            this.page = page;
        }

        public Menu menu() { return menu; }
        public JobType job() { return job; }
        public UUID target() { return target; }
        public int page() { return page; }

        @Override
        public Inventory getInventory() { return null; }
    }
}