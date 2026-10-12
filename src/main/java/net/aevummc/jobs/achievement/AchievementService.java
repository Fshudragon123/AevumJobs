package net.aevummc.jobs.achievement;

import net.aevummc.jobs.config.ConfigManager;
import net.aevummc.jobs.job.JobData;
import net.aevummc.jobs.job.JobType;
import net.aevummc.jobs.service.PlayerJobs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/** Configurable, prestige-only achievements derived from AevumJobs' persisted statistics. */
public final class AchievementService {
    public enum Metric {
        ACTIVE_PROFESSIONS, JOB_LEVEL, JOB_ACTIONS, JOB_EARNINGS, TOTAL_ACTIONS, TOTAL_EARNINGS
    }

    public record Achievement(String id, String title, String category, String description,
                              JobType job, long target, Metric metric) {}

    private final ConfigManager config;
    private volatile List<Achievement> definitions = List.of();

    public AchievementService(ConfigManager config) {
        this.config = config;
        reload();
    }

    public synchronized void reload() {
        List<Achievement> out = new ArrayList<>();
        out.add(new Achievement("career.first_profession", "First Steps", "Career",
                "Activate your first profession.", null, 1, Metric.ACTIVE_PROFESSIONS));

        int activeLimit = Math.max(1, config.activeLimit());
        if (activeLimit > 1) {
            out.add(new Achievement("career.full_employment", "Fully Employed", "Career",
                    "Fill all " + activeLimit + " active profession slots at once.",
                    null, activeLimit, Metric.ACTIVE_PROFESSIONS));
        }

        addCareerMilestones(out, "achievements.total-action-milestones", List.of(1_000, 10_000),
                Metric.TOTAL_ACTIONS, "Career Actions", "Deeds of the Realm", "Imperial Work Ethic",
                "Complete %s XP-eligible profession actions across your career.");
        addCareerMilestones(out, "achievements.total-earnings-milestones", List.of(100_000, 1_000_000),
                Metric.TOTAL_EARNINGS, "Career Earnings", "A Growing Purse", "Imperial Treasury",
                "Receive %s in successful profession level-up payouts across your career.");

        List<Integer> levelTargets = thresholds("achievements.level-milestones", List.of(10, 25, 50), 50);
        List<Integer> actionTargets = thresholds("achievements.action-milestones",
                List.of(100, 1_000, 5_000), Integer.MAX_VALUE);
        List<Integer> earningTargets = thresholds("achievements.earnings-milestones",
                List.of(10_000, 50_000), Integer.MAX_VALUE);

        for (JobType job : JobType.values()) {
            for (int target : levelTargets) {
                String title = target >= 50 ? job.getDisplayName() + " — Master"
                        : target >= 25 ? job.getDisplayName() + " — Veteran"
                        : job.getDisplayName() + " — Professional";
                out.add(new Achievement("profession." + job.key() + ".level." + target, title,
                        "Profession Level", "Reach level " + target + " in " + job.getDisplayName() + ".",
                        job, target, Metric.JOB_LEVEL));
            }
            for (int target : actionTargets) {
                out.add(new Achievement("profession." + job.key() + ".actions." + target,
                        job.getDisplayName() + " — " + actionTitle(target), "Profession Actions",
                        "Complete " + target + " XP-eligible " + job.getDisplayName() + " actions.",
                        job, target, Metric.JOB_ACTIONS));
            }
            for (int target : earningTargets) {
                out.add(new Achievement("profession." + job.key() + ".earnings." + target,
                        job.getDisplayName() + " — " + (target >= 50_000 ? "Imperial Fortune" : "First Fortune"),
                        "Profession Earnings", "Receive " + target + " in successful "
                        + job.getDisplayName() + " level-up payouts.", job, target, Metric.JOB_EARNINGS));
            }
        }

        out.sort(Comparator.comparing(Achievement::category)
                .thenComparing(a -> a.job() == null ? "" : a.job().key())
                .thenComparingLong(Achievement::target)
                .thenComparing(Achievement::id));
        definitions = List.copyOf(out);
    }

    private void addCareerMilestones(List<Achievement> out, String path, List<Integer> defaults, Metric metric,
                                     String category, String earlyTitle, String laterTitle, String description) {
        for (int target : thresholds(path, defaults, Integer.MAX_VALUE)) {
            String title = target >= defaults.get(defaults.size() - 1) ? laterTitle : earlyTitle;
            String detail = description.formatted(String.format(java.util.Locale.US, "%,d", target));
            out.add(new Achievement("career." + (metric == Metric.TOTAL_ACTIONS ? "actions." : "earnings.") + target,
                    title, category, detail, null, target, metric));
        }
    }

    private List<Integer> thresholds(String path, List<Integer> defaults, int maximum) {
        List<Integer> configured = config.config().getIntegerList(path);
        if (configured.isEmpty()) configured = defaults;
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        configured.stream().filter(value -> value != null && value > 0 && value <= maximum)
                .sorted().forEach(unique::add);
        if (unique.isEmpty()) defaults.stream().filter(value -> value > 0 && value <= maximum).forEach(unique::add);
        return List.copyOf(unique);
    }

    private String actionTitle(int target) {
        if (target >= 5_000) return "Tireless Hands";
        if (target >= 1_000) return "Veteran Hands";
        return "Steady Hands";
    }

    public List<Achievement> all() { return definitions; }

    public boolean enabled() { return config.config().getBoolean("achievements.enabled", true); }

    public List<Achievement> refresh(PlayerJobs data) {
        if (!enabled()) return List.of();
        List<Achievement> newlyUnlocked = new ArrayList<>();
        for (Achievement achievement : definitions) {
            if (!data.hasAchievement(achievement.id())
                    && progress(data, achievement) >= achievement.target()
                    && data.completeAchievement(achievement.id())) {
                newlyUnlocked.add(achievement);
            }
        }
        return List.copyOf(newlyUnlocked);
    }

    public long progress(PlayerJobs data, Achievement achievement) {
        return switch (achievement.metric()) {
            case ACTIVE_PROFESSIONS -> data.activeCount();
            case JOB_LEVEL -> achievement.job() == null ? 0 : data.get(achievement.job()).level();
            case JOB_ACTIONS -> achievement.job() == null ? 0 : data.get(achievement.job()).actions();
            case JOB_EARNINGS -> achievement.job() == null ? 0 : safeFloor(data.get(achievement.job()).totalEarned());
            case TOTAL_ACTIONS -> data.all().stream().mapToLong(JobData::actions).sum();
            case TOTAL_EARNINGS -> safeFloor(data.all().stream().mapToDouble(JobData::totalEarned).sum());
        };
    }

    private long safeFloor(double value) {
        if (!Double.isFinite(value) || value <= 0) return 0;
        return value >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.floor(value);
    }
}
