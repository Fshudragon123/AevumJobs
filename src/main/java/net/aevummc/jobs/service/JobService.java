package net.aevummc.jobs.service;

import net.aevummc.jobs.AevumJobs;
import net.aevummc.jobs.antiabuse.AntiAbuseService;
import net.aevummc.jobs.achievement.AchievementService;
import net.aevummc.jobs.config.ConfigManager;
import net.aevummc.jobs.database.Database;
import net.aevummc.jobs.job.JobData;
import net.aevummc.jobs.job.JobType;
import net.aevummc.jobs.reward.RewardService;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;

public final class JobService {
    private final AevumJobs plugin;
    private final ConfigManager config;
    private final RewardService rewards;
    private final AntiAbuseService antiAbuse;
    private final AchievementService achievements;
    private final Map<UUID,PlayerJobs> cache=new ConcurrentHashMap<>();
    private final Map<JobType,CachedBoard> leaderboardCache=new ConcurrentHashMap<>();

    public JobService(AevumJobs plugin,ConfigManager config,RewardService rewards,AntiAbuseService antiAbuse,AchievementService achievements){this.plugin=plugin;this.config=config;this.rewards=rewards;this.antiAbuse=antiAbuse;this.achievements=achievements;}
    public CompletableFuture<PlayerJobs> load(UUID uuid){PlayerJobs cached=cache.get(uuid);if(cached!=null)return CompletableFuture.completedFuture(cached);return plugin.getDatabase().load(uuid).thenApply(d->{cache.put(uuid,d);return d;});}
    public PlayerJobs get(UUID uuid){return cache.get(uuid);}
    public void cache(PlayerJobs d){cache.put(d.uuid(),d);}
    public void unload(UUID uuid){PlayerJobs d=cache.remove(uuid);if(d!=null)save(d);}
    public void save(PlayerJobs d){plugin.getDatabase().save(d);}
    public boolean join(PlayerJobs d,JobType job){boolean ok=d.activate(job,config.activeLimit());if(ok){save(d);refreshAchievements(d,Bukkit.getPlayer(d.uuid()),true);}return ok;}
    public boolean leave(PlayerJobs d,JobType job){boolean ok=d.leave(job);if(ok&&!config.preserveOnLeave()){JobData x=d.get(job);x.setLevel(0);x.setXp(0);x.setTotalXp(0);x.setTotalEarned(0);x.setActions(0);x.setLevel50Rewarded(false);}if(ok){save(d);refreshAchievements(d,Bukkit.getPlayer(d.uuid()),true);}return ok;}

    public void addXp(Player player,JobType job,double amount){
        if(amount<=0)return;
        PlayerJobs d=cache.get(player.getUniqueId());if(d==null||!d.isActive(job))return;
        JobData jd=d.get(job);
        synchronized(jd){
            jd.addXp(amount);
            jd.addActions(1);
            while(jd.level()<50&&jd.xp()>=config.requiredXp(jd.level())){
                double req=config.requiredXp(jd.level());
                jd.setXp(jd.xp()-req);
                jd.setLevel(jd.level()+1);
                rewards.handleLevelReward(player,d,job,jd.level());
            }
        }
        save(d);
        refreshAchievements(d, player, true);
    }

    public List<AchievementService.Achievement> refreshAchievements(PlayerJobs data, Player player, boolean notify) {
        List<AchievementService.Achievement> unlocked = achievements.refresh(data);
        if (unlocked.isEmpty()) return unlocked;
        plugin.getDatabase().saveAchievements(data);
        if (notify && player != null && player.isOnline()
                && config.config().getBoolean("achievements.notifications", true)) {
            for (AchievementService.Achievement achievement : unlocked) {
                player.sendMessage("§6✦ §eAchievement Unlocked §8» §f" + achievement.title()
                        + " §8(§7" + achievement.category() + "§8)");
            }
            if (config.config().getBoolean("gui.sounds.enabled", true)) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.65f, 1.2f);
            }
        }
        return unlocked;
    }

    public void reloadAchievements() {
        achievements.reload();
        for (PlayerJobs data : cache.values()) {
            refreshAchievements(data, Bukkit.getPlayer(data.uuid()), false);
        }
    }

    public AchievementService achievementService() { return achievements; }

    public double warriorMultiplier(UUID killer,UUID victim){
        double result=antiAbuse.warriorMultiplier(killer,victim,System.currentTimeMillis());
        if(result>0)plugin.getDatabase().recordPvpKill(killer,victim,System.currentTimeMillis());
        return result;
    }

    public CompletableFuture<List<Database.LeaderboardEntry>> leaderboard(JobType job,int limit){
        long ttl=config.config().getLong("leaderboards.cache-seconds",60)*1000L;
        CachedBoard cached=leaderboardCache.get(job);
        long now=System.currentTimeMillis();
        if(cached!=null&&now-cached.loadedAt<ttl)return CompletableFuture.completedFuture(cached.entries.stream().limit(limit).toList());
        return plugin.getDatabase().leaderboard(job,Math.max(limit,12)).thenApply(entries->{leaderboardCache.put(job,new CachedBoard(now,entries));return entries.stream().limit(limit).toList();});
    }

    public void invalidateLeaderboards(){leaderboardCache.clear();}
    private record CachedBoard(long loadedAt,List<Database.LeaderboardEntry> entries){}
}